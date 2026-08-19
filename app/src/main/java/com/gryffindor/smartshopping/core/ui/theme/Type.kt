package com.gryffindor.smartshopping.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography based on Figma design system.
 * Font family: Pretendard Variable (uses system default sans-serif as fallback).
 */
val AppTypography = Typography(
    // Display — 28/ExtraBold (서비스명/텍스트/display)
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.4.sp // 1.3em
    ),
    // Title-1 — 24/Bold (서비스명/텍스트/title-1)
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 31.2.sp
    ),
    // Heading — 20/Bold (서비스명/텍스트/heading)
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    // Title-2 — 18/SemiBold (서비스명/텍스트/title-2)
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.4.sp
    ),
    // Body-1 — 16/SemiBold (서비스명/텍스트/body-1)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.8.sp
    ),
    // Body-2 — 14/Regular (서비스명/텍스트/body-2)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.2.sp
    ),
    // Body-3 — 12/Medium (서비스명/텍스트/body-3)
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.6.sp
    ),
    // Caption — 12/Regular (서비스명/텍스트/caption)
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 15.6.sp
    ),
    // Label — 12/SemiBold (서비스명/텍스트/label)
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 15.6.sp
    )
)
