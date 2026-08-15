package com.gryffindor.smartshopping.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val error: ErrorDetailDto
)

@Serializable
data class ErrorDetailDto(
    val code: String,
    val message: String
)
