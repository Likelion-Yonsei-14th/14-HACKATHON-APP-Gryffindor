# Attention Policy

## 1. 목적

매장 영상에는 여러 진열 상품이 동시에 보일 수 있다.

따라서 단순히 화면에 등장한 모든 객체를 관심 상품으로 처리하지 않고, **중앙 시야에서 충분히 크게 보이거나 일정 시간 지속적으로 관찰된 후보만 Recognition 대상으로 전달**한다.

이 정책은 Eye Tracking이 아니라 카메라 시야를 이용한 heuristic이다.

---

## 2. 입력

Object Detector가 각 frame에서 제공하는 후보:

```text
boundingBox
trackingId (가능한 경우)
frameWidth
frameHeight
timestamp
```

상품 ID는 아직 모른다.

---

## 3. 계산값

### Center

Bounding Box 중심점:

```text
cx = (left + right) / 2
cy = (top + bottom) / 2
```

Center ROI 내부 여부를 normalized coordinate로 계산한다.

Center ROI 크기는 config로 관리한다.

### Occupancy Ratio

```text
bboxArea = width * height
frameArea = frameWidth * frameHeight

occupancyRatio = bboxArea / frameArea
```

### Dwell

동일 tracking candidate가 Center ROI 안에 유지된 시간을 누적한다.

trackingId가 안정적이지 않은 상황에서는 bbox 위치/크기의 시간적 유사성을 이용한 fallback을 P1로 고려할 수 있다.

---

## 4. Trigger

기본 정책:

```text
isCenter == true
AND
(
    occupancyRatio >= occupancyThreshold
    OR
    dwellMs >= dwellThresholdMs
)
```

초기 실험값:

```text
occupancyThreshold = 0.20
dwellThresholdMs = 1500
```

이 값은 제품 크기와 촬영 환경을 테스트한 뒤 변경한다.

---

## 5. Candidate 선택

여러 객체가 동시에 조건에 근접한 경우:

1. Center ROI 밖의 객체 제거
2. 중앙점과 frame center의 거리가 가까운 객체 우선
3. occupancyRatio가 큰 객체 우선
4. 한 시점에 Recognition 후보는 기본적으로 하나만 선택

복수 후보가 비슷한 경우 불필요한 API 요청을 줄이기 위해 trigger를 보류할 수 있다.

---

## 6. Recognition 호출 정책

Trigger 발생:

```text
candidate bbox
→ padding을 소량 적용한 crop
→ JPEG/PNG 변환
→ Backend /recognize
```

Backend 요청 metadata:

```text
triggerType
occupancyRatio
dwellMs
trackingId
capturedAt
```

### triggerType

- `OCCUPANCY`
- `DWELL`
- `OCCUPANCY_AND_DWELL`

---

## 7. 중복 호출 방지

다음 보호 장치를 둔다.

- 동일 trackingId recognition in-flight 중 재호출 금지
- 성공한 productId는 쇼핑 카드 중복 생성 금지
- 동일 후보 trigger에 cooldown 적용 가능
- Backend `isNew` 값을 최종 세션 중복 판단에 활용

---

## 8. Object Detector의 역할 제한

Object Detector는:

```text
어디에 객체가 있는가?
얼마나 크게 보이는가?
같은 객체가 이어지는가?
```

만 판단한다.

다음은 판단하지 않는다.

```text
이 제품의 정확한 MCM SKU는 무엇인가?
```

실제 상품 식별은 Backend Recognition의 책임이다.

---

## 9. Debug 지표

실기기 calibration 중 기록:

```text
frame size
bbox
centerDistance
occupancyRatio
dwellMs
trackingId
triggerType
recognition latency
recognition result
```

10% / 15% / 20% / 25% / 30% 수준을 비교해 실제 threshold를 확정한다.
