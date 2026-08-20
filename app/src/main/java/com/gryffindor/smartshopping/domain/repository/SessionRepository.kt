package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.Session

interface SessionRepository {
    suspend fun createSession(currency: String, storeId: String): Session
    suspend fun completeSession(sessionId: String)
}
