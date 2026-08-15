# Requirements

## 1. MVP 목표

관광객이 Meta Ray-Ban Gen 2를 착용하고 매장을 둘러볼 때, 앱은 카메라 영상에서 **관심 있게 바라보는 제품 후보**를 자동 감지한다.

관심 조건을 만족한 제품 이미지를 Backend에 전달하고, Backend가 반환한 상품 및 가격 정보를 모바일 앱 쇼핑 목록에 자동으로 적재한다.

쇼핑 종료 후 사용자는 실제 구매 상품과 미구매 관심 상품을 구분하고, 출국 정보를 입력해 환급 체크리스트와 공항 추천을 확인한다.

---

## 2. P0 기능

### 2.1 쇼핑 세션

- 사용자는 쇼핑을 시작할 수 있다.
- 시작 시 Backend Shopping Session을 생성한다.
- 세션이 ACTIVE인 동안에만 관심 행동 판정과 Recognition 요청을 수행한다.
- 사용자는 쇼핑을 종료할 수 있다.
- 종료 시 Backend Session을 COMPLETED 상태로 전환한다.

### 2.2 Meta Camera

- Meta Device Access Toolkit을 이용해 Gen 2 Camera Stream을 받을 수 있다.
- 앱 lifecycle과 DeviceSession lifecycle을 안전하게 연결한다.
- Stream start / stop / reconnect 실패가 앱 전체 crash로 이어지지 않는다.

### 2.3 Object Detection / Tracking

- 실시간 frame에서 객체 Bounding Box를 얻는다.
- 여러 객체가 보이는 상황을 고려한다.
- trackingId가 제공되는 경우 동일 객체의 시간적 지속성을 계산하는 데 사용한다.
- Object Detector의 category label을 실제 상품 ID로 사용하지 않는다.

### 2.4 관심 행동 판정

관심 후보가 다음 조건을 만족하는지 앱에서 계산한다.

- 객체 중심점이 설정된 Center ROI 내부에 있다.
- 그리고 다음 중 하나를 만족한다.
  - 화면 점유율이 `occupancyThreshold` 이상이다.
  - 동일 후보가 중앙 영역에서 `dwellThresholdMs` 이상 지속된다.

초기 실험값:

```text
occupancyThreshold = 0.20
dwellThresholdMs = 1500
```

이는 제품/매장/카메라 환경 검증 전 임시값이며 런타임 또는 build config에서 조정 가능해야 한다.

### 2.5 Recognition 요청

- 관심 trigger 발생 전에는 Backend Recognition을 호출하지 않는다.
- 관심 제품의 Bounding Box 영역을 crop해 JPEG/PNG로 Backend에 전달한다.
- 요청에는 관심 행동 metadata를 함께 전달한다.
- `MATCHED`, `AMBIGUOUS`, `UNKNOWN`, 오류 상태를 구분한다.

### 2.6 자동 상품 목록

`MATCHED` 결과를 받으면 쇼핑 화면에 다음 정보를 표시한다.

- 상품 이미지
- 브랜드 / 상품명
- 국내 정가
- 예상 환급 적용가
- 사용 통화 환산가
- 즉시환급 가능 여부

동일 상품은 카드가 중복 생성되지 않아야 한다.

### 2.7 쇼핑 Review

쇼핑 종료 후 사용자는:

- 구매한 상품을 선택할 수 있다.
- 구매하지 않았지만 관심 있는 상품을 선택할 수 있다.
- 구매 상품과 관심 상품을 Backend에 저장할 수 있다.

구매와 관심이 동시에 선택된 경우 구매가 우선한다.

### 2.8 Travel

사용자는 다음 정보를 입력할 수 있다.

- 출국 공항
- 항공편
- 공항 도착 예정 시간

### 2.9 Refund Checklist

Backend가 반환한 환급 체크리스트를 표시한다.

P0에서는 체크 완료 여부를 앱 local state로 관리해도 된다.

### 2.10 Airport Recommendation

Backend 추천 결과를 다음 유형으로 구분하여 표시한다.

- `CROSS_SELL`: 구매 상품 기반 연관 상품
- `REMINDER`: 관심했지만 구매하지 않은 상품

---

## 3. P1

- 영수증 촬영 및 OCR 연동
- 실제 환율 데이터 표시 고도화
- 위치 기반 공항 진입 감지
- Push notification
- 관심 threshold calibration UI
- 오프라인/네트워크 재시도 고도화

---

## 4. 제외 범위

- Eye Tracking
- Hand Tracking으로 제품을 들었는지 정밀 판정
- 스마트글래스 자체 디스플레이 UI
- 앱에서 OpenAI 직접 호출
- 결제 처리
- 실제 세금 환급 신청
- 실시간 공항 면세점 재고 보장
- 로그인 / 회원가입
- 고도화 Recommendation ML

---

## 5. MVP 성공 기준

다음 흐름이 실제 Gen 2에서 한 번 끝까지 동작하면 기능 MVP를 성공으로 본다.

```text
Gen 2 연결
→ 쇼핑 시작
→ 제품을 중앙 시야에서 가까이/지속 관찰
→ Object Detection
→ 관심 trigger 발생
→ Backend Recognition
→ 실제 productId 반환
→ 가격 카드 자동 등장
→ 쇼핑 종료
→ 구매/관심 선택
→ 출국 정보 입력
→ 체크리스트
→ 추천
```
