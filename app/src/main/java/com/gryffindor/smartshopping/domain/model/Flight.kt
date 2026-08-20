package com.gryffindor.smartshopping.domain.model

data class Flight(
    val id: String,
    val tripId: String?,
    val departureAirport: String?,
    val arrivalAirport: String?,
    val terminal: String?,
    val flightNumber: String?,
    val departureAt: String?,
    val arrivalAt: String?,
    val airportArrivalAt: String?,
    val createdAt: String
)
