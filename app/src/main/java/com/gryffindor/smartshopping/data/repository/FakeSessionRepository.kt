package com.gryffindor.smartshopping.data.repository

import com.gryffindor.smartshopping.domain.model.Session
import com.gryffindor.smartshopping.domain.model.SessionStatus
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import kotlinx.coroutines.delay

class FakeSessionRepository : SessionRepository {

    override suspend fun createSession(currency: String): Session {
        delay(300)
        return Session(
            sessionId = "fake-session-001",
            status = SessionStatus.ACTIVE,
            currency = currency,
            startedAt = "2026-08-15T13:30:00Z"
        )
    }

    override suspend fun completeSession(sessionId: String) {
        delay(200)
    }
}
