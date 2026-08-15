package com.gryffindor.smartshopping.data.repository

import com.gryffindor.smartshopping.domain.model.ChecklistItem
import com.gryffindor.smartshopping.domain.repository.ChecklistRepository
import kotlinx.coroutines.delay

class FakeChecklistRepository : ChecklistRepository {

    override suspend fun getRefundChecklist(sessionId: String): List<ChecklistItem> {
        delay(400)
        return listOf(
            ChecklistItem(
                id = "keep-receipt",
                title = "구매 영수증을 준비하세요",
                description = "환급 확인을 위해 구매 증빙을 준비합니다.",
                required = true
            ),
            ChecklistItem(
                id = "keep-product-sealed",
                title = "상품 포장을 유지하세요",
                description = "개봉하지 않은 상태에서 환급이 가능합니다.",
                required = true
            ),
            ChecklistItem(
                id = "visit-counter",
                title = "환급 카운터 위치를 확인하세요",
                description = "출국장 내 Tax Refund 카운터에서 신청합니다.",
                required = false
            )
        )
    }
}
