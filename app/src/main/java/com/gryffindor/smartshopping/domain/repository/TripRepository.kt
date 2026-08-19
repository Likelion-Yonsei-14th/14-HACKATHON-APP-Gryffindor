package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.HotelStay
import com.gryffindor.smartshopping.domain.model.StoreWishlistProduct
import com.gryffindor.smartshopping.domain.model.Trip
import com.gryffindor.smartshopping.domain.model.TripDetail
import com.gryffindor.smartshopping.domain.model.TripFeed
import com.gryffindor.smartshopping.domain.model.VisitReservation

interface TripRepository {

    suspend fun createTrip(
        title: String,
        destinationCity: String? = null,
        destinationCountry: String? = null,
        startsAt: String? = null,
        endsAt: String? = null
    ): Trip

    suspend fun getTrips(): List<Trip>

    suspend fun getTrip(tripId: String): TripDetail

    suspend fun updateTrip(
        tripId: String,
        title: String? = null,
        destinationCity: String? = null,
        destinationCountry: String? = null,
        startsAt: String? = null,
        endsAt: String? = null
    ): Trip

    suspend fun upsertHotel(
        tripId: String,
        name: String,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        checkInAt: String? = null,
        checkOutAt: String? = null
    ): HotelStay

    suspend fun getHotel(tripId: String): HotelStay

    suspend fun getTripFeed(tripId: String): TripFeed

    suspend fun getStoreWishlistProducts(storeId: String): List<StoreWishlistProduct>

    suspend fun createVisitReservation(
        tripId: String,
        storeId: String,
        scheduledAt: String,
        productIds: List<String> = emptyList()
    ): VisitReservation

    suspend fun getVisitReservations(tripId: String): List<VisitReservation>

    suspend fun cancelVisitReservation(reservationId: String)
}
