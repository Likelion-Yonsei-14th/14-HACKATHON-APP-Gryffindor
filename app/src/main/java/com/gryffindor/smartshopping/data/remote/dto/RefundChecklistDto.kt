package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefundChecklistDto(
    val tripId: String? = null,
    val status: String? = null,
    val items: List<ChecklistItemDto> = emptyList(),
    val notice: String? = null,
    // Legacy field kept for backward compatibility with session-based flow
    val mode: String? = null
)
