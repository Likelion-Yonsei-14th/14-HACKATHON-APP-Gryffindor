package com.gryffindor.smartshopping.domain.model

data class TripFeed(
    val tripId: String,
    val tripTitle: String,
    val recommendations: List<FeedRecommendation>
)

data class FeedRecommendation(
    val product: Product,
    val reason: String,
    val stores: List<RecommendedStore>
)

data class RecommendedStore(
    val storeId: String,
    val name: String,
    val type: String,
    val distanceFromHotelKm: Double?,
    val airportCode: String?,
    val terminal: String?,
    val hasWishlistItems: Boolean,
    val reason: String
)
