package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChecklistItemDto(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean
)
