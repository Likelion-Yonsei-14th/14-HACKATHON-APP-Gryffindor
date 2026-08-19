package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

// --- MyPage ---

@Serializable
data class UserResponseDto(
    val id: Int,
    val name: String
)

@Serializable
data class TripSummaryResponseDto(
    val id: String,
    val title: String,
    val startsAt: String? = null,
    val endsAt: String? = null
)

@Serializable
data class PurchasedProductResponseDto(
    val purchaseItemId: String,
    val product: ProductDto? = null,
    val fallbackProductName: String? = null,
    val quantity: Int? = null,
    val price: Int? = null,
    val currency: String? = null,
    val storeName: String? = null,
    val purchasedAt: String? = null
)

@Serializable
data class MyPageResponseDto(
    val user: UserResponseDto,
    val wishlist: List<ProductDto>,
    val purchasedProducts: List<PurchasedProductResponseDto>,
    val flight: FlightResponseDto? = null,
    val trips: List<TripSummaryResponseDto>
)

// --- Wishlist ---

@Serializable
data class WishlistResponseDto(
    val items: List<ProductDto>
)

// --- Receipt / Purchase ---

@Serializable
data class ReceiptItemResponseDto(
    val name: String,
    val productId: String? = null,
    val quantity: Int? = null,
    val price: Int? = null
)

@Serializable
data class ReceiptResponseDto(
    val id: String,
    val tripId: String? = null,
    val refundMethod: String? = null,
    val storeName: String? = null,
    val purchasedAt: String? = null,
    val totalAmount: Int? = null,
    val currency: String? = null,
    val items: List<ReceiptItemResponseDto>,
    val createdAt: String
)

@Serializable
data class PurchaseItemResponseDto(
    val purchaseItemId: String,
    val product: ProductDto? = null,
    val fallbackProductName: String? = null,
    val quantity: Int? = null,
    val price: Int? = null
)

@Serializable
data class PurchaseResponseDto(
    val id: String,
    val tripId: String? = null,
    val refundMethod: String? = null,
    val storeName: String? = null,
    val purchasedAt: String? = null,
    val totalAmount: Int? = null,
    val currency: String? = null,
    val items: List<PurchaseItemResponseDto>,
    val createdAt: String
)

@Serializable
data class PurchaseRefundMethodPatchRequestDto(
    val refundMethod: String
)

// --- Flight ---

@Serializable
data class FlightResponseDto(
    val id: String,
    val tripId: String? = null,
    val departureAirport: String? = null,
    val arrivalAirport: String? = null,
    val terminal: String? = null,
    val flightNumber: String? = null,
    val departureAt: String? = null,
    val arrivalAt: String? = null,
    val airportArrivalAt: String? = null,
    val createdAt: String
)

@Serializable
data class FlightPatchRequestDto(
    val departureAirport: String? = null,
    val arrivalAirport: String? = null,
    val terminal: String? = null,
    val flightNumber: String? = null,
    val departureAt: String? = null,
    val arrivalAt: String? = null,
    val airportArrivalAt: String? = null
)

// --- Trip ---

@Serializable
data class TripCreateRequestDto(
    val title: String,
    val destinationCity: String? = null,
    val destinationCountry: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null
)

@Serializable
data class TripPatchRequestDto(
    val title: String? = null,
    val destinationCity: String? = null,
    val destinationCountry: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null
)

@Serializable
data class TripResponseDto(
    val id: String,
    val title: String,
    val destinationCity: String? = null,
    val destinationCountry: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class TripListResponseDto(
    val trips: List<TripResponseDto>
)

@Serializable
data class TripDetailResponseDto(
    val trip: TripResponseDto,
    val flights: List<FlightResponseDto>,
    val hotel: HotelStayResponseDto? = null,
    val visitReservations: List<VisitReservationResponseDto>
)

// --- Hotel ---

@Serializable
data class HotelStayRequestDto(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val checkInAt: String? = null,
    val checkOutAt: String? = null
)

@Serializable
data class HotelStayResponseDto(
    val id: String,
    val tripId: String,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val checkInAt: String? = null,
    val checkOutAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

// --- Feed ---

@Serializable
data class FeedTripResponseDto(
    val id: String,
    val title: String
)

@Serializable
data class FeedStoreResponseDto(
    val storeId: String,
    val name: String,
    val type: String,
    val distanceFromCurrentLocationKm: Double? = null,
    val distanceFromHotelKm: Double? = null,
    val airportCode: String? = null,
    val terminal: String? = null,
    val hasWishlistItems: Boolean,
    val reason: String
)

@Serializable
data class FeedRecommendationResponseDto(
    val product: ProductDto,
    val reason: String,
    val stores: List<FeedStoreResponseDto>
)

@Serializable
data class TripFeedResponseDto(
    val trip: FeedTripResponseDto,
    val recommendations: List<FeedRecommendationResponseDto>
)

// --- Store Wishlist ---

@Serializable
data class StoreWishlistProductResponseDto(
    val productId: String,
    val name: String
)

// --- Visit Reservation ---

@Serializable
data class VisitReservationCreateRequestDto(
    val storeId: String,
    val scheduledAt: String,
    val productIds: List<String> = emptyList()
)

@Serializable
data class ReservationStoreResponseDto(
    val storeId: String,
    val name: String
)

@Serializable
data class VisitReservationResponseDto(
    val id: String,
    val tripId: String,
    val store: ReservationStoreResponseDto,
    val scheduledAt: String,
    val products: List<ProductDto>,
    val status: String,
    val createdAt: String
)
