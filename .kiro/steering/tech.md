# Technology Decisions

## Confirmed Decisions

다음 항목은 프로젝트 문서에서 확정된 기술 선택이다.

### Language & UI

- **Kotlin** — 앱 전체 언어
- **Jetpack Compose** — UI framework
- **Material 3** — 8/16까지 기능 검증용 기본 UI, 8/17 실제 디자인 교체

### Async & State

- **Coroutine / Flow** — 비동기 처리 및 상태 관리 기반

### Navigation

- Jetpack Compose Navigation 사용 (화면 6개: Home, Shopping, Review, Travel, Checklist, Recommendation)

### Hardware Integration

- **Meta Device Access Toolkit (DAT)** — Gen 2 카메라 스트림 수신
- 기존 Stage 0 검증 코드를 이식하여 DeviceSession, Camera lifecycle, frame callback 구현

### Validated Meta DAT Baseline

기존 Stage 0 검증 프로젝트에서 다음 사항은 Mock Device 기준 검증 완료되었다.

- Meta Wearables DAT 0.9.0 Developer Preview 환경 사용
- `mwdat-core`, `mwdat-camera`, debug `mwdat-mockdevice` dependency resolve
- DeviceSession 생성 / start / stop / recreate
- Camera capability 추가 및 제거
- Camera Stream start / stop / restart
- `videoStream.collect()`를 통한 반복 VideoFrame 수신
- VideoFrame의 buffer / width / height / presentationTimeUs 접근
- SDK frame 데이터를 앱 소유 메모리로 복사 가능
- Meta SDK type을 adapter 내부에 격리 가능
- MEDIUM / 7 FPS 및 MEDIUM / 15 FPS Mock stream 동작

새 APP의 A1은 이를 처음부터 재검증하는 것이 아니라,
검증된 Camera 코드를 새 앱의 MetaCameraSource / CameraFrame 경계로 이식하는 단계다.

실제 Gen 2에서 다음 항목은 아직 검증되지 않았다.

- 실제 기기 Camera Stream
- 벗음 / 재착용 lifecycle
- 연결 중단 / 재연결
- 장시간 streaming

### Backend Communication

- **REST API** (JSON, camelCase, ISO 8601 UTC)
- Base path: `/api/v1`
- multipart/form-data로 Recognition 이미지 전송
- Backend 저장소의 `docs/API_CONTRACT.md`가 API source of truth
- 8/16 Contract Freeze 이후 endpoint/field/enum 임의 변경 금지

### Object Detection

- On-device 실시간 Object Detection / Tracking 수행
- Object Detector는 위치/크기/지속성만 판단; 실제 상품 ID 판별은 Backend 책임

### Configuration

- Attention threshold (occupancyThreshold, dwellThresholdMs)는 하드코딩하지 않고 설정값으로 관리
- 초기 실험값: occupancy=0.20, dwell=1500ms

### Security

- OpenAI API Key를 앱 resource, BuildConfig, source에 저장하지 않음
- OpenAI API를 앱에서 직접 호출하지 않음
- Camera stream 전체를 Backend로 지속 전송하지 않음

### Architecture Constraints (from AGENTS.md)

- Meta SDK type이 Domain/ViewModel까지 새어나가지 않도록 adapter 경계를 둠
- Backend DTO와 UI model을 분리함
- UI thread에서 frame processing/networking을 수행하지 않음
- frame backlog를 무제한으로 만들지 않음
- 실제 UI 교체 시 Presentation 외 레이어 변경 최소화
- API Contract Freeze 이후 field/enum을 앱 편의로 임의 변경하지 않음

---

## Open / Undecided

다음 항목은 문서에서 아직 확정되지 않았거나 명시적으로 결정이 보류된 사항이다.

| 영역 | 상태 | 비고 |
|------|------|------|
| DI framework (Hilt 등) | "기본 DI 방식 결정" 으로만 언급 | A0에서 결정 예정 |
| HTTP client library (Retrofit 등) | 미확정 | network client 골격으로만 언급 |
| Object Detection model / library | 미확정 | ML Kit, TensorFlow Lite 등 구체적 선택 없음 |
| Image format for Recognition crop | JPEG 또는 PNG | 둘 다 허용으로 언급, 단일 선택 미확정 |
| Local persistence (Room 등) | 미확정 | P0에서 체크리스트는 앱 로컬 상태로 관리 |
| trackingId 불안정 시 fallback 방식 | P1로 보류 | bbox 위치/크기 시간적 유사성 이용 가능성 언급 |
| 정확한 Center ROI 크기 | config로 관리 | 구체적 비율 미확정 |
| frame throttling 전략 | 미확정 | 최신 frame 우선 또는 throttling 언급 |
| Debug build 구분 방식 | 미확정 | debug 영역 표시만 언급 |
