package com.gryffindor.smartshopping.feature.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripInputValidatorTest {

    // ==================== normalizeCountry ====================

    @Test
    fun `normalizeCountry - empty string returns success empty`() {
        val result = TripInputValidator.normalizeCountry("")
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - blank string returns success empty`() {
        val result = TripInputValidator.normalizeCountry("  ")
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - Korean 한국 returns KR`() {
        val result = TripInputValidator.normalizeCountry("한국")
        assertTrue(result.isSuccess)
        assertEquals("KR", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - Korean 대한민국 returns KR`() {
        val result = TripInputValidator.normalizeCountry("대한민국")
        assertTrue(result.isSuccess)
        assertEquals("KR", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - Korean 일본 returns JP`() {
        val result = TripInputValidator.normalizeCountry("일본")
        assertTrue(result.isSuccess)
        assertEquals("JP", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - Korean 미국 returns US`() {
        val result = TripInputValidator.normalizeCountry("미국")
        assertTrue(result.isSuccess)
        assertEquals("US", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - lowercase kr returns KR`() {
        val result = TripInputValidator.normalizeCountry("kr")
        assertTrue(result.isSuccess)
        assertEquals("KR", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - lowercase jp returns JP`() {
        val result = TripInputValidator.normalizeCountry("jp")
        assertTrue(result.isSuccess)
        assertEquals("JP", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - uppercase KR passes through`() {
        val result = TripInputValidator.normalizeCountry("KR")
        assertTrue(result.isSuccess)
        assertEquals("KR", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - uppercase US passes through`() {
        val result = TripInputValidator.normalizeCountry("US")
        assertTrue(result.isSuccess)
        assertEquals("US", result.getOrNull())
    }

    @Test
    fun `normalizeCountry - three letter code fails`() {
        val result = TripInputValidator.normalizeCountry("KOR")
        assertTrue(result.isFailure)
    }

    @Test
    fun `normalizeCountry - numeric input fails`() {
        val result = TripInputValidator.normalizeCountry("12")
        assertTrue(result.isFailure)
    }

    @Test
    fun `normalizeCountry - random Korean text fails`() {
        val result = TripInputValidator.normalizeCountry("서울")
        assertTrue(result.isFailure)
    }

    @Test
    fun `normalizeCountry - mixed case single char fails`() {
        val result = TripInputValidator.normalizeCountry("K")
        assertTrue(result.isFailure)
    }

    @Test
    fun `normalizeCountry - trimmed input with spaces`() {
        val result = TripInputValidator.normalizeCountry(" KR ")
        assertTrue(result.isSuccess)
        assertEquals("KR", result.getOrNull())
    }

    // ==================== toIso8601Start ====================

    @Test
    fun `toIso8601Start - empty string returns success empty`() {
        val result = TripInputValidator.toIso8601Start("")
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun `toIso8601Start - valid date converts to start of day KST`() {
        val result = TripInputValidator.toIso8601Start("2026-08-20")
        assertTrue(result.isSuccess)
        assertEquals("2026-08-20T00:00:00+09:00", result.getOrNull())
    }

    @Test
    fun `toIso8601Start - another valid date`() {
        val result = TripInputValidator.toIso8601Start("2026-12-31")
        assertTrue(result.isSuccess)
        assertEquals("2026-12-31T00:00:00+09:00", result.getOrNull())
    }

    @Test
    fun `toIso8601Start - already has offset passes through`() {
        val input = "2026-08-20T00:00:00+09:00"
        val result = TripInputValidator.toIso8601Start(input)
        assertTrue(result.isSuccess)
        assertEquals(input, result.getOrNull())
    }

    @Test
    fun `toIso8601Start - invalid date format MM-DD-YYYY fails`() {
        val result = TripInputValidator.toIso8601Start("08-20-2026")
        assertTrue(result.isFailure)
    }

    @Test
    fun `toIso8601Start - invalid date format with slashes fails`() {
        val result = TripInputValidator.toIso8601Start("2026/08/20")
        assertTrue(result.isFailure)
    }

    @Test
    fun `toIso8601Start - invalid date Feb 30 fails`() {
        val result = TripInputValidator.toIso8601Start("2026-02-30")
        assertTrue(result.isFailure)
    }

    @Test
    fun `toIso8601Start - random text fails`() {
        val result = TripInputValidator.toIso8601Start("내일")
        assertTrue(result.isFailure)
    }

    @Test
    fun `toIso8601Start - trimmed input with spaces`() {
        val result = TripInputValidator.toIso8601Start(" 2026-08-20 ")
        assertTrue(result.isSuccess)
        assertEquals("2026-08-20T00:00:00+09:00", result.getOrNull())
    }

    // ==================== toIso8601End ====================

    @Test
    fun `toIso8601End - empty string returns success empty`() {
        val result = TripInputValidator.toIso8601End("")
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun `toIso8601End - valid date converts to end of day KST`() {
        val result = TripInputValidator.toIso8601End("2026-08-23")
        assertTrue(result.isSuccess)
        assertEquals("2026-08-23T23:59:59+09:00", result.getOrNull())
    }

    @Test
    fun `toIso8601End - another valid date`() {
        val result = TripInputValidator.toIso8601End("2026-01-01")
        assertTrue(result.isSuccess)
        assertEquals("2026-01-01T23:59:59+09:00", result.getOrNull())
    }

    @Test
    fun `toIso8601End - already has offset passes through`() {
        val input = "2026-08-23T23:59:59+09:00"
        val result = TripInputValidator.toIso8601End(input)
        assertTrue(result.isSuccess)
        assertEquals(input, result.getOrNull())
    }

    @Test
    fun `toIso8601End - invalid date format fails`() {
        val result = TripInputValidator.toIso8601End("23-08-2026")
        assertTrue(result.isFailure)
    }

    @Test
    fun `toIso8601End - invalid date Feb 31 fails`() {
        val result = TripInputValidator.toIso8601End("2026-02-31")
        assertTrue(result.isFailure)
    }

    // ==================== validateDateRange ====================

    @Test
    fun `validateDateRange - both empty returns success`() {
        val result = TripInputValidator.validateDateRange("", "")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateDateRange - start empty returns success`() {
        val result = TripInputValidator.validateDateRange("", "2026-08-23")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateDateRange - end empty returns success`() {
        val result = TripInputValidator.validateDateRange("2026-08-20", "")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateDateRange - end equals start returns success`() {
        val result = TripInputValidator.validateDateRange("2026-08-20", "2026-08-20")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateDateRange - end after start returns success`() {
        val result = TripInputValidator.validateDateRange("2026-08-20", "2026-08-23")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateDateRange - end before start returns failure`() {
        val result = TripInputValidator.validateDateRange("2026-08-23", "2026-08-20")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("종료 날짜") == true)
    }

    @Test
    fun `validateDateRange - one day difference valid`() {
        val result = TripInputValidator.validateDateRange("2026-08-20", "2026-08-21")
        assertTrue(result.isSuccess)
    }
}
