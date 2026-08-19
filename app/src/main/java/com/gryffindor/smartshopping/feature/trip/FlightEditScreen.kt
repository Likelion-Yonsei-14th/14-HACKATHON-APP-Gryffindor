package com.gryffindor.smartshopping.feature.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.domain.model.Flight

@Composable
fun FlightEditScreen(
    viewModel: TripViewModel,
    flightId: String,
    tripId: String,
    onSaved: () -> Unit
) {
    val state by viewModel.detailState.collectAsState()

    // Find the flight from current detail
    val flight: Flight? = state.tripDetail?.flights?.find { it.id == flightId }

    var departureAirport by remember(flight) { mutableStateOf(flight?.departureAirport ?: "") }
    var arrivalAirport by remember(flight) { mutableStateOf(flight?.arrivalAirport ?: "") }
    var terminal by remember(flight) { mutableStateOf(flight?.terminal ?: "") }
    var flightNumber by remember(flight) { mutableStateOf(flight?.flightNumber ?: "") }
    var departureAt by remember(flight) { mutableStateOf(flight?.departureAt ?: "") }
    var arrivalAt by remember(flight) { mutableStateOf(flight?.arrivalAt ?: "") }
    var airportArrivalAt by remember(flight) { mutableStateOf(flight?.airportArrivalAt ?: "") }

    LaunchedEffect(state.flightSaved) {
        if (state.flightSaved) {
            viewModel.clearFlightSaved()
            onSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "항공편 수정",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = departureAirport,
            onValueChange = { departureAirport = it },
            label = { Text("출발 공항") },
            placeholder = { Text("예: ICN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = arrivalAirport,
            onValueChange = { arrivalAirport = it },
            label = { Text("도착 공항") },
            placeholder = { Text("예: KUL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = terminal,
            onValueChange = { terminal = it },
            label = { Text("터미널") },
            placeholder = { Text("예: 제1터미널") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = flightNumber,
            onValueChange = { flightNumber = it },
            label = { Text("편명") },
            placeholder = { Text("예: SQ 607") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = departureAt,
            onValueChange = { departureAt = it },
            label = { Text("출발 시간") },
            placeholder = { Text("예: 2026-08-23T10:00:00Z") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = arrivalAt,
            onValueChange = { arrivalAt = it },
            label = { Text("도착 시간") },
            placeholder = { Text("예: 2026-08-23T16:00:00Z") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = airportArrivalAt,
            onValueChange = { airportArrivalAt = it },
            label = { Text("공항 도착 예정 시간") },
            placeholder = { Text("예: 2026-08-23T08:00:00Z") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isSavingFlight) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Button(
                onClick = {
                    viewModel.updateFlight(
                        flightId = flightId,
                        tripId = tripId,
                        departureAirport = departureAirport,
                        arrivalAirport = arrivalAirport,
                        terminal = terminal,
                        flightNumber = flightNumber,
                        departureAt = departureAt,
                        arrivalAt = arrivalAt,
                        airportArrivalAt = airportArrivalAt
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장")
            }
        }

        state.flightError?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
