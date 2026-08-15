# UX Flow

## 1. 화면 트리

```text
Home
└─ Shopping
   └─ Shopping Review
      └─ Travel Input
         └─ Refund Checklist
            └─ Airport Recommendation
```

UI는 8월 16일까지 기능 검증용 Material 3 기본 UI로 구현한다.
8월 17일 UI 담당자가 실제 디자인으로 교체하더라도 ViewModel / Repository / API DTO를 변경하지 않는 것을 목표로 한다.

---

# 2. Home

## 목적

쇼핑 세션을 시작하고 스마트글래스 연결 상태를 확인한다.

## 최소 요소

- 앱/서비스 로고 또는 제목
- Gen 2 연결 상태
- 쇼핑 시작 버튼
- 오류 상태 및 재연결 안내

## 동작

```text
쇼핑 시작
→ POST /sessions
→ Session ACTIVE
→ Camera stream start
→ Shopping 화면
```

---

# 3. Shopping

## 목적

스마트글래스가 자동으로 인식한 관심 제품과 관광객 기준 가격 정보를 실시간으로 보여준다.

## 최소 요소

- 쇼핑 진행 상태
- Camera / Backend 상태
- 인식된 상품 카드 목록
- 쇼핑 종료 버튼

## Product Card

```text
상품 이미지
브랜드 / 상품명

정가
↓
환급 적용 예상가
↓
자국 통화 환산가

즉시환급 가능 / 확인 필요
```

새 상품은 별도 새로고침 없이 자동으로 목록에 나타나야 한다.

### Debug build 전용

기능 검증 중에는 다음 값을 작은 debug 영역에 표시할 수 있다.

- trackingId
- center 여부
- occupancyRatio
- dwellMs
- recognition status

실제 UI 교체 시 debug 정보는 숨긴다.

---

# 4. Shopping Review

## 목적

자동으로 쌓인 관찰 목록을 실제 구매와 명시적 관심으로 정리한다.

## 최소 요소

- 관찰 상품 전체 목록
- 구매 여부 선택
- 관심 여부 선택
- 다음 버튼
- 영수증 촬영 버튼은 P1이면 disabled 또는 placeholder 허용

## 규칙

- 구매된 상품은 관심 목록과 중복 저장하지 않는다.
- 사용자가 선택을 수정할 수 있어야 한다.

---

# 5. Travel Input

## 최소 입력

- 출국 공항
- 항공편 번호
- 공항 도착 예정 시간

## 동작

```text
입력
→ PUT /travel
→ Checklist 화면
```

---

# 6. Refund Checklist

Backend가 반환한 체크리스트를 표시한다.

## 최소 요소

- 체크 항목 title
- 설명
- 필수 여부
- 로컬 완료 체크 상태

---

# 7. Airport Recommendation

## 목적

구매/관심 이력을 공항 쇼핑 경험으로 연결한다.

## 최소 요소

추천 카드마다:

- 추천 유형
- 상품 이미지
- 브랜드 / 상품명
- 간단한 추천 이유

## 유형 표시

- `CROSS_SELL`: 이미 구매한 상품과 연관된 추천
- `REMINDER`: 매장에서 관심 있었지만 구매하지 않은 상품 재추천

---

# 8. 공통 상태

모든 네트워크 화면은 최소 다음 상태를 표현한다.

```text
Loading
Success
Empty
Recoverable Error
Fatal / Blocking Error
```

OpenAI 또는 Backend 실패가 앱 crash로 이어져서는 안 된다.
