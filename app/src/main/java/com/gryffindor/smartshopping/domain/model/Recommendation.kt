package com.gryffindor.smartshopping.domain.model

data class Recommendation(
    val type: RecommendationType,
    val sourceProductId: String?,
    val product: Product,
    val reasonCode: String?
)

enum class RecommendationType { CROSS_SELL, REMINDER }
