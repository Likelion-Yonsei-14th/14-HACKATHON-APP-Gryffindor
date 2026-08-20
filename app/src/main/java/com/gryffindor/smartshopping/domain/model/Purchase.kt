package com.gryffindor.smartshopping.domain.model

data class Purchase(
    val id: String,
    val tripId: String?,
    val refundMethod: RefundMethod,
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
    val purchasedAt: String?,
    // Pricing fields from Backend
    val estimatedRefundKrw: Long? = null,
    val estimatedRefundPriceKrw: Long? = null,
    val convertedPrice: String? = null,
    val convertedEstimatedRefund: String? = null,
    val convertedEstimatedRefundPrice: String? = null,
    val convertedCurrency: String? = null
)
