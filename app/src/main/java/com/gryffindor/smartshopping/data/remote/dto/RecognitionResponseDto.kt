package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecognitionResponseDto(
    val recognitionStatus: String,
    val isNew: Boolean? = null,
    val observedProduct: ObservedProductDto? = null,
    val candidateProductIds: List<String>? = null
)
