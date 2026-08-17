package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * Figma "button_L" 컴포넌트. [enabled]가 false면 온보딩 등에서 쓰인
 * 비활성 배경(#EEEDF1)/텍스트(disabled 토큰) 조합으로 표시되고 클릭이 막힌다.
 */
@Composable
fun LooketPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val backgroundColor = if (enabled) LooketColors.BrandPrimary else LooketColors.BorderDisabled
    val contentColor = if (enabled) LooketColors.TextInverse else LooketColors.TextDisabled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = LooketTextStyles.titleTwo, color = contentColor)
    }
}
