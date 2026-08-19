package com.gryffindor.smartshopping.domain.model

data class Trip(
    val id: String,
    val title: String,
    val destinationCity: String?,
    val destinationCountry: String?,
    val startsAt: String?,
    val endsAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class TripSummary(
    val id: String,
    val title: String,
    val startsAt: String?,
    val endsAt: String?
)

data class TripDetail(
    val trip: Trip,
    val flights: List<Flight>,
    val hotel: HotelStay?,
    val visitReservations: List<VisitReservation>
)
