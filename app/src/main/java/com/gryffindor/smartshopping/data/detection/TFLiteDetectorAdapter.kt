package com.gryffindor.smartshopping.data.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.gryffindor.smartshopping.domain.model.DetectionResult
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TFLite inference wrapper for SSD MobileNet V2 (COCO quantized).
 *
 * All TensorFlow Lite imports and SDK interactions are confined to this class.
 * The public API exposes only [DetectionResult] (domain model) — no TFLite types leak out.
 *
 * Model expects: 300x300 RGB uint8 input
 * Model outputs:
 *   - output 0: bounding boxes [1][10][4] (top, left, bottom, right) in [0,1]
 *   - output 1: class indices [1][10]
 *   - output 2: confidence scores [1][10]
 *   - output 3: number of detections [1]
 */
internal class TFLiteDetectorAdapter(private val context: Context) {

    companion object {
        private const val TAG = "TFLiteDetector"
        private const val MODEL_FILE = "ssd_mobilenet_v2.tflite"
        private const val LABELS_FILE = "coco_labels.txt"
        private const val INPUT_SIZE = 300
        private const val MAX_DETECTIONS = 10 // Model output limit for this quantized model
        private const val NUM_CHANNELS = 3
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputBuffer: ByteBuffer? = null

    /**
     * Load the TFLite model and label map from assets.
     * @return true if initialization succeeded, false otherwise.
     */
    fun initialize(): Boolean {
        return try {
            val modelBuffer = loadModelFile(MODEL_FILE)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            labels = loadLabels(LABELS_FILE)

            // Pre-allocate input buffer
            inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * NUM_CHANNELS).apply {
                order(ByteOrder.nativeOrder())
            }

            Log.d(TAG, "Model initialized: input=${INPUT_SIZE}x${INPUT_SIZE}, labels=${labels.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TFLite model", e)
            false
        }
    }

    /**
     * Run object detection on a Bitmap.
     * The bitmap will be resized to 300x300 internally if needed.
     *
     * @param bitmap Input image (any size, will be scaled to detector input)
     * @return List of detections with normalized coordinates, labels, and confidence
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interp = interpreter ?: return emptyList()
        val buffer = inputBuffer ?: return emptyList()

        try {
            // Resize bitmap to model input size
            val resized = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            }

            // Fill input buffer with RGB pixel data (uint8)
            buffer.rewind()
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            for (pixel in pixels) {
                buffer.put(((pixel shr 16) and 0xFF).toByte()) // R
                buffer.put(((pixel shr 8) and 0xFF).toByte())  // G
                buffer.put((pixel and 0xFF).toByte())           // B
            }

            // Clean up resized bitmap if we created a new one
            if (resized !== bitmap) {
                resized.recycle()
            }

            // Prepare output arrays
            // SSD MobileNet V1 quantized output format:
            // [0]: locations [1][MAX_DETECTIONS][4] - float
            // [1]: classes [1][MAX_DETECTIONS] - float
            // [2]: scores [1][MAX_DETECTIONS] - float
            // [3]: number of detections [1] - float
            val outputLocations = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
            val outputClasses = Array(1) { FloatArray(MAX_DETECTIONS) }
            val outputScores = Array(1) { FloatArray(MAX_DETECTIONS) }
            val numDetections = FloatArray(1)

            val outputMap = HashMap<Int, Any>()
            outputMap[0] = outputLocations
            outputMap[1] = outputClasses
            outputMap[2] = outputScores
            outputMap[3] = numDetections

            // Run inference
            buffer.rewind()
            interp.runForMultipleInputsOutputs(arrayOf(buffer), outputMap)

            // Map outputs to DetectionResult
            val count = numDetections[0].toInt().coerceIn(0, MAX_DETECTIONS)
            val results = mutableListOf<DetectionResult>()

            for (i in 0 until count) {
                val score = outputScores[0][i]
                val classIdx = outputClasses[0][i].toInt()

                // Get label from class index
                val label = if (classIdx in labels.indices) {
                    labels[classIdx]
                } else {
                    "unknown_$classIdx"
                }

                // Skip placeholder labels
                if (label == "???") continue

                // SSD MobileNet outputs: [top, left, bottom, right]
                val top = outputLocations[0][i][0].coerceIn(0f, 1f)
                val left = outputLocations[0][i][1].coerceIn(0f, 1f)
                val bottom = outputLocations[0][i][2].coerceIn(0f, 1f)
                val right = outputLocations[0][i][3].coerceIn(0f, 1f)

                results.add(
                    DetectionResult(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        label = label,
                        confidence = score.coerceIn(0f, 1f)
                    )
                )
            }

            return results
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            return emptyList()
        }
    }

    /**
     * Release model resources. Safe to call multiple times.
     */
    fun release() {
        interpreter?.close()
        interpreter = null
        inputBuffer = null
        Log.d(TAG, "Model resources released")
    }

    /** Whether the model is currently loaded and ready for inference. */
    val isInitialized: Boolean get() = interpreter != null

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(filename: String): List<String> {
        return context.assets.open(filename).bufferedReader().readLines()
    }
}
