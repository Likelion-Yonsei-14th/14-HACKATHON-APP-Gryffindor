package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

private object ShoppingSessionRoutes {
    const val LIVE = "shopping-session/live"
    const val RESULT_RECEIPT = "shopping-session/result/receipt"
    const val RESULT_PURCHASED_ITEMS = "shopping-session/result/purchased-items"
    const val RESULT_INTERESTED_PRODUCTS = "shopping-session/result/interested-products"
}

/**
 * 쇼핑-실시간 세션 전체 흐름: 실시간 쇼핑(재생/일시정지/종료) -> 종료(문 버튼) 누르면
 * 쇼핑 결과 기록(영수증 등록 -> 구매 상품 확인)으로 이어진다. 세 화면이 하나의 연속된
 * 세션이라 NavController를 공유하는 NavHost 하나로 묶었다.
 *
 * 이 안의 모든 화면의 뒤로가기(<)는 직전 화면이 아니라 항상 [onBackToStoreSelection]으로
 * 보낸다 — 매장 선택 -> 쇼핑 세션은 하나의 연속된 플로우라서, 세션 중 어느 화면에 있든
 * "뒤로"는 그 플로우의 진입점인 매장 선택 화면으로 돌아가는 것이 자연스럽다는 사용자 요청.
 */
@Composable
fun ShoppingSessionNavHost(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    onBackToStoreSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ShoppingSessionRoutes.LIVE, modifier = modifier) {
        composable(ShoppingSessionRoutes.LIVE) {
            var isSessionActive by remember { mutableStateOf(false) }
            var liveItems by remember { mutableStateOf(dummyLiveReceiptItems) }
            var isExchangeRateOn by remember { mutableStateOf(false) }

            LiveShoppingScreen(
                isSessionActive = isSessionActive,
                onPlayClick = { isSessionActive = true },
                onPauseClick = { isSessionActive = false },
                onFinishClick = { navController.navigate(ShoppingSessionRoutes.RESULT_RECEIPT) },
                onBackClick = onBackToStoreSelection,
                totalPurchaseAmount = "₩ 3,400,000",
                refundAmount = "₩ 230,000",
                items = liveItems,
                onRemoveItem = { id -> liveItems = liveItems.filterNot { it.id == id } },
                isExchangeRateOn = isExchangeRateOn,
                onExchangeRateToggle = { isExchangeRateOn = !isExchangeRateOn },
            )
        }

        composable(ShoppingSessionRoutes.RESULT_RECEIPT) {
            ShoppingResultReceiptScreen(
                onBackClick = onBackToStoreSelection,
                onRegisterClick = { navController.navigate(ShoppingSessionRoutes.RESULT_PURCHASED_ITEMS) },
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(ShoppingSessionRoutes.RESULT_PURCHASED_ITEMS) {
            var purchasedItems by remember { mutableStateOf(dummyLiveReceiptItems) }
            var viewedItems by remember { mutableStateOf(dummyViewedItems) }

            PurchasedItemsScreen(
                items = purchasedItems,
                onRemoveItem = { id -> purchasedItems = purchasedItems.filterNot { it.id == id } },
                onWishlistItem = {},
                viewedItems = viewedItems,
                onAddViewedItem = { id ->
                    viewedItems.find { it.id == id }?.let { found ->
                        purchasedItems = purchasedItems + found
                        viewedItems = viewedItems.filterNot { it.id == id }
                    }
                },
                onConfirmClick = { navController.navigate(ShoppingSessionRoutes.RESULT_INTERESTED_PRODUCTS) },
                onBackClick = onBackToStoreSelection,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(ShoppingSessionRoutes.RESULT_INTERESTED_PRODUCTS) {
            var selectedProductIds by remember { mutableStateOf(setOf<String>()) }

            InterestedProductsScreen(
                products = dummyRecommendedProducts,
                selectedProductIds = selectedProductIds,
                onToggleProduct = { id ->
                    selectedProductIds = if (id in selectedProductIds) {
                        selectedProductIds - id
                    } else {
                        selectedProductIds + id
                    }
                },
                // 세션 종료 후 진짜 이동해야 할 화면(쇼핑 홈 등)이 아직 없어서, 임시로
                // 이 NavHost의 시작 지점(LIVE)까지 스택을 정리한다.
                onCompleteClick = { navController.popBackStack(ShoppingSessionRoutes.LIVE, inclusive = false) },
                onBackClick = onBackToStoreSelection,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun ShoppingSessionNavHostPreview() {
    var selectedTab by remember { mutableStateOf(BottomNavTab.SHOP) }
    LooketTheme {
        ShoppingSessionNavHost(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onBackToStoreSelection = {},
        )
    }
}
