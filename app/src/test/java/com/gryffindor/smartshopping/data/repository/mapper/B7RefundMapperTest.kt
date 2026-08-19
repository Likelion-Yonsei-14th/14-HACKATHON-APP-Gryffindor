package com.gryffindor.smartshopping.data.repository.mapper

import com.gryffindor.smartshopping.data.remote.dto.ChecklistItemDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseItemResponseDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseResponseDto
import com.gryffindor.smartshopping.data.remote.dto.ReceiptItemResponseDto
import com.gryffindor.smartshopping.data.remote.dto.ReceiptResponseDto
import com.gryffindor.smartshopping.data.remote.dto.RefundChecklistDto
import com.gryffindor.smartshopping.domain.model.RefundChecklistStatus
import com.gryffindor.smartshopping.domain.model.RefundMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class B7RefundMapperTest {

    @Test
    fun `Receipt maps tripId and refundMethod correctly`() {
        val dto = ReceiptResponseDto(
            id = "r1",
            tripId = "trip-abc",
            refundMethod = "AIRPORT",
            storeName = "Store A",
            purchasedAt = "2026-08-20",
            totalAmount = 100000,
            currency = "KRW",
            items = listOf(ReceiptItemResponseDto(name = "Item", productId = null, quantity = 1, price = 100000)),
            createdAt = "2026-08-20T00:00:00Z"
        )
        val receipt = dto.toDomain()
        assertEquals("trip-abc", receipt.tripId)
        assertEquals(RefundMethod.AIRPORT, receipt.refundMethod)
    }

    @Test
    fun `Receipt maps null tripId and null refundMethod to UNKNOWN`() {
        val dto = ReceiptResponseDto(
            id = "r2",
            tripId = null,
            refundMethod = null,
            storeName = null,
            purchasedAt = null,
            totalAmount = null,
            currency = null,
            items = emptyList(),
            createdAt = "2026-08-20T00:00:00Z"
        )
        val receipt = dto.toDomain()
        assertNull(receipt.tripId)
        assertEquals(RefundMethod.UNKNOWN, receipt.refundMethod)
    }

    @Test
    fun `Purchase maps tripId and refundMethod correctly`() {
        val dto = PurchaseResponseDto(
            id = "p1",
            tripId = "trip-xyz",
            refundMethod = "IMMEDIATE",
            storeName = "Shop B",
            purchasedAt = "2026-08-19",
            totalAmount = 50000,
            currency = "KRW",
            items = listOf(
                PurchaseItemResponseDto(
                    purchaseItemId = "pi1",
                    product = null,
                    fallbackProductName = "Product X",
                    quantity = 1,
                    price = 50000
                )
            ),
            createdAt = "2026-08-19T00:00:00Z"
        )
        val purchase = dto.toDomain()
        assertEquals("trip-xyz", purchase.tripId)
        assertEquals(RefundMethod.IMMEDIATE, purchase.refundMethod)
    }

    @Test
    fun `Purchase null tripId backward compatible`() {
        val dto = PurchaseResponseDto(
            id = "p2",
            tripId = null,
            refundMethod = null,
            storeName = null,
            purchasedAt = null,
            totalAmount = null,
            currency = null,
            items = emptyList(),
            createdAt = "2026-08-20T00:00:00Z"
        )
        val purchase = dto.toDomain()
        assertNull(purchase.tripId)
        assertEquals(RefundMethod.UNKNOWN, purchase.refundMethod)
    }

    @Test
    fun `RefundChecklist maps status and items`() {
        val dto = RefundChecklistDto(
            tripId = "trip-1",
            status = "ACTION_REQUIRED",
            items = listOf(
                ChecklistItemDto(id = "c1", title = "서류 준비", description = "환급 서류를 준비하세요", required = true),
                ChecklistItemDto(id = "c2", title = "위치 확인", description = "카운터 위치를 확인하세요", required = false)
            ),
            notice = "공항에 일찍 도착하세요."
        )
        val checklist = dto.toDomain("trip-1")
        assertEquals("trip-1", checklist.tripId)
        assertEquals(RefundChecklistStatus.ACTION_REQUIRED, checklist.status)
        assertEquals(2, checklist.items.size)
        assertEquals("서류 준비", checklist.items[0].title)
        assertEquals(true, checklist.items[0].required)
        assertEquals("공항에 일찍 도착하세요.", checklist.notice)
    }

    @Test
    fun `RefundChecklist NO_ELIGIBLE_PURCHASES for unknown status`() {
        val dto = RefundChecklistDto(
            tripId = null,
            status = "NO_ELIGIBLE_PURCHASES",
            items = emptyList(),
            notice = null
        )
        val checklist = dto.toDomain("fallback-trip")
        assertEquals("fallback-trip", checklist.tripId)
        assertEquals(RefundChecklistStatus.NO_ELIGIBLE_PURCHASES, checklist.status)
        assertEquals(0, checklist.items.size)
        assertNull(checklist.notice)
    }

    @Test
    fun `Trip-specific purchase filtering`() {
        val purchases = listOf(
            PurchaseResponseDto(id = "p1", tripId = "trip-a", refundMethod = "AIRPORT", storeName = null, purchasedAt = null, totalAmount = null, currency = null, items = emptyList(), createdAt = "2026-08-20T00:00:00Z"),
            PurchaseResponseDto(id = "p2", tripId = "trip-b", refundMethod = "DOWNTOWN", storeName = null, purchasedAt = null, totalAmount = null, currency = null, items = emptyList(), createdAt = "2026-08-20T00:00:00Z"),
            PurchaseResponseDto(id = "p3", tripId = null, refundMethod = null, storeName = null, purchasedAt = null, totalAmount = null, currency = null, items = emptyList(), createdAt = "2026-08-20T00:00:00Z")
        ).map { it.toDomain() }

        val tripAPurchases = purchases.filter { it.tripId == "trip-a" }
        assertEquals(1, tripAPurchases.size)
        assertEquals("p1", tripAPurchases[0].id)
    }

    @Test
    fun `toRefundMethod handles all cases`() {
        assertEquals(RefundMethod.UNKNOWN, null.toRefundMethod())
        assertEquals(RefundMethod.UNKNOWN, "UNKNOWN".toRefundMethod())
        assertEquals(RefundMethod.UNKNOWN, "invalid".toRefundMethod())
        assertEquals(RefundMethod.IMMEDIATE, "IMMEDIATE".toRefundMethod())
        assertEquals(RefundMethod.DOWNTOWN, "DOWNTOWN".toRefundMethod())
        assertEquals(RefundMethod.AIRPORT, "AIRPORT".toRefundMethod())
        // Case insensitive
        assertEquals(RefundMethod.AIRPORT, "airport".toRefundMethod())
    }
}
