package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Material3 [androidx.compose.material3.IconButton]은 인디케이션(ripple)을 끌 방법이
 * 없어서, 마우스로 조작하는 Compose Preview에서 hover 상태가 눌린 것처럼 계속
 * 남아있는 것처럼 보인다(실기기 터치에서는 발생하지 않음). 아이콘 자체의 tint로
 * 상태를 표현하는 화면에서는 인디케이션이 굳이 필요 없어 여기서 꺼둔다.
 */
@Composable
fun LooketIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
