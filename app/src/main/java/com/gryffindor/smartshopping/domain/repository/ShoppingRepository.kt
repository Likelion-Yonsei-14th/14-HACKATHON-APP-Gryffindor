package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.SessionProduct

interface ShoppingRepository {
    suspend fun getProducts(sessionId: String): List<SessionProduct>
    suspend fun submitReview(
        sessionId: String,
        purchasedProductIds: List<String>,
        interestedProductIds: List<String>
    )
}
