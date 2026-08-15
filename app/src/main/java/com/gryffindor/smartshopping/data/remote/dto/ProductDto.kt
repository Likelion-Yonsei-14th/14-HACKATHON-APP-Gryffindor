package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val productId: String,
    val sku: String? = null,
    val brand: String,
    val name: String,
    val category: String? = null,
    val imageUrl: String? = null
)
