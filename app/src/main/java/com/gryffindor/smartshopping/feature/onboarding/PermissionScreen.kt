package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

@Composable
fun PermissionScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(136.dp))

        Text(
            text = "앱 사용을 위해\n접근 권한을 허용해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Permission items
        PermissionItem(
            icon = R.drawable.ic_nav_home,
            title = "알림",
            description = "알림 메시지 발송"
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionItem(
            icon = R.drawable.ic_nav_home,
            title = "Meta 계정",
            description = "글래스 시선 및 제품 인식 데이터 가져오기"
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionItem(
            icon = R.drawable.ic_nav_home,
            title = "위치",
            description = "매장 위치 정보 인식"
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "다음",
            onClick = onNext,
            enabled = false // Becomes enabled after granting
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionItem(
    icon: Int,
    title: String,
    description: String
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = colors.textPrimary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }
}
