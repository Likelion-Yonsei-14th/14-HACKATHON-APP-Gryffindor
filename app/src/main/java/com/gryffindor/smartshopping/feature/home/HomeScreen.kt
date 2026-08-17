package com.gryffindor.smartshopping.feature.home

import androidx.compose.foundation.background
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.domain.model.ChecklistItem

private val BrandPrimary = Color(0xFF616AF3)
private val BrandGradientEnd = Color(0xFF3B36CC)
private val TextPrimary = Color(0xFF1B1A22)
private val BorderDefault = Color(0xFFD7D6E1)
private val BorderDisabled = Color(0xFFEEEDF1)
private val StatusGreen = Color(0xFF00D96C)
private val StatusOrange = Color(0xFFFF8633)

private enum class HomeTab { REFUND, LOOKET }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToShopping: (sessionId: String) -> Unit,
    onNavigateToChecklist: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.sessionId) {
        uiState.sessionId?.let { sessionId ->
            onNavigateToShopping(sessionId)
            viewModel.resetSessionNavigation()
        }
    }

    var selectedTab by remember { mutableStateOf(HomeTab.REFUND) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HomeTopNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            when (selectedTab) {
                HomeTab.REFUND -> RefundTabContent(
                    summary = HomeMockData.refundSummary,
                    purchasedItems = HomeMockData.purchasedItems,
                    checklistItems = HomeMockData.checklistItems,
                    checklistCheckedIds = HomeMockData.checklistCheckedIds,
                    onChecklistClick = onNavigateToChecklist,
                )
                HomeTab.LOOKET -> LooketTabContent(
                    recommended = HomeMockData.recommendedProducts,
                    brands = HomeMockData.brandFilters,
                    myLooket = HomeMockData.looketProducts,
                )
            }
        }
    }
}

@Composable
private fun HomeTopNav(selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            // Figma 원본 pt-68은 상태바 포함 값이라 줄여서 사용 (실제 기기 보고 조정)
            .padding(top = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(41.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "looket", color = BrandPrimary, fontSize = 22.sp)
            // TODO: 실제 알림 아이콘 이미지/벡터 생기면 교체
            Text(text = "🔔", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HomeTabItem(
                label = "REFUND",
                selected = selectedTab == HomeTab.REFUND,
                onClick = { onTabSelected(HomeTab.REFUND) },
                modifier = Modifier.width(180.dp),
            )
            HomeTabItem(
                label = "LOOKET",
                selected = selectedTab == HomeTab.LOOKET,
                onClick = { onTabSelected(HomeTab.LOOKET) },
                modifier = Modifier.width(180.dp),
            )
        }
    }
}

@Composable
private fun HomeTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = if (selected) BrandPrimary else TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (selected) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BrandPrimary))
        }
    }
}

// ---------- REFUND 탭 ----------

@Composable
private fun RefundTabContent(
    summary: RefundSummary,
    purchasedItems: List<PurchasedItem>,
    checklistItems: List<ChecklistItem>,
    checklistCheckedIds: Set<String>,
    onChecklistClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        RefundSummaryCard(summary, modifier = Modifier.padding(horizontal = 16.dp))
        PurchasedItemsSection(purchasedItems)

        // 요구사항: 오늘 체크리스트 중 미완료(체크 안 된) 항목만 홈에 표시
        val incompleteChecklist = checklistItems.filter { it.id !in checklistCheckedIds }
        if (incompleteChecklist.isNotEmpty()) {
            ChecklistPreviewSection(items = incompleteChecklist, onSeeAllClick = onChecklistClick)
        }
    }
}

@Composable
private fun RefundSummaryCard(summary: RefundSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(colors = listOf(BrandPrimary, BrandGradientEnd)))
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("OO님의 환급 금액", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("총 구매 금액 ₩ ${formatKrw(summary.totalPurchaseAmountKrw)} 중", color = Color.White, fontSize = 14.sp)
        Text("₩ ${formatKrw(summary.totalRefundAmountKrw)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(summary.totalRefundAmountForeign, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            RefundStatusChip("완료 ${summary.completedCount}건", summary.completedAmountKrw)
            RefundStatusChip("진행중 ${summary.inProgressCount}건", summary.inProgressAmountKrw)
        }
    }
}

@Composable
private fun RefundStatusChip(label: String, amountKrw: Long) {
    Column {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("₩ ${formatKrw(amountKrw)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PurchasedItemsSection(items: List<PurchasedItem>) {
    var showKrw by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("구매물품별 금액", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            CurrencyToggle(checked = showKrw, onCheckedChange = { showKrw = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            items.forEach { item -> PurchasedItemRow(item) }
        }
    }
}

@Composable
private fun CurrencyToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(BrandPrimary)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFF6F6F9)),
            contentAlignment = Alignment.Center,
        ) {
            // TODO: showKrw 값에 따라 실제 통화 전환 로직 연결
            Text("₩", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
    }
}

@Composable
private fun PurchasedItemRow(item: PurchasedItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderDefault)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // TODO: 실제 상품 이미지로 교체
        Box(modifier = Modifier.size(width = 80.dp, height = 126.dp).background(BorderDefault))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.name, fontSize = 14.sp, color = Color.Black)
                    Text(item.store, fontSize = 12.sp, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₩ ${formatKrw(item.priceKrw)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("환급액: ₩ ${formatKrw(item.refundAmountKrw)}", fontSize = 12.sp, color = Color.Black)
                }
            }
            RefundStatusBadge(item.status)
        }
    }
}

@Composable
private fun RefundStatusBadge(status: RefundStatus) {
    val (label, color) = when (status) {
        RefundStatus.COMPLETED -> "완료" to StatusGreen
        RefundStatus.IN_PROGRESS -> "진행중" to StatusOrange
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun ChecklistPreviewSection(items: List<ChecklistItem>, onSeeAllClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onSeeAllClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("CHECKLIST", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("›", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        items.forEach { item -> ChecklistPreviewRow(item) }
    }
}

@Composable
private fun ChecklistPreviewRow(item: ChecklistItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(width = 1.dp, color = BrandPrimary, shape = RoundedCornerShape(8.dp))
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                if (item.required) {
                    RequiredBadge()
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.description, fontSize = 12.sp, color = Color.Black)
        }
        // 여기서는 미완료 항목만 표시되므로 항상 회색(미완료) 원
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(BorderDisabled),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- LOOKET 탭 ----------

@Composable
private fun LooketTabContent(
    recommended: List<RecommendedProduct>,
    brands: List<BrandFilter>,
    myLooket: List<LooketProduct>,
) {
    // 요구사항 3: 브랜드 로고는 다중 토글 선택 (여러 개 동시 선택 가능)
    var selectedBrandIds by remember { mutableStateOf(setOf<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "FOR YOU",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // 요구사항 2: 옆으로 스크롤
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(recommended, key = { it.id }) { product -> RecommendedProductCard(product) }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("MY LOOKET", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                brands.forEach { brand ->
                    val isSelected = selectedBrandIds.contains(brand.id)
                    BrandFilterChip(
                        brand = brand,
                        selected = isSelected,
                        onClick = {
                            selectedBrandIds = if (isSelected) {
                                selectedBrandIds - brand.id
                            } else {
                                selectedBrandIds + brand.id
                            }
                        },
                    )
                }
            }

            // 선택된 브랜드가 없으면 전체 표시, 있으면 선택된 브랜드들만 (합집합)
            val filteredProducts = if (selectedBrandIds.isEmpty()) {
                myLooket
            } else {
                myLooket.filter { selectedBrandIds.contains(it.brandId) }
            }

            filteredProducts.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    rowItems.forEach { product ->
                        LooketProductCard(product, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedProductCard(product: RecommendedProduct) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(width = 1.dp, color = BorderDisabled, shape = RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // TODO: 실제 브랜드 로고로 교체
            Box(
                modifier = Modifier.size(43.dp).clip(CircleShape).background(Color.White)
                    .border(width = 1.dp, color = BorderDefault, shape = CircleShape),
            )
            Text(product.brandName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            product.title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(product.productName, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        // TODO: 실제 상품 이미지로 교체
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(BorderDefault))
        Text(product.location, modifier = Modifier.padding(16.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}

@Composable
private fun BrandFilterChip(brand: BrandFilter, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White)
            .then(
                if (selected) Modifier.border(width = 1.dp, color = BrandPrimary, shape = CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // TODO: brand.iconRes 실제 로고 이미지로 교체
        Text(brand.name.take(1), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LooketProductCard(product: LooketProduct, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // TODO: 실제 상품 이미지로 교체
            Box(modifier = Modifier.fillMaxWidth().height(170.dp).background(BorderDefault))
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(10.dp)) {
                Text(product.name, fontSize = 14.sp, color = Color.Black)
                Text(product.store, fontSize = 12.sp, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("₩ ${formatKrw(product.priceKrw)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("• ${product.statusLabel}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
        Button(
            onClick = { /* TODO: 관심리스트에서 제거 로직 연결 */ },
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("관심리스트에서 제거", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatKrw(amount: Long): String = "%,d".format(amount)

@Composable
private fun RequiredBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(StatusOrange)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text("필수", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun RefundTabPreview() {
    RefundTabContent(
        summary = HomeMockData.refundSummary,
        purchasedItems = HomeMockData.purchasedItems,
        checklistItems = HomeMockData.checklistItems,
        checklistCheckedIds = HomeMockData.checklistCheckedIds,
        onChecklistClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun LooketTabPreview() {
    LooketTabContent(
        recommended = HomeMockData.recommendedProducts,
        brands = HomeMockData.brandFilters,
        myLooket = HomeMockData.looketProducts,
    )
}