# Backend Integration

## 1. Source of Truth

Backend 저장소의 `docs/API_CONTRACT.md`가 최종 API source of truth다.

이 문서는 Android 구현을 위한 snapshot이다.

8월 16일 Contract Freeze 이후 앱과 Backend는 endpoint / field / enum을 임의 변경하지 않는다.

---

## 2. Base

```text
Base Path: /api/v1
JSON: camelCase
Time: ISO 8601 UTC
```

OpenAI API Key는 Backend에만 둔다.

---

## 3. Session

### Create

```http
POST /api/v1/sessions
```

```json
{
  "currency": "CNY"
}
```

Response:

```json
{
  "sessionId": "uuid",
  "status": "ACTIVE",
  "currency": "CNY",
  "startedAt": "2026-08-15T13:30:00Z"
}
```

### Complete

```http
POST /api/v1/sessions/{sessionId}/complete
```

---

## 4. Recognition

```http
POST /api/v1/sessions/{sessionId}/recognize
Content-Type: multipart/form-data
```

Fields:

```text
image
capturedAt
triggerType
occupancyRatio
dwellMs
trackingId (optional)
```

`triggerType`:

```text
OCCUPANCY
DWELL
OCCUPANCY_AND_DWELL
```

### MATCHED

```json
{
  "recognitionStatus": "MATCHED",
  "isNew": true,
  "observedProduct": {
    "product": {
      "productId": "mcm_001",
      "sku": "SKU001",
      "brand": "MCM",
      "name": "Product Name",
      "category": "bag",
      "imageUrl": "https://example.com/product.jpg"
    },
    "pricing": {
      "retailPriceKrw": 1090000,
      "estimatedRefundKrw": 60000,
      "estimatedRefundPriceKrw": 1030000,
      "convertedAmount": "5210.35",
      "convertedCurrency": "CNY",
      "instantRefundEligible": true,
      "pricingMode": "MOCK"
    },
    "observation": {
      "triggerType": "OCCUPANCY_AND_DWELL",
      "occupancyRatio": 0.24,
      "dwellMs": 1500,
      "firstObservedAt": "2026-08-15T13:35:00Z",
      "lastObservedAt": "2026-08-15T13:35:00Z"
    }
  }
}
```

### AMBIGUOUS

```json
{
  "recognitionStatus": "AMBIGUOUS",
  "candidateProductIds": ["mcm_001", "mcm_002"]
}
```

### UNKNOWN

```json
{
  "recognitionStatus": "UNKNOWN"
}
```

`AMBIGUOUS`와 `UNKNOWN`은 앱 상품 카드에 자동 추가하지 않는다.

---

## 5. Observed Products

```http
GET /api/v1/sessions/{sessionId}/products
```

앱은 필요 시 Backend의 현재 세션 상품 목록과 local UI state를 재동기화할 수 있다.

---

## 6. Review

```http
PUT /api/v1/sessions/{sessionId}/review
```

```json
{
  "purchasedProductIds": ["mcm_001"],
  "interestedProductIds": ["mcm_002"]
}
```

구매와 관심에 동시에 포함된 상품은 Backend에서 PURCHASED가 우선한다.

---

## 7. Travel

```http
PUT /api/v1/sessions/{sessionId}/travel
```

```json
{
  "airportCode": "ICN",
  "flightNumber": "KE123",
  "airportArrivalAt": "2026-08-18T01:30:00Z"
}
```

---

## 8. Refund Checklist

```http
GET /api/v1/sessions/{sessionId}/refund-checklist
```

```json
{
  "items": [
    {
      "id": "keep-receipt",
      "title": "구매 영수증을 준비하세요",
      "description": "환급 확인을 위해 구매 증빙을 준비합니다.",
      "required": true
    }
  ],
  "mode": "MOCK"
}
```

---

## 9. Recommendations

```http
GET /api/v1/sessions/{sessionId}/recommendations
```

추천 유형:

```text
CROSS_SELL
REMINDER
```

---

## 10. Error Handling

공통 형태:

```json
{
  "error": {
    "code": "SESSION_NOT_ACTIVE",
    "message": "Recognition is allowed only for an active shopping session."
  }
}
```

대표 code:

```text
SESSION_NOT_FOUND
SESSION_NOT_ACTIVE
INVALID_IMAGE
RECOGNITION_PROVIDER_ERROR
PRODUCT_NOT_FOUND
INVALID_PRODUCT_SELECTION
TRAVEL_PLAN_REQUIRED
AIRPORT_CATALOG_UNAVAILABLE
```

앱은 HTTP status만 보고 사용자 메시지를 결정하지 말고 `error.code`를 mapping한다.

---

## 11. 개발 단계별 연결

### Backend B0

앱 A0 시작 가능:

- DTO
- Retrofit interface
- fake repository
- navigation/UI skeleton

### Backend B1 Mock Vertical Slice

앱 실제 API 연결 시작 가능.

### Backend B2 OpenAI Recognition + App A2 Attention 성공

첫 핵심 통합:

```text
Gen 2
→ trigger
→ /recognize
→ actual productId
→ product card
```

### Backend B4

Review / Travel / Checklist / Recommendation 화면을 실제 API에 연결한다.

### Backend B5 Contract Freeze

UI 담당자가 실제 디자인 교체를 시작한다.
