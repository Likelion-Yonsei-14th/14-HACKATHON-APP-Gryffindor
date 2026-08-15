package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefundChecklistDto(
    val items: List<ChecklistItemDto>,
    val mode: String? = null
)
