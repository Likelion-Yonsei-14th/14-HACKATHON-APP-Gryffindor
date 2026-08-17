package com.gryffindor.smartshopping.data.repository

import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.Pricing
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.PurchaseState
import com.gryffindor.smartshopping.domain.model.RecognitionResult
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import kotlinx.coroutines.delay

class FakeShoppingRepository : ShoppingRepository {

    private val fakeProducts = listOf(
        SessionProduct(
            product = Product("mcm_001", "SKU001", "MCM", "Visetos Backpack", "bag", null),
            pricing = Pricing(
                retailPriceKrw = 1090000,
                estimatedRefundKrw = 60000,
                estimatedRefundPriceKrw = 1030000,
                convertedRetailPrice = "5513.86",
                convertedEstimatedRefund = "384.45",
                convertedEstimatedRefundPrice = "5129.41",
                convertedAmount = "5129.41",
                convertedCurrency = "CNY",
                instantRefundEligible = true,
                pricingMode = "MOCK"
            ),
            purchaseState = PurchaseState.UNSET,
            interested = false
        ),
        SessionProduct(
            product = Product("mcm_002", "SKU002", "MCM", "Patricia Crossbody", "bag", null),
            pricing = Pricing(
                retailPriceKrw = 890000,
                estimatedRefundKrw = 49000,
                estimatedRefundPriceKrw = 841000,
                convertedRetailPrice = "4502.53",
                convertedEstimatedRefund = "248.04",
                convertedEstimatedRefundPrice = "4254.49",
                convertedAmount = "4254.49",
                convertedCurrency = "CNY",
                instantRefundEligible = false,
                pricingMode = "MOCK"
            ),
            purchaseState = PurchaseState.UNSET,
            interested = true
        )
    )

    override suspend fun getProducts(sessionId: String): List<SessionProduct> {
        delay(500)
        return fakeProducts
    }

    override suspend fun submitReview(
        sessionId: String,
        purchasedProductIds: List<String>,
        interestedProductIds: List<String>
    ) {
        delay(300)
    }

    override suspend fun recognize(
        sessionId: String,
        candidate: AttentionCandidate
    ): RecognitionResult {
        delay(400)
        return RecognitionResult.Unknown
    }
}
