package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ObservationDto(
    val triggerType: String,
    val occupancyRatio: Double,
    val dwellMs: Long,
    val firstObservedAt: String,
    val lastObservedAt: String
)
