package com.gryffindor.smartshopping.feature.mypage

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

private object MyPageRoutes {
    const val HOME = "mypage/home"
    const val TRAVEL = "mypage/travel"
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
                onReceiptClick = {},
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
