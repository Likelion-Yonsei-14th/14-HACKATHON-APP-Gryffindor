package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * Figma "mute" 오버레이(예: 마이페이지 로그아웃 확인, 376:5385) — 화면 전체를 50% 검정으로
 * 덮고 가운데 메시지 + 예/아니오 버튼을 띄운다. 확인이 필요한 다른 액션(삭제 등)에도
 * 재사용 가능.
 */
@Composable
fun LooketConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.common_yes),
    dismissText: String = stringResource(R.string.common_no),
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(25.dp),
            ) {
                Text(
                    text = message,
                    style = LooketTextStyles.titleOne,
                    color = LooketColors.TextInverse,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LooketSmallButton(
                        text = confirmText,
                        backgroundColor = LooketColors.BrandSecondary,
                        contentColor = LooketColors.TextInverse,
                        onClick = onConfirm,
                    )
                    LooketSmallButton(
                        text = dismissText,
                        backgroundColor = LooketColors.SurfaceSubtle,
                        contentColor = LooketColors.TextSecondary,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}
