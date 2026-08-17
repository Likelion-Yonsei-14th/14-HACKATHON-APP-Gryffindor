package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketSmallButton
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

private val ViewedSheetHeight = 650.dp
private val ViewedSheetDismissThreshold = 120.dp

/**
 * 쇼핑 결과 기록 > 구매 상품 확인 — 영수증 등록 직후, 인식된 구매 품목 목록을 보여준다.
 * Figma 쇼핑 결과 기록_구매 물품 확인(376:5099) 기준. 각 품목은 "제거"(구매 목록에서
 * 제외) / "♥"(관심 상품으로 표시) 두 버튼을 갖는다.
 *
 * 목록을 끝까지 내리면 "직접 추가하기" 버튼이 나오고, 누르면 실시간 쇼핑 중 인식된
 * "내가 본 상품"을 아래에서 위로 끌어올리는 시트로 보여준다 — LiveShoppingScreen의
 * 실시간 리스트 시트와 같은 방식(핸들 드래그, 핸들 탭으로 닫기)이다. 각 품목의 "추가"를
 * 누르면 구매 목록으로 옮겨진다.
 */
@Composable
fun PurchasedItemsScreen(
    items: List<LiveReceiptItem>,
    onRemoveItem: (String) -> Unit,
    onWishlistItem: (String) -> Unit,
    viewedItems: List<LiveReceiptItem>,
    onAddViewedItem: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isViewedSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                LooketPrimaryButton(
                    text = stringResource(R.string.common_confirm),
                    onClick = onConfirmClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LooketColors.Surface)
                        .padding(16.dp),
                )
                BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
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
                    onClick = { isViewedSheetVisible = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            if (isViewedSheetVisible) {
                ViewedProductsSheet(
                    items = viewedItems,
                    onAddClick = onAddViewedItem,
                    onDismiss = { isViewedSheetVisible = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ViewedProductsSheet(
    items: List<LiveReceiptItem>,
    onAddClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var dragDownDp by remember { mutableFloatStateOf(0f) }

    val draggableState = rememberDraggableState { deltaPx ->
        val deltaDp = with(density) { deltaPx.toDp().value }
        dragDownDp = (dragDownDp + deltaDp).coerceAtLeast(0f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ViewedSheetHeight)
            .offset(y = dragDownDp.dp)
            .background(LooketColors.Surface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = draggableState,
                    onDragStopped = {
                        if (dragDownDp > ViewedSheetDismissThreshold.value) {
                            onDismiss()
                        }
                        dragDownDp = 0f
                    },
                ),
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(144.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(LooketColors.TextPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Text(
                text = stringResource(R.string.shopping_result_viewed_products_title),
                style = LooketTextStyles.titleTwo,
                color = LooketColors.TextPrimary,
                modifier = Modifier.padding(top = 32.dp, bottom = 10.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            items.forEach { item ->
                ViewedItemRow(item = item, onAddClick = { onAddClick(item.id) })
            }
        }
    }
}

@Composable
private fun ViewedItemRow(item: LiveReceiptItem, onAddClick: () -> Unit) {
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
            LooketPrimaryButton(
                text = stringResource(R.string.shopping_result_add),
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(),
            )
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
                LooketSmallButton(
                    text = stringResource(R.string.common_remove),
                    backgroundColor = LooketColors.BrandPrimarySubtle,
                    contentColor = LooketColors.BrandPrimary,
                    onClick = onRemoveClick,
                )
                LooketSmallButton(
                    text = stringResource(R.string.shopping_result_wishlist_heart),
                    backgroundColor = LooketColors.RedLight,
                    contentColor = LooketColors.Red,
                    onClick = onWishlistClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun PurchasedItemsScreenPreview() {
    var previewItems by remember { mutableStateOf(dummyLiveReceiptItems) }
    var previewViewedItems by remember { mutableStateOf(dummyViewedItems) }
    LooketTheme {
        PurchasedItemsScreen(
            items = previewItems,
            onRemoveItem = { id -> previewItems = previewItems.filterNot { it.id == id } },
            onWishlistItem = {},
            viewedItems = previewViewedItems,
            onAddViewedItem = { id ->
                previewViewedItems.find { it.id == id }?.let { found ->
                    previewItems = previewItems + found
                    previewViewedItems = previewViewedItems.filterNot { it.id == id }
                }
            },
            onConfirmClick = {},
            onBackClick = {},
            selectedTab = BottomNavTab.SHOP,
            onTabSelected = {},
        )
    }
}

internal val dummyViewedItems = listOf(
    LiveReceiptItem("v1", "MCM 반지갑", "신세계면세점 본점", "₩ 350,000", "₩ 24,500"),
    LiveReceiptItem("v2", "MCM 크로스백", "신세계면세점 본점", "₩ 620,000", "₩ 43,400"),
)
