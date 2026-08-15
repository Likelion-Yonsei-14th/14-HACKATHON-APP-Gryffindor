# AGENTS.md

## Project

Meta Ray-Ban Gen 2 기반 관광객 쇼핑 지원 Android 앱.

## Before Coding

반드시 다음 문서를 읽는다.

1. `README.md`
2. `docs/REQUIREMENTS.md`
3. `docs/ARCHITECTURE.md`
4. `docs/ATTENTION_POLICY.md`
5. `docs/BACKEND_INTEGRATION.md`
6. `docs/META_DAT_INTEGRATION.md`
7. `docs/DEVELOPMENT_PLAN.md`
8. `docs/TEST_PLAN.md`

## Rules

- Android 앱은 Kotlin + Jetpack Compose를 사용한다.
- 별도 Web UI를 만들지 않는다.
- OpenAI API를 앱에서 직접 호출하지 않는다.
- OpenAI API Key를 앱 resource, BuildConfig, source에 저장하지 않는다.
- Camera stream 전체를 Backend로 지속 업로드하지 않는다.
- Object Detection / Attention Gate는 on-device에서 처리한다.
- 제품 신원 판별은 Backend Recognition이 담당한다.
- Meta SDK type이 Domain/ViewModel까지 새어나가지 않도록 adapter 경계를 둔다.
- Backend DTO와 UI model을 분리한다.
- threshold를 magic number로 흩뿌리지 않는다.
- UI thread에서 frame processing/networking을 수행하지 않는다.
- frame backlog를 무제한으로 만들지 않는다.
- 실제 UI 교체 시 Presentation 외 레이어 변경이 최소화되도록 한다.
- API Contract Freeze 이후 field/enum을 앱 편의로 임의 변경하지 않는다.

## Development Strategy

현재 최우선 기술 체크포인트:

```text
Gen 2
→ DAT frame
→ Object Detection
→ Center / Occupancy / Dwell
→ Trigger
→ Backend /recognize
→ actual productId
→ Product Card
```

이 경로가 성공하기 전에는 후반부 UI polish를 우선하지 않는다.

## Kiro

새 기능 구현 전 Requirements-First Spec을 사용한다.

Steering은 `.kiro/steering/`을 따른다.
