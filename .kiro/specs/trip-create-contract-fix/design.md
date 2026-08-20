# Trip Create 422 Contract Mismatch Bugfix Design

## Overview

Trip 생성 시 앱이 사용자 입력(한글 국가명, offset 없는 날짜)을 그대로 Backend에 전송하여 HTTP 422를 받는 버그를 수정한다. ViewModel 레벨에서 입력 정규화(country → ISO 3166-1 alpha-2, date → ISO 8601 with offset) 및 유효성 검사를 수행하고, 실패 시 Backend 호출 없이 사용자에게 에러 메시지를 표시한다.

## Glossary

- **Bug_Condition (C)**: `destinationCountry`가 대문자 2자 ISO 코드가 아니거나, `startsAt`/`endsAt`가 timezone offset 없는 문자열이거나, `endsAt < startsAt`인 상태로 createTrip을 호출하는 조건
- **Property (P)**: 입력을 정규화하여 Backend contract(`^[A-Z]{2}$`, ISO 8601 with offset)에 맞는 값만 전송하거나, 정규화 불가 시 에러 메시지 표시
- **Preservation**: 기존 정상 동작(빈 title 검증, null 전송, 정상 코드 직접 전달, 성공 시 navigation) 유지
- **TripInputValidator**: `feature/trip/` 내 순수 함수 유틸리티 — country 정규화, 날짜 변환, 범위 검증 담당
- **TripViewModel.createTrip()**: 사용자 입력을 검증/정규화 후 Repository에 전달하는 함수
- **RemoteTripRepository.createTrip()**: Backend API를 호출하는 Repository 구현체

## Bug Details

### Bug Condition

사용자가 TripCreateScreen에서 국가명을 한글("한국")이나 소문자 코드("kr")로 입력하거나, 날짜를 "2026-08-20" 형태(offset 없음)로 입력한 후 "생성" 버튼을 누르면, ViewModel이 입력값을 검증/변환 없이 Repository → Backend로 전달하여 422를 받는다.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type TripCreateUiState
  OUTPUT: boolean

  LET countryRaw = input.country.trim()
  LET startsAtRaw = input.startsAt.trim()
  LET endsAtRaw = input.endsAt.trim()

  LET countryBug = countryRaw.isNotEmpty()
        AND NOT countryRaw.matches("^[A-Z]{2}$")

  LET startsAtBug = startsAtRaw.isNotEmpty()
        AND NOT startsAtRaw.matches("\\d{4}-\\d{2}-\\d{2}T.+[+-]\\d{2}:\\d{2}")

  LET endsAtBug = endsAtRaw.isNotEmpty()
        AND NOT endsAtRaw.matches("\\d{4}-\\d{2}-\\d{2}T.+[+-]\\d{2}:\\d{2}")

  LET rangeBug = startsAtRaw.isNotEmpty() AND endsAtRaw.isNotEmpty()
        AND parseDate(endsAtRaw) < parseDate(startsAtRaw)

  RETURN countryBug OR startsAtBug OR endsAtBug OR rangeBug
END FUNCTION
```

### Examples

- 국가 "한국" 입력 → 앱이 "한국" 그대로 전송 → Backend 422 (expected: "KR"로 변환 후 전송)
- 국가 "kr" 입력 → 앱이 "kr" 그대로 전송 → Backend 422 (expected: "KR"로 정규화 후 전송)
- 시작일 "2026-08-20" 입력 → 앱이 그대로 전송 → Backend 422 (expected: "2026-08-20T00:00:00+09:00"으로 변환)
- 종료일 "2026-08-23" 입력 → 앱이 그대로 전송 → Backend 422 (expected: "2026-08-23T23:59:59+09:00"으로 변환)
- 시작일 > 종료일 → 앱이 그대로 전송 → Backend 422 (expected: 클라이언트 검증 에러 표시)
- 날짜 "08-20-2026" 입력 → 앱이 그대로 전송 → Backend 422 (expected: 클라이언트 검증 에러 표시)
- 국가 "XYZ" 입력 → 앱이 그대로 전송 → Backend 422 (expected: 클라이언트 검증 에러 표시)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- title이 빈 문자열일 때 "여행 이름을 입력해주세요." 에러 표시 (Backend 호출 안 함)
- title만 입력하고 나머지 비워두면 destinationCity/Country/startsAt/endsAt를 null로 전송 (Backend 허용)
- 이미 대문자 2자 ISO 코드("KR", "JP", "US")를 입력하면 그대로 전송
- startsAt/endsAt 모두 유효하고 endsAt >= startsAt이면 정상 생성
- 생성 성공 시 `onTripCreated(tripId)` 콜백을 통해 상세 화면으로 이동
- Backend가 422 외의 에러를 반환하면 해당 에러 메시지를 UI에 표시
- city 필드는 변환 없이 그대로 전송
- startsAt만 입력하고 endsAt을 비우면 (또는 반대) 입력된 값만 ISO 8601 변환 후 전송, 나머지는 null

**Scope:**
국가 필드 정규화와 날짜 필드 ISO 8601 변환에만 영향을 주며, 그 외 모든 Trip 생성 로직(title 검증, null 처리, navigation, 에러 표시)은 기존과 동일하게 유지한다.

## Hypothesized Root Cause

Based on the bug description, the most likely issues are:

1. **입력 정규화 로직 부재**: `TripViewModel.createTrip()`이 `state.country`와 `state.startsAt`/`state.endsAt` 값을 `ifBlank { null }` 처리만 하고, Backend contract에 맞는 형식 변환 없이 그대로 Repository에 전달한다.

2. **Client-side Validation 미구현**: Backend가 요구하는 `^[A-Z]{2}$` 패턴 검사, ISO 8601 offset 형식 검사, 날짜 범위 검사가 앱에 전혀 없어서 잘못된 값이 네트워크 요청까지 도달한다.

3. **UI Placeholder 오해 유도**: 국가 필드 placeholder가 "예: 한국"으로 되어 있어 사용자가 한글 국가명을 입력하도록 유도한다.

4. **로깅 부족**: `RemoteTripRepository`가 title만 로깅하여 어떤 필드가 422를 유발했는지 디버깅이 어렵다.

## Correctness Properties

Property 1: Bug Condition - 국가 코드 정규화 및 날짜 ISO 8601 변환

_For any_ input where the bug condition holds (isBugCondition returns true), the fixed `createTrip()` function SHALL either normalize the input to Backend-compatible format (country → uppercase ISO alpha-2, date → ISO 8601 with +09:00 offset) and send it successfully, OR reject the input with a descriptive Korean validation error message without calling Backend.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7**

Property 2: Preservation - 기존 정상 입력 동작 유지

_For any_ input where the bug condition does NOT hold (isBugCondition returns false — i.e., country is already uppercase 2-letter or empty, dates are already ISO 8601 with offset or empty, and date range is valid), the fixed `createTrip()` SHALL produce the same result as the original function, preserving title validation, null handling, successful creation, navigation, and error display.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `app/src/main/java/com/gryffindor/smartshopping/feature/trip/TripInputValidator.kt` (NEW)

**Purpose**: 순수 함수 유틸리티 — country 정규화, 날짜 변환, 범위 검증

**Specific Changes**:
1. **`normalizeCountry(input: String): Result<String>`**
   - 빈 문자열 → `Result.success("")` (null로 처리될 것)
   - 한글 매핑: "한국"→"KR", "일본"→"JP", "미국"→"US", "중국"→"CN" 등 주요 관광국 매핑
   - 소문자 2자 → uppercase ("kr"→"KR")
   - 이미 대문자 2자 → 그대로 통과
   - 그 외 → `Result.failure(IllegalArgumentException("유효하지 않은 국가 코드입니다. 2자리 영문 코드를 입력해주세요. (예: KR)"))`

2. **`toIso8601Start(dateStr: String): Result<String>`**
   - 빈 문자열 → `Result.success("")`
   - YYYY-MM-DD 패턴 파싱 → "${dateStr}T00:00:00+09:00"
   - 이미 offset 포함 형식 → 그대로 통과
   - 파싱 실패 → `Result.failure(IllegalArgumentException("날짜 형식이 올바르지 않습니다. (예: 2026-08-20)"))`

3. **`toIso8601End(dateStr: String): Result<String>`**
   - 빈 문자열 → `Result.success("")`
   - YYYY-MM-DD 패턴 파싱 → "${dateStr}T23:59:59+09:00"
   - 이미 offset 포함 형식 → 그대로 통과
   - 파싱 실패 → `Result.failure`

4. **`validateDateRange(startStr: String, endStr: String): Result<Unit>`**
   - 둘 중 하나라도 빈 문자열이면 → `Result.success(Unit)` (단일 날짜 허용)
   - 파싱 후 end < start → `Result.failure(IllegalArgumentException("종료 날짜는 시작 날짜 이후여야 합니다."))`
   - end >= start → `Result.success(Unit)`

---

**File**: `app/src/main/java/com/gryffindor/smartshopping/feature/trip/TripViewModel.kt`

**Function**: `createTrip()`

**Specific Changes**:
1. **Country 정규화 삽입**: title blank 체크 이후, `TripInputValidator.normalizeCountry(state.country)` 호출. failure 시 `_createState.error` 설정 후 return
2. **날짜 변환 삽입**: `toIso8601Start(state.startsAt)`, `toIso8601End(state.endsAt)` 호출. failure 시 에러 설정 후 return
3. **날짜 범위 검증 삽입**: 두 날짜 모두 비어있지 않으면 `validateDateRange()` 호출. failure 시 에러 설정 후 return
4. **정규화된 값 전달**: `tripRepository.createTrip()`에 정규화된 country, startsAt, endsAt 전달

---

**File**: `app/src/main/java/com/gryffindor/smartshopping/feature/trip/TripCreateScreen.kt`

**Specific Changes**:
1. **Placeholder 변경**: 국가 필드 placeholder를 `"예: 한국"` → `"예: KR"`로 변경

---

**File**: `app/src/main/java/com/gryffindor/smartshopping/data/repository/RemoteTripRepository.kt`

**Function**: `createTrip()`

**Specific Changes**:
1. **로그 확장**: 기존 `"createTrip: POST /api/v1/me/trips title=$title"` →  
   `"createTrip: POST /api/v1/me/trips title=$title city=$destinationCity country=$destinationCountry startsAt=$startsAt endsAt=$endsAt"`

## Testing Strategy

### Validation Approach

2단계 접근: 먼저 현재 코드에서 버그를 재현하는 테스트를 작성하여 근본 원인을 확인하고, 수정 후 정규화가 올바르게 동작하며 기존 동작이 보존됨을 검증한다.

### Exploratory Bug Condition Checking

**Goal**: 수정 전 코드에서 `TripInputValidator`가 없는 상태의 `createTrip()` 동작을 확인하여 버그 재현

**Test Plan**: `TripInputValidator`의 정규화 함수에 버그 조건 입력을 넣어 현재 앱이 어떻게 처리하는지 확인. 현재 코드에는 validator가 없으므로, raw 입력이 그대로 Repository까지 도달함을 확인.

**Test Cases**:
1. **한글 국가명 테스트**: "한국" 입력 시 현재 코드가 변환 없이 전달함을 확인 (will fail on unfixed code)
2. **소문자 코드 테스트**: "kr" 입력 시 현재 코드가 그대로 전달함을 확인 (will fail on unfixed code)
3. **날짜 offset 없음 테스트**: "2026-08-20" 입력 시 offset 추가 없이 전달함을 확인 (will fail on unfixed code)
4. **역순 날짜 테스트**: startsAt > endsAt 입력 시 검증 없이 전달함을 확인 (will fail on unfixed code)

**Expected Counterexamples**:
- `normalizeCountry("한국")` → 변환 로직 없음 → "한국" 그대로 전달 → Backend 422
- `toIso8601Start("2026-08-20")` → 변환 로직 없음 → "2026-08-20" 그대로 전달 → Backend 422

### Fix Checking

**Goal**: 버그 조건의 모든 입력에 대해 수정된 함수가 올바른 정규화 또는 유효성 에러를 반환하는지 검증

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := TripInputValidator.normalizeCountry(input.country)
  IF input.country is normalizable THEN
    ASSERT result.isSuccess
    ASSERT result.getOrNull().matches("^[A-Z]{2}$")
  ELSE
    ASSERT result.isFailure
    ASSERT result.exceptionOrNull().message is descriptive Korean string
  END IF

  resultStart := TripInputValidator.toIso8601Start(input.startsAt)
  IF input.startsAt matches YYYY-MM-DD THEN
    ASSERT resultStart.isSuccess
    ASSERT resultStart.getOrNull() contains "+09:00"
  ELSE IF input.startsAt is malformed THEN
    ASSERT resultStart.isFailure
  END IF
END FOR
```

### Preservation Checking

**Goal**: 버그 조건이 아닌 모든 입력에 대해 수정된 함수가 원래 함수와 동일한 결과를 생성하는지 검증

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  // Country: empty or already ^[A-Z]{2}$
  ASSERT TripInputValidator.normalizeCountry(input.country).getOrNull() == input.country

  // Dates: empty or already has offset
  IF input.startsAt.isEmpty() THEN
    ASSERT toIso8601Start(input.startsAt).getOrNull() == ""
  END IF

  // Overall createTrip behavior unchanged
  ASSERT createTrip_fixed(input) == createTrip_original(input)
END FOR
```

**Testing Approach**: Property-based testing이 이상적이지만, 프로젝트에 PBT 라이브러리가 없으므로 parameterized JUnit 테스트로 다양한 입력 조합을 커버한다.

**Test Cases**:
1. **빈 국가 보존**: country="" → null 전달, 기존과 동일
2. **정상 ISO 코드 보존**: country="KR" → "KR" 그대로 전달
3. **빈 날짜 보존**: startsAt="" → null 전달, 기존과 동일
4. **정상 범위 보존**: startsAt="2026-08-20", endsAt="2026-08-23" → 변환 후 정상 전송
5. **title 빈값 보존**: title="" → 기존과 동일하게 에러 표시

### Unit Tests

- `TripInputValidator.normalizeCountry()`: 한글 매핑, 소문자 변환, 대문자 통과, 유효하지 않은 값 에러
- `TripInputValidator.toIso8601Start()`: YYYY-MM-DD 변환, 빈 문자열, 잘못된 형식 에러
- `TripInputValidator.toIso8601End()`: YYYY-MM-DD 변환, 빈 문자열, 잘못된 형식 에러
- `TripInputValidator.validateDateRange()`: 정상 범위, 역순 에러, 한쪽 빈 값 허용
- `TripViewModel.createTrip()`: 정규화 성공 → Repository 호출, 정규화 실패 → 에러 상태 + Repository 미호출

### Property-Based Tests

- Parameterized test로 다양한 country 입력(한글, 소문자, 대문자, 3자 이상, 숫자 포함)에 대해 정규화 결과 검증
- Parameterized test로 다양한 날짜 형식에 대해 변환 결과 검증
- 정상 입력 집합에 대해 원래 동작과 동일한 결과 확인

### Integration Tests

- Full flow: 한글 국가명 + 날짜 입력 → 정규화 → 성공적 API 호출 (mock Backend)
- Validation error flow: 잘못된 입력 → 에러 메시지 표시 → Backend 미호출
- 기존 정상 flow: 이미 올바른 형식 입력 → 기존과 동일하게 성공
