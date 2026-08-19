package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.PersonalizationApiService
import com.gryffindor.smartshopping.data.remote.dto.HotelStayRequestDto
import com.gryffindor.smartshopping.data.remote.dto.TripCreateRequestDto
import com.gryffindor.smartshopping.data.remote.dto.TripPatchRequestDto
import com.gryffindor.smartshopping.data.remote.dto.VisitReservationCreateRequestDto
import com.gryffindor.smartshopping.data.repository.mapper.toDomain
import com.gryffindor.smartshopping.domain.model.HotelStay
import com.gryffindor.smartshopping.domain.model.RefundChecklist
import com.gryffindor.smartshopping.domain.model.StoreWishlistProduct
import com.gryffindor.smartshopping.domain.model.Trip
import com.gryffindor.smartshopping.domain.model.TripDetail
import com.gryffindor.smartshopping.domain.model.TripFeed
import com.gryffindor.smartshopping.domain.model.VisitReservation
import com.gryffindor.smartshopping.domain.repository.TripRepository

/**
 * Real TripRepository — connects to B5/B6 Backend APIs.
 * Handles Trip CRUD, Hotel, Feed, Store Wishlist, and Visit Reservations.
 */
class RemoteTripRepository(
    private val apiService: PersonalizationApiService
) : TripRepository {

    override suspend fun createTrip(
        title: String,
        destinationCity: String?,
        destinationCountry: String?,
        startsAt: String?,
        endsAt: String?
    ): Trip {
        Log.d(TAG, "createTrip: POST /api/v1/me/trips title=$title city=$destinationCity country=$destinationCountry startsAt=$startsAt endsAt=$endsAt")
        val request = TripCreateRequestDto(
            title = title,
            destinationCity = destinationCity,
            destinationCountry = destinationCountry,
            startsAt = startsAt,
            endsAt = endsAt
        )
        return apiService.createTrip(request).toDomain()
    }

    override suspend fun getTrips(): List<Trip> {
        Log.d(TAG, "getTrips: GET /api/v1/me/trips")
        return apiService.getTrips().trips.map { it.toDomain() }
    }

    override suspend fun getTrip(tripId: String): TripDetail {
        Log.d(TAG, "getTrip: GET /api/v1/me/trips/$tripId")
        return apiService.getTrip(tripId).toDomain()
    }

    override suspend fun updateTrip(
        tripId: String,
        title: String?,
        destinationCity: String?,
        destinationCountry: String?,
        startsAt: String?,
        endsAt: String?
    ): Trip {
        Log.d(TAG, "updateTrip: PATCH /api/v1/me/trips/$tripId")
        val request = TripPatchRequestDto(
            title = title,
            destinationCity = destinationCity,
            destinationCountry = destinationCountry,
            startsAt = startsAt,
            endsAt = endsAt
        )
        return apiService.updateTrip(tripId, request).toDomain()
    }

    override suspend fun upsertHotel(
        tripId: String,
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        checkInAt: String?,
        checkOutAt: String?
    ): HotelStay {
        Log.d(TAG, "upsertHotel: PUT /api/v1/me/trips/$tripId/hotel name=$name")
        val request = HotelStayRequestDto(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            checkInAt = checkInAt,
            checkOutAt = checkOutAt
        )
        return apiService.upsertHotel(tripId, request).toDomain()
    }

    override suspend fun getHotel(tripId: String): HotelStay {
        Log.d(TAG, "getHotel: GET /api/v1/me/trips/$tripId/hotel")
        return apiService.getHotel(tripId).toDomain()
    }

    override suspend fun getTripFeed(
        tripId: String,
        latitude: Double?,
        longitude: Double?
    ): TripFeed {
        Log.d(TAG, "getTripFeed: GET /api/v1/me/trips/$tripId/feed lat=$latitude lng=$longitude")
        return apiService.getTripFeed(tripId, latitude, longitude).toDomain()
    }

    override suspend fun getStoreWishlistProducts(storeId: String): List<StoreWishlistProduct> {
        Log.d(TAG, "getStoreWishlistProducts: GET /api/v1/me/stores/$storeId/wishlist-products")
        return apiService.getStoreWishlistProducts(storeId).map { it.toDomain() }
    }

    override suspend fun createVisitReservation(
        tripId: String,
        storeId: String,
        scheduledAt: String,
        productIds: List<String>
    ): VisitReservation {
        Log.d(TAG, "createVisitReservation: POST /api/v1/me/trips/$tripId/visit-reservations storeId=$storeId")
        val request = VisitReservationCreateRequestDto(
            storeId = storeId,
            scheduledAt = scheduledAt,
            productIds = productIds
        )
        return apiService.createVisitReservation(tripId, request).toDomain()
    }

    override suspend fun getVisitReservations(tripId: String): List<VisitReservation> {
        Log.d(TAG, "getVisitReservations: GET /api/v1/me/trips/$tripId/visit-reservations")
        return apiService.getVisitReservations(tripId).map { it.toDomain() }
    }

    override suspend fun cancelVisitReservation(reservationId: String) {
        Log.d(TAG, "cancelVisitReservation: DELETE /api/v1/me/visit-reservations/$reservationId")
        apiService.cancelVisitReservation(reservationId)
    }

    override suspend fun getRefundChecklist(tripId: String): RefundChecklist {
        Log.d(TAG, "getRefundChecklist: GET /api/v1/me/trips/$tripId/refund-checklist")
        return apiService.getTripRefundChecklist(tripId).toDomain(tripId)
    }

    companion object {
        private const val TAG = "RemoteTripRepo"
    }
}
