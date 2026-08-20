package com.gryffindor.smartshopping.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 온보딩_접근권한(Figma 376:5288). feat/production-ui의 실제 런타임 권한 요청 로직을
 * 우리 디자인 시스템(LooketColors/LooketPrimaryButton)으로 옮겨왔다 — 기존 버전은
 * 행을 누르면 그냥 로컬 Set에 추가만 하는 가짜 UI였음(실제 권한 다이얼로그가 안 떴음).
 */
@Composable
fun PermissionScreen(onNext: () -> Unit) {
    val context = LocalContext.current

    var notificationGranted by remember { mutableStateOf(checkNotificationPermission(context)) }
    var bluetoothGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, granted) ->
            when (permission) {
                Manifest.permission.BLUETOOTH_CONNECT -> bluetoothGranted = granted
                Manifest.permission.ACCESS_FINE_LOCATION -> locationGranted = granted
                Manifest.permission.POST_NOTIFICATIONS -> notificationGranted = granted
            }
        }
    }

    val allGranted = bluetoothGranted && locationGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LooketColors.Surface)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(136.dp))

        Text(
            text = "앱 사용을 위해\n접근 권한을 허용해주세요.",
            style = LooketTextStyles.titleOne,
            color = LooketColors.TextPrimary,
        )

        Spacer(modifier = Modifier.height(144.dp))

        PermissionItem(
            icon = R.drawable.ic_permission_notification,
            granted = notificationGranted,
            title = "알림",
            description = "알림 메시지 발송",
        )
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            icon = R.drawable.ic_permission_bluetooth,
            granted = bluetoothGranted,
            title = "Meta 계정",
            description = "글래스 시선 및 제품 인식 데이터 가져오기",
        )
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            icon = R.drawable.ic_permission_location,
            granted = locationGranted,
            title = "위치",
            description = "매장 위치 정보 인식",
        )

        Spacer(modifier = Modifier.weight(1f))

        LooketPrimaryButton(
            text = "다음",
            onClick = {
                if (!allGranted) {
                    val permissions = mutableListOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                } else {
                    onNext()
                }
            },
            enabled = allGranted,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Figma 규칙: 허용됨 → 주황색(BrandSecondary), 미허용 → 회색(TextDisabled)
@Composable
private fun PermissionItem(icon: Int, granted: Boolean, title: String, description: String) {
    val iconTint = if (granted) LooketColors.BrandSecondary else LooketColors.TextDisabled

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = iconTint,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = description, style = LooketTextStyles.bodyTwo, color = LooketColors.TextSecondary)
    }
}

private fun checkNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PermissionScreenPreview() {
    LooketTheme {
        PermissionScreen(onNext = {})
    }
}
