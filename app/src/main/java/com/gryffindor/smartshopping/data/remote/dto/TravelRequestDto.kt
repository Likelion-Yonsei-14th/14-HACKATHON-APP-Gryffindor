package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TravelRequestDto(
    val airportCode: String,
    val flightNumber: String,
    val airportArrivalAt: String
)
