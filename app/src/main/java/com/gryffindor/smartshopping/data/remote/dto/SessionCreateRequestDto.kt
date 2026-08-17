package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionCreateRequestDto(
    val currency: String,
    val storeId: String
)
