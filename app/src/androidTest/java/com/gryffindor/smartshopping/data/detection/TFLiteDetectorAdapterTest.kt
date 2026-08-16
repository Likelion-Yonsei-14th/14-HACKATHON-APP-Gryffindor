package com.gryffindor.smartshopping.data.detection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for TFLiteDetectorAdapter.
 *
 * Verifies:
 * - Model loads successfully from assets
 * - Inference runs without crash
 * - Output detections have valid normalized coordinates
 * - Labels are non-empty strings
 * - Confidence values are in [0.0, 1.0]
 * - Release does not crash
 *
 * Uses a test image (test_detection.jpg) containing COCO-recognizable object shapes.
 */
@RunWith(AndroidJUnit4::class)
class TFLiteDetectorAdapterTest {

    private lateinit var adapter: TFLiteDetectorAdapter

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        adapter = TFLiteDetectorAdapter(context)
    }

    @After
    fun tearDown() {
        adapter.release()
    }

    @Test
    fun initialization_succeeds() {
        val result = adapter.initialize()
        assertTrue("Model initialization should succeed", result)
        assertTrue("Adapter should report initialized", adapter.isInitialized)
    }

    @Test
    fun detect_withTestImage_producesValidOutput() {
        // Initialize model
        val initResult = adapter.initialize()
        assertTrue("Model should initialize", initResult)

        // Load test image from androidTest assets
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open("test_detection.jpg").use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
        assertNotNull("Test image should decode", bitmap)

        // Run inference
        val detections = adapter.detect(bitmap!!)

        // Verify output structure (may be empty for synthetic image, but should not crash)
        assertNotNull("Detections list should not be null", detections)

        // Log results for diagnostic purposes
        println("=== Controlled Detection Test Results ===")
        println("Total detections: ${detections.size}")
        for ((i, det) in detections.withIndex()) {
            println("  [$i] label=${det.label} conf=${"%.3f".format(det.confidence)} " +
                "bbox=(${det.left}, ${det.top}, ${det.right}, ${det.bottom})")
        }
        println("=========================================")

        // Verify all detections have valid fields
        for (det in detections) {
            // Bounding box: normalized [0, 1]
            assertTrue("left >= 0: ${det.left}", det.left >= 0f)
            assertTrue("top >= 0: ${det.top}", det.top >= 0f)
            assertTrue("right <= 1: ${det.right}", det.right <= 1f)
            assertTrue("bottom <= 1: ${det.bottom}", det.bottom <= 1f)
            assertTrue("left <= right: ${det.left} <= ${det.right}", det.left <= det.right)
            assertTrue("top <= bottom: ${det.top} <= ${det.bottom}", det.top <= det.bottom)

            // Label: non-empty
            assertTrue("label should not be empty", det.label.isNotEmpty())

            // Confidence: [0, 1]
            assertTrue("confidence >= 0: ${det.confidence}", det.confidence >= 0f)
            assertTrue("confidence <= 1: ${det.confidence}", det.confidence <= 1f)
        }

        bitmap.recycle()
    }

    @Test
    fun detect_withSolidColorBitmap_doesNotCrash() {
        val initResult = adapter.initialize()
        assertTrue("Model should initialize", initResult)

        // Create a plain gray bitmap (unlikely to produce detections, but must not crash)
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.GRAY)
        }

        val detections = adapter.detect(bitmap)
        assertNotNull("Detections should not be null", detections)

        // Verify no invalid outputs even for a featureless image
        for (det in detections) {
            assertTrue("confidence in range", det.confidence in 0f..1f)
            assertTrue("label non-empty", det.label.isNotEmpty())
        }

        bitmap.recycle()
    }

    @Test
    fun detect_beforeInitialization_returnsEmpty() {
        // Do NOT call initialize
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val detections = adapter.detect(bitmap)
        assertTrue("Should return empty before init", detections.isEmpty())
        bitmap.recycle()
    }

    @Test
    fun release_isIdempotent() {
        adapter.initialize()
        adapter.release()
        adapter.release() // second call should not crash
        assertFalse("Should not be initialized after release", adapter.isInitialized)
    }

    @Test
    fun detect_afterRelease_returnsEmpty() {
        adapter.initialize()
        adapter.release()

        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val detections = adapter.detect(bitmap)
        assertTrue("Should return empty after release", detections.isEmpty())
        bitmap.recycle()
    }
}
