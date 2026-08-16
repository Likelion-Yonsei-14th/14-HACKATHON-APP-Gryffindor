# Implementation Plan: On-Device Object Detection (A2)

## Overview

Integrate on-device object detection into the existing A1 camera pipeline. The pipeline consumes `CameraFrame` from `CameraFrameProvider.frames`, performs frame sampling, format conversion, and TFLite inference, then exposes SDK-independent `DetectionFrameResult` through a domain boundary.

**Hackathon-first strategy:** Tasks are ordered to reveal real Gen2 risks earliest. Format verification comes first, followed by the minimal detection boundary, then progressive integration toward sustained real-device operation.

**Critical path:**

```text
Gen2 CameraFrame format discovery (Task 1)
→ Detection model + domain boundary (Task 2)
→ Controlled input validation (Task 3)
→ First real Gen2 detection (Task 4)
→ Backpressure hardening (Task 5)
→ Metrics + long-duration verification (Task 6)
→ Full acceptance (Task 7)
```

---

## Tasks

* [ ] 1. Real Gen2 CameraFrame Format/Codec Verification

  * [x] 1.1 Implement FormatVerifier diagnostic tool (code complete — awaiting real Gen2 verification)

    * Create `data/detection/FormatVerifier.kt`

    * On the first `CameraFrame` received, log through the `DetectionDiag` tag:

      * `isCompressed`
      * `width`, `height`
      * `data.size`
      * bytes-per-pixel ratio: `data.size / (width * height).toFloat()`
      * approximate row-size diagnostic: `data.size / height`

    * If `isCompressed = false`:

      * Compare the byte length against candidate raw format families.
      * If `data.size == width * height * 1.5`, classify only as a `YUV420-family candidate`.
      * Do NOT claim NV21, NV12, or another YUV420 layout from byte length alone.
      * If `data.size == width * height * 4`, record RGBA as a candidate.
      * If multiple formats remain possible, use the minimum additional real-device probe or visual conversion test required to determine the actual layout.

    * If `isCompressed = true`:

      * Log the first bytes as a hex signature for diagnostics.
      * Inspect available metadata and byte patterns for possible JPEG, PNG, HEVC, H.264, or other formats.
      * Treat signature inspection as a diagnostic hint only; do NOT claim a codec solely from an ambiguous byte prefix.

    * If the format cannot be identified, label it `UNKNOWN`.

    * Emit an internal `FormatDiagnostic` record containing:

      * format label / candidate family
      * width
      * height
      * byte length
      * bytes-per-pixel ratio
      * verification state

    * Execute all diagnostics outside the Main thread.

    * Do NOT lock in NV21 or another format assumption.

    * Do NOT modify `CameraFrame`, `CameraFrameProvider`, or `data/meta/`.

    * **Key files:**

      * `app/src/main/java/.../data/detection/FormatVerifier.kt` (NEW)
      * `app/src/main/java/.../data/detection/FormatDiagnostic.kt` (NEW)

    * **Done when:**

      * The real Gen2 stream produces a diagnostic record.
      * The actual format/codec is either identified or explicitly recorded as unresolved.
      * No guessed raw layout is treated as verified.

    * **Verification:**

      ```bash
      ./gradlew clean assembleDebug
      adb logcat -s DetectionDiag
      ```

    * *Requirements: 1.1–1.7*

---

* [x] 2. Object Detection Dependency + Model + Domain Boundary

  * [x] 2.1 Add TFLite dependencies and model asset

    * Add the required TensorFlow Lite dependencies to `app/build.gradle.kts`.

    * Add SSD MobileNet V2 COCO `.tflite` model to:

      ```text
      app/src/main/assets/ssd_mobilenet_v2.tflite
      ```

    * Keep the model local to the app; no runtime model download is required.

    * **Key files:**

      * `app/build.gradle.kts` (MODIFIED)
      * `app/src/main/assets/ssd_mobilenet_v2.tflite` (NEW)

    * **Done when:**

      ```bash
      ./gradlew clean assembleDebug
      ```

      succeeds with the model asset and TFLite dependencies present.

    * *Requirements: 4.1, 4.3, 4.8, 10.10*

  * [x] 2.2 Create domain detection models and interface

    * Create `DetectionResult` with:

      * normalized `left`
      * normalized `top`
      * normalized `right`
      * normalized `bottom`
      * `label`
      * `confidence`

    * Create `DetectionFrameResult` with:

      * `frameTimestampUs`
      * `detections: List<DetectionResult>`

    * Create `DetectionResultProvider` with:

      * `Flow<DetectionFrameResult>`
      * SDK-independent pipeline state if required by the existing architecture

    * No TensorFlow Lite imports may appear in the domain layer.

    * **Key files:**

      * `domain/model/DetectionResult.kt` (NEW)
      * `domain/model/DetectionFrameResult.kt` (NEW)
      * `domain/detection/DetectionResultProvider.kt` (NEW)

    * **Done when:**

      * Domain models compile.
      * Domain contains no ML SDK dependency.

    * *Requirements: 5.1–5.6, 6.1–6.7, 7.4–7.5*

  * [x] 2.3 Create TFLiteDetectorAdapter

    * Create `data/detection/TFLiteDetectorAdapter.kt`.

    * Responsibilities:

      * load the `.tflite` model
      * create and reuse one TFLite interpreter
      * accept detector-compatible image input
      * run inference
      * map model outputs to `DetectionResult`
      * close interpreter resources safely

    * Use CPU inference first.

    * Do NOT add GPU or NNAPI optimization unless real-device measurement later shows it is needed.

    * All TensorFlow Lite imports remain inside `data/detection/`.

    * **Key files:**

      * `data/detection/TFLiteDetectorAdapter.kt` (NEW)

    * **Done when:**

      * Adapter initializes successfully.
      * Model loads without runtime failure.

    * *Requirements: 4.1–4.5, 4.8–4.9, 5.1–5.5*

  * [x] 2.4 Add detection configuration

    * Extend the existing configuration source with detection parameters:

      * frame sampling interval
      * confidence threshold
      * maximum detections per frame
      * inference timeout
      * metrics window

    * A provisional format assumption may remain represented in configuration only if required by existing requirements, but it MUST NOT be treated as verified during real Gen2 format discovery.

    * Do NOT wire `DetectionPipeline` into `AppContainer` yet.

    * `DetectionPipeline` does not exist until Task 4.

    * **Key files:**

      * `core/config/AppConfig.kt` (MODIFIED)

    * **Done when:**

      ```bash
      ./gradlew clean assembleDebug
      ```

      succeeds.

    * *Requirements: 10.2, 11.3, 11.4*

---

* [ ] 3. Controlled Input Detection Test

  * [ ] 3.1 Verify TFLiteDetectorAdapter with controlled image input (test code ready — awaiting device run)

    * Create an instrumented test or lightweight developer test.

    * Load the TFLite model.

    * Provide a known detector-compatible Bitmap or test resource.

    * Execute inference.

    * Verify:

      * no crash
      * output list is produced
      * bounding boxes remain in normalized range
      * labels are non-empty
      * confidence values are in `[0.0, 1.0]`

    * If possible, use an actual image containing a COCO-recognizable object instead of relying only on a solid-color synthetic bitmap.

    * Verify adapter release does not crash.

    * **Key files:**

      * `androidTest/.../TFLiteDetectorAdapterTest.kt` (NEW or equivalent)

    * **Done when:**

      * Model initialization succeeds.
      * At least one controlled inference cycle completes correctly.

    * *Requirements: 4.1, 4.2, 4.4, 4.10, 11.1, 11.2*

  * [x] 3.2 Verify ML SDK isolation

    * Run an architecture grep:

      ```bash
      grep -r "org.tensorflow" app/src/main/java/ --include="*.kt" | grep -v "data/detection/"
      ```

    * Expected result: no matches.

    * Verify domain and feature layers contain no ML SDK imports.

    * **Done when:**

      * ML SDK isolation check is clean.

    * *Requirements: 5.1–5.6, 10.11*

---

* [x] 4. Gen2 CameraFrame → Conversion → Detection First Success

  * [x] 4.1 Implement FrameConverter from actual Task 1 findings

    * Create `data/detection/FrameConverter.kt`.

    * Implemented YUV420 conversion path based on real Gen2 findings:

      ```text
      Gen2 504x896, isCompressed=false, YUV420_FAMILY
      FrameConverter probe=NV21
      ```

    * Conversion path: NV21 → YuvImage → JPEG → Bitmap → 300×300 resize

    * **Verified on real Gen2:** format probe succeeds, conversion produces valid Bitmaps.

    * *Requirements: 3.1–3.8*

  * [x] 4.2 Implement realtime DetectionPipeline and first live Gen2 detection

    * Created `FrameSampler`, `DetectionPipeline` implementing `DetectionResultProvider`.

    * Pipeline:

      ```text
      CameraFrameProvider.frames
      → Flow.conflate() (latest-frame-wins)
      → FrameSampler (time-gate)
      → FormatVerifier (one-shot)
      → FrameConverter (YUV→Bitmap)
      → TFLiteDetectorAdapter (inference + timeout)
      → confidence filter + max count
      → DetectionFrameResult emission
      ```

    * **Verified on real Gen2:** live detection results with bbox, label, confidence, frameTimestampUs.

    * *Requirements: 2.1–2.9, 4.1–4.7, 7.1–7.7, 8.1–8.7, 11.1–11.5*

---

* [x] 5. Checkpoint — First Gen2 Detection Confirmed

  * **Confirmed on real Gen2 hardware:**

    * Live Gen2 camera frame produces repeated generic object detections.
    * Valid bbox coordinates (normalized 0–1 range).
    * Valid labels (COCO classes: person, bottle, etc.).
    * Valid confidence values.
    * Correct source frame timestamps.
    * Shopping stop → detection stops.
    * Shopping restart → detection resumes with new camera stream.

  * Build passes:

    ```bash
    ./gradlew clean assembleDebug
    ```

  * *Requirements: 1–8*

---

* [ ] 6. Realtime Sampling, Backpressure, and Metrics

  * [x] 6.1 Harden sustained realtime processing

    * Validated operation against real Gen2 frame rate (~7–15 FPS input).

    * Sampling interval adjusted to 200ms (~5 FPS target detection rate).

    * Backpressure structure verified:

      ```text
      Flow.conflate() — latest-frame-wins, zero backlog ✓
      FrameSampler — 200ms time-gate (configurable) ✓
      Sequential .collect{} — single inference at a time ✓
      MutableSharedFlow(buffer=1, DROP_OLDEST) — bounded output ✓
      Dispatchers.Default — never Main ✓
      ```

    * Dropped-frame accounting added via DetectionMetrics.

    * **Key files:**

      * `FrameSampler.kt` (unchanged — interval from AppConfig)
      * `DetectionPipeline.kt` (MODIFIED — metrics integration)
      * `AppConfig.kt` (MODIFIED — DETECTION_FRAME_INTERVAL_MS=200L)

    * **Awaiting:** Real Gen2 60-second sustained operation measurement.

    * *Requirements: 2.2–2.7, 7.6, 8.3*

  * [x] 6.2 Add lightweight DetectionMetrics

    * Created `DetectionMetrics.kt` with:

      * frames received / sampled / processed / dropped counters
      * conversion / inference / total latency tracking (rolling buffer)
      * effective detection FPS calculation (time-window based)
      * p95 total latency (percentile from ring buffer)
      * 5-second summary Logcat output with per-window delta + cumulative stats
      * Thread-safe: AtomicLong counters + synchronized latency buffers

    * Format:

      ```text
      DetectionMetrics Summary
      fps=4.8
      received=75
      sampled=25
      processed=24
      dropped=1
      avgConversionMs=...
      avgInferenceMs=...
      avgTotalMs=...
      p95TotalMs=...
      cumulative: recv=... proc=... drop=...
      ```

    * **Key files:**

      * `data/detection/DetectionMetrics.kt` (NEW)
      * `DetectionPipeline.kt` (MODIFIED)
      * `AppConfig.kt` (MODIFIED — added DETECTION_METRICS_SUMMARY_INTERVAL_MS, DETECTION_METRICS_MAX_SAMPLES)

    * **Awaiting:** Real Gen2 Logcat verification of sustained metrics output.

    * *Requirements: 9.1–9.8*

---

* [ ] 7. Final Real Gen2 Acceptance

  * [ ] 7.1 Execute complete A2 acceptance test

    * Run the actual Gen2 pipeline continuously for at least 60 seconds.

    * Use generic objects that the COCO detector can reasonably recognize, such as:

      * person
      * bottle
      * chair
      * backpack / handbag

    * Verify:

      ```text
      Gen2 Camera STREAMING
      → CameraFrame
      → sampling
      → conversion
      → object detection
      → DetectionFrameResult
      ```

    * Detection requirements:

      * repeated detections occur
      * bbox coordinates are valid
      * labels are non-empty
      * confidence values are valid
      * empty detection frames may still emit `DetectionFrameResult`

    * Realtime requirements:

      * effective detection FPS measured on real device
      * processing latency measured on real device
      * no Main Thread inference
      * no ANR
      * no unbounded backlog
      * latest frames remain prioritized

    * Initial performance target:

      ```text
      effective detection FPS >= 3
      p95 total processing latency < 400 ms
      ```

    * Do NOT mark performance PASS without actual measurements.

    * Lifecycle requirements:

      ```text
      Shopping start
      → Camera STREAMING
      → detection starts

      Shopping stop
      → detection stops
      → A1 camera cleanup remains correct

      Shopping start again
      → new camera stream
      → detection resumes
      ```

    * Run repeated start/stop cycles sufficient to demonstrate stable restart behavior.

    * Architecture verification:

      ```bash
      grep -r "org.tensorflow" app/src/main/java/ --include="*.kt" | grep -v "data/detection/"
      ```

      Expected: no output.

    * Verify:

      * `data/meta/` unchanged
      * `CameraFrame.kt` unchanged
      * `CameraFrameProvider.kt` unchanged
      * existing app navigation remains functional

    * Final build:

      ```bash
      ./gradlew clean assembleDebug
      ```

    * **Done when:**

      * Real Gen2 acceptance path passes.
      * Actual FPS and latency values are documented.
      * Architecture boundary remains clean.
      * Existing A1 camera lifecycle remains intact.

    * *Requirements: 1–11*

---

## Notes

* **A0/A1 preservation:** `data/meta/`, `CameraFrame`, and `CameraFrameProvider` MUST remain unchanged unless an unexpected blocker is discovered and explicitly documented.
* **Format discovery first:** Task 1 must resolve the actual Gen2 frame format or codec before Task 4 conversion is finalized.
* **No byte-size overclaim:** `W * H * 1.5` identifies only a possible YUV420-family layout, not specifically NV21 or NV12.
* **Compressed frames:** `isCompressed = true` does NOT imply JPEG or PNG. HEVC/H.264 or another video codec must be handled according to real Gen2 findings.
* **Real Gen2 required:** Mock Device success does not satisfy final A2 acceptance.
* **No premature optimization:** GPU delegate, NNAPI, model quantization, complex buffer pools, and custom executors are deferred unless actual measurements show they are required.
* **Out of scope:** Tracking, Center/Occupancy/Dwell, Attention Trigger, candidate crop generation, Backend `/recognize`, OpenAI product recognition, and productId resolution.
* **Hackathon priority:** Reach the first real Gen2 detection as quickly as possible, then harden only the parts proven necessary by real measurements.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["2.3", "2.4"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["4.1", "4.2"] },
    { "id": 4, "tasks": ["5"] },
    { "id": 5, "tasks": ["6.1", "6.2"] },
    { "id": 6, "tasks": ["7.1"] }
  ]
}
```
