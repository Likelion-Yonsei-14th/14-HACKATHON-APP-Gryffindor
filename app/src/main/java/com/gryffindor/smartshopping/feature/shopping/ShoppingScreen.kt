package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.common.UiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    sessionId: String,
    currency: String,
    wishlistIds: Set<String> = emptySet(),
    onWishlistToggle: (String) -> Unit = {},
    onNavigateToReview: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadProducts(sessionId, currency)
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.retry() }) {
                    Text("다시 시도")
                }
            }
        }

        is UiState.Success -> {
            val data = state.data

            LaunchedEffect(data.isSessionActive) {
                if (!data.isSessionActive) {
                    onNavigateToReview()
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header row with title and currency toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "쇼핑 중",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    // Currency toggle chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = data.displayCurrency == DisplayCurrency.KRW,
                            onClick = {
                                if (data.displayCurrency != DisplayCurrency.KRW) {
                                    viewModel.toggleCurrency()
                                }
                            },
                            label = { Text("KRW") }
                        )
                        FilterChip(
                            selected = data.displayCurrency == DisplayCurrency.CONVERTED,
                            onClick = {
                                if (data.displayCurrency != DisplayCurrency.CONVERTED) {
                                    viewModel.toggleCurrency()
                                }
                            },
                            enabled = data.convertedAvailable,
                            label = { Text(data.sessionCurrency) }
                        )
                    }
                }

                // Summary section
                if (data.products.isNotEmpty()) {
                    SummarySection(data)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.products) { product ->
                        ProductCard(
                            sessionProduct = product,
                            displayCurrency = data.displayCurrency,
                            sessionCurrency = data.sessionCurrency,
                            isWishlisted = wishlistIds.contains(product.product.productId),
                            onWishlistToggle = onWishlistToggle
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.endShopping(sessionId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("쇼핑 종료")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySection(data: ShoppingUiState) {
    val summary = data.summary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (data.displayCurrency == DisplayCurrency.KRW) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "총 구매 금액",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatKrw(summary.totalRetailKrw),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "예상 환급 금액",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatKrw(summary.totalEstimatedRefundKrw),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val symbol = currencySymbol(data.sessionCurrency)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "총 구매 금액",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$symbol${summary.totalConvertedRetail ?: "-"}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "예상 환급 금액",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$symbol${summary.totalConvertedRefund ?: "-"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatKrw(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "\u20A9${formatter.format(amount)}"
}

internal fun currencySymbol(currencyCode: String): String {
    return when (currencyCode.uppercase()) {
        "USD" -> "$"
        "CNY" -> "\u00A5"
        "KRW" -> "\u20A9"
        else -> currencyCode
    }
}
