package com.gryffindor.smartshopping.domain.model

data class Session(
    val sessionId: String,
    val status: SessionStatus,
    val currency: String,
    val startedAt: String // ISO 8601
)

enum class SessionStatus { ACTIVE, COMPLETED }
