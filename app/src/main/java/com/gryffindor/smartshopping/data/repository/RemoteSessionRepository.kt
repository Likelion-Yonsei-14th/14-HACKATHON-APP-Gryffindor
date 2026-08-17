package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.remote.dto.SessionCreateRequestDto
import com.gryffindor.smartshopping.domain.model.Session
import com.gryffindor.smartshopping.domain.model.SessionStatus
import com.gryffindor.smartshopping.domain.repository.SessionRepository

/**
 * Real SessionRepository implementation that communicates with the Backend API.
 * A4 Checkpoint 1: createSession calls POST /api/v1/sessions.
 */
class RemoteSessionRepository(
    private val apiService: ShoppingApiService
) : SessionRepository {

    override suspend fun createSession(currency: String): Session {
        Log.d(TAG, "createSession: POST /api/v1/sessions currency=$currency")
        val response = apiService.createSession(SessionCreateRequestDto(currency = currency))
        Log.d(TAG, "createSession: response sessionId=${response.sessionId}, status=${response.status}")
        return Session(
            sessionId = response.sessionId,
            status = mapStatus(response.status),
            currency = response.currency,
            startedAt = response.startedAt
        )
    }

    override suspend fun completeSession(sessionId: String) {
        Log.d(TAG, "completeSession: POST /api/v1/sessions/$sessionId/complete")
        apiService.completeSession(sessionId)
    }

    private fun mapStatus(raw: String): SessionStatus {
        return when (raw.uppercase()) {
            "ACTIVE" -> SessionStatus.ACTIVE
            "COMPLETED" -> SessionStatus.COMPLETED
            else -> SessionStatus.ACTIVE
        }
    }

    companion object {
        private const val TAG = "RemoteSessionRepo"
    }
}
