package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewRequestDto(
    val purchasedProductIds: List<String>,
    val interestedProductIds: List<String>
)
