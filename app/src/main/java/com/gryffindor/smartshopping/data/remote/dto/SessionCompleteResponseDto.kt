package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionCompleteResponseDto(
    val sessionId: String,
    val status: String,
    val completedAt: String
)
