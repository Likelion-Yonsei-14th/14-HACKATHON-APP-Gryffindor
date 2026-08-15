package com.gryffindor.smartshopping.data.repository.mapper

import com.gryffindor.smartshopping.data.remote.dto.SessionCreateResponseDto
import com.gryffindor.smartshopping.domain.model.Session
import com.gryffindor.smartshopping.domain.model.SessionStatus

fun SessionCreateResponseDto.toDomain(): Session = Session(
    sessionId = sessionId,
    status = SessionStatus.valueOf(status),
    currency = currency,
    startedAt = startedAt
)
