package com.gryffindor.smartshopping.feature.home

import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.domain.model.ChecklistItem

data class RefundSummary(
    val totalPurchaseAmountKrw: Long,
    val totalRefundAmountKrw: Long,
    val completedCount: Int,
    val completedAmountKrw: Long,
    val inProgressCount: Int,
    val inProgressAmountKrw: Long,
    val totalPurchaseAmountUsd: String? = null,
    val totalRefundAmountUsd: String? = null,
)

enum class RefundStatus { COMPLETED, IN_PROGRESS }

data class PurchasedItem(
    val id: String,
    val name: String,
    val store: String,
    val priceKrw: Long,
    val refundAmountKrw: Long,
    val status: RefundStatus,
    val imageUrl: String? = null,
    val priceUsd: String? = null,
    val refundAmountUsd: String? = null,
)

data class BrandFilter(
    val id: String,
    val name: String,
    val iconRes: Int,
)

data class LooketProduct(
    val id: String,
    val name: String,
    val store: String,
    val priceKrw: Long,
    val brandId: String,
    val statusLabel: String, // "구매" 또는 "관심"
    val imageUrl: String? = null,
)

data class RecommendedProduct(
    val id: String,
    val brandName: String,
    val title: String,
    val productName: String,
    val location: String,
    val imageUrl: String? = null,
    val storeId: String? = null,
    val storeName: String? = null,
)

/**
 * Production Demo Data — Backend seed catalog 기반 실제 상품 데이터.
 * Backend products.seed.json과 동일한 productId / brand / name / priceKrw / imageUrl 사용.
 * 이 데이터는 Home에서 API 없이 "실제 카탈로그" 느낌을 주기 위한 로컬 데이터이다.
 */
object HomeProductionData {
    val refundSummary = RefundSummary(
        totalPurchaseAmountKrw = 3_400_000,
        totalRefundAmountKrw = 230_000,
        completedCount = 1,
        completedAmountKrw = 150_000,
        inProgressCount = 1,
        inProgressAmountKrw = 80_000,
    )

    val purchasedItems = listOf(
        PurchasedItem(
            id = "mcm_aren_ew_shoulder_s_001",
            name = "Aren 비세토스 E/W 숄더백 S",
            store = "MCM 롯데면세점 명동본점",
            priceKrw = 1_090_000,
            refundAmountKrw = 76_000,
            status = RefundStatus.COMPLETED,
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWSGSTA02CO001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        PurchasedItem(
            id = "mcm_new_liz_shopper_m_001",
            name = "New Liz 비세토스 숄더백 M",
            store = "MCM 현대면세점 인천공항 T1",
            priceKrw = 1_490_000,
            refundAmountKrw = 104_000,
            status = RefundStatus.COMPLETED,
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWPFSLR03CO001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        PurchasedItem(
            id = "mcm_eau_de_parfum_50_001",
            name = "MCM 오 드 퍼퓸 50ml",
            store = "MCM 롯데면세점 명동본점",
            priceKrw = 118_000,
            refundAmountKrw = 8_000,
            status = RefundStatus.IN_PROGRESS,
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MPFBSMM02CO001_02/mcm-cognac-50ml?\$w1000\$&fmt=auto&qlt=default",
        ),
    )

    val checklistItems = listOf(
        ChecklistItem(id = "1", title = "세관 신고하기", description = "출국 전 세관에서 구매물품 신고", required = true),
        ChecklistItem(id = "2", title = "미환급 물품 환급받기", description = "면세구역 내 환급 카운터에서 수령", required = true),
    )
    val checklistCheckedIds = setOf<String>()

    val recommendedProducts = listOf(
        RecommendedProduct(
            id = "mcm_diamond_shoulder_mini_001",
            brandName = "MCM",
            title = "당신만을 위한 오늘의 셀렉션",
            productName = "Diamond 비세토스 레더 믹스 숄더백 Mini",
            location = "Terminal1 3F Gate 28-30",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWRGAAK01BK001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        RecommendedProduct(
            id = "mcm_pina_studded_tote_m_001",
            brandName = "MCM",
            title = "당신만을 위한 오늘의 셀렉션",
            productName = "Pina 스터디드 토트백 M",
            location = "Terminal1 3F Gate 28-30",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWTGATA02CO001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        RecommendedProduct(
            id = "mcm_cozy_cat_100_001",
            brandName = "MCM",
            title = "당신만을 위한 오늘의 셀렉션",
            productName = "Cozy Cat 오 드 퍼퓸 100ml",
            location = "Terminal1 3F Gate 28-30",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MPFGAMM02CO001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
    )

    val brandFilters = listOf(
        BrandFilter("mcm", "MCM", R.drawable.logo_mcm),
        BrandFilter("mcm2", "MCM2", R.drawable.logo_mcm2),
    )

    val looketProducts = listOf(
        LooketProduct(
            id = "mcm_aren_ew_shoulder_s_001",
            name = "Aren 비세토스 E/W 숄더백 S",
            store = "MCM 롯데면세점 명동본점",
            priceKrw = 1_090_000,
            brandId = "mcm",
            statusLabel = "구매",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWSGSTA02CO001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        LooketProduct(
            id = "mcm_aren_ew_lotus_pink_s_001",
            name = "Aren E/W 숄더백 Lotus Pink S",
            store = "MCM 현대면세점 인천공항 T1",
            priceKrw = 1_490_000,
            brandId = "mcm",
            statusLabel = "구매",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWSGSTA01QA001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        LooketProduct(
            id = "mcm_mighty_bear_100_001",
            name = "Mighty Bear 오 드 퍼퓸 100ml",
            store = "MCM 롯데면세점 명동본점",
            priceKrw = 210_000,
            brandId = "mcm2",
            statusLabel = "관심",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MPFFSMM04CO001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
        LooketProduct(
            id = "mcm_diamond_shoulder_mini_001",
            name = "Diamond 비세토스 레더 믹스 숄더백 Mini",
            store = "MCM 현대면세점 인천공항 T1",
            priceKrw = 930_000,
            brandId = "mcm2",
            statusLabel = "관심",
            imageUrl = "https://images.mcmworldwide.com/i/mcmworldwide/MWRGAAK01BK001_01?\$w1000\$&fmt=auto&qlt=default",
        ),
    )
}
