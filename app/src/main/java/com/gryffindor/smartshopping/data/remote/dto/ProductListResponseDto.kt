package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductListResponseDto(
    val sessionId: String,
    val items: List<ProductListItemDto>
)
