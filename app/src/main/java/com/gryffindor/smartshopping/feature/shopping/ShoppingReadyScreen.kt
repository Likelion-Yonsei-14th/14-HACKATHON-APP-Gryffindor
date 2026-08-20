package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.AppTopBar
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Shopping ready/start screen — Figma node 376:5236 (쇼핑-실시간-쇼핑시작)
 *
 * Layout:
 * - Top bar with back button
 * - FAB_start center (96x96, brand primary, radius 8dp) — large play/start icon
 * - Bottom: "실시간 리스트" button (primary, full width)
 */
@Composable
fun ShoppingReadyScreen(
    sessionId: String,
    currency: String,
    storeName: String = "",
    onStart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
    ) {
        // Top bar — Figma: top bar with back arrow, 68dp top padding
        AppTopBar(
            title = storeName.ifBlank { "쇼핑" },
            showBackButton = true,
            onBackClick = onNavigateBack
        )

        // Center content with FAB start button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // FAB_start — Figma: 96x96, brand primary, radius 8dp, centered
            Button(
                onClick = onStart,
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_shop),
                    contentDescription = "쇼핑 시작",
                    modifier = Modifier.size(32.dp),
                    tint = colors.textInverse
                )
            }
        }

        // Bottom button — Figma: "실시간 리스트" primary button
        PrimaryButton(
            text = "실시간 리스트",
            onClick = onStart,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(56.dp))
    }
}
