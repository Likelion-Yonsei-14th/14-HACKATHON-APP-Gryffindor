package com.gryffindor.smartshopping.feature.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.domain.model.SupportedCountry
import com.gryffindor.smartshopping.feature.feed.FeedSection
import com.gryffindor.smartshopping.feature.feed.FeedViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    onNavigateToStoreSelection: (currency: String) -> Unit,
    onNavigateToTripList: () -> Unit = {},
    onNavigateToStore: (storeId: String) -> Unit = {},
    onNavigateToVisitReservation: (storeId: String, storeName: String) -> Unit = { _, _ -> },
    wishlistIds: Set<String> = emptySet(),
    onWishlistToggle: (productId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val feedUiState by feedViewModel.uiState.collectAsState()

    // Load trips and feed on first composition
    LaunchedEffect(Unit) {
        feedViewModel.loadTripsAndFeed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Smart Shopping",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show trip title if available
        feedUiState.selectedTrip?.let { trip ->
            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        } ?: run {
            Text(
                text = "Meta Ray-Ban으로 쇼핑을 시작하세요",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.datUpdateRequired) {
            // DAT update required UI
            DatUpdateSection(
                errorMessage = uiState.errorMessage,
                datUpdateError = uiState.datUpdateError,
                onRequestUpdate = { viewModel.requestGlassesUpdate() },
                onRetry = { viewModel.retryCamera() }
            )
        } else {
            // Country selector
            Text(
                text = "국가 선택",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportedCountry.entries.forEach { country ->
                    FilterChip(
                        selected = uiState.selectedCountry == country,
                        onClick = { viewModel.selectCountry(country) },
                        label = {
                            Text("${country.displayName} (${country.currencyCode})")
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onNavigateToStoreSelection(uiState.selectedCountry.currencyCode)
                }
            ) {
                Text("쇼핑 시작")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToTripList
            ) {
                Text("내 여행")
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

        // --- AI Feed Section ---
        if (feedUiState.trips.isNotEmpty() || feedUiState.isLoadingTrips || feedUiState.isLoadingFeed) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            FeedSection(
                uiState = feedUiState,
                onRetry = { feedViewModel.retry() },
                onStoreClick = onNavigateToStore,
                onWishlistToggle = onWishlistToggle,
                onVisitReservation = onNavigateToVisitReservation,
                wishlistIds = wishlistIds,
                modifier = Modifier.fillMaxWidth()
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
