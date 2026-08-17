# Design — Faster Attention UX Experiment

Use one continuous dwell state per tracking ID and derive two evidence paths:

```text
FAST_OCCUPANCY = center AND occupancy >= 0.22 AND stable >= 200ms
DWELL          = center AND occupancy >= 0.12 AND dwell >= 600ms
TRIGGER        = FAST_OCCUPANCY OR DWELL
```

The unchanged moderate occupancy floor prevents tiny background boxes from building
dwell. The fast path requires two sampled observations at the 5 FPS target instead of
accepting a single noisy box.

At recognition entry, remove only the global post-response cooldown. Keep the two
non-blocking semaphore permits, per-tracking-ID in-flight reservation, pipeline
suppression, session cancellation, stale-result guard, and product-ID dedup.

No detector, tracker, crop, network DTO, API, repository, or UI change is included.
