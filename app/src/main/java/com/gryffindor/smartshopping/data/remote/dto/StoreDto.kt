package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    val brand: String,
    val country: String,
    val city: String,
    val type: String,
    val airportCode: String? = null
)

@Serializable
data class StoreListResponseDto(
    val stores: List<StoreDto>
)
