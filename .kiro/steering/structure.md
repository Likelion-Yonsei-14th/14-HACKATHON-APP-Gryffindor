# Project Structure & Architectural Boundaries

## Layer Boundaries

### Presentation

```text
Compose Screen → ViewModel → UI State
```

- 화면 렌더링, 사용자 입력, navigation, loading/error/empty state 처리
- DAT SDK나 네트워크 클라이언트를 직접 호출하지 않음
- 8/17 UI 교체 시 주 수정 대상: `feature/*/Screen`, 공통 UI component, theme, navigation animation

### Domain

핵심 use case:

- StartShopping
- StopShopping
- EvaluateAttention
- RecognizeCandidate
- UpdateObservedProducts
- SubmitShoppingReview
- SaveTravelPlan
- LoadRefundChecklist
- LoadRecommendations

관심 행동 판정 정책은 UI와 분리한다.

### Data

- MetaCameraSource
- ObjectDetectorAdapter
- BackendApi
- SessionRepository
- RecognitionRepository
- ShoppingRepository

SDK 구현 세부사항을 ViewModel이 직접 알지 않도록 adapter/repository 경계를 둔다.

---

## Recommended Package Structure

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

이 구조는 Meta SDK sample 프로젝트와 통합할 때 조정될 수 있다. 기존 SDK sample 구조와 충돌하는 경우 이 구조를 억지로 적용하지 않고, 아래의 경계 원칙을 유지하면서 현재 SDK sample 구조에 맞춰 조정한다.

---

## Isolation Rules

### Meta SDK Types

- Meta SDK type은 Domain layer나 ViewModel layer로 새어나가서는 안 된다.
- `data/meta/` 내부에서 DAT frame을 앱 내부 표준 표현 (예: `CameraFrame`)으로 변환한다.
- Object Detection과 관심 행동 정책이 Meta SDK의 구체적인 frame type에 직접 결합되지 않도록 adapter 경계를 둔다.
- 기존 Stage 0에서 `VideoFrame`의 buffer, dimensions, timestamp를 앱 소유 데이터로
복사하여 SDK 외부 처리 계층으로 전달할 수 있음을 Mock Device 기준 검증했다.
- 새 앱에서는 기존 `FramePacket` 이름을 그대로 유지할 필요는 없으며, 현재 Architecture의 표준 모델인 `CameraFrame`으로 정규화한다.

### Backend DTOs

- Backend API DTO는 UI model과 분리해야 한다.
- `data/remote/`에 DTO를 두고, repository에서 Domain model 또는 UI model로 mapping한다.
- API Contract Freeze 이후 DTO field/enum을 Presentation 요구 때문에 변경하지 않는다.

### UI 교체 시 보호 영역

8/17 디자인 교체 시 가급적 수정하지 않는 영역:

```text
data/meta/
data/detection/
data/remote/
domain/attention/
API DTO
repository
```

기능 검증용 Material UI와 실제 디자인 UI가 동일한 ViewModel contract를 공유해야 한다.
