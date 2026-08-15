# Android App

스마트글래스 기반 관광객 쇼핑 지원 서비스의 Android 앱 저장소다.

이 앱은 **Meta Ray-Ban Gen 2의 카메라 스트림을 받아 사용자의 관심 행동을 판정하고, 관심 조건을 만족한 제품 이미지만 Backend에 전달해 제품 식별 결과를 받는 역할**을 담당한다.

사용자에게 보이는 모든 UX는 Android 앱에서 처리한다. 별도 Web Frontend는 사용하지 않는다.

## MVP 사용자 흐름

```text
쇼핑 시작
→ Meta Gen2 Camera Stream
→ Object Detection / Tracking
→ 중앙 시야 + 화면 점유율 + 지속 시간 판정
→ 관심 조건 만족 시 제품 영역 crop
→ Backend /recognize
→ 제품 카드 자동 적재
→ 쇼핑 종료
→ 구매 상품 / 관심 상품 선택
→ 출국 정보 입력
→ 환급 체크리스트
→ 공항 추천
```

## 앱 책임

- Meta Device Access Toolkit 연결 및 Camera lifecycle
- Camera frame 수신
- 실시간 Object Detection / Tracking
- Bounding Box 기반 중앙 시야 여부 계산
- Bounding Box / Frame 면적 기반 화면 점유율 계산
- 동일 객체의 중앙 시야 지속 시간 계산
- 관심 행동 trigger 판정
- trigger가 발생한 이미지 crop 및 Backend 전송
- 쇼핑 세션 화면 및 사용자 상태 관리
- 구매/관심 선택 UI
- 여행 정보 / 체크리스트 / 공항 추천 UI
- loading / empty / error 상태 처리

## Backend 책임

앱은 아래 로직을 직접 구현하지 않는다.

- OpenAI API 호출
- 실제 MCM 상품 식별
- 상품 Catalog
- 환급 적용가 계산
- 환율 적용
- 구매/관심 이력 영속화
- 체크리스트 생성
- 추천 생성

## 핵심 원칙

1. OpenAI API Key를 앱에 저장하지 않는다.
2. Camera stream 전체를 Backend로 지속 전송하지 않는다.
3. 관심 조건을 만족한 순간의 crop만 Backend에 전송한다.
4. 제품 신원 판별은 Object Detector가 아니라 Backend Recognition이 담당한다.
5. Object Detector의 Bounding Box와 trackingId는 관심 행동 판정용 신호다.
6. threshold는 하드코딩하지 않고 설정값으로 관리한다.
7. 8월 17일 실제 UI 교체 시 Domain/Data 레이어를 수정하지 않아도 되도록 UI와 로직을 분리한다.
8. Backend API DTO가 동결된 뒤에는 앱에서 임의로 필드명을 변경하지 않는다.

## 문서 읽는 순서

1. `docs/REQUIREMENTS.md`
2. `docs/UX_FLOW.md`
3. `docs/ARCHITECTURE.md`
4. `docs/ATTENTION_POLICY.md`
5. `docs/BACKEND_INTEGRATION.md`
6. `docs/META_DAT_INTEGRATION.md`
7. `docs/DEVELOPMENT_PLAN.md`
8. `docs/TEST_PLAN.md`

Kiro는 `.kiro/steering/`의 프로젝트 지침도 함께 따라야 한다.

## Kiro Spec

첫 구현 전에 `.kiro/specs/`를 수동으로 만들지 않는다.

Kiro에서 Requirements-First Feature Spec을 생성해 이 문서들을 바탕으로:

```text
requirements.md
→ design.md
→ tasks.md
```

순서로 구현 계획을 만든 뒤 작업한다.
