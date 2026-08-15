package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.Recommendation

interface RecommendationRepository {
    suspend fun getRecommendations(sessionId: String): List<Recommendation>
}
