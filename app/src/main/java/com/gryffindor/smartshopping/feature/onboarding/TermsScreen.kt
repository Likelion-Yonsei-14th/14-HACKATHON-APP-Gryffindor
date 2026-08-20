package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Terms screen — Figma node 376:5318 (온보딩_이용약관)
 *
 * Layout:
 * - Background: muted overlay (rgba(0,0,0,0.5)) over user info screen
 * - Bottom sheet modal (surface bg, 12dp top radius):
 *   - Handle indicator
 *   - Title: "앱 사용을 위해\n정보수집에 동의해주세요." (title-1)
 *   - 4 policy items with check + label + right arrow
 *   - Button "시작하기" (disabled until all checked)
 */
@Composable
fun TermsScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current

    var termsOfService by remember { mutableStateOf(false) }
    var privacyPolicy by remember { mutableStateOf(false) }
    var locationData by remember { mutableStateOf(false) }
    var marketingOptional by remember { mutableStateOf(false) }

    val requiredAllChecked = termsOfService && privacyPolicy && locationData

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background — muted overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundMuted)
        )

        // Bottom sheet modal — Figma: positioned at y=458, height=554 in 1012 frame
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(colors.backgroundSurface)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle indicator — Figma: indicator component at top
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderDisabled)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title — Figma: title-1 (24/Bold)
            Text(
                text = "앱 사용을 위해\n정보수집에 동의해주세요.",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Policy items — Figma: 16dp gap between items
            Column(
                modifier = Modifier.padding(horizontal = 27.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PolicyItem(
                    label = "이용약관",
                    checked = termsOfService,
                    onCheckedChange = { termsOfService = it }
                )
                PolicyItem(
                    label = "개인정보처리방침",
                    checked = privacyPolicy,
                    onCheckedChange = { privacyPolicy = it }
                )
                PolicyItem(
                    label = "위치 정보 이용 동의",
                    checked = locationData,
                    onCheckedChange = { locationData = it }
                )
                PolicyItem(
                    label = "마케팅 수신 동의 (선택)",
                    checked = marketingOptional,
                    onCheckedChange = { marketingOptional = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Button — Figma: "시작하기", disabled state
            PrimaryButton(
                text = "시작하기",
                onClick = onNext,
                enabled = requiredAllChecked,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Policy item row — Figma: policy component
 * Layout: [check icon 44x44] [label body-2] --- [right arrow 24x24]
 */
@Composable
private fun PolicyItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check icon — Figma: check component, 44x44
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left), // TODO: replace with check icon
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = if (checked) colors.brandPrimary else colors.textDisabled
            )
            // Label — Figma: body-2 (14/Regular)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        // Right arrow — Figma: right icon 24x24
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left), // TODO: replace with right arrow icon
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = colors.textTertiary
        )
    }
}
