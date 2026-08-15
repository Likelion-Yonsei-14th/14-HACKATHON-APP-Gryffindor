package com.gryffindor.smartshopping.domain.repository

interface TravelRepository {
    suspend fun submitTravel(
        sessionId: String,
        airportCode: String,
        flightNumber: String,
        airportArrivalAt: String
    )
}
