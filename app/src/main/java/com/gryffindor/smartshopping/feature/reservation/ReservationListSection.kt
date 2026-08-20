package com.gryffindor.smartshopping.feature.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gryffindor.smartshopping.domain.model.ReservationStatus
import com.gryffindor.smartshopping.domain.model.VisitReservation

/**
 * Section to display in TripDetailScreen showing visit reservations for a trip.
 */
@Composable
fun ReservationListSection(
    reservations: List<VisitReservation>,
    cancellingIds: Set<String> = emptySet(),
    onCancelReservation: (reservationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "방문 예약",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (reservations.isEmpty()) {
            Text(
                text = "등록된 방문 예약이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            reservations.forEach { reservation ->
                ReservationCard(
                    reservation = reservation,
                    isCancelling = cancellingIds.contains(reservation.id),
                    onCancel = { onCancelReservation(reservation.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: VisitReservation,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Store name
                    Text(
                        text = reservation.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Scheduled time
                    Text(
                        text = formatReservationTime(reservation.scheduledAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                val statusText = when (reservation.status) {
                    ReservationStatus.RESERVED -> "예약 완료"
                    ReservationStatus.CANCELLED -> "취소됨"
                }
                val statusColor = when (reservation.status) {
                    ReservationStatus.RESERVED -> MaterialTheme.colorScheme.primary
                    ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.error
                }
                val statusBgColor = when (reservation.status) {
                    ReservationStatus.RESERVED -> MaterialTheme.colorScheme.primaryContainer
                    ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Products
            if (reservation.products.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "준비 요청 상품 (${reservation.products.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                reservation.products.forEach { product ->
                    Text(
                        text = "• ${product.name}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "방문 예약만 (상품 요청 없음)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Cancel button (only for RESERVED status)
            if (reservation.status == ReservationStatus.RESERVED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isCancelling,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("예약 취소")
                }
            }
        }
    }
}

/**
 * Format scheduledAt for display in reservation list.
 */
private fun formatReservationTime(scheduledAt: String): String {
    return try {
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
