package com.gryffindor.smartshopping.feature.reservation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun VisitReservationScreen(
    viewModel: VisitReservationViewModel,
    tripId: String,
    storeId: String,
    storeName: String,
    onReservationCreated: () -> Unit,
    onNavigateToReservationList: () -> Unit
) {
    val state by viewModel.createState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(storeId) {
        viewModel.loadStoreWishlistProducts(storeId)
    }

    // Navigate after successful creation
    LaunchedEffect(state.createdReservation) {
        // Don't navigate on initial null
    }

    when {
        state.createdReservation != null -> {
            // Success state
            ReservationSuccessContent(
                reservation = state.createdReservation!!,
                onNavigateToList = onNavigateToReservationList
            )
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "방문 예약",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Store name
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "매장",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Date selection
                Text(
                    text = "방문 날짜",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                viewModel.updateScheduledDate(
                                    String.format("%04d-%02d-%02d", year, month + 1, day)
                                )
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.scheduledDate.isBlank()) "날짜 선택" else state.scheduledDate
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time selection
                Text(
                    text = "방문 시간",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.updateScheduledTime(
                                    String.format("%02d:%02d", hour, minute)
                                )
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.scheduledTime.isBlank()) "시간 선택" else state.scheduledTime
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Wishlist products for this store
                Text(
                    text = "준비할 관심 상품",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    state.isLoadingProducts -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.storeWishlistProducts.isEmpty() -> {
                        Text(
                            text = "이 매장에서 준비 가능한 관심 상품이 없습니다.\n방문 예약만 진행할 수 있습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        state.storeWishlistProducts.forEach { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.selectedProductIds.contains(product.productId),
                                    onCheckedChange = {
                                        viewModel.toggleProductSelection(product.productId)
                                    }
                                )
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Submit button
                Button(
                    onClick = { viewModel.createReservation(tripId, storeId) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("예약하기")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReservationSuccessContent(
    reservation: com.gryffindor.smartshopping.domain.model.VisitReservation,
    onNavigateToList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "방문 예약이 완료되었습니다.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Store
                Text(
                    text = "매장",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = reservation.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Time
                Text(
                    text = "방문 시간",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatScheduledAt(reservation.scheduledAt),
                    style = MaterialTheme.typography.bodyLarge
                )

                // Products
                if (reservation.products.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "준비 요청 상품",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    reservation.products.forEach { product ->
                        Text(
                            text = "• ${product.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("예약 내역 보기")
        }
    }
}

/**
 * Formats ISO-8601 scheduledAt to user-friendly Korean format.
 * e.g. "2026-08-21T15:00:00+09:00" → "8월 21일 오후 3:00"
 */
private fun formatScheduledAt(scheduledAt: String): String {
    return try {
        // Parse date and time from ISO string
        val dateTime = scheduledAt.substringBefore("+").substringBefore("-", scheduledAt)
        val parts = scheduledAt.split("T")
        if (parts.size != 2) return scheduledAt

        val datePart = parts[0]
        val timePart = parts[1].substringBefore("+").substringBefore("-")

        val dateParts = datePart.split("-")
        val timeParts = timePart.split(":")
        if (dateParts.size < 3 || timeParts.size < 2) return scheduledAt

        val month = dateParts[1].toIntOrNull() ?: return scheduledAt
        val day = dateParts[2].toIntOrNull() ?: return scheduledAt
        val hour = timeParts[0].toIntOrNull() ?: return scheduledAt
        val minute = timeParts[1].toIntOrNull() ?: return scheduledAt

        val amPm = if (hour < 12) "오전" else "오후"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour

        "${month}월 ${day}일 $amPm ${displayHour}:${String.format("%02d", minute)}"
    } catch (e: Exception) {
        scheduledAt
    }
}
