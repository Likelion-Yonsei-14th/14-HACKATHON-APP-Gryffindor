# Recognition Bounded Concurrency Tasks

- [x] Replace global recognition single-flight with two bounded slots.
- [x] Track same-object requests and all active recognition jobs.
- [x] Cancel all jobs and guarantee cleanup on session completion.
- [x] Add concurrency, cancellation, failure cleanup, same-object, and Product Card dedup tests.
- [x] Run `./gradlew test`.
