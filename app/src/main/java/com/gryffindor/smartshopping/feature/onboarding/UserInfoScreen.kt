package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.core.ui.theme.Pretendard

@Composable
fun UserInfoScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current
    var nickname by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("한국어") }
    var selectedCurrency by remember { mutableStateOf("₩") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(136.dp))

        Text(
            text = "Welcome!\n사용자 정보를 입력해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(59.dp))

        // 닉네임
        Text(
            text = "닉네임",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )

        BasicTextField(
            value = nickname,
            onValueChange = { nickname = it },
            textStyle = TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
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
                    HorizontalDivider(color = colors.textPrimary, thickness = 1.dp)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 언어
        Text(
            text = "언어",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        InlineDropdown(
            value = selectedLanguage,
            options = listOf("한국어", "English", "中文"),
            onSelected = { selectedLanguage = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 사용 화폐
        Text(
            text = "사용 화폐",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        InlineDropdown(
            value = selectedCurrency,
            options = listOf("₩", "$", "¥"),
            onSelected = { selectedCurrency = it }
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
 * Inline dropdown — 선택 시 아래로 옵션 리스트가 펼쳐지는 방식.
 * 화살표는 항상 아래 방향 고정.
 * Figma: dropdown-s (180dp, border #D7D6E1, radius 10dp)
 */
@Composable
private fun InlineDropdown(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.width(180.dp)) {
        // 선택된 값 표시 + 화살표 (아래 방향 고정)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.borderDefault,
                    shape = RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = if (expanded) 0.dp else 10.dp,
                        bottomEnd = if (expanded) 0.dp else 10.dp
                    )
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )
            // 화살표 아래 방향 고정 (ic_arrow_left를 -90도 회전)
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(-90f)
            )
        }

        // 옵션 리스트 — 아래로 펼쳐짐 (애니메이션 포함)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.borderDefault,
                        shape = RoundedCornerShape(
                            bottomStart = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
            ) {
                options.filter { it != value }.forEach { option ->
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(option)
                                expanded = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
