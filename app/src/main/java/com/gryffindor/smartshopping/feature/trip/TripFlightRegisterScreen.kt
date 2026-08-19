package com.gryffindor.smartshopping.feature.trip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Flight ticket photo registration screen for trip creation.
 * Uses TripRegistrationViewModel — only analyzes the flight (OCR), does NOT create Trip.
 * Trip creation happens after user confirmation on the confirm screen.
 */
@Composable
fun TripFlightRegisterScreen(
    viewModel: TripRegistrationViewModel,
    onFlightAnalyzed: (flightId: String) -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            imageBytes = bytes
        }
    }

    // React to state changes
    when (val state = uiState) {
        is TripRegistrationUiState.FlightReady -> {
            onFlightAnalyzed(state.flight.id)
        }
        else -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "이번 여행의 항공편 사진을\n등록해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "항공권 사진을 분석하여 여행 정보를 자동으로 등록합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Image placeholder / selected area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            shape = RoundedCornerShape(8.dp),
            color = colors.backgroundEmphasized,
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState is TripRegistrationUiState.Analyzing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = colors.brandPrimary
                        )
                    }
                    selectedImageUri != null -> {
                        Text(
                            text = "이미지 선택됨\n탭하여 다시 선택",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    else -> {
                        Text(
                            text = "탭하여 항공권 사진을 선택해주세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        if (uiState is TripRegistrationUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (uiState as TripRegistrationUiState.Error).message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.semanticRed
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "분석하기",
            onClick = {
                imageBytes?.let { bytes ->
                    viewModel.analyzeFlight(bytes)
                }
            },
            enabled = imageBytes != null && uiState !is TripRegistrationUiState.Analyzing
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "건너뛰기",
            onClick = onSkip,
            enabled = uiState !is TripRegistrationUiState.Analyzing
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
