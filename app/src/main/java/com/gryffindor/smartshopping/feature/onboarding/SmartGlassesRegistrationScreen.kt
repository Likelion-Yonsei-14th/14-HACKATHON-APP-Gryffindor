package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.data.meta.MetaCameraSource
import com.gryffindor.smartshopping.domain.model.CameraState
import kotlinx.coroutines.delay

/**
 * Smart Glasses registration screen in onboarding.
 * Reads from MetaCameraSource to determine device connectivity state.
 * Does not create new Wearables SDK wrappers — reuses existing MetaCameraSource state.
 */
@Composable
fun SmartGlassesRegistrationScreen(
    metaCameraSource: MetaCameraSource,
    onComplete: () -> Unit
) {
    val colors = LocalAppColors.current
    val cameraState by metaCameraSource.cameraState.collectAsState()

    // Simple device state check
    var checkComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        checkComplete = true
    }

    val deviceState = when {
        !checkComplete -> DeviceState.CHECKING
        cameraState is CameraState.Streaming -> DeviceState.CONNECTED
        cameraState is CameraState.Ready -> DeviceState.REGISTERED
        cameraState is CameraState.Connecting -> DeviceState.CHECKING
        else -> DeviceState.REGISTERED // Default: assume registered but not streaming
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "스마트 글래스를\n등록해 주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Meta Ray-Ban Gen 2와 연결하여\n쇼핑 시 제품 인식 기능을 사용합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Device status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.backgroundEmphasized
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_shop),
                    contentDescription = "Smart Glasses",
                    modifier = Modifier.size(64.dp),
                    tint = colors.brandPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Meta Ray-Ban Gen 2",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (deviceState) {
                    DeviceState.CHECKING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.brandPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "기기 상태 확인 중...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                    DeviceState.CONNECTED -> {
                        Text(
                            text = "연결됨",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.brandPrimary
                        )
                    }
                    DeviceState.REGISTERED -> {
                        Text(
                            text = "등록됨 · 연결 대기",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    DeviceState.NOT_FOUND -> {
                        Text(
                            text = "기기를 찾을 수 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.semanticRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "※ 기기가 연결되지 않아도 앱을 시작할 수 있습니다.\n   쇼핑 시작 전 기기 연결이 필요합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = if (deviceState == DeviceState.CHECKING) "확인 중..." else "시작하기",
            onClick = onComplete,
            enabled = deviceState != DeviceState.CHECKING
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private enum class DeviceState {
    CHECKING,
    CONNECTED,
    REGISTERED,
    NOT_FOUND
}
