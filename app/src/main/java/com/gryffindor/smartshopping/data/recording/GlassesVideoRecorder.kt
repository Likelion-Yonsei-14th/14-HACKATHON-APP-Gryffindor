package com.gryffindor.smartshopping.data.recording

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.gryffindor.smartshopping.domain.model.CameraFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Records DAT camera frames (packed I420) to an MP4 file using MediaCodec + MediaMuxer.
 *
 * Design:
 * - A bounded [Channel] accepts frames without blocking the caller.
 * - A dedicated coroutine drains the channel, converts I420→NV12, and feeds MediaCodec.
 * - MediaMuxer writes the encoded H.264 stream to a temp file, then moves it to MediaStore.
 * - All failures are logged and swallowed — recording never blocks shopping/recognition.
 */
class GlassesVideoRecorder(private val context: Context) {

    companion object {
        private const val TAG = "GlassesVideoRecorder"

        /** Bounded channel capacity — small to avoid memory bloat. DROP_OLDEST on overflow. */
        private const val FRAME_CHANNEL_CAPACITY = 3

        /** H.264 bitrate — conservative for 504x896@15fps. ~2 Mbps. */
        private const val BITRATE = 2_000_000

        /** I-frame interval in seconds. */
        private const val I_FRAME_INTERVAL = 2

        /** MediaCodec input buffer timeout in microseconds. */
        private const val CODEC_TIMEOUT_US = 10_000L

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    // --- State ---

    enum class State { IDLE, RECORDING, STOPPING }

    @Volatile
    private var state: State = State.IDLE

    private var encoderScope: CoroutineScope? = null
    private var encoderJob: Job? = null

    private var frameChannel: Channel<CameraFrame>? = null

    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex: Int = -1
    private var muxerStarted: Boolean = false

    private var tempFile: File? = null
    private var sessionId: String? = null

    private val frameCount = AtomicLong(0)
    private val dropCount = AtomicInteger(0)
    private var startTimeMs: Long = 0L
    private var firstFrameTimestampUs: Long = -1L

    // --- Public API ---

    /**
     * Start recording. Safe to call multiple times (no-op if already recording).
     * Never throws — logs errors internally.
     */
    fun startRecording(sessionId: String) {
        if (state != State.IDLE) {
            Log.w(TAG, "startRecording called while state=$state — ignoring")
            return
        }

        this.sessionId = sessionId
        frameCount.set(0)
        dropCount.set(0)
        startTimeMs = System.currentTimeMillis()
        firstFrameTimestampUs = -1L

        frameChannel = Channel(FRAME_CHANNEL_CAPACITY, BufferOverflow.DROP_OLDEST)
        state = State.RECORDING

        Log.i(TAG, "recording_start sessionId=$sessionId")
    }

    /**
     * Enqueue a frame for recording. Non-blocking — drops if channel is full.
     * Safe to call from the camera collector thread.
     */
    fun tryEnqueueFrame(frame: CameraFrame) {
        if (state != State.RECORDING) return

        // Skip compressed frames — we need raw I420 for encoding
        if (frame.isCompressed) return

        val result = frameChannel?.trySend(frame)
        if (result != null && !result.isSuccess) {
            val count = dropCount.incrementAndGet()
            if (count == 1 || count % 30 == 0) {
                Log.w(TAG, "recording_frame_drop count=$count")
            }
        }
    }

    /**
     * Stop recording, finalize the MP4, and save to MediaStore.
     * Non-blocking from the caller's perspective — finalization runs in background.
     * Safe to call multiple times or without a prior start.
     *
     * @return Job that completes when finalization is done, or null if not recording.
     */
    fun stopRecording(): Job? {
        if (state != State.RECORDING) {
            Log.w(TAG, "stopRecording called while state=$state — ignoring")
            return null
        }
        state = State.STOPPING

        // Close the channel to signal the encoder coroutine to drain remaining and stop.
        frameChannel?.close()

        // The encoder job will handle finalization. Return it so callers can optionally await.
        val job = encoderJob
        return job
    }

    /**
     * Cancel recording without saving. Releases all resources immediately.
     */
    fun cancelRecording() {
        Log.i(TAG, "recording_cancelled sessionId=$sessionId")
        state = State.STOPPING
        frameChannel?.close()
        encoderScope?.cancel()
        releaseEncoder()
        deleteTempFile()
        state = State.IDLE
    }

    // --- Internal: Encoder pipeline ---

    /**
     * Called when the first frame arrives — sets up MediaCodec and MediaMuxer
     * with the actual frame dimensions. Runs on the encoder coroutine.
     */
    private fun initializeEncoder(width: Int, height: Int) {
        try {
            // Create temp file in app cache (will be moved to MediaStore on success)
            tempFile = File(context.cacheDir, "recording_${sessionId}_${System.currentTimeMillis()}.mp4")

            // Configure MediaCodec encoder
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, 15)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE).also { encoder ->
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()
            }

            // Configure MediaMuxer
            muxer = MediaMuxer(tempFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            trackIndex = -1
            muxerStarted = false

            Log.i(TAG, "recording_encoder_initialized width=$width height=$height")
        } catch (e: Exception) {
            Log.e(TAG, "recording_failed reason=encoder_init_error", e)
            releaseEncoder()
        }
    }

    /**
     * Feed a single I420 frame to the encoder after converting to NV12.
     */
    private fun encodeFrame(frame: CameraFrame) {
        val encoder = codec ?: return

        // Convert packed I420 → NV12 (what MediaCodec expects for COLOR_FormatYUV420SemiPlanar)
        val nv12Data = convertI420ToNv12(frame.data, frame.width, frame.height) ?: run {
            Log.w(TAG, "Frame conversion failed: size=${frame.data.size} expected=${frame.width * frame.height * 3 / 2}")
            return
        }

        // Compute presentation time relative to first frame
        val presentationTimeUs = if (firstFrameTimestampUs < 0) {
            firstFrameTimestampUs = frame.timestampUs
            0L
        } else {
            frame.timestampUs - firstFrameTimestampUs
        }

        // Dequeue input buffer and fill
        val inputIndex = encoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()
            val dataSize = minOf(nv12Data.size, inputBuffer.remaining())
            inputBuffer.put(nv12Data, 0, dataSize)
            encoder.queueInputBuffer(inputIndex, 0, dataSize, presentationTimeUs, 0)
        }

        // Drain output
        drainEncoder(false)
    }

    /**
     * Signal end-of-stream and drain all remaining output.
     */
    private fun signalEndOfStream() {
        val encoder = codec ?: return
        val inputIndex = encoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex >= 0) {
            encoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainEncoder(true)
    }

    /**
     * Drain encoder output buffers and write to muxer.
     */
    private fun drainEncoder(endOfStream: Boolean) {
        val encoder = codec ?: return
        val mux = muxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                    // If EOS, keep trying briefly
                    break
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) {
                        Log.w(TAG, "Output format changed after muxer started")
                        break
                    }
                    val newFormat = encoder.outputFormat
                    trackIndex = mux.addTrack(newFormat)
                    mux.start()
                    muxerStarted = true
                    Log.i(TAG, "recording_muxer_started format=$newFormat")
                }
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // Codec config data — not actual frame data
                        encoder.releaseOutputBuffer(outputIndex, false)
                        continue
                    }

                    if (muxerStarted && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mux.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }

                    encoder.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
                else -> break
            }
        }
    }

    /**
     * Release MediaCodec and MediaMuxer resources.
     */
    private fun releaseEncoder() {
        try {
            codec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping codec", e)
        }
        try {
            codec?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing codec", e)
        }
        codec = null

        try {
            if (muxerStarted) {
                muxer?.stop()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping muxer", e)
        }
        try {
            muxer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing muxer", e)
        }
        muxer = null
        muxerStarted = false
        trackIndex = -1
    }

    /**
     * Move the temp file to MediaStore Movies/Looket.
     */
    private fun saveToMediaStore(): Uri? {
        val file = tempFile ?: return null
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Temp file empty or missing — nothing to save")
            deleteTempFile()
            return null
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startTimeMs))
        val displayName = "looket_session_${sessionId}_$timestamp.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Looket")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            Log.e(TAG, "recording_failed reason=mediastore_insert_failed")
            deleteTempFile()
            return null
        }

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 8192)
                }
            }

            // Mark as complete
            val updateValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, updateValues, null, null)

            val durationMs = System.currentTimeMillis() - startTimeMs
            Log.i(TAG, "recording_saved uri=$uri durationMs=$durationMs frames=${frameCount.get()} drops=${dropCount.get()} file=$displayName")

            deleteTempFile()
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "recording_failed reason=mediastore_write_error", e)
            // Clean up the pending entry
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            deleteTempFile()
            return null
        }
    }

    private fun deleteTempFile() {
        try {
            tempFile?.delete()
        } catch (_: Exception) {}
        tempFile = null
    }

    /**
     * Launch the encoder coroutine that processes frames from the channel.
     * Called internally after startRecording — the first frame triggers encoder init.
     */
    internal fun launchEncoderLoop() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        encoderScope = scope

        encoderJob = scope.launch {
            var encoderReady = false

            try {
                val channel = frameChannel ?: return@launch

                for (frame in channel) {
                    if (!isActive) break

                    // Initialize encoder on first frame (we now know dimensions)
                    if (!encoderReady) {
                        initializeEncoder(frame.width, frame.height)
                        encoderReady = codec != null
                        if (!encoderReady) {
                            Log.e(TAG, "recording_failed reason=encoder_not_ready")
                            break
                        }
                        Log.i(TAG, "recording_first_frame width=${frame.width} height=${frame.height}")
                    }

                    encodeFrame(frame)
                    frameCount.incrementAndGet()
                }

                // Channel closed — signal EOS and finalize
                if (encoderReady) {
                    signalEndOfStream()
                }
            } catch (e: Exception) {
                Log.e(TAG, "recording_failed reason=encoder_loop_error", e)
            } finally {
                releaseEncoder()
                if (encoderReady && frameCount.get() > 0) {
                    saveToMediaStore()
                } else {
                    deleteTempFile()
                }
                state = State.IDLE
                encoderScope = null
                encoderJob = null
            }
        }
    }

    // --- I420 → NV12 conversion ---

    /**
     * Convert packed I420 (YYYYYYYY UUUU VVVV) to NV12 (YYYYYYYY UVUVUVUV).
     *
     * NV12 is the format expected by Android MediaCodec for COLOR_FormatYUV420SemiPlanar.
     * I420: Y plane + U plane + V plane (planar)
     * NV12: Y plane + interleaved UV chroma plane (semi-planar)
     *
     * Important: NOT NV21 (which is VU interleaved). MediaCodec needs UV order.
     */
    private fun convertI420ToNv12(data: ByteArray, width: Int, height: Int): ByteArray? {
        val expectedSize = width * height * 3 / 2
        if (data.size != expectedSize || width <= 0 || height <= 0 || width % 2 != 0 || height % 2 != 0) {
            return null
        }

        val output = ByteArray(expectedSize)
        val yPlaneSize = width * height
        val chromaPlaneSize = yPlaneSize / 4

        // Copy Y plane as-is
        data.copyInto(output, destinationOffset = 0, startIndex = 0, endIndex = yPlaneSize)

        // Interleave U and V into UV pairs (NV12 order: U first, then V)
        val uPlaneOffset = yPlaneSize
        val vPlaneOffset = yPlaneSize + chromaPlaneSize
        var destOffset = yPlaneSize
        for (i in 0 until chromaPlaneSize) {
            output[destOffset++] = data[uPlaneOffset + i]
            output[destOffset++] = data[vPlaneOffset + i]
        }

        return output
    }
}
