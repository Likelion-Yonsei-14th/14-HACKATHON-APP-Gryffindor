package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketIconButton
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

data class LiveReceiptItem(
    val id: String,
    val name: String,
    val storeName: String,
    val price: String,
    val refundAmount: String,
)

// Figma 좌표 기준(376:5236/5240/5246) 근사값. 리스트 시트는 795dp(접힘, button_L 위치)에서
// 200dp(펼침 = FAB(56dp+96dp) + gap 48dp)까지 이동한다.
private val SheetCollapsedTop = 795.dp
private val SheetExpandedTop = 200.dp
private val FabDragThreshold = 48.dp // UI 담당자가 알려준 값: 이만큼 끌어올리기 전까진 FAB는 안 움직임
private val FabExpandedTop = 56.dp // UI 담당자가 알려준 값: FAB가 올라갈 수 있는 최대 높이(프레임 top 기준)
private val FabSize = 96.dp

/**
 * 쇼핑 - 실시간 메인 화면. Figma 쇼핑-실시간-쇼핑시작(376:5236)/쇼핑완료(376:5240, 376:5246)
 * 세 장을 하나의 화면으로 합쳤다 — 세 장 다 같은 화면의 서로 다른 상태(세션 시작 전/후,
 * 실시간 리스트 시트를 얼마나 끌어올렸는지)이기 때문이다.
 *
 * - [isSessionActive]가 false면 중앙에 재생 버튼, true면 일시정지/종료(문 아이콘) 버튼 쌍.
 * - "실시간 리스트" 영역은 위아래로 드래그 가능한 시트. 48dp 이상 끌어올리면 FAB도 같이
 *   따라 올라가고, 최대로는 프레임 위에서 56dp 지점까지만 올라간다.
 * - 드래그를 놓으면 절반 지점을 기준으로 완전히 접히거나 펼쳐진 상태로 스냅한다(애니메이션
 *   없이 즉시 스냅 — 필요하면 나중에 easing 추가).
 */
@Composable
fun LiveShoppingScreen(
    isSessionActive: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onFinishClick: () -> Unit,
    onBackClick: () -> Unit,
    totalPurchaseAmount: String,
    refundAmount: String,
    items: List<LiveReceiptItem>,
    onRemoveItem: (String) -> Unit,
    isExchangeRateOn: Boolean,
    onExchangeRateToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val maxDragDp = (SheetCollapsedTop - SheetExpandedTop).value
    var dragDp by remember { mutableFloatStateOf(0f) }
    val expanded = dragDp > maxDragDp / 2f

    val draggableState = rememberDraggableState { deltaPx ->
        val deltaDp = with(density) { -deltaPx.toDp().value }
        dragDp = (dragDp + deltaDp).coerceIn(0f, maxDragDp)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LooketColors.Surface),
    ) {
        val naturalFabTopDp = ((maxHeight - FabSize) / 2).value
        val fabExtraDrag = (dragDp - FabDragThreshold.value).coerceAtLeast(0f)
        val fabTopDp = (naturalFabTopDp - fabExtraDrag).coerceAtLeast(FabExpandedTop.value)
        val sheetTopDp = (SheetCollapsedTop.value - dragDp).coerceIn(SheetExpandedTop.value, SheetCollapsedTop.value)

        if (!isSessionActive) {
            Box(modifier = Modifier.padding(top = 68.dp)) {
                LooketIconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = LooketColors.TextPrimary,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = fabTopDp.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            if (isSessionActive) {
                FabButton(iconRes = R.drawable.ic_fab_pause, onClick = onPauseClick)
                FabButton(iconRes = R.drawable.ic_fab_finish, onClick = onFinishClick)
            } else {
                FabButton(iconRes = R.drawable.ic_fab_play, onClick = onPlayClick)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = sheetTopDp.dp)
                .background(LooketColors.Surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = draggableState,
                        onDragStopped = {
                            dragDp = if (dragDp > maxDragDp / 2f) maxDragDp else 0f
                        },
                    ),
            ) {
                if (!expanded) {
                    LooketPrimaryButton(
                        text = stringResource(R.string.shopping_live_list),
                        onClick = { dragDp = maxDragDp },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .width(144.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(LooketColors.TextPrimary)
                                // 핸들바를 탭하면 확실하게 접힘 상태로 되돌아간다(드래그가 익숙하지
                                // 않은 사용자를 위한 안전장치).
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { dragDp = 0f },
                                ),
                        )
                        Text(
                            text = stringResource(R.string.shopping_live_list),
                            style = LooketTextStyles.titleTwo,
                            color = LooketColors.TextPrimary,
                            modifier = Modifier.padding(top = 32.dp, bottom = 10.dp),
                        )
                    }
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                ) {
                    RefundSummary(
                        totalPurchaseAmount = totalPurchaseAmount,
                        refundAmount = refundAmount,
                        isExchangeRateOn = isExchangeRateOn,
                        onExchangeRateToggle = onExchangeRateToggle,
                    )
                    Spacer(Modifier.height(16.dp))
                    items.forEach { item ->
                        LiveReceiptRow(item = item, onRemoveClick = { onRemoveItem(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FabButton(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(FabSize)
            .clip(RoundedCornerShape(8.dp))
            .background(LooketColors.BrandPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun RefundSummary(
    totalPurchaseAmount: String,
    refundAmount: String,
    isExchangeRateOn: Boolean,
    onExchangeRateToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            ExchangeRateToggle(isOn = isExchangeRateOn, onToggle = onExchangeRateToggle)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.shopping_live_refund_title),
            style = LooketTextStyles.titleTwo,
            color = LooketColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.shopping_live_refund_of_total, totalPurchaseAmount),
            style = LooketTextStyles.bodyTwo,
            color = LooketColors.TextPrimary,
        )
        Text(
            text = refundAmount,
            style = LooketTextStyles.titleOne,
            color = LooketColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LooketColors.BrandSecondary),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.shopping_live_refund_type_post),
                style = LooketTextStyles.bodyOne,
                color = LooketColors.TextPrimary,
            )
        }
    }
}

/** Figma "환율" 토글(351:3141). 다른 환급 관련 화면에서도 재등장하면 core/ui로 옮길 예정. */
@Composable
private fun ExchangeRateToggle(isOn: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(LooketColors.BrandPrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(if (isOn) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(LooketColors.SurfaceSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.shopping_live_currency_symbol),
                style = LooketTextStyles.bodyThree,
                color = LooketColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun LiveReceiptRow(item: LiveReceiptItem, onRemoveClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 126.dp)
                    .background(LooketColors.SurfaceEmphasized),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
                Text(text = item.storeName, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = item.price, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
                    Text(text = stringResource(R.string.shopping_live_refund_amount, item.refundAmount), style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LooketPrimaryButton(
            text = stringResource(R.string.common_remove),
            onClick = onRemoveClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun LiveShoppingScreenPreview() {
    var isSessionActive by remember { mutableStateOf(false) }
    var previewItems by remember { mutableStateOf(dummyLiveReceiptItems) }
    var isExchangeRateOn by remember { mutableStateOf(false) }

    LooketTheme {
        LiveShoppingScreen(
            isSessionActive = isSessionActive,
            onPlayClick = { isSessionActive = true },
            onPauseClick = { isSessionActive = false },
            onFinishClick = { isSessionActive = false },
            onBackClick = {},
            totalPurchaseAmount = "₩ 3,400,000",
            refundAmount = "₩ 230,000",
            items = previewItems,
            onRemoveItem = { id -> previewItems = previewItems.filterNot { it.id == id } },
            isExchangeRateOn = isExchangeRateOn,
            onExchangeRateToggle = { isExchangeRateOn = !isExchangeRateOn },
        )
    }
}

private val dummyLiveReceiptItems = listOf(
    LiveReceiptItem("1", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000", "₩ 76,000"),
    LiveReceiptItem("2", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000", "₩ 76,000"),
    LiveReceiptItem("3", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000", "₩ 76,000"),
    LiveReceiptItem("4", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000", "₩ 76,000"),
)
