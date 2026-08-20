package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.repository.mapper.toDomain
import com.gryffindor.smartshopping.domain.model.ChecklistItem
import com.gryffindor.smartshopping.domain.repository.ChecklistRepository

class RemoteChecklistRepository(
    private val apiService: ShoppingApiService
) : ChecklistRepository {

    override suspend fun getRefundChecklist(sessionId: String): List<ChecklistItem> {
        Log.d(TAG, "getRefundChecklist: GET /sessions/$sessionId/refund-checklist")
        val response = apiService.getRefundChecklist(sessionId)
        return response.items.map { it.toDomain() }
    }

    companion object {
        private const val TAG = "RemoteChecklistRepo"
    }
}
