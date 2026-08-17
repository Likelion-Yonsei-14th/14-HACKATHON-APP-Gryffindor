package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

private object MyPageRoutes {
    const val HOME = "mypage/home"
    const val TRAVEL = "mypage/travel"
    const val RECEIPT = "mypage/receipt"
    const val RECEIPT_REGISTER = "mypage/receipt/register"
    const val RECEIPT_STORE = "mypage/receipt/{storeId}"

    fun receiptStore(storeId: String) = "mypage/receipt/$storeId"
}

// 매장별 더미 영수증. 실제로는 온보딩에서 등록한 값을 백엔드/로컬 저장소에서 가져와야 한다.
private fun dummyReceiptsFor(storeId: String): List<MyPageReceiptItem> = when (storeId) {
    "1" -> listOf(
        MyPageReceiptItem("r1", "2026.08.15", "MCM 백팩", "₩890,000"),
        MyPageReceiptItem("r2", "2026.08.16", "MCM 반지갑", "₩350,000"),
    )
    "2" -> listOf(
        MyPageReceiptItem("r3", "2026.08.14", "MCM 크로스백", "₩620,000"),
    )
    else -> emptyList()
}

/**
 * 마이페이지 탭 하위 화면 전환. 진짜 유저 데이터/Repository 연결 전까지는
 * 더미 값으로 채워두고, 실제 배선은 AppNavGraph에 이 컴포저블을 얹을 때 진행한다.
 */
@Composable
fun MyPageNavHost(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MyPageRoutes.HOME, modifier = modifier) {
        composable(MyPageRoutes.HOME) {
            var selectedLanguage by remember { mutableStateOf<String?>(null) }
            var selectedCurrency by remember { mutableStateOf<String?>(null) }

            MyPageScreen(
                nickname = "gryffindor0825",
                selectedLanguage = selectedLanguage,
                selectedCurrency = selectedCurrency,
                onLanguageSelected = { selectedLanguage = it },
                onCurrencySelected = { selectedCurrency = it },
                onTravelClick = { navController.navigate(MyPageRoutes.TRAVEL) },
                onReceiptClick = { navController.navigate(MyPageRoutes.RECEIPT) },
                onLogoutClick = {},
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(MyPageRoutes.TRAVEL) {
            MyPageTravelScreen(
                departureAirport = "BEJ",
                arrivalAirport = "ICN",
                terminal = "인천공항 T2",
                departureTime = "2026.08.21 10:00",
                arrivalTime = "2026.08.25 19:00",
                airportArrivalEstimate = "2026.08.25 15:00",
                onBackClick = { navController.popBackStack() },
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(MyPageRoutes.RECEIPT) {
            var selectedStoreId by remember { mutableStateOf<String?>(null) }

            MyPageReceiptScreen(
                selectedStoreId = selectedStoreId,
                onStoreSelected = { selectedStoreId = it },
                onAddReceiptClick = { navController.navigate(MyPageRoutes.RECEIPT_REGISTER) },
                onConfirmClick = {
                    selectedStoreId?.let { navController.navigate(MyPageRoutes.receiptStore(it)) }
                },
                onBackClick = { navController.popBackStack() },
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(MyPageRoutes.RECEIPT_REGISTER) {
            MyPageReceiptRegisterScreen(
                onBackClick = { navController.popBackStack() },
                onRetakePhotoClick = {},
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        composable(
            route = MyPageRoutes.RECEIPT_STORE,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: return@composable
            val storeName = dummyStores.find { it.id == storeId }?.name ?: storeId

            MyPageReceiptListScreen(
                storeName = storeName,
                receipts = dummyReceiptsFor(storeId),
                onBackClick = { navController.popBackStack() },
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun MyPageNavHostPreview() {
    var selectedTab by remember { mutableStateOf(BottomNavTab.MY_PAGE) }
    LooketTheme {
        MyPageNavHost(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
    }
}
