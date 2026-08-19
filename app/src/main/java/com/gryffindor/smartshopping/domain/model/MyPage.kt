package com.gryffindor.smartshopping.domain.model

data class MyPage(
    val user: DemoUser,
    val wishlist: List<Product>,
    val purchasedProducts: List<PurchasedProduct>,
    val flight: Flight?,
    val trips: List<TripSummary>
)
