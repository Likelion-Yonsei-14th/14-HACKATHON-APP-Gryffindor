package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ObservedProductDto(
    val product: ProductDto,
    val pricing: PricingDto,
    val observation: ObservationDto
)
