package com.gryffindor.smartshopping.domain.model

data class VisitReservation(
    val id: String,
    val tripId: String,
    val storeId: String,
    val storeName: String,
    val scheduledAt: String,
    val products: List<Product>,
    val status: ReservationStatus,
    val createdAt: String
)

enum class ReservationStatus {
    RESERVED,
    CANCELLED
}
