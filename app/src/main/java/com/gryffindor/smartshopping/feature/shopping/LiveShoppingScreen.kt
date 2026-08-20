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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketIconButton
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

data class LiveReceiptItem(
    val id: String,
    val name: String,
    val storeName: String,
    val price: String,
    val refundAmount: String,
    val imageUrl: String? = null,
    val priceUsd: String? = null,
    val refundAmountUsd: String? = null,
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
    totalPurchaseAmountUsd: String? = null,
    refundAmountUsd: String? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val maxDragDp = (SheetCollapsedTop - SheetExpandedTop).value
    var dragDp by remember { mutableFloatStateOf(0f) }
    val expanded = dragDp > maxDragDp / 2f

    // Auto-expand sheet when a NEW item arrives via recognition (not on initial load).
    var previousItemCount by remember { mutableIntStateOf(items.size) }
    LaunchedEffect(items.size) {
        if (items.size > previousItemCount) {
            Log.d("LiveShoppingScreen", "items updated count=${items.size} — auto-expanding sheet")
            dragDp = maxDragDp
        }
        previousItemCount = items.size
    }

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
                        totalPurchaseAmountUsd = totalPurchaseAmountUsd,
                        refundAmountUsd = refundAmountUsd,
                    )
                    Spacer(Modifier.height(16.dp))
                    items.forEach { item ->
                        LiveReceiptRow(item = item, isExchangeRateOn = isExchangeRateOn, onRemoveClick = { onRemoveItem(item.id) })
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
    totalPurchaseAmountUsd: String? = null,
    refundAmountUsd: String? = null,
) {
    // Toggle ON = KRW, OFF = USD (per UX requirement).
    val showUsd = !isExchangeRateOn && totalPurchaseAmountUsd != null
    val primaryTotal = if (showUsd) "$${totalPurchaseAmountUsd}" else totalPurchaseAmount
    val primaryRefund = if (showUsd && refundAmountUsd != null) "$${refundAmountUsd}" else refundAmount
    val secondaryTotal: String? = if (showUsd) totalPurchaseAmount else totalPurchaseAmountUsd?.let { "$$it" }
    val secondaryRefund: String? = if (showUsd) refundAmount else refundAmountUsd?.let { "$$it" }

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
            text = stringResource(R.string.shopping_live_refund_of_total, primaryTotal),
            style = LooketTextStyles.bodyTwo,
            color = LooketColors.TextPrimary,
        )
        if (secondaryTotal != null) {
            Text(
                text = "($secondaryTotal)",
                style = LooketTextStyles.bodyThree,
                color = LooketColors.TextSecondary,
            )
        }
        Text(
            text = primaryRefund,
            style = LooketTextStyles.titleOne,
            color = LooketColors.TextPrimary,
        )
        if (secondaryRefund != null) {
            Text(
                text = secondaryRefund,
                style = LooketTextStyles.bodyTwo,
                color = LooketColors.TextSecondary,
            )
        }
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

/** 통화 전환 세그먼트 토글. 왼쪽 ₩ / 오른쪽 $. 선택된 쪽이 하이라이트. */
@Composable
private fun ExchangeRateToggle(isOn: Boolean, onToggle: () -> Unit) {
    // isOn = true → KRW 선택 (왼쪽 활성), isOn = false → USD 선택 (오른쪽 활성)
    val krwSelected = isOn
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(LooketColors.SurfaceEmphasized)
            .padding(2.dp),
    ) {
        // KRW 버튼
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (krwSelected) LooketColors.BrandPrimary else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!krwSelected) onToggle() },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "₩",
                style = LooketTextStyles.bodyTwo,
                color = if (krwSelected) Color.White else LooketColors.TextSecondary,
            )
        }
        // USD 버튼
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (!krwSelected) LooketColors.BrandPrimary else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (krwSelected) onToggle() },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$",
                style = LooketTextStyles.bodyTwo,
                color = if (!krwSelected) Color.White else LooketColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun LiveReceiptRow(item: LiveReceiptItem, isExchangeRateOn: Boolean, onRemoveClick: () -> Unit) {
    // Toggle ON = KRW, OFF = USD (per UX requirement).
    val showUsd = !isExchangeRateOn && item.priceUsd != null
    val primaryPrice = if (showUsd) "$${item.priceUsd}" else item.price
    val primaryRefund = if (showUsd && item.refundAmountUsd != null) "$${item.refundAmountUsd}" else item.refundAmount
    val secondaryPrice: String? = if (showUsd) item.price else item.priceUsd?.let { "$$it" }
    val secondaryRefund: String? = if (showUsd) item.refundAmount else item.refundAmountUsd?.let { "$$it" }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 80.dp, height = 126.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 126.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LooketColors.SurfaceEmphasized),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
                Text(text = item.storeName, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(text = primaryPrice, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
                        if (secondaryPrice != null) {
                            Text(text = secondaryPrice, style = LooketTextStyles.bodyThree, color = LooketColors.TextSecondary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = stringResource(R.string.shopping_live_refund_amount, primaryRefund), style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                        if (secondaryRefund != null) {
                            Text(text = secondaryRefund, style = LooketTextStyles.bodyThree, color = LooketColors.TextSecondary)
                        }
                    }
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
