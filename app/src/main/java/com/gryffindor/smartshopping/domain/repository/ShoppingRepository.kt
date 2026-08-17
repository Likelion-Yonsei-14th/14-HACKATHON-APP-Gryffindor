package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.RecognitionResult
import com.gryffindor.smartshopping.domain.model.SessionProduct

interface ShoppingRepository {
    suspend fun getProducts(sessionId: String): List<SessionProduct>
    suspend fun submitReview(
        sessionId: String,
        purchasedProductIds: List<String>,
        interestedProductIds: List<String>
    )
    suspend fun recognize(sessionId: String, candidate: AttentionCandidate): RecognitionResult
}
