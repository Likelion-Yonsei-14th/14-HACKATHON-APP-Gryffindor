package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TravelResponseDto(
    val sessionId: String,
    val airportCode: String,
    val flightNumber: String,
    val airportArrivalAt: String
)
