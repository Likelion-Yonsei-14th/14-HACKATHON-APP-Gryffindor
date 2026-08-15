package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationItemDto(
    val type: String,
    val sourceProductId: String? = null,
    val product: ProductDto,
    val reasonCode: String? = null
)
