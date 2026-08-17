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

private object ShoppingResultRoutes {
    const val RECEIPT = "shopping-result/receipt"
    const val PURCHASED_ITEMS = "shopping-result/purchased-items"
}

/**
 * 쇼핑 결과 기록 흐름 — 실시간 쇼핑 종료(문 버튼) 후 이 지점부터 시작한다.
 * 영수증 등록 -> (OCR로 인식했다고 가정한) 구매 상품 목록. 아직 AppNavGraph에는
 * 연결하지 않았고, 진짜로는 LiveShoppingScreen의 onFinishClick에서 이 NavHost
 * 진입점(RECEIPT)으로 이동해야 한다.
 */
@Composable
fun ShoppingResultNavHost(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ShoppingResultRoutes.RECEIPT, modifier = modifier) {
        composable(ShoppingResultRoutes.RECEIPT) {
            ShoppingResultReceiptScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterClick = { navController.navigate(ShoppingResultRoutes.PURCHASED_ITEMS) },
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(ShoppingResultRoutes.PURCHASED_ITEMS) {
            var items by remember { mutableStateOf(dummyLiveReceiptItems) }

            PurchasedItemsScreen(
                items = items,
                onRemoveItem = { id -> items = items.filterNot { it.id == id } },
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
private fun ShoppingResultNavHostPreview() {
    var selectedTab by remember { mutableStateOf(BottomNavTab.SHOP) }
    LooketTheme {
        ShoppingResultNavHost(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    }
}
