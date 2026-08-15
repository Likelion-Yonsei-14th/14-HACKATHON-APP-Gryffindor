package com.gryffindor.smartshopping.domain.model

data class SessionProduct(
    val product: Product,
    val pricing: Pricing,
    val purchaseState: PurchaseState,
    val interested: Boolean
)

enum class PurchaseState { UNSET, PURCHASED }
