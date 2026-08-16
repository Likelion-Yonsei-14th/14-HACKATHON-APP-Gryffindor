package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * 접힌 상태엔 [label]을 고정 표시하고(선택된 값이 아님), 펼치면 [options] 목록에서
 * [selectedOption]에 해당하는 항목만 강조 표시한다 — Figma "dropdown-s" 컴포넌트 동작 그대로.
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
    val borderColor = if (expanded) LooketColors.BrandPrimary else LooketColors.BorderDefault

    Column(modifier = modifier.width(180.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .background(LooketColors.Surface)
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = LooketColors.TextPrimary,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(2.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, LooketColors.BrandPrimary, RoundedCornerShape(10.dp)),
            ) {
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Text(
                        text = option,
                        style = LooketTextStyles.bodyTwo,
                        color = LooketColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) LooketColors.BrandPrimarySubtle else LooketColors.Surface)
                            .clickable {
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
