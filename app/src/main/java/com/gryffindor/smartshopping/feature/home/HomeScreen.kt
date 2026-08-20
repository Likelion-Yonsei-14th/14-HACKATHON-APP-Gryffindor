package com.gryffindor.smartshopping.feature.home

import androidx.compose.foundation.Image
import androidx.compose.material3.Scaffold
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.HomeTopBar
import com.gryffindor.smartshopping.core.ui.component.HomeTopBarTab
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme
import com.gryffindor.smartshopping.domain.model.ChecklistItem
import com.gryffindor.smartshopping.domain.model.FeedRecommendation
import com.gryffindor.smartshopping.domain.model.PurchasedProduct
import com.gryffindor.smartshopping.domain.model.RefundChecklist

// LooketColors에 아직 없는 상태/배지 색상만 로컬로 유지 (팀 팔레트에 추가되면 교체)
private val StatusGreen = Color(0xFF00D96C)
private val StatusOrange = Color(0xFFFF8633)
private val TextSecondaryOnDisabled = Color(0xFF9C9AAE)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToShopping: (sessionId: String) -> Unit,
    onNavigateToChecklist: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh Backend data every time Home becomes visible (e.g. after Review → Home)
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    LaunchedEffect(uiState.sessionId) {
        uiState.sessionId?.let { sessionId ->
            onNavigateToShopping(sessionId)
            viewModel.resetSessionNavigation()
        }
    }

    var selectedTopTab by remember { mutableStateOf(HomeTopBarTab.REFUND) }

    Scaffold(
        bottomBar = { BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected) },
        containerColor = LooketColors.Surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface),
        ) {
            HomeTopBar(selectedTab = selectedTopTab, onTabSelected = { selectedTopTab = it })

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                when (selectedTopTab) {
                    HomeTopBarTab.REFUND -> {
                        // Loading state — show progress indicator, no fake data
                        if (uiState.isHomeDataLoading && !uiState.homeDataLoaded) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = LooketColors.BrandPrimary)
                            }
                        } else {
                            // Use actual purchased products from Backend
                            val actualPurchasedItems = if (uiState.purchasedProducts.isNotEmpty()) {
                                uiState.purchasedProducts.map { it.toPurchasedItem() }
                            } else {
                                emptyList()
                            }

                            val summary = if (uiState.purchasedProducts.isNotEmpty()) {
                                computeRefundSummary(uiState.purchasedProducts)
                            } else {
                                RefundSummary(0, 0, 0, 0, 0, 0)
                            }

                            RefundTabContent(
                                summary = summary,
                                purchasedItems = actualPurchasedItems,
                                refundChecklist = uiState.refundChecklist,
                                isChecklistLoading = uiState.isChecklistLoading,
                                onChecklistClick = onNavigateToChecklist,
                            )
                        }
                    }
                    HomeTopBarTab.LOOKET -> {
                        // Use actual recommendations from Backend; fallback to static data
                        val actualRecommended = if (uiState.recommendations.isNotEmpty()) {
                            uiState.recommendations.map { it.toRecommendedProduct() }
                        } else {
                            HomeProductionData.recommendedProducts
                        }

                        LooketTabContent(
                            recommended = actualRecommended,
                            brands = HomeProductionData.brandFilters,
                            myLooket = HomeProductionData.looketProducts,
                        )
                    }
                }
            }
        }
    }
}

// ---------- REFUND 탭 ----------

@Composable
private fun RefundTabContent(
    summary: RefundSummary,
    purchasedItems: List<PurchasedItem>,
    refundChecklist: RefundChecklist?,
    isChecklistLoading: Boolean,
    onChecklistClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        RefundSummaryCard(summary, modifier = Modifier.padding(horizontal = 16.dp))
        PurchasedItemsSection(purchasedItems)

        // Real Backend checklist — show loading or actual items
        if (isChecklistLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = LooketColors.BrandPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        } else if (refundChecklist != null && refundChecklist.items.isNotEmpty()) {
            ChecklistPreviewSection(items = refundChecklist.items, onSeeAllClick = onChecklistClick)
            if (refundChecklist.notice != null) {
                Text(
                    refundChecklist.notice,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    color = LooketColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun RefundSummaryCard(summary: RefundSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(colors = listOf(LooketColors.BrandPrimary, LooketColors.BrandGradientEnd)))
            .padding(vertical = 20.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("OO님의 환급 금액", color = LooketColors.TextInverse, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("총 구매 금액 ₩${formatKrw(summary.totalPurchaseAmountKrw)} 중", color = LooketColors.TextInverse, fontSize = 14.sp)
        Column {
            Text("₩${formatKrw(summary.totalRefundAmountKrw)}", color = LooketColors.TextInverse, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        ) {
            RefundStatusChip("완료 ${summary.completedCount}건", summary.completedAmountKrw, dotColor = StatusGreen)
            RefundStatusChip("진행중 ${summary.inProgressCount}건", summary.inProgressAmountKrw, dotColor = StatusOrange)
        }
    }
}

@Composable
private fun RefundStatusChip(label: String, amountKrw: Long, dotColor: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            }
            Box(modifier = Modifier.padding(10.dp)) {
                Text(label, color = LooketColors.TextInverse, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(modifier = Modifier.padding(10.dp)) {
            Text("₩${formatKrw(amountKrw)}", color = LooketColors.TextInverse, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PurchasedItemsSection(items: List<PurchasedItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("구매물품별 금액", color = LooketColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            items.forEach { item -> PurchasedItemRow(item) }
        }
    }
}

@Composable
private fun PurchasedItemRow(item: PurchasedItem) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LooketColors.BorderDefault))
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 상품 이미지: imageUrl이 있으면 실제 이미지, 없으면 placeholder
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 126.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LooketColors.BorderDefault),
            ) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            item.name,
                            fontSize = 14.sp,
                            color = Color.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            item.store,
                            fontSize = 12.sp,
                            color = LooketColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        modifier = Modifier.widthIn(min = 90.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            "₩${formatKrw(item.priceKrw)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            "환급액: ₩${formatKrw(item.refundAmountKrw)}",
                            fontSize = 12.sp,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                RefundStatusBadge(item.status)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LooketColors.BorderDefault))
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
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LooketColors.TextPrimary)
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
            Text("CHECKLIST", color = LooketColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("›", color = LooketColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
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
            .background(LooketColors.Surface)
            .border(width = 1.dp, color = LooketColors.BrandPrimary, shape = RoundedCornerShape(8.dp))
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(item.description, fontSize = 12.sp, color = Color.Black)
            }
        }
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(LooketColors.BorderDisabled),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2713", color = TextSecondaryOnDisabled, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
    var selectedBrandIds by remember { mutableStateOf(setOf<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "FOR YOU",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = LooketColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
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
            Text("MY LOOKET", color = LooketColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

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
            .background(LooketColors.Surface)
            .border(width = 1.dp, color = LooketColors.BorderDisabled, shape = RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_mcm),
                contentDescription = product.brandName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(43.dp)
                    .clip(CircleShape)
                    .border(width = 1.dp, color = LooketColors.BorderDefault, shape = CircleShape),
            )
            Text(product.brandName, color = LooketColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            product.title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = LooketColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(product.productName, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        // 상품 이미지: imageUrl이 있으면 실제 이미지, 없으면 placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(LooketColors.BorderDefault),
        ) {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Text(
            product.location,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BrandFilterChip(brand: BrandFilter, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(LooketColors.Surface)
            .then(
                if (selected) Modifier.border(width = 1.dp, color = LooketColors.BrandPrimary, shape = CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = brand.iconRes),
            contentDescription = brand.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
    }
}

@Composable
private fun LooketProductCard(product: LooketProduct, modifier: Modifier = Modifier) {
    val statusColor = if (product.statusLabel == "구매") {
        LooketColors.BrandPrimary
    } else {
        LooketColors.BrandPrimarySubtle
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 상품 이미지: imageUrl이 있으면 실제 이미지, 없으면 placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(LooketColors.BorderDefault),
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth().background(LooketColors.Surface).padding(10.dp)) {
                Text(product.name, fontSize = 14.sp, color = Color.Black)
                Text(product.store, fontSize = 12.sp, color = LooketColors.TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("₩${formatKrw(product.priceKrw)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                        Text(product.statusLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LooketColors.TextPrimary)
                    }
                }
            }
        }
        Button(
            onClick = { /* TODO: 관심리스트에서 제거 로직 연결 */ },
            colors = ButtonDefaults.buttonColors(containerColor = LooketColors.BrandPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("관심리스트에서 제거", color = LooketColors.TextInverse, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatKrw(amount: Long): String = "%,d".format(amount)

// --- Domain → Home UI model mappers ---

/**
 * Maps a Backend PurchasedProduct to the Home UI's PurchasedItem model.
 */
private fun PurchasedProduct.toPurchasedItem(): PurchasedItem = PurchasedItem(
    id = purchaseItemId,
    name = product?.name ?: fallbackProductName ?: "상품",
    store = storeName ?: "",
    priceKrw = (price ?: 0).toLong(),
    refundAmountKrw = 0L, // Refund amount not available from this endpoint
    status = RefundStatus.COMPLETED,
    imageUrl = product?.imageUrl,
)

/**
 * Maps a Backend FeedRecommendation to the Home UI's RecommendedProduct model.
 */
private fun FeedRecommendation.toRecommendedProduct(): RecommendedProduct = RecommendedProduct(
    id = product.productId,
    brandName = product.brand,
    title = "당신만을 위한 오늘의 셀렉션",
    productName = product.name,
    location = stores.firstOrNull()?.let { store ->
        val terminal = store.terminal ?: ""
        val name = store.name
        if (terminal.isNotEmpty()) "$terminal · $name" else name
    } ?: "",
    imageUrl = product.imageUrl,
)

/**
 * Computes a summary for the refund card from actual purchased products.
 */
private fun computeRefundSummary(products: List<PurchasedProduct>): RefundSummary {
    val totalAmount = products.sumOf { (it.price ?: 0).toLong() }
    return RefundSummary(
        totalPurchaseAmountKrw = totalAmount,
        totalRefundAmountKrw = 0L, // Detailed refund breakdown not available from GET /me
        completedCount = products.size,
        completedAmountKrw = 0L,
        inProgressCount = 0,
        inProgressAmountKrw = 0L,
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun HomeScreenPreview() {
    var selectedTab by remember { mutableStateOf(HomeTopBarTab.REFUND) }
    var selectedBottomTab by remember { mutableStateOf(BottomNavTab.HOME) }

    LooketTheme {
        Scaffold(
            topBar = { HomeTopBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) },
            bottomBar = {
                BottomNavBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { selectedBottomTab = it },
                )
            },
            containerColor = LooketColors.Surface,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                when (selectedTab) {
                    HomeTopBarTab.REFUND -> RefundTabContent(
                        summary = HomeProductionData.refundSummary,
                        purchasedItems = HomeProductionData.purchasedItems,
                        refundChecklist = RefundChecklist(
                            tripId = "preview",
                            status = com.gryffindor.smartshopping.domain.model.RefundChecklistStatus.ACTION_REQUIRED,
                            items = HomeProductionData.checklistItems,
                            notice = null,
                        ),
                        isChecklistLoading = false,
                        onChecklistClick = {},
                    )
                    HomeTopBarTab.LOOKET -> LooketTabContent(
                        recommended = HomeProductionData.recommendedProducts,
                        brands = HomeProductionData.brandFilters,
                        myLooket = HomeProductionData.looketProducts,
                    )
                }
            }
        }
    }
}
