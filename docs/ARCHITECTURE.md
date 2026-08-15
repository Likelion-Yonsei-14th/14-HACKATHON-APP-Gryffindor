# Architecture

## 1. 전체 구조

```text
Meta Ray-Ban Gen 2
        ↓
Meta Device Access Toolkit
        ↓
Camera Frame
        ↓
Object Detection / Tracking
        ↓
Attention Gate
  - Center ROI
  - Occupancy
  - Dwell
        ↓
Triggered Candidate Crop
        ↓
Backend API
        ↓
RecognitionResult + Product + Pricing
        ↓
Shopping ViewModel / State
        ↓
Compose UI
```

---

## 2. 레이어

### Presentation

```text
Compose Screen
↓
ViewModel
↓
UI State
```

역할:

- 화면 렌더링
- 사용자 입력
- navigation
- loading / error / empty state

Presentation 레이어에서 DAT SDK나 Retrofit을 직접 호출하지 않는다.

### Domain

핵심 use case:

```text
StartShopping
StopShopping
EvaluateAttention
RecognizeCandidate
UpdateObservedProducts
SubmitShoppingReview
SaveTravelPlan
LoadRefundChecklist
LoadRecommendations
```

관심 행동 판정 정책은 UI와 분리한다.

### Data

```text
MetaCameraSource
ObjectDetectorAdapter
BackendApi
SessionRepository
RecognitionRepository
ShoppingRepository
```

SDK 구현 세부사항을 ViewModel이 직접 알지 않도록 adapter/repository 경계를 둔다.

---

## 3. 권장 패키지 구조

```text
app/
└─ src/main/java/.../
   ├─ app/
   │  ├─ App.kt
   │  └─ navigation/
   │
   ├─ feature/
   │  ├─ home/
   │  ├─ shopping/
   │  ├─ review/
   │  ├─ travel/
   │  ├─ checklist/
   │  └─ recommendation/
   │
   ├─ domain/
   │  ├─ model/
   │  ├─ usecase/
   │  └─ attention/
   │
   ├─ data/
   │  ├─ meta/
   │  ├─ detection/
   │  ├─ remote/
   │  └─ repository/
   │
   ├─ core/
   │  ├─ network/
   │  ├─ config/
   │  ├─ logging/
   │  └─ common/
   │
   └─ debug/
```

실제 기존 Meta sample 구조와 충돌하는 경우 Kiro는 이 구조를 억지로 적용하지 말고, 경계 원칙을 유지하면서 현재 SDK sample 구조에 맞춰 조정한다.

---

## 4. 상태 소유권

### Session State

앱은 현재 `sessionId`, status를 메모리/ViewModel 상태로 유지한다.
영속 데이터의 source of truth는 Backend다.

### Observation State

앱:

- 현재 tracking 후보
- center 여부
- occupancyRatio
- dwellMs
- trigger cooldown

Backend:

- 최종 MATCHED product
- 세션 내 observed product
- first/last observed metadata

### Checklist Completion

P0에서는 앱 로컬 상태로 관리한다.

---

## 5. Thread / Frame 원칙

Camera frame 처리는 UI thread를 block하지 않는다.

실시간 frame마다 Backend를 호출하지 않는다.

```text
모든 frame
→ local detection

조건 만족 frame만
→ Backend recognition
```

Recognition 요청 진행 중에는 동일 tracking candidate에 대한 중복 호출을 억제한다.

---

## 6. UI 교체를 위한 경계

8월 17일 UI 담당자가 다음만 수정하도록 한다.

```text
feature/*/Screen
공통 UI component
theme
navigation animation
```

가급적 수정하지 않는 영역:

```text
data/meta
data/detection
data/remote
domain/attention
API DTO
repository
```

따라서 기능 검증용 Material UI와 실제 디자인 UI가 동일 ViewModel contract를 공유해야 한다.
