package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * 접힌 상태엔 [selectedOption]이 있으면 그 값을, 없으면 [label]을 표시한다. 펼치면 [options]
 * 목록에서 [selectedOption]에 해당하는 항목만 강조 표시한다 — Figma "dropdown-s" 컴포넌트 동작
 * 그대로. 펼친 목록은 [Popup]으로 띄워서 레이아웃 흐름에 영향을 주지 않고(아래 콘텐츠를 밀어내지
 * 않고) 그 위에 겹쳐서 표시된다.
 */
@Composable
fun LooketDropdown(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val borderColor = if (expanded) LooketColors.BrandPrimary else LooketColors.BorderDefault
    val headerInteractionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier.width(180.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .background(LooketColors.Surface)
                .clickable(
                    interactionSource = headerInteractionSource,
                    indication = null,
                ) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = selectedOption ?: label, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = LooketColors.TextPrimary,
            )
        }

        if (expanded) {
            val gapPx = with(density) { 2.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, headerHeightPx + gapPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, LooketColors.BrandPrimary, RoundedCornerShape(10.dp)),
                ) {
                    options.forEach { option ->
                        val isSelected = option == selectedOption
                        val itemInteractionSource = remember { MutableInteractionSource() }
                        Text(
                            text = option,
                            style = LooketTextStyles.bodyTwo,
                            color = LooketColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) LooketColors.BrandPrimarySubtle else LooketColors.Surface)
                                .clickable(
                                    interactionSource = itemInteractionSource,
                                    indication = null,
                                ) {
                                    onOptionSelected(option)
                                    expanded = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
