package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PricingDto(
    val retailPriceKrw: Long,
    val estimatedRefundKrw: Long,
    val estimatedRefundPriceKrw: Long,
    val convertedAmount: String? = null,
    val convertedCurrency: String? = null,
    val instantRefundEligible: Boolean,
    val pricingMode: String? = null
)
