package com.gryffindor.smartshopping.domain.model

data class Receipt(
    val id: String,
    val tripId: String?,
    val refundMethod: RefundMethod,
    val storeName: String?,
    val purchasedAt: String?,
    val totalAmount: Int?,
    val currency: String?,
    val items: List<ReceiptItem>,
    val createdAt: String
)

data class ReceiptItem(
    val name: String,
    val productId: String?,
    val quantity: Int?,
    val price: Int?
)
