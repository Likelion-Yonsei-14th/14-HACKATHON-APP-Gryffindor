package com.gryffindor.smartshopping.feature.shoppingresult

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.domain.model.Product

/**
 * 관심 제품 등록 screen.
 * Shows all products detected during shopping, user selects which ones they're interested in.
 * Matches Figma "쇼핑 결과 기록_관심제품 등록".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestProductScreen(
    products: List<Product>,
    onComplete: (selectedIds: List<String>) -> Unit
) {
    val colors = LocalAppColors.current
    val selectedIds = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "관심 상품",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "아쉽게 못 산 제품이 있으신가요?\n관심 상품으로 등록해두세요",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "관심 상품을 기반으로 맞춤 제품을 추천해드려요",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }

        // Product grid
        FlowRow(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            products.forEach { product ->
                val isSelected = product.productId in selectedIds
                InterestProductCard(
                    product = product,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) selectedIds.remove(product.productId)
                        else selectedIds.add(product.productId)
                    }
                )
            }
        }

        // Bottom button
        Column(modifier = Modifier.padding(16.dp)) {
            PrimaryButton(
                text = "완료",
                onClick = { onComplete(selectedIds.toList()) }
            )
        }
    }
}

@Composable
private fun InterestProductCard(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val borderColor = if (isSelected) colors.brandPrimary else colors.borderDefault
    val bgColor = if (isSelected) colors.backgroundEmphasized else colors.borderDefault

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
    ) {
        // Image placeholder
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(bgColor)
        )
        // Info
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = product.brand,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}
