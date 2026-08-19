package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.Flight
import com.gryffindor.smartshopping.domain.model.MyPage
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.Purchase
import com.gryffindor.smartshopping.domain.model.Receipt

interface PersonalizationRepository {

    suspend fun getMyPage(): MyPage

    suspend fun getWishlist(): List<Product>

    suspend fun addWishlist(productId: String): Product

    suspend fun deleteWishlist(productId: String)

    suspend fun analyzeReceipt(imageBytes: ByteArray): Receipt

    suspend fun getPurchases(): List<Purchase>

    suspend fun analyzeFlight(imageBytes: ByteArray, tripId: String? = null): Flight

    suspend fun updateFlight(
        flightId: String,
        departureAirport: String? = null,
        arrivalAirport: String? = null,
        terminal: String? = null,
        flightNumber: String? = null,
        departureAt: String? = null,
        arrivalAt: String? = null,
        airportArrivalAt: String? = null
    ): Flight
}
