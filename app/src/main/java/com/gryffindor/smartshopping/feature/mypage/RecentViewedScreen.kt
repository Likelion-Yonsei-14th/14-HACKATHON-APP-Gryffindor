package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.AppTopBar
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Recent viewed products screen — Figma node 54:894 (최근 본 제품)
 *
 * NOTE: No dedicated backend endpoint exists for "recently viewed" products.
 * This screen shows an empty state rather than creating fake data.
 * When a persistent history contract is available, this will be connected.
 * UI is kept per Figma even though no data is available yet.
 */
@Composable
fun RecentViewedScreen(
    onNavigateBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
    ) {
        // Top bar — Figma: top bar with back button
        AppTopBar(
            title = "최근 본 제품",
            showBackButton = true,
            onBackClick = onNavigateBack
        )

        // Empty state — no persistent recent history endpoint
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "최근 본 제품이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary
                )
                // TODO: Connect when backend persistent history contract is established
            }
        }
    }
}
