package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

@Composable
fun TermsScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current

    var allAgreed by remember { mutableStateOf(false) }
    var termsOfService by remember { mutableStateOf(false) }
    var privacyPolicy by remember { mutableStateOf(false) }
    var locationData by remember { mutableStateOf(false) }
    var marketingOptional by remember { mutableStateOf(false) }

    // All required terms must be checked
    val requiredAllChecked = termsOfService && privacyPolicy && locationData

    // Sync "allAgreed" with individual states
    fun updateAllAgreed() {
        allAgreed = termsOfService && privacyPolicy && locationData && marketingOptional
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "서비스 이용 약관에\n동의해 주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // All agree
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val newState = !allAgreed
                    allAgreed = newState
                    termsOfService = newState
                    privacyPolicy = newState
                    locationData = newState
                    marketingOptional = newState
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = allAgreed,
                onCheckedChange = { checked ->
                    allAgreed = checked
                    termsOfService = checked
                    privacyPolicy = checked
                    locationData = checked
                    marketingOptional = checked
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.brandPrimary,
                    uncheckedColor = colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "전체 동의",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        HorizontalDivider(color = colors.borderDisabled)

        Spacer(modifier = Modifier.height(8.dp))

        // Individual terms
        TermItem(
            label = "[필수] 서비스 이용약관 동의",
            checked = termsOfService,
            onCheckedChange = {
                termsOfService = it
                updateAllAgreed()
            },
            colors = colors
        )

        TermItem(
            label = "[필수] 개인정보 처리방침 동의",
            checked = privacyPolicy,
            onCheckedChange = {
                privacyPolicy = it
                updateAllAgreed()
            },
            colors = colors
        )

        TermItem(
            label = "[필수] 위치 정보 이용 동의",
            checked = locationData,
            onCheckedChange = {
                locationData = it
                updateAllAgreed()
            },
            colors = colors
        )

        TermItem(
            label = "[선택] 마케팅 정보 수신 동의",
            checked = marketingOptional,
            onCheckedChange = {
                marketingOptional = it
                updateAllAgreed()
            },
            colors = colors
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "다음",
            onClick = onNext,
            enabled = requiredAllChecked
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TermItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: com.gryffindor.smartshopping.core.ui.theme.AppColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = colors.brandPrimary,
                uncheckedColor = colors.textSecondary
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
    }
}
