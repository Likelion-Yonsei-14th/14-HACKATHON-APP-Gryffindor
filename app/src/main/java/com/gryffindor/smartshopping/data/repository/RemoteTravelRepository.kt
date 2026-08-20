package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.remote.dto.TravelRequestDto
import com.gryffindor.smartshopping.domain.repository.TravelRepository

class RemoteTravelRepository(
    private val apiService: ShoppingApiService
) : TravelRepository {

    override suspend fun submitTravel(
        sessionId: String,
        airportCode: String,
        flightNumber: String,
        airportArrivalAt: String
    ) {
        Log.d(TAG, "submitTravel: PUT /sessions/$sessionId/travel")
        apiService.submitTravel(
            sessionId = sessionId,
            request = TravelRequestDto(
                airportCode = airportCode,
                flightNumber = flightNumber,
                airportArrivalAt = airportArrivalAt
            )
        )
    }

    companion object {
        private const val TAG = "RemoteTravelRepo"
    }
}
