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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Terms screen — Figma node 376:5318 (온보딩_이용약관)
 *
 * Figma policy component:
 * - Left: check icon (44x44) — 탭하면 동의 토글. 동의됨=primary, 미동의=disabled
 * - Center: 약관 이름 (body-2)
 * - Right: right arrow (24x24) — 탭하면 전문 보기
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

    var showTermsDetail by remember { mutableStateOf<String?>(null) }

    val requiredAllChecked = termsOfService && privacyPolicy && locationData

    Box(modifier = Modifier.fillMaxSize()) {
        // Muted background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundMuted)
        )

        // Bottom sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(colors.backgroundSurface)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderDisabled)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "앱 사용을 위해\n정보수집에 동의해주세요.",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (showTermsDetail != null) {
                // 전문 보기 (동일 페이지 내)
                TermsDetailView(
                    title = showTermsDetail!!,
                    onBack = { showTermsDetail = null }
                )
            } else {
                // 약관 체크리스트 — Figma policy component 그대로
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    PolicyItem(
                        label = "이용약관",
                        checked = termsOfService,
                        onCheckedChange = { termsOfService = it },
                        onDetailClick = { showTermsDetail = "이용약관" }
                    )
                    PolicyItem(
                        label = "개인정보처리방침",
                        checked = privacyPolicy,
                        onCheckedChange = { privacyPolicy = it },
                        onDetailClick = { showTermsDetail = "개인정보처리방침" }
                    )
                    PolicyItem(
                        label = "위치 정보 이용 동의",
                        checked = locationData,
                        onCheckedChange = { locationData = it },
                        onDetailClick = { showTermsDetail = "위치 정보 이용 동의" }
                    )
                    PolicyItem(
                        label = "마케팅 수신 동의 (선택)",
                        checked = marketingOptional,
                        onCheckedChange = { marketingOptional = it },
                        onDetailClick = { showTermsDetail = "마케팅 수신 동의" }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(
                    text = "시작하기",
                    onClick = onNext,
                    enabled = requiredAllChecked,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Figma policy component 재현:
 * Row { [check 44x44] [label body-2] --- [right 24x24] }
 *
 * check: Icons.Filled.Check (체크마크), 동의=primary, 미동의=disabled
 * right: Icons.AutoMirrored.Filled.KeyboardArrowRight (오른쪽 화살표)
 */
@Composable
private fun PolicyItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDetailClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 왼쪽: 체크 아이콘 + 라벨 (탭=토글)
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check icon — Figma: check component 44x44
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (checked) colors.brandSecondary else colors.textDisabled
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )
        }

        // 오른쪽: right arrow — Figma: right 24x24 (전문 보기)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onDetailClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.gryffindor.smartshopping.R.drawable.ic_arrow_left),
                contentDescription = "전문 보기",
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f), // left → right
                tint = colors.textTertiary
            )
        }
    }
}

@Composable
private fun TermsDetailView(
    title: String,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.gryffindor.smartshopping.R.drawable.ic_arrow_left),
                contentDescription = "뒤로",
                modifier = Modifier.size(24.dp),
                tint = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "약관 전문 내용이 여기에 표시됩니다.\n\n(추후 업데이트 예정)",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = "확인",
            onClick = onBack
        )
    }
}
