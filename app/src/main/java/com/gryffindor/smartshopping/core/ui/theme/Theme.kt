package com.gryffindor.smartshopping.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LooketLightColorScheme = lightColorScheme(
    primary = LooketColors.BrandPrimary,
    onPrimary = LooketColors.Surface,
    primaryContainer = LooketColors.BrandPrimarySubtle,
    onPrimaryContainer = LooketColors.BrandPrimary,
    background = LooketColors.Surface,
    surface = LooketColors.Surface,
    onBackground = LooketColors.TextPrimary,
    onSurface = LooketColors.TextPrimary,
    outline = LooketColors.BorderDisabled,
    onSurfaceVariant = LooketColors.TextDisabled,
)

// Figma 디자인에 다크 모드 배리언트가 없어 라이트 테마만 정의한다.
@Composable
fun LooketTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LooketLightColorScheme,
        typography = LooketTypography,
        content = content
    )
}
