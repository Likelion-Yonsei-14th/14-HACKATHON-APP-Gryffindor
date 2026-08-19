package com.gryffindor.smartshopping.feature.feed

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
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.domain.model.FeedRecommendation
import com.gryffindor.smartshopping.domain.model.RecommendedStore
import com.gryffindor.smartshopping.domain.model.TripFeed

/**
 * Feed section displayed on the Home screen.
 * Shows AI-recommended products with purchasable stores and distances.
 */
@Composable
fun FeedSection(
    uiState: FeedUiState,
    onRetry: () -> Unit,
    onStoreClick: (storeId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoadingTrips || uiState.isLoadingFeed -> {
                FeedLoadingContent()
            }
            uiState.error != null -> {
                FeedErrorContent(error = uiState.error, onRetry = onRetry)
            }
            uiState.isEmpty -> {
                FeedEmptyContent()
            }
            uiState.isSuccess -> {
                FeedSuccessContent(
                    feed = uiState.feed!!,
                    onStoreClick = onStoreClick
                )
            }
            // No trips at all — don't show feed section
            uiState.trips.isEmpty() && !uiState.isLoadingTrips -> {
                // Nothing to show
            }
        }
    }
}

@Composable
private fun FeedLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "여행에 맞는 상품을 추천하고 있어요...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeedErrorContent(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun FeedEmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "아직 추천할 상품이 없어요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "쇼핑 중 관심 있는 상품을 확인해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeedSuccessContent(
    feed: TripFeed,
    onStoreClick: (storeId: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = "AI 추천 상품",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        feed.recommendations.forEach { recommendation ->
            FeedRecommendationCard(
                recommendation = recommendation,
                onStoreClick = onStoreClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeedRecommendationCard(
    recommendation: FeedRecommendation,
    onStoreClick: (storeId: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Product image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // Show brand initials as placeholder (matching existing ProductCard pattern)
                Text(
                    text = recommendation.product.brand.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brand
            Text(
                text = recommendation.product.brand,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Product name
            Text(
                text = recommendation.product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Recommendation reason
            Text(
                text = "\"${recommendation.reason}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Stores section
            if (recommendation.stores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "구매 가능한 매장",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                recommendation.stores.forEach { store ->
                    FeedStoreRow(store = store, onStoreClick = onStoreClick)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedStoreRow(
    store: RecommendedStore,
    onStoreClick: (storeId: String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Store type badge
                Text(
                    text = store.type.toDisplayStoreType(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Airport info if available
            if (store.airportCode != null) {
                val airportText = buildString {
                    append(store.airportCode)
                    if (store.terminal != null) append(" ${store.terminal}")
                }
                Text(
                    text = airportText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Distance display
            val distanceText = store.distanceDisplayText()
            if (distanceText != null) {
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Store detail / reservation button (action hook for future use)
        OutlinedButton(
            onClick = { onStoreClick(store.storeId) },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("매장 보기", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// --- Helper extensions ---

/**
 * Distance display priority:
 * 1. distanceFromCurrentLocationKm -> "현재 위치에서 X.Xkm"
 * 2. distanceFromHotelKm -> "숙소에서 X.Xkm"
 * 3. null -> no display
 */
private fun RecommendedStore.distanceDisplayText(): String? {
    return when {
        distanceFromCurrentLocationKm != null ->
            "현재 위치에서 ${"%.1f".format(distanceFromCurrentLocationKm)}km"
        distanceFromHotelKm != null ->
            "숙소에서 ${"%.1f".format(distanceFromHotelKm)}km"
        else -> null
    }
}

/**
 * Convert backend store type to user-friendly Korean label.
 */
private fun String.toDisplayStoreType(): String = when (this) {
    "DEPARTMENT_STORE" -> "백화점"
    "DUTY_FREE" -> "면세점"
    else -> this
}
