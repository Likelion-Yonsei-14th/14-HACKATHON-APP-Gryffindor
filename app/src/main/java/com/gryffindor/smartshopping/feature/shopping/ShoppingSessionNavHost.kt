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
}

/**
 * 쇼핑-실시간 세션 전체 흐름: 실시간 쇼핑(재생/일시정지/종료) -> 종료(문 버튼) 누르면
 * 쇼핑 결과 기록(영수증 등록 -> 구매 상품 확인)으로 이어진다. 세 화면이 하나의 연속된
 * 세션이라 NavController를 공유하는 NavHost 하나로 묶었다. 아직 AppNavGraph에는
 * 연결하지 않음 — 매장 선택 확인 후 이 NavHost로 들어오는 배선은 다음 단계.
 */
@Composable
fun ShoppingSessionNavHost(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
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
                onBackClick = { navController.popBackStack() },
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
                onBackClick = { navController.popBackStack() },
                onRegisterClick = { navController.navigate(ShoppingSessionRoutes.RESULT_PURCHASED_ITEMS) },
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(ShoppingSessionRoutes.RESULT_PURCHASED_ITEMS) {
            var purchasedItems by remember { mutableStateOf(dummyLiveReceiptItems) }

            PurchasedItemsScreen(
                items = purchasedItems,
                onRemoveItem = { id -> purchasedItems = purchasedItems.filterNot { it.id == id } },
                onWishlistItem = {},
                onAddManuallyClick = {},
                onBackClick = { navController.popBackStack() },
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
        ShoppingSessionNavHost(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    }
}
