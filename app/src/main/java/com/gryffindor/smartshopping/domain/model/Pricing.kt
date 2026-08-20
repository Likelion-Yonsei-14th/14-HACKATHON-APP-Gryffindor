package com.gryffindor.smartshopping.domain.model

data class Pricing(
    val retailPriceKrw: Long,
    val estimatedRefundKrw: Long,
    val estimatedRefundPriceKrw: Long,
    val convertedRetailPrice: String?,
    val convertedEstimatedRefund: String?,
    val convertedEstimatedRefundPrice: String?,
    val convertedAmount: String?,
    val convertedCurrency: String?,
    val instantRefundEligible: Boolean,
    val pricingMode: String?
)
