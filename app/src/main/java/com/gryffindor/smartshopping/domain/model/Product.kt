package com.gryffindor.smartshopping.domain.model

data class Product(
    val productId: String,
    val sku: String?,
    val brand: String,
    val name: String,
    val category: String?,
    val imageUrl: String?
)
