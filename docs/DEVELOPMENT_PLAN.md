# Development Plan

## 일정 원칙

### 8월 15일 ~ 16일

디자인 완성보다 **기능 전체를 한 번 끝까지 관통**시키는 것이 목표다.

### 8월 17일

- UI 담당: 기능 검증용 Compose UI를 실제 디자인으로 교체
- Backend 담당: Mock Provider / fixture를 실제 데이터와 로직으로 교체
- Android 기능 담당: 통합 오류와 실기기 안정화

Contract와 ViewModel 경계를 유지해 동시 작업 충돌을 최소화한다.

---

# Dependency Map

```text
Backend B0
   ↓
App A0 시작

Backend B1 ───────────────┐
                          ├→ App A3 API 연결 가능
App A1 → App A2 ──────────┘

Backend B2 + App A2
   ↓
Integration Checkpoint #1
Gen2 → Attention → OpenAI → Product

Backend B3
   ↓
App A4 Shopping 실제 연결

Backend B4
   ↓
App A5 Review/Travel/Checklist/Recommendation 실제 연결

Backend B5 Contract Freeze
   ↓
8/17 UI 교체 + Backend Mock 교체 병렬
```

---

# A0. Repository Bootstrap

Backend B0 성공 즉시 시작한다.

구현:

- Android project 확인/생성
- Kotlin
- Jetpack Compose
- Material 3
- Navigation
- Coroutine / Flow 기반 state
- network client 골격
- DTO 골격
- fake repository
- 기본 DI 방식 결정
- debug config

화면 skeleton:

```text
Home
Shopping
Review
Travel
Checklist
Recommendation
```

완료 기준:

- 앱 빌드/실행
- 모든 화면 navigation 가능
- fake data Product Card 표시
- Backend DTO와 UI model 경계 존재

---

# A1. Meta DAT Migration

B1/B2와 병렬 진행한다.

구현:

- 기존 Stage 0 검증 코드 이식
- DeviceSession
- Camera lifecycle
- frame callback
- debug connection state

완료 기준:

```text
Gen 2
→ App
→ Camera frame 확인
```

쇼핑 start/stop과 stream lifecycle을 연결한다.

---

# A2. Object Detection + Attention Gate

A1 성공 후 즉시 진행한다.

구현:

- 실시간 Object Detector adapter
- stream mode
- multiple object detection
- Bounding Box
- trackingId
- Center ROI
- occupancyRatio
- dwell
- candidate selection
- threshold config
- trigger state

초기 설정:

```text
occupancy = 0.20
dwell = 1500ms
```

완료 기준:

실기기 debug 화면/log에서:

```text
trackingId
center=true/false
occupancy=...
dwellMs=...
trigger=...
```

확인 가능.

---

# A3. Backend Recognition Integration

Backend B1 완료 후 HTTP client 구현은 미리 시작할 수 있다.

Backend B2 + App A2가 모두 성공하면 실제 통합한다.

구현:

```text
trigger
→ candidate crop
→ multipart /recognize
→ MATCHED / AMBIGUOUS / UNKNOWN
```

완료 기준:

### Integration Checkpoint #1

```text
실제 Gen 2
→ 제품 바라봄
→ attention trigger
→ Backend
→ OpenAI
→ actual productId
→ 앱 표시
```

이 체크포인트를 통과하기 전에는 후반부 UX 구현에 과도하게 시간을 쓰지 않는다.

---

# A4. Shopping Session

Backend B3와 연결한다.

구현:

- session create
- active state
- matched Product Card 자동 추가
- isNew / productId 기반 중복 방지
- shopping product list
- session complete

완료 기준:

```text
Home
→ Shopping Start
→ 실제 제품 카드 자동 등장
→ 중복 없음
→ Shopping End
```

여기까지가 매장 데모의 핵심이다.

---

# A5. Post-Shopping Journey

Backend B4 이후 실제 API 연결.

구현:

```text
Review
→ Travel
→ Checklist
→ Recommendation
```

### Review

- purchased select
- interested select

### Travel

- airport
- flight
- arrival time

### Checklist

- backend items 표시
- local checked state

### Recommendation

- CROSS_SELL
- REMINDER

완료 기준:

한 세션이 앱에서 처음부터 마지막 화면까지 실행된다.

---

# A6. Contract Freeze / UI Handoff

Backend B5와 동시에 수행한다.

8월 16일 종료 전:

- DTO 확정
- ViewModel public state/action 확정
- screen별 sample state 제공
- loading / empty / error sample 제공
- 실제 UI 담당이 Camera/Data layer 없이 화면 교체 가능한지 확인

이후 Presentation 요구 때문에 API DTO를 변경하지 않는다.

---

# A7. 8월 17일 Parallel Work

## UI 담당

주 수정 영역:

```text
feature/*/Screen
components
theme
navigation presentation
```

## Android 기능 담당

- 실기기 안정화
- attention threshold calibration
- network 오류
- duplicate trigger
- lifecycle
- latency

## Backend 담당

- 실제 Recognition
- Catalog
- FX / Refund
- Recommendation 데이터 개선

---

# A8. Demo Hardening

필수:

- OpenAI timeout 시 앱 crash 방지
- Backend unavailable 상태
- camera reconnect
- recognition in-flight lock
- duplicate card 방지
- demo reset
- debug/manual trigger fallback
- 로그로 end-to-end latency 확인

---

# 지금 첫 작업

```text
1. 이 문서 세트를 repo에 넣기
2. Kiro Requirements-First Spec 생성
3. A0 구현
4. 동시에 Backend B1 상태 확인
5. A1 DAT migration
6. A2 attention
7. B2+A2 Integration Checkpoint #1
```
