# Test Plan

## 1. 테스트 우선순위

```text
P0: 실제 Gen 2 end-to-end
P0: 관심 trigger 오탐/미탐
P0: Recognition network/error
P0: 중복 상품
P0: Shopping lifecycle

P1: 후반 UX
P1: configuration
P2: 장시간 안정성
```

---

# 2. Attention Unit Tests

테스트 대상은 순수 함수/Domain class로 분리한다.

### Center

- 중심점이 ROI 안이면 true
- 경계 밖이면 false
- bbox가 화면 경계를 일부 넘는 경우 normalize/clamp 정책 확인

### Occupancy

```text
bboxArea / frameArea
```

정상 계산 검증.

### Trigger

```text
center && occupancy >= threshold
center && dwell >= threshold
center && both
not center
```

각 경우 검증.

### Candidate Selection

복수 object에서:

- center distance 우선
- occupancy tie-break
- 후보 없음

검증.

---

# 3. Repository / API Tests

Fake Backend를 사용해:

- MATCHED
- AMBIGUOUS
- UNKNOWN
- timeout
- 4xx
- 5xx
- malformed response

UI state가 올바르게 변하는지 검증한다.

---

# 4. Shopping Tests

- session create 성공
- session create 실패
- 동일 productId 중복 response
- isNew=false
- session complete 후 recognition 중지
- review 저장

---

# 5. Lifecycle Tests

실기기:

```text
stream start
→ stop
→ restart

stream
→ 앱 background
→ foreground

stream
→ glasses disconnect
→ reconnect
```

가능한 범위를 반복 검증한다.

---

# 6. 실제 매장 유사 테스트

한 화면에 여러 제품을 배치한다.

시나리오:

### A. 멀리 여러 상품

기대:

- Recognition 요청이 무차별 발생하지 않음.

### B. 한 상품을 중앙 가까이

기대:

- occupancy trigger 발생.

### C. 가까이 가지 않고 1.5초 이상 중앙 유지

기대:

- dwell trigger 발생.

### D. 여러 상품 중 한 상품을 중앙에 둠

기대:

- 중앙 후보 하나 우선.

### E. 빠르게 스쳐 지나감

기대:

- dwell만으로 trigger되지 않음.

---

# 7. Threshold Calibration

후보 값:

```text
occupancy
0.10
0.15
0.20
0.25
0.30
```

실제 Gen 2 영상에서 비교한다.

기록:

```text
제품
거리/상황
occupancy
dwell
trigger
OpenAI 결과
오탐/미탐
```

최종 threshold는 발표 전 config로 확정한다.

---

# 8. Demo Acceptance

발표용 성공 시나리오:

```text
1. Gen 2 연결
2. 쇼핑 시작
3. 제품을 바라봄
4. 관심 trigger
5. 실제 Recognition
6. Product Card 자동 등장
7. 가격 3단 정보 확인
8. 쇼핑 종료
9. 구매/관심 선택
10. Travel 입력
11. Checklist
12. Recommendation
```

이 흐름을 최소 3회 연속 성공시키는 것을 Demo readiness 기준으로 한다.
