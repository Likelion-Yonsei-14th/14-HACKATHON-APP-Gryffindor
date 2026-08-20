package com.gryffindor.smartshopping.feature.onboarding

import android.Manifest
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

@Composable
fun PermissionScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    // Track permission states
    var notificationGranted by remember { mutableStateOf(checkNotificationPermission(context)) }
    var bluetoothGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launchers
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

    // All required permissions granted
    val allGranted = bluetoothGranted && locationGranted

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
            icon = R.drawable.ic_permission_notification,
            title = "알림",
            description = "알림 메시지 발송",
            granted = notificationGranted
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionItem(
            icon = R.drawable.ic_permission_bluetooth,
            title = "Meta 계정",
            description = "글래스 시선 및 제품 인식 데이터 가져오기",
            granted = bluetoothGranted
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionItem(
            icon = R.drawable.ic_permission_location,
            title = "위치",
            description = "매장 위치 정보 인식",
            granted = locationGranted
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!allGranted) {
            PrimaryButton(
                text = "권한 요청하기",
                onClick = {
                    val permissions = mutableListOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "다음",
            onClick = onNext,
            enabled = allGranted
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionItem(
    icon: Int,
    title: String,
    description: String,
    granted: Boolean = false
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
            tint = if (granted) colors.brandPrimary else colors.textPrimary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        if (granted) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = colors.brandPrimary
            )
        }
    }
}

private fun checkNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true // Pre-Tiramisu doesn't require runtime permission for notifications
    }
}
