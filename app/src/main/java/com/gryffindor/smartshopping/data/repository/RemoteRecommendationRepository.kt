package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.repository.mapper.toDomain
import com.gryffindor.smartshopping.domain.model.Recommendation
import com.gryffindor.smartshopping.domain.repository.RecommendationRepository

class RemoteRecommendationRepository(
    private val apiService: ShoppingApiService
) : RecommendationRepository {

    override suspend fun getRecommendations(sessionId: String): List<Recommendation> {
        Log.d(TAG, "getRecommendations: GET /sessions/$sessionId/recommendations")
        val response = apiService.getRecommendations(sessionId)
        return response.items.map { it.toDomain() }
    }

    companion object {
        private const val TAG = "RemoteRecommendationRepo"
    }
}
