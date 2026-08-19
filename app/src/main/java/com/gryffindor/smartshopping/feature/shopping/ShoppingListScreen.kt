package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.domain.model.SessionProduct
import java.text.NumberFormat
import java.util.Locale

/**
 * Shopping List (Frame 45) — shows products from the completed shopping session.
 * Reuses ShoppingViewModel's product state.
 */
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel,
    sessionId: String,
    currency: String,
    wishlistIds: Set<String>,
    onWishlistToggle: (String) -> Unit,
    onNavigateHome: () -> Unit
) {
    val colors = LocalAppColors.current
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
                CircularProgressIndicator(color = colors.brandPrimary)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.semanticRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(text = "홈으로 돌아가기", onClick = onNavigateHome)
                }
            }
        }
        is UiState.Success -> {
            val shoppingState = state.data
            val products = shoppingState.products

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.backgroundSurface)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "쇼핑 리스트",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${products.size}개의 제품을 확인했습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (products.isNotEmpty()) {
                    item {
                        Text(
                            text = "인식된 제품",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(products) { sessionProduct ->
                        ShoppingListProductItem(
                            sessionProduct = sessionProduct,
                            isWishlisted = wishlistIds.contains(sessionProduct.product.productId),
                            onWishlistToggle = { onWishlistToggle(sessionProduct.product.productId) }
                        )
                        HorizontalDivider(color = colors.borderDisabled)
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = "인식된 제품이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    PrimaryButton(
                        text = "홈으로 돌아가기",
                        onClick = onNavigateHome
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ShoppingListProductItem(
    sessionProduct: SessionProduct,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit
) {
    val colors = LocalAppColors.current
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sessionProduct.product.name,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sessionProduct.product.brand,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₩${formatter.format(sessionProduct.pricing.retailPriceKrw)}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )
            if (sessionProduct.pricing.estimatedRefundKrw > 0) {
                Text(
                    text = "환급 예상: ₩${formatter.format(sessionProduct.pricing.estimatedRefundKrw)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.brandPrimary
                )
            }
        }

        TextButton(onClick = onWishlistToggle) {
            Text(
                text = if (isWishlisted) "♥" else "♡",
                style = MaterialTheme.typography.titleLarge,
                color = if (isWishlisted) colors.semanticRed else colors.textSecondary
            )
        }
    }
}
