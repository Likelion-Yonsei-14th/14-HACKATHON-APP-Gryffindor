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

  * [ ] 1.1 Implement FormatVerifier diagnostic tool

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

* [ ] 2. Object Detection Dependency + Model + Domain Boundary

  * [ ] 2.1 Add TFLite dependencies and model asset

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

  * [ ] 2.2 Create domain detection models and interface

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

  * [ ] 2.3 Create TFLiteDetectorAdapter

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

  * [ ] 2.4 Add detection configuration

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

  * [ ] 3.1 Verify TFLiteDetectorAdapter with controlled image input

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

  * [ ] 3.2 Verify ML SDK isolation

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

* [ ] 4. Gen2 CameraFrame → Conversion → Detection First Success

  * [ ] 4.1 Implement FrameConverter from actual Task 1 findings

    * Create `data/detection/FrameConverter.kt`.

    * Implement only the conversion path required by the actual format or codec discovered in Task 1.

    * Supported decision paths:

      ```text
      Raw YUV420-family
      → determine actual verified layout
      → minimum raw conversion path
      → detector-compatible Bitmap

      Raw RGBA
      → Bitmap creation
      → detector input resize

      JPEG / PNG / BitmapFactory-compatible image
      → BitmapFactory decode
      → detector input resize

      HEVC / H.264 / other video codec
      → minimum codec-specific decode path
      → detector-compatible image

      UNKNOWN
      → discard frame
      → log diagnostic
      ```

    * Do NOT assume:

      ```text
      isCompressed = true
      → JPEG / PNG
      ```

    * Do NOT implement every possible conversion path in advance.

    * Implement only the minimum path required by actual Gen2 output.

    * If the real Gen2 format remains unresolved, stop here and resolve the format before finalizing the converter.

    * Execute conversion outside the Main thread.

    * Do NOT modify A1 camera types or `data/meta/`.

    * **Key files:**

      * `data/detection/FrameConverter.kt` (NEW)

    * **Done when:**

      * A real Gen2 `CameraFrame` can be converted into detector-compatible image input.

    * *Requirements: 3.1–3.8*

  * [ ] 4.2 Implement realtime DetectionPipeline and first live Gen2 detection

    * Create `FrameSampler`.

    * Create `DetectionPipeline` implementing `DetectionResultProvider`.

    * Pipeline:

      ```text
      CameraFrameProvider.frames
      → latest-frame sampling
      → format verification
      → FrameConverter
      → TFLiteDetectorAdapter
      → confidence filter
      → DetectionFrameResult
      ```

    * Use `Flow.conflate()` or equivalent latest-frame behavior.

    * Detector execution must be serialized.

    * Heavy work must run outside Main.

    * Output flow should use bounded latest-result delivery.

    * Tie detection execution to the existing camera `Streaming` lifecycle.

    * Keep lifecycle implementation minimal; do not introduce a new framework.

    * Apply confidence threshold and max result count.

    * Add the completed detection pipeline to the existing `AppContainer`.

    * Expose it through `DetectionResultProvider`.

    * **Key files:**

      * `data/detection/FrameSampler.kt` (NEW)
      * `data/detection/DetectionPipeline.kt` (NEW)
      * `app/AppContainer.kt` (MODIFIED)

    * **Done when:**

      * A live real Gen2 frame produces at least one valid `DetectionFrameResult`.
      * Logcat shows:

        ```text
        bbox
        label
        confidence
        frameTimestampUs
        ```

    * **Verification:**

      ```bash
      ./gradlew clean assembleDebug
      adb logcat
      ```

    * *Requirements: 2.1–2.9, 4.1–4.7, 7.1–7.7, 8.1–8.7, 11.1–11.5*

---

* [ ] 5. Checkpoint — First Gen2 Detection Confirmed

  * Confirm Tasks 1–4 on real Gen2.

  * A live Gen2 camera frame must have produced at least one generic object detection.

  * Verify:

    * valid bbox
    * valid label
    * valid confidence
    * correct source frame timestamp

  * Confirm:

    ```bash
    ./gradlew clean assembleDebug
    ```

    succeeds.

  * If the first real detection fails, fix only the blocking issue before proceeding to realtime hardening.

---

* [ ] 6. Realtime Sampling, Backpressure, and Metrics

  * [ ] 6.1 Harden sustained realtime processing

    * Validate operation against the real Gen2 frame rate.

    * Keep latest-frame-wins behavior.

    * Ensure no unbounded queue exists.

    * Verify only one detector inference runs at a time.

    * Add dropped-frame accounting.

    * Verify no heavy detection work occurs on Main.

    * Avoid extra buffering layers unless real measurements require them.

    * Initial target:

      ```text
      detection sampling ≈ 5 FPS
      ```

    * Adjust sampling interval only from real-device measurements.

    * **Key files:**

      * `FrameSampler.kt` (MODIFIED if needed)
      * `DetectionPipeline.kt` (MODIFIED if needed)

    * **Done when:**

      * Real Gen2 operates continuously for at least 60 seconds.
      * No ANR.
      * No increasing frame backlog.
      * Detection continues to reflect recent camera content.

    * *Requirements: 2.2–2.7, 7.6, 8.3*

  * [ ] 6.2 Add lightweight DetectionMetrics

    * Create `DetectionMetrics.kt`.

    * Record:

      * frames received by detection pipeline
      * frames sampled
      * frames processed
      * frames dropped
      * conversion duration
      * inference duration
      * total processing duration
      * effective detection FPS

    * Use Logcat only.

    * Do NOT add a monitoring framework.

    * **Key files:**

      * `data/detection/DetectionMetrics.kt` (NEW)
      * `DetectionPipeline.kt` (MODIFIED)

    * **Done when:**

      * Real Gen2 Logcat shows consistent performance measurements during sustained operation.

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
