package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.ChecklistItem

interface ChecklistRepository {
    suspend fun getRefundChecklist(sessionId: String): List<ChecklistItem>
}
