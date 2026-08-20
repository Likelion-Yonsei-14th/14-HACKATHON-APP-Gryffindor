package com.gryffindor.smartshopping.data.remote.api

import com.gryffindor.smartshopping.data.remote.dto.FlightResponseDto
import com.gryffindor.smartshopping.data.remote.dto.FlightPatchRequestDto
import com.gryffindor.smartshopping.data.remote.dto.HotelStayRequestDto
import com.gryffindor.smartshopping.data.remote.dto.HotelStayResponseDto
import com.gryffindor.smartshopping.data.remote.dto.MyPageResponseDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseRefundMethodPatchRequestDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseResponseDto
import com.gryffindor.smartshopping.data.remote.dto.ReceiptResponseDto
import com.gryffindor.smartshopping.data.remote.dto.RefundChecklistDto
import com.gryffindor.smartshopping.data.remote.dto.StoreWishlistProductResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripCreateRequestDto
import com.gryffindor.smartshopping.data.remote.dto.TripDetailResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripFeedResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripListResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripPatchRequestDto
import com.gryffindor.smartshopping.data.remote.dto.TripResponseDto
import com.gryffindor.smartshopping.data.remote.dto.VisitReservationCreateRequestDto
import com.gryffindor.smartshopping.data.remote.dto.VisitReservationResponseDto
import com.gryffindor.smartshopping.data.remote.dto.WishlistResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit API interface for B5/B6 personalization & trip endpoints.
 * Base path: /api/v1/me
 */
interface PersonalizationApiService {

    // --- MyPage ---

    @GET("me")
    suspend fun getMyPage(): MyPageResponseDto

    // --- Wishlist ---

    @GET("me/wishlist")
    suspend fun getWishlist(): WishlistResponseDto

    @POST("me/wishlist/{productId}")
    suspend fun addWishlist(
        @Path("productId") productId: String
    ): com.gryffindor.smartshopping.data.remote.dto.ProductDto

    @DELETE("me/wishlist/{productId}")
    suspend fun deleteWishlist(
        @Path("productId") productId: String
    )

    // --- Purchase / Receipt ---

    @Multipart
    @POST("me/receipts/analyze")
    suspend fun analyzeReceipt(
        @Part image: MultipartBody.Part,
        @Part("tripId") tripId: RequestBody? = null
    ): ReceiptResponseDto

    @GET("me/purchases")
    suspend fun getPurchases(): List<PurchaseResponseDto>

    @PATCH("me/purchases/{purchaseId}")
    suspend fun updatePurchaseRefundMethod(
        @Path("purchaseId") purchaseId: String,
        @Body request: PurchaseRefundMethodPatchRequestDto
    ): PurchaseResponseDto

    // --- Flight ---

    @Multipart
    @POST("me/flights/analyze")
    suspend fun analyzeFlight(
        @Part image: MultipartBody.Part,
        @Part("tripId") tripId: RequestBody?
    ): FlightResponseDto

    @PATCH("me/flights/{flightId}")
    suspend fun updateFlight(
        @Path("flightId") flightId: String,
        @Body request: FlightPatchRequestDto
    ): FlightResponseDto

    // --- Trip ---

    @POST("me/trips")
    suspend fun createTrip(
        @Body request: TripCreateRequestDto
    ): TripResponseDto

    @GET("me/trips")
    suspend fun getTrips(): TripListResponseDto

    @GET("me/trips/{tripId}")
    suspend fun getTrip(
        @Path("tripId") tripId: String
    ): TripDetailResponseDto

    @PATCH("me/trips/{tripId}")
    suspend fun updateTrip(
        @Path("tripId") tripId: String,
        @Body request: TripPatchRequestDto
    ): TripResponseDto

    // --- Hotel ---

    @PUT("me/trips/{tripId}/hotel")
    suspend fun upsertHotel(
        @Path("tripId") tripId: String,
        @Body request: HotelStayRequestDto
    ): HotelStayResponseDto

    @GET("me/trips/{tripId}/hotel")
    suspend fun getHotel(
        @Path("tripId") tripId: String
    ): HotelStayResponseDto

    // --- Refund Checklist ---

    @GET("me/trips/{tripId}/refund-checklist")
    suspend fun getTripRefundChecklist(
        @Path("tripId") tripId: String
    ): RefundChecklistDto

    // --- Feed ---

    @GET("me/trips/{tripId}/feed")
    suspend fun getTripFeed(
        @Path("tripId") tripId: String,
        @retrofit2.http.Query("latitude") latitude: Double? = null,
        @retrofit2.http.Query("longitude") longitude: Double? = null
    ): TripFeedResponseDto

    // --- Store Wishlist ---

    @GET("me/stores/{storeId}/wishlist-products")
    suspend fun getStoreWishlistProducts(
        @Path("storeId") storeId: String
    ): List<StoreWishlistProductResponseDto>

    // --- Visit Reservations ---

    @POST("me/trips/{tripId}/visit-reservations")
    suspend fun createVisitReservation(
        @Path("tripId") tripId: String,
        @Body request: VisitReservationCreateRequestDto
    ): VisitReservationResponseDto

    @GET("me/trips/{tripId}/visit-reservations")
    suspend fun getVisitReservations(
        @Path("tripId") tripId: String
    ): List<VisitReservationResponseDto>

    @DELETE("me/visit-reservations/{reservationId}")
    suspend fun cancelVisitReservation(
        @Path("reservationId") reservationId: String
    )
}
