package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationsResponseDto(
    val airportCode: String,
    val items: List<RecommendationItemDto>,
    val mode: String? = null
)
