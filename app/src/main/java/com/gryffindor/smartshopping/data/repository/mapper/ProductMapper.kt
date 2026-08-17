package com.gryffindor.smartshopping.data.repository.mapper

import com.gryffindor.smartshopping.data.remote.dto.ObservationDto
import com.gryffindor.smartshopping.data.remote.dto.ObservedProductDto
import com.gryffindor.smartshopping.data.remote.dto.PricingDto
import com.gryffindor.smartshopping.data.remote.dto.ProductDto
import com.gryffindor.smartshopping.data.remote.dto.ProductListItemDto
import com.gryffindor.smartshopping.data.remote.dto.RecognitionResponseDto
import com.gryffindor.smartshopping.domain.model.Observation
import com.gryffindor.smartshopping.domain.model.ObservedProduct
import com.gryffindor.smartshopping.domain.model.Pricing
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.PurchaseState
import com.gryffindor.smartshopping.domain.model.RecognitionResult
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.model.TriggerType

fun ProductListItemDto.toDomain(): SessionProduct = SessionProduct(
    product = product.toDomain(),
    pricing = pricing.toDomain(),
    purchaseState = PurchaseState.valueOf(purchaseState),
    interested = interested
)

fun ObservedProductDto.toDomain(): ObservedProduct = ObservedProduct(
    product = product.toDomain(),
    pricing = pricing.toDomain(),
    observation = observation.toDomain()
)

fun ProductDto.toDomain(): Product = Product(
    productId = productId,
    sku = sku,
    brand = brand,
    name = name,
    category = category,
    imageUrl = imageUrl
)

fun PricingDto.toDomain(): Pricing = Pricing(
    retailPriceKrw = retailPriceKrw,
    estimatedRefundKrw = estimatedRefundKrw,
    estimatedRefundPriceKrw = estimatedRefundPriceKrw,
    convertedAmount = convertedAmount,
    convertedCurrency = convertedCurrency,
    instantRefundEligible = instantRefundEligible,
    pricingMode = pricingMode
)

fun ObservationDto.toDomain(): Observation = Observation(
    triggerType = TriggerType.valueOf(triggerType),
    occupancyRatio = occupancyRatio,
    dwellMs = dwellMs,
    firstObservedAt = firstObservedAt,
    lastObservedAt = lastObservedAt
)

fun RecognitionResponseDto.toDomain(): RecognitionResult {
    return when (recognitionStatus.uppercase()) {
        "MATCHED" -> {
            val observed = observedProduct?.toDomain()
                ?: throw IllegalStateException("MATCHED response missing observedProduct")
            RecognitionResult.Matched(
                isNew = isNew ?: true,
                observedProduct = observed
            )
        }
        "AMBIGUOUS" -> RecognitionResult.Ambiguous(
            candidateProductIds = candidateProductIds ?: emptyList()
        )
        else -> RecognitionResult.Unknown
    }
}
