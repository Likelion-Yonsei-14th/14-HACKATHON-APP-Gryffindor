package com.gryffindor.smartshopping.domain.model

/**
 * Domain model representing a physical store where shopping takes place.
 */
data class Store(
    val id: String,
    val name: String,
    val brand: String,
    val country: String,
    val city: String?,
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
