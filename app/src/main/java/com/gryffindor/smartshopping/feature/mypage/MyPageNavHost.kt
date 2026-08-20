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
    const val RECEIPT = "mypage/receipt"
    const val RECEIPT_REGISTER = "mypage/receipt/register"
    const val RECEIPT_STORE = "mypage/receipt/{storeId}"

    fun receiptStore(storeId: String) = "mypage/receipt/$storeId"
}

/**
 * 마이페이지 탭 하위 화면 전환. 진짜 유저 데이터/Repository 연결 전까지는
 * 더미 값으로 채워두고, 실제 배선은 AppNavGraph에 이 컴포저블을 얹을 때 진행한다.
 *
 * TRAVEL/WISHLIST 메뉴는 이 NavHost 내부가 아니라 [onNavigateToTripList]/[onNavigateToWishlist]를
 * 통해 최상위 AppNavGraph의 진짜 여행(Trip)/위시리스트 플로우로 나간다 — 둘 다 실제
 * Repository(appContainer)가 필요한데 이 NavHost는 아직 그걸 모르기 때문. 목업이었던
 * MyPageTravelScreen은 실제 TripListScreen/TripDetailScreen 연결로 대체됨.
 */
@Composable
fun MyPageNavHost(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    onNavigateToTripList: () -> Unit,
    onNavigateToWishlist: () -> Unit,
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
                onTravelClick = onNavigateToTripList,
                onReceiptClick = { navController.navigate(MyPageRoutes.RECEIPT) },
                onWishlistClick = onNavigateToWishlist,
                onLogoutClick = {},
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

            MyPageReceiptViewScreen(
                storeName = storeName,
                onBackClick = { navController.popBackStack() },
                onRetakePhotoClick = {},
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
        MyPageNavHost(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onNavigateToTripList = {},
            onNavigateToWishlist = {},
        )
    }
}
