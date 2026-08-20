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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketSmallButton
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.domain.model.SessionProduct
import java.text.NumberFormat
import java.util.Locale

/**
 * 실시간 쇼핑 세션 종료 후 진입하는 리뷰 화면. 실제 백엔드([ReviewViewModel])는 세션 중
 * 인식된 상품 하나의 리스트에 "구매"/"관심" 두 플래그만 있어서, 원래 목업으로 나눠 만들었던
 * 구매 물품 확인 화면 + 관심 상품 추천 화면(내가 본 상품 풀, 추천 상품 풀 포함)을 이 화면
 * 하나로 합쳤다 — 그 두 풀 개념은 실제 데이터가 없어서 들어낸 것.
 *
 * [LiveShoppingScreen]과 마찬가지로 하단 네비게이션 바 없이 몰입형으로 둔다(백엔드 실제
 * ReviewScreen도 동일).
 */
@Composable
fun ShoppingReviewScreen(
    products: List<SessionProduct>,
    purchasedIds: Set<String>,
    interestedIds: Set<String>,
    onTogglePurchased: (String) -> Unit,
    onToggleInterested: (String) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface),
        ) {
            LooketTopBar(title = stringResource(R.string.shopping_review_title))

            Text(
                text = stringResource(R.string.shopping_review_guide),
                style = LooketTextStyles.titleOne,
                color = LooketColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.shopping_review_empty),
                        style = LooketTextStyles.bodyTwo,
                        color = LooketColors.TextSecondary,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(products, key = { it.product.productId }) { sessionProduct ->
                        ReviewItemRow(
                            sessionProduct = sessionProduct,
                            isPurchased = sessionProduct.product.productId in purchasedIds,
                            isInterested = sessionProduct.product.productId in interestedIds,
                            onTogglePurchased = { onTogglePurchased(sessionProduct.product.productId) },
                            onToggleInterested = { onToggleInterested(sessionProduct.product.productId) },
                        )
                    }
                }
            }

            LooketPrimaryButton(
                text = stringResource(R.string.common_confirm),
                onClick = onConfirmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ReviewItemRow(
    sessionProduct: SessionProduct,
    isPurchased: Boolean,
    isInterested: Boolean,
    onTogglePurchased: () -> Unit,
    onToggleInterested: () -> Unit,
) {
    val product = sessionProduct.product
    val pricing = sessionProduct.pricing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val color = LooketColors.BorderDefault
                drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 126.dp)
                .background(LooketColors.SurfaceEmphasized),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = product.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
                    Text(text = product.brand, style = LooketTextStyles.bodyThree, color = LooketColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatKrw(pricing.retailPriceKrw), style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
                    Text(
                        text = stringResource(R.string.shopping_live_refund_amount, formatKrw(pricing.estimatedRefundKrw)),
                        style = LooketTextStyles.bodyThree,
                        color = LooketColors.TextPrimary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                LooketSmallButton(
                    text = stringResource(R.string.shopping_review_purchase),
                    backgroundColor = if (isPurchased) LooketColors.BrandPrimary else LooketColors.BrandPrimarySubtle,
                    contentColor = if (isPurchased) LooketColors.TextInverse else LooketColors.BrandPrimary,
                    onClick = onTogglePurchased,
                )
                LooketSmallButton(
                    text = stringResource(R.string.shopping_review_interested),
                    backgroundColor = if (isInterested) LooketColors.Red else LooketColors.RedLight,
                    contentColor = if (isInterested) LooketColors.TextInverse else LooketColors.Red,
                    onClick = onToggleInterested,
                )
            }
        }
    }
}

private fun formatKrw(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "₩ ${formatter.format(amount)}"
}
