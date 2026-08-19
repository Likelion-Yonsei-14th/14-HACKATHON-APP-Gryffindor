package com.gryffindor.smartshopping.app.navigation

object Routes {
    const val HOME = "home"
    const val STORE_SELECTION = "store_selection/{currency}"
    const val SHOPPING = "shopping/{sessionId}/{currency}"
    const val REVIEW = "review/{sessionId}"
    const val TRAVEL = "travel/{sessionId}"
    const val CHECKLIST = "checklist/{sessionId}"
    const val RECOMMENDATION = "recommendation/{sessionId}"

    // Trip flow
    const val TRIP_LIST = "trip_list"
    const val TRIP_CREATE = "trip_create"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val FLIGHT_EDIT = "flight_edit/{tripId}/{flightId}"
    const val HOTEL_EDIT = "hotel_edit/{tripId}"

    // Visit reservation flow
    const val VISIT_RESERVATION = "visit_reservation/{tripId}/{storeId}/{storeName}"
    const val RESERVATION_LIST = "reservation_list/{tripId}"

    fun storeSelection(currency: String) = "store_selection/$currency"
    fun shopping(sessionId: String, currency: String) = "shopping/$sessionId/$currency"
    fun review(sessionId: String) = "review/$sessionId"
    fun travel(sessionId: String) = "travel/$sessionId"
    fun checklist(sessionId: String) = "checklist/$sessionId"
    fun recommendation(sessionId: String) = "recommendation/$sessionId"

    fun tripDetail(tripId: String) = "trip_detail/$tripId"
    fun flightEdit(tripId: String, flightId: String) = "flight_edit/$tripId/$flightId"
    fun hotelEdit(tripId: String) = "hotel_edit/$tripId"
    fun visitReservation(tripId: String, storeId: String, storeName: String) =
        "visit_reservation/$tripId/$storeId/${java.net.URLEncoder.encode(storeName, "UTF-8")}"
    fun reservationList(tripId: String) = "reservation_list/$tripId"
}
