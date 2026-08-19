package com.gryffindor.smartshopping.feature.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.domain.model.Flight

/**
 * Flight info confirmation screen for trip registration.
 * Shows actual OCR-analyzed flight data (not hardcoded defaults).
 * On confirm → creates Trip → attaches Flight → navigates to TripDetail.
 */
@Composable
fun TripFlightConfirmScreen(
    viewModel: TripRegistrationViewModel,
    onTripCreated: (tripId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val uiState by viewModel.uiState.collectAsState()

    // Get the analyzed flight from the ViewModel
    val flight = viewModel.getAnalyzedFlight()

    // React to completion
    when (val state = uiState) {
        is TripRegistrationUiState.Completed -> {
            onTripCreated(state.trip.id)
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
            text = "항공편 정보가 맞는지\n다시 한번 확인해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (flight != null) {
            // Display actual analyzed flight data
            FlightInfoRow(label = "출발지", value = flight.departureAirport ?: "-")
            FlightInfoRow(label = "도착지", value = flight.arrivalAirport ?: "-")
            FlightInfoRow(label = "터미널", value = flight.terminal ?: "-")
            FlightInfoRow(label = "편명", value = flight.flightNumber ?: "-")
            FlightInfoRow(label = "출발 시간", value = flight.departureAt ?: "-")
            FlightInfoRow(label = "도착 시간", value = flight.arrivalAt ?: "-")
            FlightInfoRow(label = "공항 도착 예정", value = flight.airportArrivalAt ?: "-")
        } else {
            Text(
                text = "항공편 정보를 불러올 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.semanticRed
            )
        }

        if (uiState is TripRegistrationUiState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (uiState as TripRegistrationUiState.Error).message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.semanticRed
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (uiState is TripRegistrationUiState.Creating) {
            CircularProgressIndicator(
                color = colors.brandPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        PrimaryButton(
            text = "확인",
            onClick = {
                viewModel.confirmAndCreateTrip()
            },
            enabled = flight != null && uiState !is TripRegistrationUiState.Creating
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FlightInfoRow(
    label: String,
    value: String
) {
    val colors = LocalAppColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary
            )
        }
        HorizontalDivider(color = colors.borderDisabled)
    }
}
