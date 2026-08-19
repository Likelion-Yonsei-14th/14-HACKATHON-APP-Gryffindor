package com.gryffindor.smartshopping.feature.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TripCreateScreen(
    viewModel: TripViewModel,
    onTripCreated: (tripId: String) -> Unit
) {
    val state by viewModel.createState.collectAsState()

    LaunchedEffect(state.createdTripId) {
        state.createdTripId?.let { tripId ->
            onTripCreated(tripId)
            viewModel.resetCreateState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "새 여행 만들기",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = { viewModel.updateCreateTitle(it) },
            label = { Text("여행 이름 *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.city,
            onValueChange = { viewModel.updateCreateCity(it) },
            label = { Text("도시") },
            placeholder = { Text("예: 서울") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.country,
            onValueChange = { viewModel.updateCreateCountry(it) },
            label = { Text("국가") },
            placeholder = { Text("예: KR") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.startsAt,
            onValueChange = { viewModel.updateCreateStartsAt(it) },
            label = { Text("시작 날짜") },
            placeholder = { Text("예: 2026-08-20") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.endsAt,
            onValueChange = { viewModel.updateCreateEndsAt(it) },
            label = { Text("종료 날짜") },
            placeholder = { Text("예: 2026-08-23") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isCreating) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Button(
                onClick = { viewModel.createTrip() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("생성")
            }
        }

        state.error?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
