package com.gryffindor.smartshopping.domain.model

data class Observation(
    val triggerType: TriggerType,
    val occupancyRatio: Double,
    val dwellMs: Long,
    val firstObservedAt: String,
    val lastObservedAt: String
)

enum class TriggerType { OCCUPANCY, DWELL, OCCUPANCY_AND_DWELL }
