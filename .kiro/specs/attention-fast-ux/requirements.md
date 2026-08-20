# Requirements — Faster Attention UX Experiment

## Hypothesis

An evidence-tiered attention gate, followed by immediate admission of independent
tracks, can reduce look-to-dispatch latency without broadly lowering every guard.

## Requirements

1. A high-occupancy bbox in the center ROI may trigger after a short multi-frame
   stability period; one frame alone must not trigger.
2. A moderate-occupancy bbox must still satisfy center ROI and continuous dwell.
3. Production dwell is reduced only from 800ms to 600ms; center ROI and the moderate
   occupancy floor remain unchanged.
4. Trigger metadata must distinguish `OCCUPANCY`, `DWELL`, and
   `OCCUPANCY_AND_DWELL` according to the evidence present at trigger time.
5. A completed request must not impose a global cooldown on a different tracking ID.
6. Bounded recognition concurrency remains exactly 2; a third concurrent request is
   dropped, the same tracking ID cannot run concurrently, session end cancels all
   in-flight work, and product-card dedup remains unchanged.
7. `AttentionTiming` must retain track → trigger → candidate → dispatch timing logs.
8. Backend/API/UI contracts and OpenCLIP threshold remain unchanged.

## Success Evidence

- Unit tests demonstrate ~200ms high-occupancy trigger and 600ms dwell trigger.
- A one-frame high-occupancy detection and small background detections do not trigger.
- Dispatch tests prove a freed slot accepts an independent candidate immediately.
- The full `testDebugUnitTest` suite passes.
