package com.gryffindor.smartshopping.feature.home

import com.gryffindor.smartshopping.domain.model.ChecklistItem

data class RefundSummary(
    val totalPurchaseAmountKrw: Long,
    val totalRefundAmountKrw: Long,
    val totalRefundAmountForeign: String, // 예: "¥1,095" — TODO: 실제 환율 연동
    val completedCount: Int,
    val completedAmountKrw: Long,
    val inProgressCount: Int,
    val inProgressAmountKrw: Long,
)

enum class RefundStatus { COMPLETED, IN_PROGRESS }

data class PurchasedItem(
    val id: String,
    val name: String,
    val store: String,
    val priceKrw: Long,
    val refundAmountKrw: Long,
    val status: RefundStatus,
)

data class BrandFilter(
    val id: String,
    val name: String,
    // val iconRes: Int, // TODO: 브랜드 로고 리소스 추가되면 연결
)

data class LooketProduct(
    val id: String,
    val name: String,
    val store: String,
    val priceKrw: Long,
    val brandId: String,
    val statusLabel: String, // "구매" 또는 "관심"
)

data class RecommendedProduct(
    val id: String,
    val brandName: String,
    val title: String,
    val productName: String,
    val location: String,
)

// TODO: 전부 실제 API/ViewModel 데이터로 교체
object HomeMockData {
    val refundSummary = RefundSummary(
        totalPurchaseAmountKrw = 3_400_000,
        totalRefundAmountKrw = 230_000,
        totalRefundAmountForeign = "¥1,095",
        completedCount = 1,
        completedAmountKrw = 150_000,
        inProgressCount = 1,
        inProgressAmountKrw = 80_000,
    )

    val purchasedItems = listOf(
        PurchasedItem("1", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, 76_000, RefundStatus.COMPLETED),
        PurchasedItem("2", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, 76_000, RefundStatus.COMPLETED),
        PurchasedItem("3", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, 76_000, RefundStatus.IN_PROGRESS),
    )

    // 요구사항: 오늘 체크리스트 중 완료 안 된 것만 홈에 보여줘야 함
    // 실제 ChecklistViewModel/ChecklistUiState와 같은 구조(items + checkedIds)로 맞춰둠
    // TODO: HomeViewModel에 ChecklistRepository 연결되면 이 mock 대신 실제 sessionId로 로드
    val checklistItems = listOf(
        ChecklistItem(id = "1", title = "세관 신고하기", description = "출국 전 세관에서 구매물품 신고", required = true),
        ChecklistItem(id = "2", title = "미환급 물품 환급받기", description = "면세구역 내 환급 카운터에서 수령", required = true),
    )
    val checklistCheckedIds = setOf<String>() // 아직 아무것도 체크 안 된 상태 mock

    val recommendedProducts = listOf(
        RecommendedProduct("1", "MCM", "당신만을 위한 오늘의 셀렉션", "Aren 비세토스 E/W 숄더백", "Terminal1 3F Gate 130-150"),
        RecommendedProduct("2", "MCM", "당신만을 위한 오늘의 셀렉션", "Aren 비세토스 E/W 숄더백", "Terminal1 3F Gate 130-150"),
        RecommendedProduct("3", "MCM", "당신만을 위한 오늘의 셀렉션", "Aren 비세토스 E/W 숄더백", "Terminal1 3F Gate 130-150"),
    )

    val brandFilters = listOf(
        BrandFilter("mcm", "MCM"),
        BrandFilter("other", "Brand"),
    )

    val looketProducts = listOf(
        LooketProduct("1", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, "mcm", "구매"),
        LooketProduct("2", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, "mcm", "구매"),
        LooketProduct("3", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, "other", "관심"),
        LooketProduct("4", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", 1_090_000, "other", "관심"),
    )
}