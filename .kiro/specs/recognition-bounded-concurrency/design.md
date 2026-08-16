# Recognition Bounded Concurrency Design

## Current flow

`AttentionCandidateProvider` emits a candidate, `ShoppingViewModel` dispatches it, and `ShoppingRepository.recognize` calls the existing Backend endpoint. The previous `AtomicBoolean.compareAndSet(false, true)` guard dropped every candidate while any one request was active.

## Minimal change

- Replace the global boolean with a fair coroutine `Semaphore` containing two permits.
- Keep the candidate collector non-blocking by using `tryAcquire`; do not queue a third candidate.
- Track all recognition jobs so session completion can cancel a snapshot of them.
- Reserve a non-null `trackingId` while its request is active to suppress duplicate work for the same tracked object.
- Serialize request registration and session shutdown with one small state lock.
- Return permits and tracking reservations in request cleanup, with a completion-handler fallback for cancellation before coroutine entry.
- Count active cooldown windows so overlapping completions cannot clear the global 800 ms cooldown early.
- Keep the existing main-thread `recognizedProductIds.add(productId)` check as the final Product Card deduplication guard.

