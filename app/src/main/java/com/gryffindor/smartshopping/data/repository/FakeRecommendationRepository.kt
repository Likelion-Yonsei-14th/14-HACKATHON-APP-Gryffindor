package com.gryffindor.smartshopping.data.repository

import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.Recommendation
import com.gryffindor.smartshopping.domain.model.RecommendationType
import com.gryffindor.smartshopping.domain.repository.RecommendationRepository
import kotlinx.coroutines.delay

class FakeRecommendationRepository : RecommendationRepository {

    override suspend fun getRecommendations(sessionId: String): List<Recommendation> {
        delay(400)
        return listOf(
            Recommendation(
                type = RecommendationType.CROSS_SELL,
                sourceProductId = "mcm_001",
                product = Product("mcm_010", null, "MCM", "Charm Keyring", "accessory", null),
                reasonCode = "SAME_BRAND_DIFFERENT_CATEGORY"
            ),
            Recommendation(
                type = RecommendationType.REMINDER,
                sourceProductId = "mcm_002",
                product = Product("mcm_002", "SKU002", "MCM", "Patricia Crossbody", "bag", null),
                reasonCode = "INTERESTED_NOT_PURCHASED"
            )
        )
    }
}
