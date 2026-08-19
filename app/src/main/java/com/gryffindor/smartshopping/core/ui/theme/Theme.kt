package com.gryffindor.smartshopping.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended color palette accessible via LocalAppColors.current.
 */
data class AppColors(
    val brandPrimary: Color = BrandPrimary,
    val brandPrimaryPressed: Color = BrandPrimaryPressed,
    val brandPrimarySubtle: Color = BrandPrimarySubtle,
    val brandPrimaryDisabled: Color = BrandPrimaryDisabled,
    val brandSecondary: Color = BrandSecondary,
    val backgroundSurface: Color = BackgroundSurface,
    val backgroundSubtle: Color = BackgroundSubtle,
    val backgroundEmphasized: Color = BackgroundEmphasized,
    val backgroundMuted: Color = BackgroundMuted,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val textDisabled: Color = TextDisabled,
    val textBrand: Color = TextBrand,
    val textInverse: Color = TextInverse,
    val borderDefault: Color = BorderDefault,
    val borderDisabled: Color = BorderDisabled,
    val semanticRed: Color = SemanticRed,
    val semanticRedLight: Color = SemanticRedLight,
    val semanticGreen: Color = SemanticGreen
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = TextInverse,
    primaryContainer = BrandPrimarySubtle,
    secondary = BrandSecondary,
    onSecondary = TextInverse,
    background = BackgroundSurface,
    onBackground = TextPrimary,
    surface = BackgroundSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSubtle,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    outlineVariant = BorderDisabled,
    error = SemanticRed,
    onError = TextInverse
)

@Composable
fun SmartShoppingTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
