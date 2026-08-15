package com.gryffindor.smartshopping.domain.model

data class Pricing(
    val retailPriceKrw: Long,
    val estimatedRefundKrw: Long,
    val estimatedRefundPriceKrw: Long,
    val convertedAmount: String?,
    val convertedCurrency: String?,
    val instantRefundEligible: Boolean,
    val pricingMode: String?
)
