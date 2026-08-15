package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionCreateResponseDto(
    val sessionId: String,
    val status: String,
    val currency: String,
    val startedAt: String
)
