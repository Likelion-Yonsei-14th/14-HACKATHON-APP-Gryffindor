package com.gryffindor.smartshopping.domain.model

data class RefundChecklist(
    val tripId: String,
    val status: RefundChecklistStatus,
    val items: List<ChecklistItem>,
    val notice: String?
)

enum class RefundChecklistStatus {
    NO_ELIGIBLE_PURCHASES,
    IMMEDIATE_REFUND_ONLY,
    ACTION_REQUIRED
}
