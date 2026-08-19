package com.gryffindor.smartshopping.feature.trip

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Pure utility for normalizing and validating Trip creation input
 * to match Backend TripCreateRequest contract.
 *
 * Backend contract:
 * - destinationCountry: nullable, but if present must match ^[A-Z]{2}$
 * - startsAt / endsAt: nullable datetime, but if present must include timezone offset
 * - endsAt >= startsAt
 */
object TripInputValidator {

    private val ISO_COUNTRY_REGEX = Regex("^[A-Z]{2}$")
    private val DATE_ONLY_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val HAS_OFFSET_REGEX = Regex("[+-]\\d{2}:\\d{2}$")

    private const val KST_OFFSET = "+09:00"

    /** Korean name → ISO 3166-1 alpha-2 mapping for common tourist destinations */
    private val KOREAN_COUNTRY_MAP = mapOf(
        "한국" to "KR",
        "대한민국" to "KR",
        "일본" to "JP",
        "미국" to "US",
        "중국" to "CN",
        "대만" to "TW",
        "태국" to "TH",
        "베트남" to "VN",
        "싱가포르" to "SG",
        "호주" to "AU",
        "영국" to "GB",
        "프랑스" to "FR",
        "독일" to "DE",
        "이탈리아" to "IT",
        "스페인" to "ES",
        "캐나다" to "CA"
    )

    /**
     * Normalizes country input to ISO 3166-1 alpha-2 code.
     *
     * - Empty/blank → Result.success("") (will be treated as null by caller)
     * - Korean name (e.g., "한국") → Result.success("KR")
     * - Lowercase 2-char (e.g., "kr") → Result.success("KR")
     * - Already uppercase 2-char (e.g., "KR") → Result.success("KR")
     * - Otherwise → Result.failure
     */
    fun normalizeCountry(input: String): Result<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.success("")

        // Check Korean name mapping
        KOREAN_COUNTRY_MAP[trimmed]?.let {
            return Result.success(it)
        }

        // Try uppercase conversion for 2-char input
        val uppercased = trimmed.uppercase()
        if (ISO_COUNTRY_REGEX.matches(uppercased)) {
            return Result.success(uppercased)
        }

        return Result.failure(
            IllegalArgumentException("유효하지 않은 국가 코드입니다. 2자리 영문 코드를 입력해주세요. (예: KR)")
        )
    }

    /**
     * Converts a date string to timezone-aware ISO 8601 for trip start.
     * "2026-08-20" → "2026-08-20T00:00:00+09:00"
     *
     * - Empty/blank → Result.success("")
     * - Already has offset → pass through
     * - YYYY-MM-DD → append T00:00:00+09:00
     * - Invalid → Result.failure
     */
    fun toIso8601Start(dateStr: String): Result<String> {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return Result.success("")

        // Already has timezone offset — pass through
        if (HAS_OFFSET_REGEX.containsMatchIn(trimmed)) {
            return Result.success(trimmed)
        }

        // Try to parse as YYYY-MM-DD
        if (DATE_ONLY_REGEX.matches(trimmed)) {
            return try {
                LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
                Result.success("${trimmed}T00:00:00$KST_OFFSET")
            } catch (e: DateTimeParseException) {
                Result.failure(
                    IllegalArgumentException("날짜 형식이 올바르지 않습니다. (예: 2026-08-20)")
                )
            }
        }

        return Result.failure(
            IllegalArgumentException("날짜 형식이 올바르지 않습니다. (예: 2026-08-20)")
        )
    }

    /**
     * Converts a date string to timezone-aware ISO 8601 for trip end.
     * "2026-08-23" → "2026-08-23T23:59:59+09:00"
     *
     * - Empty/blank → Result.success("")
     * - Already has offset → pass through
     * - YYYY-MM-DD → append T23:59:59+09:00
     * - Invalid → Result.failure
     */
    fun toIso8601End(dateStr: String): Result<String> {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return Result.success("")

        // Already has timezone offset — pass through
        if (HAS_OFFSET_REGEX.containsMatchIn(trimmed)) {
            return Result.success(trimmed)
        }

        // Try to parse as YYYY-MM-DD
        if (DATE_ONLY_REGEX.matches(trimmed)) {
            return try {
                LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE)
                Result.success("${trimmed}T23:59:59$KST_OFFSET")
            } catch (e: DateTimeParseException) {
                Result.failure(
                    IllegalArgumentException("날짜 형식이 올바르지 않습니다. (예: 2026-08-20)")
                )
            }
        }

        return Result.failure(
            IllegalArgumentException("날짜 형식이 올바르지 않습니다. (예: 2026-08-20)")
        )
    }

    /**
     * Validates that end date is not before start date.
     * Both inputs should be YYYY-MM-DD strings (pre-conversion).
     *
     * - Either empty → Result.success(Unit)
     * - end < start → Result.failure
     * - end >= start → Result.success(Unit)
     */
    fun validateDateRange(startStr: String, endStr: String): Result<Unit> {
        val startTrimmed = startStr.trim()
        val endTrimmed = endStr.trim()

        if (startTrimmed.isEmpty() || endTrimmed.isEmpty()) {
            return Result.success(Unit)
        }

        return try {
            val startDate = LocalDate.parse(startTrimmed, DateTimeFormatter.ISO_LOCAL_DATE)
            val endDate = LocalDate.parse(endTrimmed, DateTimeFormatter.ISO_LOCAL_DATE)
            if (endDate.isBefore(startDate)) {
                Result.failure(
                    IllegalArgumentException("종료 날짜는 시작 날짜 이후여야 합니다.")
                )
            } else {
                Result.success(Unit)
            }
        } catch (e: DateTimeParseException) {
            // Date parsing errors are handled by toIso8601Start/End; 
            // if we get here with unparseable dates, just pass through
            Result.success(Unit)
        }
    }
}
