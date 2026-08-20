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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 온보딩_이용약관(Figma 376:5318). feat/production-ui의 약관 동의 체크리스트 로직을
 * 우리 디자인 시스템으로 옮겨왔다 — 원래 우리 쪽엔 별도 이용약관 단계가 없었음.
 *
 * Figma policy component: [check 44x44][라벨][전문 보기 화살표 24x24].
 */
@Composable
fun TermsScreen(onNext: () -> Unit) {
    var termsOfService by remember { mutableStateOf(false) }
    var privacyPolicy by remember { mutableStateOf(false) }
    var locationData by remember { mutableStateOf(false) }
    var marketingOptional by remember { mutableStateOf(false) }

    var showTermsDetail by remember { mutableStateOf<String?>(null) }

    val requiredAllChecked = termsOfService && privacyPolicy && locationData

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(LooketColors.Surface)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LooketColors.BorderDisabled),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "앱 사용을 위해\n정보수집에 동의해주세요.",
                style = LooketTextStyles.titleOne,
                color = LooketColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (showTermsDetail != null) {
                TermsDetailView(
                    title = showTermsDetail!!,
                    onBack = { showTermsDetail = null },
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PolicyItem(
                        label = "이용약관",
                        checked = termsOfService,
                        onCheckedChange = { termsOfService = it },
                        onDetailClick = { showTermsDetail = "이용약관" },
                    )
                    PolicyItem(
                        label = "개인정보처리방침",
                        checked = privacyPolicy,
                        onCheckedChange = { privacyPolicy = it },
                        onDetailClick = { showTermsDetail = "개인정보처리방침" },
                    )
                    PolicyItem(
                        label = "위치 정보 이용 동의",
                        checked = locationData,
                        onCheckedChange = { locationData = it },
                        onDetailClick = { showTermsDetail = "위치 정보 이용 동의" },
                    )
                    PolicyItem(
                        label = "마케팅 수신 동의 (선택)",
                        checked = marketingOptional,
                        onCheckedChange = { marketingOptional = it },
                        onDetailClick = { showTermsDetail = "마케팅 수신 동의" },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                LooketPrimaryButton(
                    text = "시작하기",
                    onClick = onNext,
                    enabled = requiredAllChecked,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun PolicyItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDetailClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "✓",
                    style = LooketTextStyles.titleOne,
                    color = if (checked) LooketColors.BrandSecondary else LooketColors.TextDisabled,
                )
            }
            Text(text = label, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onDetailClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "전문 보기",
                modifier = Modifier.size(20.dp).rotate(180f),
                tint = LooketColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun TermsDetailView(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "뒤로",
                modifier = Modifier.size(24.dp),
                tint = LooketColors.TextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "약관 전문 내용이 여기에 표시됩니다.\n\n(추후 업데이트 예정)",
                style = LooketTextStyles.bodyTwo,
                color = LooketColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LooketPrimaryButton(text = "확인", onClick = onBack)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun TermsScreenPreview() {
    LooketTheme {
        TermsScreen(onNext = {})
    }
}
