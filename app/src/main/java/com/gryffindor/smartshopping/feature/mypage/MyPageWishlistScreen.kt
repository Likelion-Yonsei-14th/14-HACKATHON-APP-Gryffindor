package com.gryffindor.smartshopping.feature.mypage

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
import androidx.compose.material3.IconButton
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
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel

/**
 * MyPage Wishlist screen — shows all wishlisted products.
 * Uses WishlistViewModel backed by real PersonalizationRepository.
 */
@Composable
fun MyPageWishlistScreen(
    viewModel: WishlistViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val wishlistIds by viewModel.wishlistIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWishlist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                text = "위시리스트",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.brandPrimary)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.semanticRed
                    )
                }
            }
            wishlistIds.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "위시리스트가 비어있습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(wishlistIds.toList()) { productId ->
                        WishlistItem(
                            productId = productId,
                            onRemove = { viewModel.toggleWishlist(productId) }
                        )
                        HorizontalDivider(color = colors.borderDisabled)
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistItem(
    productId: String,
    onRemove: () -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = productId, // TODO: Fetch product details when endpoint available
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary
            )
        }
        TextButton(onClick = onRemove) {
            Text(
                text = "삭제",
                style = MaterialTheme.typography.bodySmall,
                color = colors.semanticRed
            )
        }
    }
}
