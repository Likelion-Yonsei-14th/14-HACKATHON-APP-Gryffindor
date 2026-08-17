package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 쇼핑 결과 기록 > 구매 상품 확인 — 영수증 등록 직후, 인식된 구매 품목 목록을 보여준다.
 * Figma 쇼핑 결과 기록_구매 물품 확인(376:5099) 기준. 각 품목은 "제거"(구매 목록에서
 * 제외) / "♥"(관심 상품으로 표시) 두 버튼을 갖고, 목록에 없는 품목은 "직접 추가하기"로
 * 추가한다(추가 화면은 아직 없음, 화면 우선 제작 단계).
 */
@Composable
fun PurchasedItemsScreen(
    items: List<LiveReceiptItem>,
    onRemoveItem: (String) -> Unit,
    onWishlistItem: (String) -> Unit,
    onAddManuallyClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = { BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface)
                .verticalScroll(rememberScrollState()),
        ) {
            LooketTopBar(title = stringResource(R.string.shopping_result_purchased_title), onBackClick = onBackClick)
            Spacer(Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEach { item ->
                    PurchasedItemRow(
                        item = item,
                        onRemoveClick = { onRemoveItem(item.id) },
                        onWishlistClick = { onWishlistItem(item.id) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            LooketPrimaryButton(
                text = stringResource(R.string.shopping_result_add_manually),
                onClick = onAddManuallyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PurchasedItemRow(
    item: LiveReceiptItem,
    onRemoveClick: () -> Unit,
    onWishlistClick: () -> Unit,
) {
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
                    Text(text = item.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
                    Text(text = item.storeName, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = item.price, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
                    Text(
                        text = stringResource(R.string.shopping_live_refund_amount, item.refundAmount),
                        style = LooketTextStyles.bodyThree,
                        color = LooketColors.TextPrimary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                SmallPillButton(
                    text = stringResource(R.string.common_remove),
                    backgroundColor = LooketColors.BrandPrimarySubtle,
                    contentColor = LooketColors.BrandPrimary,
                    onClick = onRemoveClick,
                )
                SmallPillButton(
                    text = stringResource(R.string.shopping_result_wishlist_heart),
                    backgroundColor = LooketColors.RedLight,
                    contentColor = LooketColors.Red,
                    onClick = onWishlistClick,
                )
            }
        }
    }
}

@Composable
private fun SmallPillButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = LooketTextStyles.bodyThree, color = contentColor)
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun PurchasedItemsScreenPreview() {
    var previewItems by remember { mutableStateOf(dummyLiveReceiptItems) }
    LooketTheme {
        PurchasedItemsScreen(
            items = previewItems,
            onRemoveItem = { id -> previewItems = previewItems.filterNot { it.id == id } },
            onWishlistItem = {},
            onAddManuallyClick = {},
            onBackClick = {},
            selectedTab = BottomNavTab.SHOP,
            onTabSelected = {},
        )
    }
}
