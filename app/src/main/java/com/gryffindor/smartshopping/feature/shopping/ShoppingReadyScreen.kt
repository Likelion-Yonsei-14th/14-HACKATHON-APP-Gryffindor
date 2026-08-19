package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Shopping ready screen — user must explicitly tap "시작" to begin live shopping.
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        Text(
            text = "쇼핑 준비 완료",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (storeName.isNotBlank()) {
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.brandPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "스마트 글래스를 착용하고\n매장을 둘러보세요.\n\n관심 있게 바라본 제품이\n자동으로 인식됩니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "쇼핑 시작",
            onClick = onStart
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
