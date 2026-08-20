# Recognition Bounded Concurrency Requirements

## Goal

Allow a second attention candidate to reach `/recognize` while a previous request is waiting for a slow fallback, without changing the Backend contract or recognition policy.

## Requirements

1. At most two `/recognize` requests may be active at once.
2. A candidate is not dropped merely because one request is active; it is dropped when both slots are occupied or the existing 800 ms cooldown is active.
3. Concurrent requests for the same non-null `trackingId` are suppressed. No synthetic identity is introduced when `trackingId` is null.
4. Ending a session cancels every recognition request registered for that session.
5. A concurrency slot and tracking reservation are released after success, failure, or cancellation.
6. Stale results and duplicate `productId` values must not add Product Cards.
7. The Backend API, providers, thresholds, attention/dwell policy, cooldown duration, detection/tracking/crop pipeline, and UI remain unchanged.

