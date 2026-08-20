package com.gryffindor.smartshopping.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.feature.checklist.ChecklistScreen
import com.gryffindor.smartshopping.feature.checklist.ChecklistViewModel
import com.gryffindor.smartshopping.feature.home.HomeScreen
import com.gryffindor.smartshopping.feature.home.HomeViewModel
import com.gryffindor.smartshopping.feature.login.LoginScreen
import com.gryffindor.smartshopping.feature.mypage.MyPageNavHost
import com.gryffindor.smartshopping.feature.mypage.MyPageWishlistScreen
import com.gryffindor.smartshopping.feature.mypage.MyPageWishlistViewModel
import com.gryffindor.smartshopping.feature.onboarding.FlightInfoConfirmScreen
import com.gryffindor.smartshopping.feature.onboarding.FlightInfoField
import com.gryffindor.smartshopping.feature.onboarding.FlightRegisterScreen
import com.gryffindor.smartshopping.feature.onboarding.OnboardingPurchaseConfirmScreen
import com.gryffindor.smartshopping.feature.onboarding.OnboardingReceiptRegisterScreen
import com.gryffindor.smartshopping.feature.onboarding.PermissionScreen
import com.gryffindor.smartshopping.feature.onboarding.PurchaseConfirmItem
import com.gryffindor.smartshopping.feature.onboarding.UserInfoScreen
import com.gryffindor.smartshopping.feature.recommendation.RecommendationScreen
import com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel
import com.gryffindor.smartshopping.feature.reservation.ReservationListScreen
import com.gryffindor.smartshopping.feature.reservation.VisitReservationScreen
import com.gryffindor.smartshopping.feature.reservation.VisitReservationViewModel
import com.gryffindor.smartshopping.feature.review.ReviewScreen
import com.gryffindor.smartshopping.feature.review.ReviewViewModel
import com.gryffindor.smartshopping.feature.shopping.ShoppingScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingSessionNavHost
import com.gryffindor.smartshopping.feature.shopping.ShoppingStoreSelectionScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel
import com.gryffindor.smartshopping.feature.splash.SplashScreen
import com.gryffindor.smartshopping.feature.travel.TravelScreen
import com.gryffindor.smartshopping.feature.travel.TravelViewModel
import com.gryffindor.smartshopping.feature.trip.FlightEditScreen
import com.gryffindor.smartshopping.feature.trip.HotelEditScreen
import com.gryffindor.smartshopping.feature.trip.TripCreateScreen
import com.gryffindor.smartshopping.feature.trip.TripDetailScreen
import com.gryffindor.smartshopping.feature.trip.TripListScreen
import com.gryffindor.smartshopping.feature.trip.TripViewModel
import com.gryffindor.smartshopping.domain.model.TripDates
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer
) {
    // 하단 네비게이션 바(HOME/SHOP/MY PAGE) 공용 탭 전환 핸들러. 각 탭의 시작 목적지로
    // 이동하되, 뒤로가기 스택이 계속 쌓이지 않도록 popUpTo/launchSingleTop/restoreState를 쓴다.
    val onBottomTabSelected: (BottomNavTab) -> Unit = { tab ->
        val route = when (tab) {
            BottomNavTab.HOME -> Routes.HOME
            BottomNavTab.SHOP -> Routes.SHOP_TAB
            BottomNavTab.MY_PAGE -> Routes.MY_PAGE
        }
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // 1. 스플래시 (2초 후 로그인으로 이동)
        composable(Routes.SPLASH) {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(2000L)
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        // 2. 로그인 (카카오/게스트 로그인 시 온보딩 첫 단계인 권한 화면으로 이동)
        composable(Routes.LOGIN) {
            LoginScreen(
                onKakaoLogin = {
                    navController.navigate(Routes.ONBOARDING_PERMISSION) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGuestLogin = {
                    navController.navigate(Routes.ONBOARDING_PERMISSION) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // --- 온보딩 플로우 시작 ---

        // 3. 권한 허용 화면
        composable(Routes.ONBOARDING_PERMISSION) {
            PermissionScreen(
                onNext = {
                    navController.navigate(Routes.ONBOARDING_USER_INFO)
                }
            )
        }

        // 4. 사용자 정보 입력 화면
        composable(Routes.ONBOARDING_USER_INFO) {
            UserInfoScreen(
                onComplete = {
                    navController.navigate(Routes.ONBOARDING_FLIGHT_REGISTER)
                }
            )
        }

        // 5. 항공편 사진 등록 화면
        composable(Routes.ONBOARDING_FLIGHT_REGISTER) {
            var hasPhoto by remember { mutableStateOf(false) }

            FlightRegisterScreen(
                hasPhoto = hasPhoto,
                onBackClick = { navController.popBackStack() },
                onCaptureClick = {
                    // TODO: 실제 사진 촬영/선택 로직 구현
                    if (!hasPhoto) {
                        hasPhoto = true // 사진 등록 상태로 변경 예시
                    } else {
                        // 사진 등록 완료 후 확인 화면으로 이동
                        navController.navigate(Routes.ONBOARDING_FLIGHT_CONFIRM)
                    }
                },
                onSkipClick = {
                    // 항공편 확인 단계를 건너뛰고 바로 영수증 등록으로 이동
                    navController.navigate(Routes.ONBOARDING_RECEIPT_REGISTER)
                }
            )
        }

        // 6. 항공편 정보 확인 화면 (온보딩 마지막)
        composable(Routes.ONBOARDING_FLIGHT_CONFIRM) {
            val initialFields = listOf(
                FlightInfoField("1", "출발지", "BEJ"),
                FlightInfoField("2", "도착지", "ICN"),
                FlightInfoField("3", "터미널", "인천공항 T2"),
                FlightInfoField("4", "출발 시간", "2026.08.21 10:00"),
                FlightInfoField("5", "도착 시간", "2026.08.25 19:00"),
                FlightInfoField("6", "공항 도착 예정시간", "2026.08.25 15:00"),
            )

            FlightInfoConfirmScreen(
                fields = initialFields,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { updatedFields ->
                    // 체크리스트 날짜 네비게이터 초기값으로 쓸 수 있게 확인한 출발 날짜를 보관
                    appContainer.tripDates = TripDates(departureDate = parseDepartureDate(updatedFields))
                    // 항공편 확인 완료 후 영수증 등록 단계로 이동
                    navController.navigate(Routes.ONBOARDING_RECEIPT_REGISTER)
                }
            )
        }

        // 7. 영수증 등록 화면 (건너뛰면 홈으로 바로 이동)
        composable(Routes.ONBOARDING_RECEIPT_REGISTER) {
            var hasPhoto by remember { mutableStateOf(false) }

            OnboardingReceiptRegisterScreen(
                hasPhoto = hasPhoto,
                onBackClick = { navController.popBackStack() },
                onCaptureClick = {
                    // TODO: 실제 영수증 촬영/선택 로직 구현
                    if (!hasPhoto) {
                        hasPhoto = true
                    } else {
                        navController.navigate(Routes.ONBOARDING_PURCHASE_CONFIRM)
                    }
                },
                onSkipClick = {
                    // 온보딩 완료! 홈 화면으로 이동하며 온보딩 스택들을 백스택에서 제거
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING_PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        // 8. 구매 물품 목록 확인 화면 (온보딩 마지막)
        composable(Routes.ONBOARDING_PURCHASE_CONFIRM) {
            // TODO: 실제 인식된 구매 물품 목록으로 교체
            val initialItems = listOf(
                PurchaseConfirmItem("1", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000", "환급액: ₩ 76,000"),
                PurchaseConfirmItem("2", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000", "환급액: ₩ 76,000"),
            )
            var items by remember { mutableStateOf(initialItems) }

            OnboardingPurchaseConfirmScreen(
                items = items,
                onBackClick = { navController.popBackStack() },
                onRemoveItem = { id -> items = items.filterNot { it.id == id } },
                onConfirmClick = {
                    // 온보딩 완료! 홈 화면으로 이동하며 온보딩 스택들을 백스택에서 제거
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING_PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        // --- 메인 홈 및 기능 화면들 ---

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    appContainer.sessionRepository,
                    appContainer.cameraFrameProvider
                )
            )
            val uiState by viewModel.uiState.collectAsState()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToShopping = { sessionId ->
                    // TODO: 화면에 아직 통화 선택 UI가 없어서 임시로 KRW 고정. 백엔드 통화
                    // 선택 로직(SupportedCountry 등) 연결 시 실제 선택값으로 교체 필요.
                    navController.navigate(Routes.shopping(sessionId, "KRW"))
                },
                onNavigateToChecklist = {
                    // sessionId는 쇼핑 화면 이동 즉시 null로 리셋되므로, 홈에 머무는 동안엔
                    // 가장 최근에 시작한 세션(lastSessionId)으로 체크리스트를 연다.
                    uiState.lastSessionId?.let { sessionId ->
                        navController.navigate(Routes.checklist(sessionId))
                    }
                },
                selectedTab = BottomNavTab.HOME,
                onTabSelected = onBottomTabSelected,
            )
        }

        // --- 하단 네비게이션 SHOP/MY PAGE 탭 ---
        // 화면 우선 제작 단계라 더미 데이터 기반. 실제 매장/세션 데이터 연결은 다음 단계.

        composable(Routes.SHOP_TAB) {
            var selectedStoreId by remember { mutableStateOf<String?>(null) }

            ShoppingStoreSelectionScreen(
                selectedStoreId = selectedStoreId,
                onStoreSelected = { selectedStoreId = it },
                onConfirmClick = { navController.navigate(Routes.SHOPPING_SESSION) },
                onBackClick = { navController.popBackStack() },
                selectedTab = BottomNavTab.SHOP,
                onTabSelected = onBottomTabSelected,
            )
        }

        composable(Routes.SHOPPING_SESSION) {
            ShoppingSessionNavHost(
                selectedTab = BottomNavTab.SHOP,
                onTabSelected = onBottomTabSelected,
                onBackToStoreSelection = { navController.popBackStack() },
            )
        }

        composable(Routes.MY_PAGE) {
            MyPageNavHost(
                selectedTab = BottomNavTab.MY_PAGE,
                onTabSelected = onBottomTabSelected,
                onNavigateToTripList = { navController.navigate(Routes.TRIP_LIST) },
                onNavigateToWishlist = { navController.navigate(Routes.WISHLIST) },
            )
        }

        composable(Routes.WISHLIST) {
            val viewModel: MyPageWishlistViewModel = viewModel(
                factory = MyPageWishlistViewModel.Factory(appContainer.personalizationRepository)
            )
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.loadWishlist() }
            MyPageWishlistScreen(
                uiState = uiState,
                onRemoveClick = { productId -> viewModel.removeFromWishlist(productId) },
                onBackClick = { navController.popBackStack() },
                selectedTab = BottomNavTab.MY_PAGE,
                onTabSelected = onBottomTabSelected,
            )
        }

        // --- 여행(Trip) 관리 플로우. 마이페이지의 TRAVEL 메뉴에서 진입한다. ---

        composable(Routes.TRIP_LIST) {
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(appContainer.tripRepository, appContainer.personalizationRepository)
            )
            Scaffold(
                bottomBar = { BottomNavBar(selectedTab = BottomNavTab.MY_PAGE, onTabSelected = onBottomTabSelected) },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TripListScreen(
                        viewModel = viewModel,
                        onNavigateToCreate = { navController.navigate(Routes.TRIP_CREATE) },
                        onNavigateToDetail = { tripId -> navController.navigate(Routes.tripDetail(tripId)) },
                    )
                }
            }
        }

        composable(Routes.TRIP_CREATE) {
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(appContainer.tripRepository, appContainer.personalizationRepository)
            )
            TripCreateScreen(
                viewModel = viewModel,
                onTripCreated = { tripId ->
                    navController.navigate(Routes.tripDetail(tripId)) {
                        popUpTo(Routes.TRIP_LIST) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(appContainer.tripRepository, appContainer.personalizationRepository)
            )
            Scaffold(
                bottomBar = { BottomNavBar(selectedTab = BottomNavTab.MY_PAGE, onTabSelected = onBottomTabSelected) },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TripDetailScreen(
                        viewModel = viewModel,
                        tripId = tripId,
                        onNavigateToFlightEdit = { flightId -> navController.navigate(Routes.flightEdit(tripId, flightId)) },
                        onNavigateToHotelEdit = { navController.navigate(Routes.hotelEdit(tripId)) },
                        onNavigateToVisitReservation = { storeId, storeName ->
                            navController.navigate(Routes.visitReservation(tripId, storeId, storeName))
                        },
                    )
                }
            }
        }

        composable(
            route = Routes.FLIGHT_EDIT,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("flightId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val flightId = backStackEntry.arguments?.getString("flightId") ?: return@composable
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(appContainer.tripRepository, appContainer.personalizationRepository)
            )
            FlightEditScreen(
                viewModel = viewModel,
                flightId = flightId,
                tripId = tripId,
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.HOTEL_EDIT,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(appContainer.tripRepository, appContainer.personalizationRepository)
            )
            HotelEditScreen(
                viewModel = viewModel,
                tripId = tripId,
                onSaved = { navController.popBackStack() },
            )
        }

        // --- 방문예약 플로우. 여행 상세의 매장에서 진입한다. ---

        composable(
            route = Routes.VISIT_RESERVATION,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("storeId") { type = NavType.StringType },
                navArgument("storeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val storeId = backStackEntry.arguments?.getString("storeId") ?: return@composable
            val storeName = backStackEntry.arguments?.getString("storeName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: return@composable
            val viewModel: VisitReservationViewModel = viewModel(
                factory = VisitReservationViewModel.Factory(appContainer.tripRepository, appContainer.storeRepository)
            )
            VisitReservationScreen(
                viewModel = viewModel,
                tripId = tripId,
                storeId = storeId,
                storeName = storeName,
                onReservationCreated = {
                    navController.navigate(Routes.reservationList(tripId)) {
                        popUpTo(Routes.tripDetail(tripId)) { inclusive = false }
                    }
                },
                onNavigateToReservationList = {
                    navController.navigate(Routes.reservationList(tripId)) {
                        popUpTo(Routes.tripDetail(tripId)) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = Routes.RESERVATION_LIST,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val viewModel: VisitReservationViewModel = viewModel(
                factory = VisitReservationViewModel.Factory(appContainer.tripRepository, appContainer.storeRepository)
            )
            val state by viewModel.listState.collectAsState()
            LaunchedEffect(tripId) { viewModel.loadReservations(tripId) }
            ReservationListScreen(
                reservations = state.reservations,
                isLoading = state.isLoading,
                error = state.error,
                cancellingIds = state.cancellingIds,
                onCancelReservation = { reservationId -> viewModel.cancelReservation(reservationId, tripId) },
                onRetry = { viewModel.loadReservations(tripId) },
            )
        }

        composable(
            route = Routes.SHOPPING,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            val viewModel: ShoppingViewModel = viewModel(
                factory = ShoppingViewModel.Factory(
                    appContainer.shoppingRepository,
                    appContainer.sessionRepository,
                    appContainer.cameraFrameProvider,
                    appContainer.detectionResultProvider,
                    appContainer.attentionCandidateProvider
                )
            )
            ShoppingScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                currency = currency,
                onNavigateToReview = {
                    navController.navigate(Routes.review(sessionId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val viewModel: ReviewViewModel = viewModel(
                factory = ReviewViewModel.Factory(appContainer.shoppingRepository)
            )
            ReviewScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateToTravel = {
                    navController.navigate(Routes.travel(sessionId))
                }
            )
        }

        composable(
            route = Routes.TRAVEL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val viewModel: TravelViewModel = viewModel(
                factory = TravelViewModel.Factory(appContainer.travelRepository)
            )
            TravelScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateToChecklist = {
                    navController.navigate(Routes.checklist(sessionId))
                }
            )
        }

        composable(
            route = Routes.CHECKLIST,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val viewModel: ChecklistViewModel = viewModel(
                factory = ChecklistViewModel.Factory(appContainer.checklistRepository)
            )
            ChecklistScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                initialDate = appContainer.tripDates.departureDate ?: LocalDate.now(),
                onNavigateToRecommendation = {
                    navController.navigate(Routes.recommendation(sessionId))
                }
            )
        }

        composable(
            route = Routes.RECOMMENDATION,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val viewModel: RecommendationViewModel = viewModel(
                factory = RecommendationViewModel.Factory(appContainer.recommendationRepository)
            )
            RecommendationScreen(
                viewModel = viewModel,
                sessionId = sessionId
            )
        }
    }
}

private val flightDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * [FlightInfoConfirmScreen]의 "출발 시간" 필드("2026.08.21 10:00")에서 날짜만 뽑아낸다.
 * 사용자가 자유 텍스트로 수정 가능한 필드라 형식이 안 맞을 수 있어 실패하면 null.
 */
private fun parseDepartureDate(fields: List<FlightInfoField>): LocalDate? {
    val rawValue = fields.firstOrNull { it.label == "출발 시간" }?.value ?: return null
    return try {
        LocalDate.parse(rawValue.substringBefore(" "), flightDateFormatter)
    } catch (e: DateTimeParseException) {
        null
    }
}