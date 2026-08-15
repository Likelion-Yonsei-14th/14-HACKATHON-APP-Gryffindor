package com.gryffindor.smartshopping.domain.model

data class ObservedProduct(
    val product: Product,
    val pricing: Pricing,
    val observation: Observation
)
