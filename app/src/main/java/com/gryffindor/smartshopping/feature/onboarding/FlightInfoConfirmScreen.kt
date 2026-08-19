package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

@Composable
fun FlightInfoConfirmScreen(
    departureAirport: String = "BEJ",
    arrivalAirport: String = "ICN",
    terminal: String = "인천공항 T2",
    departureAt: String = "2026.08.21 10:00",
    arrivalAt: String = "2026.08.25 19:00",
    airportArrivalAt: String = "2026.08.25 15:00",
    onConfirm: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "항공편 정보가 맞는지 \n다시 한번 확인해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Flight info fields
        FlightInfoRow(label = "출발지", value = departureAirport)
        FlightInfoRow(label = "도착지", value = arrivalAirport)
        FlightInfoRow(label = "터미널", value = terminal)
        FlightInfoRow(label = "출발 시간", value = departureAt)
        FlightInfoRow(label = "도착 시간", value = arrivalAt)
        FlightInfoRow(label = "공항 도착 예정시간", value = airportArrivalAt)

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "확인",
            onClick = onConfirm
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
                color = colors.textPrimary,
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
