package com.gryffindor.smartshopping.app.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
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
import com.gryffindor.smartshopping.feature.review.ReviewUiState
import com.gryffindor.smartshopping.feature.review.ReviewViewModel
import com.gryffindor.smartshopping.feature.shopping.DisplayCurrency
import com.gryffindor.smartshopping.feature.shopping.LiveReceiptItem
import com.gryffindor.smartshopping.feature.shopping.LiveShoppingScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingReceiptViewModel
import com.gryffindor.smartshopping.feature.shopping.ShoppingResultReceiptScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingReviewScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingStoreSelectionScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingUiState
import com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel
import com.gryffindor.smartshopping.feature.shopping.toLooketStore
import com.gryffindor.smartshopping.feature.splash.SplashScreen
import com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel
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

        composable(Routes.SHOP_TAB) {
            // TODO: 화면에 아직 통화 선택 UI가 없어서 임시로 KRW 고정(HOME의 쇼핑 이동과 동일한
            // 이유).
            val viewModel: StoreSelectionViewModel = viewModel(
                factory = StoreSelectionViewModel.Factory(
                    appContainer.storeRepository,
                    appContainer.sessionRepository,
                    "KRW",
                )
            )
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            // 매장 선택 확인 -> 실제 세션 생성(confirmSelection) -> 세션이 만들어지면 그
            // sessionId로 진짜 쇼핑 화면(Routes.SHOPPING)으로 이동. 목업 실시간 쇼핑
            // 플로우(SHOPPING_SESSION)는 삭제 — 이제 여기서 바로 실제 세션으로 들어간다.
            LaunchedEffect(uiState.sessionCreated) {
                uiState.sessionCreated?.let { event ->
                    navController.navigate(Routes.shopping(event.sessionId, event.currency))
                    viewModel.consumeSessionCreatedEvent()
                }
            }

            // 매장 목록 로딩 실패나 세션 생성 실패는 화면에 아무 표시가 없어서 "버튼을 눌러도
            // 반응이 없다"처럼 보였다 — 최소한 원인을 알 수 있게 Toast로라도 보여준다.
            LaunchedEffect(uiState.errorMessage) {
                uiState.errorMessage?.let { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }

            ShoppingStoreSelectionScreen(
                stores = uiState.stores.map { it.toLooketStore() },
                selectedStoreId = uiState.selectedStoreId,
                onStoreSelected = { viewModel.selectStore(it) },
                onConfirmClick = { viewModel.confirmSelection() },
                onBackClick = { navController.popBackStack() },
                selectedTab = BottomNavTab.SHOP,
                onTabSelected = onBottomTabSelected,
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
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(sessionId) { viewModel.loadProducts(sessionId, currency) }

            // 목업 LiveShoppingScreen UI를 실제 ShoppingViewModel 상태로 구동한다.
            // - 제거(onRemoveItem)는 무력화: 백엔드에 세션에서 개별 상품을 빼는 API가 없다.
            // - 재생/일시정지는 백엔드 의미가 없는 순수 로컬 UI 상태다 — 매장 선택 확인
            //   시점에 세션이 이미 시작되어 카메라도 자동으로 켜져 있다.
            var isSessionActive by remember { mutableStateOf(false) }

            when (uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LooketColors.BrandPrimary)
                    }
                }
                is UiState.Error -> {
                    val message = (uiState as UiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = message,
                                style = LooketTextStyles.bodyTwo,
                                color = LooketColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LooketPrimaryButton(
                                text = stringResource(R.string.common_retry),
                                onClick = { viewModel.retry() },
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val data = (uiState as UiState.Success<ShoppingUiState>).data
                    LaunchedEffect(data.isSessionActive) {
                        if (!data.isSessionActive) {
                            navController.navigate(Routes.shoppingReceipt(sessionId)) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        }
                    }
                    LiveShoppingScreen(
                        isSessionActive = isSessionActive,
                        onPlayClick = { isSessionActive = true },
                        onPauseClick = { isSessionActive = false },
                        onFinishClick = { viewModel.endShopping(sessionId) },
                        onBackClick = { navController.popBackStack() },
                        totalPurchaseAmount = formatKrw(data.summary.totalRetailKrw),
                        refundAmount = formatKrw(data.summary.totalEstimatedRefundKrw),
                        items = data.products.map { sessionProduct ->
                            LiveReceiptItem(
                                id = sessionProduct.product.productId,
                                name = sessionProduct.product.name,
                                storeName = sessionProduct.product.brand,
                                price = formatKrw(sessionProduct.pricing.retailPriceKrw),
                                refundAmount = formatKrw(sessionProduct.pricing.estimatedRefundKrw),
                            )
                        },
                        onRemoveItem = {},
                        isExchangeRateOn = data.displayCurrency == DisplayCurrency.CONVERTED,
                        onExchangeRateToggle = { viewModel.toggleCurrency() },
                    )
                }
            }
        }

        composable(
            route = Routes.SHOPPING_RECEIPT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val context = LocalContext.current
            val viewModel: ShoppingReceiptViewModel = viewModel(
                factory = ShoppingReceiptViewModel.Factory(appContainer.personalizationRepository)
            )
            // 세션은 특정 여행(Trip)에 묶여 있지 않아 tripId 없이 analyzeReceipt를 호출한다
            // (PersonalizationRepository.analyzeReceipt의 tripId는 선택값). OCR 완료를
            // 기다리지 않고 바로 리뷰로 넘어간다 — ReviewViewModel.submitReview()와 동일하게
            // 이 앱 전반에서 쓰는 "제출은 비동기로 흘려보내고 화면은 바로 다음으로" 패턴.
            val receiptPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                uri?.let {
                    val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                    if (bytes != null) {
                        viewModel.analyzeReceipt(bytes)
                    }
                    navController.navigate(Routes.review(sessionId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            }
            ShoppingResultReceiptScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterClick = {
                    receiptPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                selectedTab = BottomNavTab.SHOP,
                onTabSelected = onBottomTabSelected,
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
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(sessionId) { viewModel.loadProducts(sessionId) }

            when (uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LooketColors.BrandPrimary)
                    }
                }
                is UiState.Error -> {
                    val message = (uiState as UiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = message,
                                style = LooketTextStyles.bodyTwo,
                                color = LooketColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LooketPrimaryButton(
                                text = stringResource(R.string.common_retry),
                                onClick = { viewModel.retry() },
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val data = (uiState as UiState.Success<ReviewUiState>).data
                    ShoppingReviewScreen(
                        products = data.products,
                        purchasedIds = data.purchasedIds,
                        interestedIds = data.interestedIds,
                        onTogglePurchased = { viewModel.togglePurchased(it) },
                        onToggleInterested = { viewModel.toggleInterested(it) },
                        onConfirmClick = {
                            viewModel.submitReview(sessionId)
                            navController.navigate(Routes.travel(sessionId))
                        },
                    )
                }
            }
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

private fun formatKrw(amount: Long): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
    return "₩ ${formatter.format(amount)}"
}

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