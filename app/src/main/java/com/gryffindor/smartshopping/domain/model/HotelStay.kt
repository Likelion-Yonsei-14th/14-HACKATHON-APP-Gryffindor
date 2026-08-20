package com.gryffindor.smartshopping.domain.model

data class HotelStay(
    val id: String,
    val tripId: String,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val checkInAt: String?,
    val checkOutAt: String?,
    val createdAt: String,
    val updatedAt: String
)
