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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HotelEditScreen(
    viewModel: TripViewModel,
    tripId: String,
    onSaved: () -> Unit
) {
    val state by viewModel.detailState.collectAsState()
    val existingHotel = state.tripDetail?.hotel

    var name by remember(existingHotel) { mutableStateOf(existingHotel?.name ?: "") }
    var address by remember(existingHotel) { mutableStateOf(existingHotel?.address ?: "") }

    LaunchedEffect(state.hotelSaved) {
        if (state.hotelSaved) {
            viewModel.clearHotelSaved()
            onSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = if (existingHotel == null) "숙소 등록" else "숙소 수정",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("숙소명 *") },
            placeholder = { Text("예: 롯데호텔 명동") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("주소") },
            placeholder = { Text("예: 서울시 중구 을지로 30") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "* 위치 좌표는 자동으로 서울 중심부로 설정됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isSavingHotel) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    viewModel.upsertHotel(
                        tripId = tripId,
                        name = name,
                        address = address.ifBlank { null },
                        latitude = existingHotel?.latitude ?: HotelDefaults.DEFAULT_LATITUDE,
                        longitude = existingHotel?.longitude ?: HotelDefaults.DEFAULT_LONGITUDE
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("저장")
            }
        }

        state.hotelError?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
