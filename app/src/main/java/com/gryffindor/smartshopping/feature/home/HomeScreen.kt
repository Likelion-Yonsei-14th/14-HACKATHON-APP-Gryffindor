package com.gryffindor.smartshopping.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToShopping: (sessionId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when sessionId is available
    LaunchedEffect(uiState.sessionId) {
        uiState.sessionId?.let { sessionId ->
            onNavigateToShopping(sessionId)
            viewModel.resetSessionNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Smart Shopping",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Meta Ray-Ban으로 쇼핑을 시작하세요",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.datUpdateRequired) {
            // DAT update required UI
            DatUpdateSection(
                errorMessage = uiState.errorMessage,
                datUpdateError = uiState.datUpdateError,
                onRequestUpdate = { viewModel.requestGlassesUpdate() },
                onRetry = { viewModel.retryCamera() }
            )
        } else if (uiState.isStarting) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { viewModel.startShopping() }) {
                Text("쇼핑 시작")
            }
        }

        // General error message (non-DAT-update)
        if (!uiState.datUpdateRequired && uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DatUpdateSection(
    errorMessage: String?,
    datUpdateError: String?,
    onRequestUpdate: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = errorMessage ?: "스마트글래스 앱 업데이트가 필요합니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRequestUpdate) {
            Text("안경 앱 업데이트")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = onRetry) {
            Text("재시도")
        }

        // Show specific navigation error if update failed
        datUpdateError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
