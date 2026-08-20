package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.core.ui.theme.Pretendard

/**
 * User info screen — Figma node 376:5297 (온보딩_유저정보)
 *
 * Layout:
 * - Section guide: 136dp top, title "Welcome!\n사용자 정보를 입력해주세요."
 * - Nickname input (underline style): label "닉네임", input field with bottom border
 * - Language dropdown: label "언어", dropdown "LANGUAGE"
 * - Currency dropdown: label "사용 화폐", dropdown "CURRENCY"
 * - Button "다음" at bottom (disabled until nickname filled)
 */
@Composable
fun UserInfoScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current
    var nickname by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("한국어") }
    var selectedCurrency by remember { mutableStateOf("KRW") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(horizontal = 16.dp)
    ) {
        // Figma: 136dp top padding
        Spacer(modifier = Modifier.height(136.dp))

        // Title — Figma: title-1 (24/Bold)
        Text(
            text = "Welcome!\n사용자 정보를 입력해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        // Figma: 59dp gap between title and nickname
        Spacer(modifier = Modifier.height(59.dp))

        // Nickname label — Figma: body-1 (16/SemiBold)
        Text(
            text = "닉네임",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )

        // Nickname input — Figma: underline style (bottom border only)
        BasicTextField(
            value = nickname,
            onValueChange = { nickname = it },
            textStyle = TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 0.dp),
            singleLine = true,
            decorationBox = { innerTextField ->
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        if (nickname.isEmpty()) {
                            Text(
                                text = "닉네임을 입력해주세요",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textTertiary
                            )
                        }
                        innerTextField()
                    }
                    HorizontalDivider(
                        color = colors.textPrimary,
                        thickness = 1.dp
                    )
                }
            }
        )

        // Figma: 24dp gap then language/currency section
        Spacer(modifier = Modifier.height(24.dp))

        // Language — Figma: label "언어" + dropdown
        Text(
            text = "언어",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        DropdownSelector(
            value = selectedLanguage,
            onClick = { /* TODO: show language picker */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Currency — Figma: label "사용 화폐" + dropdown
        Text(
            text = "사용 화폐",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        DropdownSelector(
            value = selectedCurrency,
            onClick = { /* TODO: show currency picker */ }
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "다음",
            onClick = onNext,
            enabled = nickname.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Dropdown selector — Figma: dropdown-s component
 * 180dp wide, 12px 14px padding, border #D7D6E1, radius 10dp
 */
@Composable
private fun DropdownSelector(
    value: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .width(180.dp)
            .border(
                width = 1.dp,
                color = colors.borderDefault,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
        )
    }
}
