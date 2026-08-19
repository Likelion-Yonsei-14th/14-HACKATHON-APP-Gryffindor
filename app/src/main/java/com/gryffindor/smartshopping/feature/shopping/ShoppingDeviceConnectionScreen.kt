package com.gryffindor.smartshopping.feature.shopping

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
 * Device connection check screen before shopping starts.
 * Shows Meta glasses connection status using existing MetaCameraSource.
 */
@Composable
fun ShoppingDeviceConnectionScreen(
    metaCameraSource: MetaCameraSource,
    onDeviceReady: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val cameraState by metaCameraSource.cameraState.collectAsState()
    var checkComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        checkComplete = true
    }

    val isStreaming = cameraState is CameraState.Streaming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "기기 연결 확인",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "스마트 글래스의 연결 상태를 확인합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.backgroundEmphasized)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_shop),
                    contentDescription = "Smart Glasses",
                    modifier = Modifier.size(48.dp),
                    tint = if (isStreaming || checkComplete) colors.brandPrimary else colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Meta Ray-Ban Gen 2",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (!checkComplete) {
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
                            text = "연결 확인 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                } else if (isStreaming) {
                    Text(
                        text = "연결됨 · 카메라 활성",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.brandPrimary
                    )
                } else {
                    Text(
                        text = "대기 중 · 쇼핑 시작 시 자동 연결",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "※ 기기가 대기 상태여도 쇼핑을 시작할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "다음",
            onClick = onDeviceReady,
            enabled = checkComplete
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
