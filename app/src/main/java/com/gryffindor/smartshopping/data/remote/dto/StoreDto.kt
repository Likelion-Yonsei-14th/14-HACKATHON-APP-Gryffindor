package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    val brand: String,
    val country: String,
    val city: String? = null,
    val type: String,
    val airportCode: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val terminal: String? = null,
    val openingHours: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class StoreListResponseDto(
    val stores: List<StoreDto>
)
