# Product Context

## Purpose

Meta Ray-Ban Gen 2 스마트글래스를 착용한 관광객이 매장을 둘러보면, Android 앱이 카메라 영상에서 관심 있게 바라보는 제품을 자동 감지하여 상품 정보와 환급 적용 가격을 실시간으로 제공하는 쇼핑 지원 서비스다.

모든 UX는 Android 앱에서 처리하며, 별도 Web Frontend는 없다.

---

## User Flow (MVP)

```text
쇼핑 시작
→ Meta Gen 2 Camera Stream 수신
→ Object Detection / Tracking (on-device)
→ 중앙 시야 + 화면 점유율 + 지속 시간으로 관심 행동 판정
→ 관심 조건 만족 시 제품 영역 crop
→ Backend /recognize 호출
→ 제품 카드 자동 적재 (정가 / 환급 적용가 / 자국 통화 환산가)
→ 쇼핑 종료
→ 구매 상품 / 관심 상품 선택 (Shopping Review)
→ 출국 정보 입력 (Travel Input)
→ 환급 체크리스트 (Refund Checklist)
→ 공항 추천 (Airport Recommendation)
```

화면 트리:

```text
Home → Shopping → Shopping Review → Travel Input → Refund Checklist → Airport Recommendation
```

---

## P0 Scope

1. 쇼핑 세션 생성/종료 (Backend Session lifecycle)
2. Meta Device Access Toolkit으로 Gen 2 Camera Stream 수신
3. 실시간 Object Detection / Tracking (on-device)
4. 관심 행동 판정 (Center ROI + Occupancy + Dwell)
5. 관심 trigger 시 crop → Backend /recognize 요청
6. MATCHED 결과로 상품 카드 자동 표시 (중복 방지)
7. Shopping Review: 구매/관심 선택 → Backend 저장
8. Travel Input: 출국 공항, 항공편, 도착 예정 시간
9. Refund Checklist: Backend 반환 항목 표시 (로컬 체크 상태)
10. Airport Recommendation: CROSS_SELL / REMINDER 구분 표시
11. 모든 네트워크 화면: Loading / Success / Empty / Recoverable Error / Fatal Error 상태 처리

---

## Excluded Scope

- Eye Tracking
- Hand Tracking으로 제품을 들었는지 정밀 판정
- 스마트글래스 자체 디스플레이 UI
- 앱에서 OpenAI 직접 호출
- 결제 처리
- 실제 세금 환급 신청
- 실시간 공항 면세점 재고 보장
- 로그인 / 회원가입
- 고도화 Recommendation ML
- 영수증 OCR (P1)
- Push notification (P1)
- 위치 기반 공항 진입 감지 (P1)
- 관심 threshold calibration UI (P1)

---

## MVP Success Criteria

다음 흐름이 실제 Gen 2 기기에서 한 번 끝까지 동작하면 기능 MVP 성공:

```text
Gen 2 연결 → 쇼핑 시작 → 제품 관찰 → Object Detection → 관심 trigger
→ Backend Recognition → productId 반환 → 가격 카드 자동 등장
→ 쇼핑 종료 → 구매/관심 선택 → 출국 정보 → 체크리스트 → 추천
```
