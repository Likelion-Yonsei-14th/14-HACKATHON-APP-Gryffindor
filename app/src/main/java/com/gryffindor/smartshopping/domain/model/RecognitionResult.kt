package com.gryffindor.smartshopping.domain.model

/**
 * Domain representation of a Backend /recognize response.
 * No Backend DTO types are exposed here.
 */
sealed class RecognitionResult {

    data class Matched(
        val isNew: Boolean,
        val observedProduct: ObservedProduct
    ) : RecognitionResult()

    data class Ambiguous(
        val candidateProductIds: List<String>
    ) : RecognitionResult()

    data object Unknown : RecognitionResult()
}
