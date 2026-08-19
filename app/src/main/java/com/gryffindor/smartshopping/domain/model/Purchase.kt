package com.gryffindor.smartshopping.domain.model

data class Purchase(
    val id: String,
    val storeName: String?,
    val purchasedAt: String?,
    val totalAmount: Int?,
    val currency: String?,
    val items: List<PurchaseItem>,
    val createdAt: String
)

data class PurchaseItem(
    val purchaseItemId: String,
    val product: Product?,
    val fallbackProductName: String?,
    val quantity: Int?,
    val price: Int?
)

data class PurchasedProduct(
    val purchaseItemId: String,
    val product: Product?,
    val fallbackProductName: String?,
    val quantity: Int?,
    val price: Int?,
    val currency: String?,
    val storeName: String?,
    val purchasedAt: String?
)
