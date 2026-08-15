package com.gryffindor.smartshopping.data.repository

import com.gryffindor.smartshopping.domain.repository.TravelRepository
import kotlinx.coroutines.delay

class FakeTravelRepository : TravelRepository {

    override suspend fun submitTravel(
        sessionId: String,
        airportCode: String,
        flightNumber: String,
        airportArrivalAt: String
    ) {
        delay(300)
    }
}
