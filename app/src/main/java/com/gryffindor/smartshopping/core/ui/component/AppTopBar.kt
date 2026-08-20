package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * App top bar — Figma: top bar component (189:755)
 *
 * Specs: 68dp top padding (status bar area), then center-aligned title
 * with left (back) and right (optional) icon areas (36x36 each)
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Status bar padding — Figma: 68dp top
        Spacer(modifier = Modifier.height(52.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            if (showBackButton) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                        .clickable { onBackClick() }
                        .padding(6.dp),
                    tint = colors.textPrimary
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium, // title-2: 18/SemiBold
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            if (trailingContent != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    trailingContent()
                }
            }
        }
    }
}
