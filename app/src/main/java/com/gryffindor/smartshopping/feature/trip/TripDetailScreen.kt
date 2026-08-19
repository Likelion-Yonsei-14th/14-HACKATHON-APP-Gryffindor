package com.gryffindor.smartshopping.feature.trip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.domain.model.Flight
import com.gryffindor.smartshopping.domain.model.HotelStay
import com.gryffindor.smartshopping.domain.model.Purchase
import com.gryffindor.smartshopping.domain.model.RefundChecklist
import com.gryffindor.smartshopping.domain.model.RefundChecklistStatus
import com.gryffindor.smartshopping.domain.model.RefundMethod
import com.gryffindor.smartshopping.feature.reservation.ReservationListSection

@Composable
fun TripDetailScreen(
    viewModel: TripViewModel,
    tripId: String,
    onNavigateToFlightEdit: (flightId: String) -> Unit,
    onNavigateToHotelEdit: () -> Unit,
    onNavigateToVisitReservation: (storeId: String, storeName: String) -> Unit = { _, _ -> }
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(tripId) {
        viewModel.loadTripDetail(tripId)
    }

    // Photo picker for flight ticket
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                viewModel.analyzeFlight(bytes, tripId)
            }
        }
    }

    // Photo picker for receipt (B7)
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                viewModel.analyzeReceipt(bytes, tripId)
            }
        }
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.error != null && state.tripDetail == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.loadTripDetail(tripId) }) {
                        Text("다시 시도")
                    }
                }
            }
        }

        state.tripDetail != null -> {
            val detail = state.tripDetail!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Trip header
                Text(
                    text = detail.trip.title,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (detail.trip.destinationCity != null || detail.trip.destinationCountry != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listOfNotNull(detail.trip.destinationCity, detail.trip.destinationCountry)
                            .joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (detail.trip.startsAt != null || detail.trip.endsAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${detail.trip.startsAt ?: "?"} - ${detail.trip.endsAt ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // Flight section
                Text(
                    text = "항공편",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.isAnalyzingFlight) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("항공권 정보를 확인하고 있어요...")
                        }
                    }
                } else if (detail.flights.isNotEmpty()) {
                    detail.flights.forEach { flight ->
                        FlightInfoCard(
                            flight = flight,
                            onEdit = { onNavigateToFlightEdit(flight.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text(
                        text = "등록된 항공편이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.flightError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isAnalyzingFlight
                ) {
                    Text(if (detail.flights.isEmpty()) "항공권 등록" else "항공권 추가")
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // Hotel section
                Text(
                    text = "숙소",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (detail.hotel != null) {
                    HotelInfoCard(hotel = detail.hotel!!)
                } else {
                    Text(
                        text = "등록된 숙소가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.hotelError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onNavigateToHotelEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (detail.hotel == null) "숙소 등록" else "숙소 수정")
                }

                // ===== Reservation section =====
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                ReservationListSection(
                    reservations = detail.visitReservations,
                    cancellingIds = emptySet(),
                    onCancelReservation = { reservationId ->
                        viewModel.cancelReservation(reservationId, tripId)
                    }
                )

                // ===== B7: Purchases / Receipt Section =====
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                PurchaseReceiptSection(
                    tripId = tripId,
                    purchases = state.purchases,
                    isLoadingPurchases = state.isLoadingPurchases,
                    isAnalyzingReceipt = state.isAnalyzingReceipt,
                    receiptMessage = state.receiptMessage,
                    updatingRefundMethodIds = state.updatingRefundMethodIds,
                    onPickReceipt = {
                        receiptPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUpdateRefundMethod = { purchaseId, method ->
                        viewModel.updateRefundMethod(purchaseId, tripId, method)
                    }
                )

                // ===== B7: Refund Checklist Section =====
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                RefundChecklistSection(
                    refundChecklist = state.refundChecklist,
                    isLoading = state.isLoadingRefundChecklist,
                    error = state.refundChecklistError,
                    checkedIds = state.checkedChecklistIds,
                    onToggleItem = { itemId -> viewModel.toggleChecklistItem(itemId) }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FlightInfoCard(
    flight: Flight,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${flight.departureAirport ?: "미등록"} → ${flight.arrivalAirport ?: "미등록"}",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(onClick = onEdit) {
                    Text("수정")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlightField("편명", flight.flightNumber)
            FlightField("터미널", flight.terminal)
            FlightField("출발", flight.departureAt)
            FlightField("도착", flight.arrivalAt)
            FlightField("공항 도착 예정", flight.airportArrivalAt)
        }
    }
}

@Composable
private fun FlightField(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: "미등록",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun HotelInfoCard(hotel: HotelStay) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = hotel.name,
                style = MaterialTheme.typography.titleMedium
            )
            hotel.address?.let { address ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hotel.checkInAt != null || hotel.checkOutAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "체크인: ${hotel.checkInAt ?: "미정"} / 체크아웃: ${hotel.checkOutAt ?: "미정"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ===== B7: Purchase / Receipt Section =====

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurchaseReceiptSection(
    tripId: String,
    purchases: List<Purchase>,
    isLoadingPurchases: Boolean,
    isAnalyzingReceipt: Boolean,
    receiptMessage: String?,
    updatingRefundMethodIds: Set<String>,
    onPickReceipt: () -> Unit,
    onUpdateRefundMethod: (purchaseId: String, RefundMethod) -> Unit
) {
    Text(
        text = "구매 내역 / 영수증",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (isAnalyzingReceipt) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("영수증을 분석하고 있어요...")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    receiptMessage?.let { msg ->
        Text(
            text = msg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (isLoadingPurchases) {
        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
    } else if (purchases.isEmpty()) {
        Text(
            text = "등록된 구매 내역이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        purchases.forEach { purchase ->
            PurchaseCard(
                purchase = purchase,
                isUpdating = purchase.id in updatingRefundMethodIds,
                onUpdateRefundMethod = { method -> onUpdateRefundMethod(purchase.id, method) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onPickReceipt,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isAnalyzingReceipt
    ) {
        Text("영수증 등록")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurchaseCard(
    purchase: Purchase,
    isUpdating: Boolean,
    onUpdateRefundMethod: (RefundMethod) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Store name and date
            Text(
                text = purchase.storeName ?: "알 수 없는 매장",
                style = MaterialTheme.typography.titleMedium
            )
            purchase.purchasedAt?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Total amount
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${purchase.totalAmount?.let { "%,d".format(it) } ?: "-"} ${purchase.currency ?: ""}",
                style = MaterialTheme.typography.titleSmall
            )

            // Items
            if (purchase.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                purchase.items.forEach { item ->
                    val displayName = item.product?.let { "${it.brand} ${it.name}" }
                        ?: item.fallbackProductName
                        ?: "상품"
                    Text(
                        text = "• $displayName",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Refund method selector
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "환급 방식",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    RefundMethod.UNKNOWN to "미정",
                    RefundMethod.IMMEDIATE to "즉시환급",
                    RefundMethod.DOWNTOWN to "도심환급",
                    RefundMethod.AIRPORT to "공항환급"
                )
                options.forEach { (method, label) ->
                    FilterChip(
                        selected = purchase.refundMethod == method,
                        onClick = {
                            if (!isUpdating && purchase.refundMethod != method) {
                                onUpdateRefundMethod(method)
                            }
                        },
                        label = { Text(label) },
                        enabled = !isUpdating
                    )
                }
            }
        }
    }
}

// ===== B7: Refund Checklist Section =====

@Composable
private fun RefundChecklistSection(
    refundChecklist: RefundChecklist?,
    isLoading: Boolean,
    error: String?,
    checkedIds: Set<String>,
    onToggleItem: (String) -> Unit
) {
    Text(
        text = "환급 체크리스트",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(12.dp))

    when {
        isLoading -> {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        error != null -> {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        refundChecklist != null -> {
            // Status message
            val statusMessage = when (refundChecklist.status) {
                RefundChecklistStatus.NO_ELIGIBLE_PURCHASES ->
                    "환급 대상 구매 내역이 없습니다."
                RefundChecklistStatus.IMMEDIATE_REFUND_ONLY ->
                    "등록된 환급 대상 구매는 즉시환급 처리된 내역입니다."
                RefundChecklistStatus.ACTION_REQUIRED ->
                    "출국 전 아래 환급 절차를 확인하세요."
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            refundChecklist.notice?.let { notice ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (refundChecklist.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                refundChecklist.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = item.id in checkedIds,
                            onCheckedChange = { onToggleItem(item.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (item.required) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "필수",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        else -> {
            // Initial state before loading starts — show loading indicator
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
    }
}
