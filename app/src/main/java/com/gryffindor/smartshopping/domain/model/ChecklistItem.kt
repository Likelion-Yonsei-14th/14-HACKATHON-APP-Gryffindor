package com.gryffindor.smartshopping.domain.model

data class ChecklistItem(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean
)
