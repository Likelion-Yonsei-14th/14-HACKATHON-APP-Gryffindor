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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Permission screen — Figma node 376:5288 (온보딩_접근권한)
 *
 * Layout:
 * - 136dp top padding
 * - Title: "앱 사용을 위해\n접근 권한을 허용해주세요." (title-1: 24/Bold)
 * - 144dp gap
 * - 3 permission items (32dp gap between items):
 *   1. 알림 (bell icon) — 알림 메시지 발송
 *   2. Meta 계정 (Meta icon, orange #FF8633) — 글래스 시선 및 제품 인식 데이터 가져오기
 *   3. 위치 (location icon) — 매장 위치 정보 인식
 * - Bottom: "다음" button (disabled until permissions granted)
 */
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
            .padding(horizontal = 16.dp)
    ) {
        // Figma: section guide padding top 136dp
        Spacer(modifier = Modifier.height(136.dp))

        // Title — Figma: title-1 (24/Bold)
        Text(
            text = "앱 사용을 위해\n접근 권한을 허용해주세요.",
            style = MaterialTheme.typography.headlineMedium, // 24/Bold
            color = colors.textPrimary
        )

        // Figma: 144dp gap between section guide and permission items
        Spacer(modifier = Modifier.height(144.dp))

        // Permission items — Figma: 32dp gap between items
        PermissionItem(
            icon = R.drawable.ic_permission_notification,
            iconTint = Color.Unspecified, // Use original icon color
            title = "알림",
            description = "알림 메시지 발송"
        )
        Spacer(modifier = Modifier.height(32.dp))
        PermissionItem(
            icon = R.drawable.ic_permission_bluetooth,
            iconTint = colors.brandSecondary, // Figma: Meta icon uses orange #FF8633
            title = "Meta 계정",
            description = "글래스 시선 및 제품 인식 데이터 가져오기"
        )
        Spacer(modifier = Modifier.height(32.dp))
        PermissionItem(
            icon = R.drawable.ic_permission_location,
            iconTint = Color.Unspecified,
            title = "위치",
            description = "매장 위치 정보 인식"
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom button — Figma: positioned at y=805 from top
        PrimaryButton(
            text = "다음",
            onClick = {
                if (!allGranted) {
                    val permissions = mutableListOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                } else {
                    onNext()
                }
            },
            enabled = allGranted
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Individual permission item row — Figma: authority component
 *
 * Layout: [44x44 icon area] [title body-1] [description body-2]
 */
@Composable
private fun PermissionItem(
    icon: Int,
    iconTint: Color,
    title: String,
    description: String
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Figma: 44x44 icon container
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = if (iconTint == Color.Unspecified) Color.Unspecified else iconTint
        )
        // Figma: title (body-1: 16/SemiBold) — in a 44-height row
        Column(
            modifier = Modifier.padding(start = 0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge, // body-1: 16/SemiBold
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        // Figma: description (body-2: 14/Regular) — in a 44-height row
        Column(
            modifier = Modifier.padding(start = 0.dp)
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium, // body-2: 14/Regular
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 10.dp)
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
        true
    }
}
