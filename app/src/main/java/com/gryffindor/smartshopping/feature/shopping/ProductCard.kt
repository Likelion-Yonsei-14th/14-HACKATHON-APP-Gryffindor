package com.gryffindor.smartshopping.feature.shopping

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.domain.model.SessionProduct
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductCard(
    sessionProduct: SessionProduct,
    displayCurrency: DisplayCurrency = DisplayCurrency.KRW,
    sessionCurrency: String = "KRW",
    isWishlisted: Boolean = false,
    onWishlistToggle: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val product = sessionProduct.product
    val pricing = sessionProduct.pricing

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.brand.take(2),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (onWishlistToggle != null) {
                        IconButton(
                            onClick = { onWishlistToggle(product.productId) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                text = if (isWishlisted) "\u2665" else "\u2661",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (displayCurrency) {
                    DisplayCurrency.KRW -> KrwPricing(pricing)
                    DisplayCurrency.CONVERTED -> ConvertedPricing(pricing, sessionCurrency)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Instant refund eligibility
                Text(
                    text = if (pricing.instantRefundEligible) "즉시환급 가능" else "즉시환급 불가",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pricing.instantRefundEligible)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KrwPricing(pricing: com.gryffindor.smartshopping.domain.model.Pricing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "정가",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatKrw(pricing.retailPriceKrw),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "예상 환급액",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatKrw(pricing.estimatedRefundKrw),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ConvertedPricing(
    pricing: com.gryffindor.smartshopping.domain.model.Pricing,
    sessionCurrency: String
) {
    val symbol = currencySymbol(sessionCurrency)
    val retailText = pricing.convertedRetailPrice?.let { "$symbol$it" } ?: "-"
    val refundText = pricing.convertedEstimatedRefund?.let { "$symbol$it" } ?: "-"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "정가",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = retailText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "예상 환급액",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = refundText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatKrw(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "\u20A9${formatter.format(amount)}"
}
