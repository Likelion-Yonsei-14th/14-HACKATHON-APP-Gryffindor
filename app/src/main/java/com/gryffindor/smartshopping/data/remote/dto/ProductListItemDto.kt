package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductListItemDto(
    val product: ProductDto,
    val pricing: PricingDto,
    val purchaseState: String,
    val interested: Boolean
)
