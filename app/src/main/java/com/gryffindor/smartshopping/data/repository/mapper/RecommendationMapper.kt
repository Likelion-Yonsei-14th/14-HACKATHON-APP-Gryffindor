package com.gryffindor.smartshopping.data.repository.mapper

import com.gryffindor.smartshopping.data.remote.dto.RecommendationItemDto
import com.gryffindor.smartshopping.domain.model.Recommendation
import com.gryffindor.smartshopping.domain.model.RecommendationType

fun RecommendationItemDto.toDomain(): Recommendation = Recommendation(
    type = RecommendationType.valueOf(type),
    sourceProductId = sourceProductId,
    product = product.toDomain(),
    reasonCode = reasonCode
)
