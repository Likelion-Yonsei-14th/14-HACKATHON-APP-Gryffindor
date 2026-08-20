# Implementation Plan

- [ ] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Trip Create 422 Input Normalization Missing
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists (inputs bypass validation and reach Backend raw)
  - **Scoped PBT Approach**: Scope the property to concrete failing cases from Bug Condition in design:
    - `normalizeCountry("한국")` should return `Result.success("KR")` but no such function exists yet
    - `normalizeCountry("kr")` should return `Result.success("KR")` but no such function exists yet
    - `toIso8601Start("2026-08-20")` should return `Result.success("2026-08-20T00:00:00+09:00")` but no such function exists yet
    - `toIso8601End("2026-08-23")` should return `Result.success("2026-08-23T23:59:59+09:00")` but no such function exists yet
    - `validateDateRange("2026-08-23", "2026-08-20")` should return `Result.failure` but no such function exists yet
  - Create test file: `app/src/test/java/com/gryffindor/smartshopping/feature/trip/TripInputValidatorBugConditionTest.kt`
  - Test that TripInputValidator functions handle bug-condition inputs correctly (these will fail because the class doesn't exist yet)
  - Run test on UNFIXED code: `./gradlew testDebugUnitTest --tests "*.TripInputValidatorBugConditionTest"`
  - **EXPECTED OUTCOME**: Test FAILS (compilation error - TripInputValidator doesn't exist, confirming the bug: no validation/normalization layer)
  - Document counterexamples found to understand root cause
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [ ] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Trip Create Behavior Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs (cases where isBugCondition returns false):
    - Observe: title="" → error "여행 이름을 입력해주세요." without Backend call
    - Observe: title="테스트여행", country="" → null sent as destinationCountry
    - Observe: title="테스트여행", country="KR" → "KR" sent as-is
    - Observe: title="테스트여행", city="서울" → "서울" sent as-is without transformation
    - Observe: title="테스트여행", startsAt="" → null sent as startsAt
  - Create test file: `app/src/test/java/com/gryffindor/smartshopping/feature/trip/TripCreatePreservationTest.kt`
  - Write parameterized tests capturing observed behavior patterns from Preservation Requirements:
    - Title blank validation still produces error without Backend call
    - Empty optional fields still result in null transmission
    - Already-valid ISO codes ("KR", "JP", "US") pass through unchanged
    - City field passes through without transformation
    - Single date input (only startsAt or only endsAt) still sends null for the missing one
  - Use a mock/fake TripRepository to verify what values reach `createTrip()`
  - Run tests on UNFIXED code: `./gradlew testDebugUnitTest --tests "*.TripCreatePreservationTest"`
  - **EXPECTED OUTCOME**: Tests PASS (confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [ ] 3. Fix for Trip Create 422 Contract Mismatch

  - [ ] 3.1 Create TripInputValidator utility
    - Create NEW file: `app/src/main/java/com/gryffindor/smartshopping/feature/trip/TripInputValidator.kt`
    - Implement `normalizeCountry(input: String): Result<String>`
      - Empty → Result.success("")
      - Korean mapping: "한국"→"KR", "일본"→"JP", "미국"→"US", "중국"→"CN", etc.
      - Lowercase 2-char → uppercase ("kr"→"KR")
      - Already uppercase 2-char → pass through
      - Otherwise → Result.failure with Korean error message
    - Implement `toIso8601Start(dateStr: String): Result<String>`
      - Empty → Result.success("")
      - YYYY-MM-DD → "${dateStr}T00:00:00+09:00"
      - Already has offset → pass through
      - Parse failure → Result.failure with Korean error message
    - Implement `toIso8601End(dateStr: String): Result<String>`
      - Empty → Result.success("")
      - YYYY-MM-DD → "${dateStr}T23:59:59+09:00"
      - Already has offset → pass through
      - Parse failure → Result.failure with Korean error message
    - Implement `validateDateRange(startStr: String, endStr: String): Result<Unit>`
      - Either empty → Result.success(Unit)
      - end < start → Result.failure with Korean error message
      - end >= start → Result.success(Unit)
    - Use java.time.LocalDate for parsing (minSdk 31, no desugaring needed)
    - _Bug_Condition: isBugCondition(input) where country not ^[A-Z]{2}$, or dates lack offset, or end < start_
    - _Expected_Behavior: normalize to Backend contract or reject with Korean validation error_
    - _Preservation: empty inputs return success(""), valid ISO codes pass through unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ] 3.2 Modify TripViewModel.createTrip() to use TripInputValidator
    - After existing title blank check, add:
      1. `TripInputValidator.normalizeCountry(state.country)` → on failure: set error, return
      2. `TripInputValidator.toIso8601Start(state.startsAt)` → on failure: set error, return
      3. `TripInputValidator.toIso8601End(state.endsAt)` → on failure: set error, return
      4. `TripInputValidator.validateDateRange(normalizedStart, normalizedEnd)` → on failure: set error, return
    - Pass normalized country, startsAt, endsAt to `tripRepository.createTrip()`
    - Keep `ifBlank { null }` logic for empty normalized values
    - _Bug_Condition: raw user input bypasses validation and reaches Backend_
    - _Expected_Behavior: validate/normalize before Backend call, show error on failure_
    - _Preservation: title blank check, null handling, navigation, error display unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 3.3 Update TripCreateScreen placeholder
    - Change country field placeholder from `"예: 한국"` to `"예: KR"`
    - _Requirements: 2.9_

  - [ ] 3.4 Expand RemoteTripRepository log
    - Change createTrip() Log.d from `"createTrip: POST /api/v1/me/trips title=$title"` to include all fields: `"createTrip: POST /api/v1/me/trips title=$title city=$destinationCity country=$destinationCountry startsAt=$startsAt endsAt=$endsAt"`
    - _Requirements: 2.8_

  - [ ] 3.5 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Trip Create 422 Input Normalization Missing
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior (normalizeCountry, toIso8601Start, toIso8601End, validateDateRange)
    - When this test passes, it confirms the expected behavior is satisfied
    - Run: `./gradlew testDebugUnitTest --tests "*.TripInputValidatorBugConditionTest"`
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

  - [ ] 3.6 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Trip Create Behavior Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run: `./gradlew testDebugUnitTest --tests "*.TripCreatePreservationTest"`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all preservation tests still pass after fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run full test suite: `./gradlew testDebugUnitTest`
  - Run build verification: `./gradlew assembleDebug`
  - Ensure all tests pass, ask the user if questions arise.
