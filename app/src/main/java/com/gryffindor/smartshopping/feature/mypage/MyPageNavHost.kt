package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.domain.model.Store
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import com.gryffindor.smartshopping.domain.repository.StoreRepository
import com.gryffindor.smartshopping.feature.shopping.toLooketStore

private object MyPageRoutes {
    const val HOME = "mypage/home"
    const val RECEIPT = "mypage/receipt"
    const val RECEIPT_REGISTER = "mypage/receipt/register"
    const val RECEIPT_STORE = "mypage/receipt/{storeId}"

    fun receiptStore(storeId: String) = "mypage/receipt/$storeId"
}

/**
 * 마이페이지 탭 하위 화면 전환.
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
    personalizationRepository: PersonalizationRepository? = null,
    storeRepository: StoreRepository? = null,
) {
    val navController = rememberNavController()

    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    LaunchedEffect(storeRepository) {
        stores = try {
            storeRepository?.getStores() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    NavHost(navController = navController, startDestination = MyPageRoutes.HOME, modifier = modifier) {
        composable(MyPageRoutes.HOME) {
            var selectedLanguage by remember { mutableStateOf<String?>(null) }
            var selectedCurrency by remember { mutableStateOf<String?>(null) }

            val myPageViewModel: MyPageViewModel? = personalizationRepository?.let {
                viewModel(factory = MyPageViewModel.Factory(it))
            }
            val myPageUiState = myPageViewModel?.uiState?.collectAsState()?.value

            LaunchedEffect(myPageViewModel) {
                myPageViewModel?.loadMyPage()
            }

            MyPageScreen(
                nickname = myPageUiState?.myPage?.user?.name ?: "gryffindor0825",
                purchasedProducts = myPageUiState?.myPage?.purchasedProducts ?: emptyList(),
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
                stores = stores.map { it.toLooketStore() },
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
            val storeName = stores.find { it.id == storeId }?.name ?: storeId

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
