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
            pricing = Pricing(1090000, 60000, 1030000, "5210.35", "CNY", true, "MOCK"),
            purchaseState = PurchaseState.UNSET,
            interested = false
        ),
        SessionProduct(
            product = Product("mcm_002", "SKU002", "MCM", "Patricia Crossbody", "bag", null),
            pricing = Pricing(890000, 49000, 841000, "4254.80", "CNY", false, "MOCK"),
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
