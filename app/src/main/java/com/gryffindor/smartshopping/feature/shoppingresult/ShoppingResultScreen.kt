package com.gryffindor.smartshopping.feature.shoppingresult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.domain.model.Product

/**
 * Shopping Result / 구매 물품 확인 screen.
 * Shows products detected during shopping session with remove/wishlist actions.
 * Matches Figma Frame 45 "쇼핑 결과 기록_구매 물품 확인".
 */
@Composable
fun ShoppingResultScreen(
    products: List<Product>,
    onRemoveProduct: (String) -> Unit,
    onWishlistProduct: (String) -> Unit,
    onAddManually: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "구매 상품",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        // Product list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            products.forEach { product ->
                ProductResultCard(
                    product = product,
                    onRemove = { onRemoveProduct(product.productId) },
                    onWishlist = { onWishlistProduct(product.productId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (products.isEmpty()) {
                Text(
                    text = "인식된 상품이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }

        // Bottom actions
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(
                text = "직접 추가하기",
                onClick = onAddManually
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                text = "완료",
                onClick = onNext
            )
        }
    }
}

@Composable
private fun ProductResultCard(
    product: Product,
    onRemove: () -> Unit,
    onWishlist: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image placeholder
            Spacer(
                modifier = Modifier
                    .size(80.dp, 100.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.backgroundEmphasized)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionButton(
                        text = "제거",
                        containerColor = colors.brandPrimarySubtle,
                        textColor = colors.brandPrimary,
                        onClick = onRemove
                    )
                    SmallActionButton(
                        text = "\u2665",
                        containerColor = colors.semanticRedLight,
                        textColor = colors.semanticRed,
                        onClick = onWishlist
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
