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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
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
import com.gryffindor.smartshopping.core.ui.component.LooketIconButton
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
import com.gryffindor.smartshopping.feature.onboarding.OnboardingUiState
import com.gryffindor.smartshopping.feature.onboarding.OnboardingViewModel
import com.gryffindor.smartshopping.feature.onboarding.PurchaseConfirmItem
import com.gryffindor.smartshopping.feature.onboarding.UserInfoScreen
import com.gryffindor.smartshopping.feature.recommendation.RecommendationScreen
import com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel
import com.gryffindor.smartshopping.feature.reservation.ReservationListScreen
import com.gryffindor.smartshopping.feature.reservation.VisitReservationFormScreen
import com.gryffindor.smartshopping.feature.reservation.VisitReservationStoreSelectionScreen
import com.gryffindor.smartshopping.feature.reservation.VisitReservationStoreSelectionViewModel
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
    // 하단 네비게이션 바(HOME/SHOP/MY PAGE) 공용 탭 전환 핸들러. 탭을 누르면 그 탭의 시작
    // 목적지로 무조건 이동한다 — saveState/restoreState를 쓰지 않는 이유: 예를 들어 MY PAGE ->
    // TRAVEL(최상위 Trip 플로우로 나감) -> 다시 MY PAGE 탭을 누르는 상황에서, 저장된 상태를
    // 복원하려다 마이페이지 메인이 아닌 다른 화면으로 가는 문제가 있었다. 매번 깨끗하게
    // 시작 화면으로 리셋하는 대신 스크롤 위치 등은 유지되지 않는다.
    val onBottomTabSelected: (BottomNavTab) -> Unit = { tab ->
        val route = when (tab) {
            BottomNavTab.HOME -> Routes.HOME
            BottomNavTab.SHOP -> Routes.SHOP_TAB
            BottomNavTab.MY_PAGE -> Routes.MY_PAGE
        }
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
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
                    // 이 시점엔 블루투스/위치 권한이 실제로 허용된 상태 — 이제 Wearables SDK
                    // 초기화/등록을 진행한다(로그인 전에 시스템 권한 다이얼로그가 뜨는 걸
                    // 막으려고 MainActivity 시작 시점이 아니라 여기서 트리거).
                    appContainer.onRequestWearablesSetup?.invoke()
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

        // 5. 항공편 사진 등록 화면 — PersonalizationRepository.analyzeFlight로 실제 OCR 후
        // TripRepository.createTrip으로 여행을 자동 생성한다(OnboardingViewModel.createTripFromFlight).
        composable(Routes.ONBOARDING_FLIGHT_REGISTER) {
            val context = LocalContext.current
            val viewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(appContainer.personalizationRepository, appContainer.tripRepository)
            )
            val onboardingState by viewModel.uiState.collectAsState()
            var hasPhoto by remember { mutableStateOf(false) }

            val flightPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                uri?.let {
                    val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                    if (bytes != null) {
                        hasPhoto = true
                        viewModel.createTripFromFlight(bytes)
                    }
                }
            }

            // 분석이 끝나면(성공) 확인 화면으로 자동 이동. 실패하면 Toast로 원인을 보여준다 —
            // hasPhoto는 true로 유지(사진 자체는 골랐으니 "다시찍기"가 맞다).
            LaunchedEffect(onboardingState) {
                when (val state = onboardingState) {
                    is OnboardingUiState.FlightAnalyzed -> {
                        navController.navigate(Routes.ONBOARDING_FLIGHT_CONFIRM)
                    }
                    is OnboardingUiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }

            FlightRegisterScreen(
                hasPhoto = hasPhoto,
                onBackClick = { navController.popBackStack() },
                onCaptureClick = {
                    flightPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSkipClick = {
                    // 항공편 확인 단계를 건너뛰고 바로 영수증 등록으로 이동
                    navController.navigate(Routes.ONBOARDING_RECEIPT_REGISTER)
                }
            )
        }

        // 6. 항공편 정보 확인 화면 (온보딩 마지막) — 직전 화면(5번)과 같은 OnboardingViewModel
        // 인스턴스를 그 백스택 엔트리에서 가져와 실제 분석된 항공편 값을 보여준다.
        composable(Routes.ONBOARDING_FLIGHT_CONFIRM) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.ONBOARDING_FLIGHT_REGISTER)
            }
            val viewModel: OnboardingViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = OnboardingViewModel.Factory(appContainer.personalizationRepository, appContainer.tripRepository)
            )
            val flight = viewModel.getAnalyzedFlight()

            val initialFields = listOf(
                FlightInfoField("1", "출발지", flight?.departureAirport ?: ""),
                FlightInfoField("2", "도착지", flight?.arrivalAirport ?: ""),
                FlightInfoField("3", "터미널", flight?.terminal ?: ""),
                FlightInfoField("4", "출발 시간", flight?.departureAt ?: ""),
                FlightInfoField("5", "도착 시간", flight?.arrivalAt ?: ""),
                FlightInfoField("6", "공항 도착 예정시간", flight?.airportArrivalAt ?: ""),
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
                    appContainer.cameraFrameProvider,
                    appContainer.personalizationRepository,
                    appContainer.tripRepository,
                )
            )
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            HomeScreen(
                viewModel = viewModel,
                onNavigateToShopping = { sessionId ->
                    // TODO: 화면에 아직 통화 선택 UI가 없어서 임시로 USD 고정. 백엔드 통화
                    // 선택 로직(SupportedCountry 등) 연결 시 실제 선택값으로 교체 필요.
                    navController.navigate(Routes.shopping(sessionId, "USD"))
                },
                onNavigateToChecklist = {
                    // sessionId는 쇼핑 화면 이동 즉시 null로 리셋되므로, 홈에 머무는 동안엔
                    // 가장 최근에 시작한 세션(lastSessionId)으로 체크리스트를 연다.
                    uiState.lastSessionId?.let { sessionId ->
                        navController.navigate(Routes.checklist(sessionId))
                    }
                },
                onNavigateToVisitReservation = { storeId, storeName ->
                    // FOR YOU 추천 상품을 눌렀을 때 진입 — 매장 선택을 건너뛰고 바로 예약 폼으로.
                    val tripId = uiState.currentTripId
                    if (tripId != null) {
                        navController.navigate(Routes.visitReservation(tripId, storeId, storeName))
                    } else {
                        Toast.makeText(context, "먼저 여행을 등록해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                selectedTab = BottomNavTab.HOME,
                onTabSelected = onBottomTabSelected,
            )
        }

        // --- 하단 네비게이션 SHOP/MY PAGE 탭 ---

        composable(Routes.SHOP_TAB) {
            // TODO: 화면에 아직 통화 선택 UI가 없어서 임시로 USD 고정(HOME의 쇼핑 이동과 동일한
            // 이유). Backend는 USD/CNY만 허용한다.
            val viewModel: StoreSelectionViewModel = viewModel(
                factory = StoreSelectionViewModel.Factory(
                    appContainer.storeRepository,
                    appContainer.sessionRepository,
                    "USD",
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
                personalizationRepository = appContainer.personalizationRepository,
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
                topBar = { TripBackBar(onBackClick = { navController.popBackStack() }) },
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
                topBar = { TripBackBar(onBackClick = { navController.popBackStack() }) },
                bottomBar = { BottomNavBar(selectedTab = BottomNavTab.MY_PAGE, onTabSelected = onBottomTabSelected) },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TripDetailScreen(
                        viewModel = viewModel,
                        tripId = tripId,
                        onNavigateToFlightEdit = { flightId -> navController.navigate(Routes.flightEdit(tripId, flightId)) },
                        onNavigateToHotelEdit = { navController.navigate(Routes.hotelEdit(tripId)) },
                        onNavigateToVisitReservation = {
                            navController.navigate(Routes.visitReservationStoreSelect(tripId))
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

        // --- 방문예약 플로우. 홈 FOR YOU 추천 상품을 눌러서 진입한다. ---

        composable(
            route = Routes.VISIT_RESERVATION_STORE_SELECT,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val context = LocalContext.current
            val viewModel: VisitReservationStoreSelectionViewModel = viewModel(
                factory = VisitReservationStoreSelectionViewModel.Factory(appContainer.storeRepository)
            )
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.loadStores() }
            LaunchedEffect(uiState.error) {
                uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            }

            VisitReservationStoreSelectionScreen(
                stores = uiState.stores.map { it.toLooketStore() },
                selectedStoreId = uiState.selectedStoreId,
                onStoreSelected = { viewModel.selectStore(it) },
                onConfirmClick = {
                    uiState.selectedStoreId?.let { storeId ->
                        val storeName = uiState.stores.find { it.id == storeId }?.name ?: ""
                        navController.navigate(Routes.visitReservation(tripId, storeId, storeName))
                    }
                },
                onBackClick = { navController.popBackStack() },
                selectedTab = BottomNavTab.HOME,
                onTabSelected = onBottomTabSelected,
            )
        }

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
            // storeName은 지금 폼 화면엔 안 보여주지만(Figma에 매장명 표시가 없음), 라우트
            // 계약을 유지해 두면 나중에 필요할 때 바로 쓸 수 있어 인코딩/디코딩은 그대로 둔다.
            backStackEntry.arguments?.getString("storeName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

            val context = LocalContext.current
            val viewModel: VisitReservationViewModel = viewModel(
                factory = VisitReservationViewModel.Factory(appContainer.tripRepository, appContainer.storeRepository)
            )
            val createState by viewModel.createState.collectAsState()

            var selectedDate by remember { mutableStateOf(LocalDate.now()) }
            var selectedTimeRange by remember { mutableStateOf<String?>(null) }
            var selectedPurpose by remember { mutableStateOf<String?>(null) }
            var visitorName by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            LaunchedEffect(createState.createdReservation) {
                if (createState.createdReservation != null) {
                    navController.navigate(Routes.reservationList(tripId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                    viewModel.resetCreateState()
                }
            }
            LaunchedEffect(createState.error) {
                createState.error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }

            VisitReservationFormScreen(
                date = selectedDate,
                onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
                onNextDay = { selectedDate = selectedDate.plusDays(1) },
                selectedTimeRange = selectedTimeRange,
                onTimeRangeSelected = { selectedTimeRange = it },
                selectedPurpose = selectedPurpose,
                onPurposeSelected = { selectedPurpose = it },
                visitorName = visitorName,
                onVisitorNameChange = { visitorName = it },
                note = note,
                onNoteChange = { note = it },
                onBackClick = { navController.popBackStack() },
                onCompleteClick = {
                    val timeRange = selectedTimeRange
                    if (timeRange == null) {
                        Toast.makeText(context, context.getString(R.string.reservation_time_required), Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateScheduledDate(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        viewModel.updateScheduledTime(timeRange.substringBefore("-"))
                        viewModel.createReservation(tripId, storeId)
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
            val currency = backStackEntry.arguments?.getString("currency") ?: "USD"
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
            var isSessionActive by remember { mutableStateOf(true) }

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
                                imageUrl = sessionProduct.product.imageUrl,
                                priceUsd = sessionProduct.pricing.convertedRetailPrice,
                                refundAmountUsd = sessionProduct.pricing.convertedEstimatedRefund,
                            )
                        },
                        onRemoveItem = {},
                        isExchangeRateOn = data.displayCurrency == DisplayCurrency.CONVERTED,
                        onExchangeRateToggle = { viewModel.toggleCurrency() },
                        totalPurchaseAmountUsd = data.summary.totalConvertedRetail,
                        refundAmountUsd = data.summary.totalConvertedRefund,
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
                onSkipClick = {
                    navController.navigate(Routes.review(sessionId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
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

                    LaunchedEffect(Unit) {
                        viewModel.submitSuccess.collect {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    ShoppingReviewScreen(
                        products = data.products,
                        purchasedIds = data.purchasedIds,
                        interestedIds = data.interestedIds,
                        onTogglePurchased = { viewModel.togglePurchased(it) },
                        onToggleInterested = { viewModel.toggleInterested(it) },
                        onConfirmClick = {
                            viewModel.submitReview(sessionId)
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

/** 백엔드가 만든 Trip 화면들(TripListScreen/TripDetailScreen)은 자체 뒤로가기 버튼이 없어서
 * 여기서 최소한의 뒤로가기 바를 얹어준다. */
@Composable
private fun TripBackBar(onBackClick: () -> Unit) {
    Box(modifier = Modifier.padding(top = 68.dp, start = 16.dp, bottom = 12.dp)) {
        LooketIconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = LooketColors.TextPrimary,
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