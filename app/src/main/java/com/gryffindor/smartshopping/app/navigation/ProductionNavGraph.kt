package com.gryffindor.smartshopping.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.feature.login.LoginScreen
import com.gryffindor.smartshopping.feature.onboarding.PermissionScreen
import com.gryffindor.smartshopping.feature.onboarding.SmartGlassesRegistrationScreen
import com.gryffindor.smartshopping.feature.onboarding.TermsScreen
import com.gryffindor.smartshopping.feature.onboarding.UserInfoScreen
import com.gryffindor.smartshopping.feature.shell.MainShellScreen
import com.gryffindor.smartshopping.feature.splash.SplashScreen

/**
 * Production UI navigation graph.
 *
 * Target IA:
 *   Splash -> Login -> Onboarding (Permission -> UserInfo -> Terms -> DeviceRegistration) -> MainShell
 *
 * MainShell contains bottom navigation (Home / Shop / MyPage).
 * Full-screen flows (shopping, trip, etc.) are registered at this root level
 * so they overlay the shell without losing bottom nav state.
 */
@Composable
fun ProductionNavGraph(
    navController: NavHostController,
    appContainer: AppContainer
) {
    NavHost(
        navController = navController,
        startDestination = ProductionRoutes.SPLASH
    ) {
        // ===== Pre-auth flow =====

        composable(ProductionRoutes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(ProductionRoutes.LOGIN) {
                        popUpTo(ProductionRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(ProductionRoutes.LOGIN) {
            LoginScreen(
                onKakaoLogin = {
                    navController.navigate(ProductionRoutes.ONBOARDING_PERMISSION) {
                        popUpTo(ProductionRoutes.LOGIN) { inclusive = true }
                    }
                },
                onGuestLogin = {
                    navController.navigate(ProductionRoutes.ONBOARDING_PERMISSION) {
                        popUpTo(ProductionRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ===== Onboarding =====

        composable(ProductionRoutes.ONBOARDING_PERMISSION) {
            PermissionScreen(
                onNext = {
                    navController.navigate(ProductionRoutes.ONBOARDING_USER_INFO)
                }
            )
        }

        composable(ProductionRoutes.ONBOARDING_USER_INFO) {
            UserInfoScreen(
                onNext = {
                    navController.navigate(ProductionRoutes.ONBOARDING_TERMS)
                }
            )
        }

        composable(ProductionRoutes.ONBOARDING_TERMS) {
            TermsScreen(
                onNext = {
                    navController.navigate(ProductionRoutes.MAIN_SHELL) {
                        popUpTo(ProductionRoutes.ONBOARDING_PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(ProductionRoutes.ONBOARDING_DEVICE_REGISTRATION) {
            SmartGlassesRegistrationScreen(
                metaCameraSource = appContainer.metaCameraSource,
                onComplete = {
                    navController.navigate(ProductionRoutes.MAIN_SHELL) {
                        popUpTo(ProductionRoutes.ONBOARDING_PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        // ===== Main Shell (Bottom Navigation) =====

        composable(ProductionRoutes.MAIN_SHELL) {
            MainShellScreen(
                navController = navController,
                appContainer = appContainer
            )
        }

        // ===== Trip Flight Registration (from Home) =====
        // Uses TripRegistrationViewModel: analyze only (no trip creation until user confirms)

        composable(ProductionRoutes.TRIP_FLIGHT_REGISTER) {
            val tripRegViewModel: com.gryffindor.smartshopping.feature.trip.TripRegistrationViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripRegistrationViewModel.Factory(
                        appContainer.personalizationRepository,
                        appContainer.tripRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripFlightRegisterScreen(
                viewModel = tripRegViewModel,
                onFlightAnalyzed = { flightId ->
                    navController.navigate(ProductionRoutes.tripFlightConfirm(flightId))
                },
                onSkip = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = ProductionRoutes.TRIP_FLIGHT_CONFIRM,
            arguments = listOf(navArgument("flightId") { type = NavType.StringType })
        ) { backStackEntry ->
            val flightId = backStackEntry.arguments?.getString("flightId") ?: return@composable
            val tripRegViewModel: com.gryffindor.smartshopping.feature.trip.TripRegistrationViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripRegistrationViewModel.Factory(
                        appContainer.personalizationRepository,
                        appContainer.tripRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripFlightConfirmScreen(
                viewModel = tripRegViewModel,
                onTripCreated = { tripId ->
                    navController.navigate(Routes.tripDetail(tripId)) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== Shopping Flow =====

        composable(ProductionRoutes.SHOP_STORE_SELECTION) {
            val storeViewModel: com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel.Factory(
                        appContainer.storeRepository,
                        appContainer.sessionRepository,
                        "KRW"
                    )
                )
            com.gryffindor.smartshopping.feature.storeselection.StoreSelectionScreen(
                viewModel = storeViewModel,
                onNavigateToShopping = { sessionId, currency ->
                    // After store confirmed + session created -> device connection -> ready
                    navController.navigate(ProductionRoutes.shopReady(sessionId, currency)) {
                        popUpTo(ProductionRoutes.SHOP_STORE_SELECTION) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ProductionRoutes.SHOP_DEVICE_CONNECTION) {
            com.gryffindor.smartshopping.feature.shopping.ShoppingDeviceConnectionScreen(
                metaCameraSource = appContainer.metaCameraSource,
                onDeviceReady = {
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ProductionRoutes.SHOP_READY,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            com.gryffindor.smartshopping.feature.shopping.ShoppingReadyScreen(
                sessionId = sessionId,
                currency = currency,
                onStart = {
                    navController.navigate(ProductionRoutes.shopLive(sessionId, currency)) {
                        popUpTo(ProductionRoutes.SHOP_READY) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ProductionRoutes.SHOP_LIVE,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            val shoppingViewModel: com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel.Factory(
                        appContainer.shoppingRepository,
                        appContainer.sessionRepository,
                        appContainer.cameraFrameProvider,
                        appContainer.detectionResultProvider,
                        appContainer.attentionCandidateProvider
                    )
                )
            val wishlistViewModel: com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel.Factory(
                        appContainer.personalizationRepository
                    )
                )
            val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()

            LaunchedEffect(Unit) { wishlistViewModel.loadWishlist() }

            com.gryffindor.smartshopping.feature.shopping.ShoppingScreen(
                viewModel = shoppingViewModel,
                sessionId = sessionId,
                currency = currency,
                wishlistIds = wishlistIds,
                onWishlistToggle = { productId -> wishlistViewModel.toggleWishlist(productId) },
                onNavigateToReview = {
                    // Navigate to Shopping List instead of old Review chain
                    navController.navigate(ProductionRoutes.shopList(sessionId, currency)) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = ProductionRoutes.SHOP_LIST,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            val shoppingViewModel: com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel.Factory(
                        appContainer.shoppingRepository,
                        appContainer.sessionRepository,
                        appContainer.cameraFrameProvider,
                        appContainer.detectionResultProvider,
                        appContainer.attentionCandidateProvider
                    )
                )
            val wishlistViewModel: com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel.Factory(
                        appContainer.personalizationRepository
                    )
                )
            val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()

            LaunchedEffect(Unit) { wishlistViewModel.loadWishlist() }

            com.gryffindor.smartshopping.feature.shopping.ShoppingListScreen(
                viewModel = shoppingViewModel,
                sessionId = sessionId,
                currency = currency,
                wishlistIds = wishlistIds,
                onWishlistToggle = { productId -> wishlistViewModel.toggleWishlist(productId) },
                onNavigateHome = {
                    navController.navigate(ProductionRoutes.MAIN_SHELL) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = true }
                    }
                }
            )
        }

        // ===== MyPage sub-screens =====

        composable(ProductionRoutes.MY_PAGE_WISHLIST) {
            val wishlistViewModel: com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel.Factory(
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.mypage.MyPageWishlistScreen(
                viewModel = wishlistViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ProductionRoutes.MY_PAGE_RECENT) {
            com.gryffindor.smartshopping.feature.mypage.RecentViewedScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== Existing deep functional routes (preserved) =====

        // Store Selection (legacy route kept for compatibility)
        composable(
            route = Routes.STORE_SELECTION,
            arguments = listOf(navArgument("currency") { type = NavType.StringType })
        ) { backStackEntry ->
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            val storeViewModel: com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel.Factory(
                        appContainer.storeRepository,
                        appContainer.sessionRepository,
                        currency
                    )
                )
            com.gryffindor.smartshopping.feature.storeselection.StoreSelectionScreen(
                viewModel = storeViewModel,
                onNavigateToShopping = { sessionId, sessionCurrency ->
                    navController.navigate(ProductionRoutes.shopReady(sessionId, sessionCurrency)) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Shopping (legacy route for backward compat)
        composable(
            route = Routes.SHOPPING,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("currency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            val shoppingViewModel: com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel.Factory(
                        appContainer.shoppingRepository,
                        appContainer.sessionRepository,
                        appContainer.cameraFrameProvider,
                        appContainer.detectionResultProvider,
                        appContainer.attentionCandidateProvider
                    )
                )
            val wishlistViewModel: com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel.Factory(
                        appContainer.personalizationRepository
                    )
                )
            val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()
            LaunchedEffect(Unit) { wishlistViewModel.loadWishlist() }

            com.gryffindor.smartshopping.feature.shopping.ShoppingScreen(
                viewModel = shoppingViewModel,
                sessionId = sessionId,
                currency = currency,
                wishlistIds = wishlistIds,
                onWishlistToggle = { productId -> wishlistViewModel.toggleWishlist(productId) },
                onNavigateToReview = {
                    navController.navigate(ProductionRoutes.shopList(sessionId, currency)) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = false }
                    }
                }
            )
        }

        // Review (preserved for legacy access, not primary flow)
        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val reviewViewModel: com.gryffindor.smartshopping.feature.review.ReviewViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.review.ReviewViewModel.Factory(
                        appContainer.shoppingRepository
                    )
                )
            com.gryffindor.smartshopping.feature.review.ReviewScreen(
                viewModel = reviewViewModel,
                sessionId = sessionId,
                onNavigateToTravel = {
                    navController.navigate(Routes.travel(sessionId))
                }
            )
        }

        // Travel (preserved, not primary flow)
        composable(
            route = Routes.TRAVEL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val travelViewModel: com.gryffindor.smartshopping.feature.travel.TravelViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.travel.TravelViewModel.Factory(
                        appContainer.travelRepository
                    )
                )
            com.gryffindor.smartshopping.feature.travel.TravelScreen(
                viewModel = travelViewModel,
                sessionId = sessionId,
                onNavigateToChecklist = {
                    navController.navigate(Routes.checklist(sessionId))
                }
            )
        }

        // Checklist (preserved)
        composable(
            route = Routes.CHECKLIST,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val checklistViewModel: com.gryffindor.smartshopping.feature.checklist.ChecklistViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.checklist.ChecklistViewModel.Factory(
                        appContainer.checklistRepository
                    )
                )
            com.gryffindor.smartshopping.feature.checklist.ChecklistScreen(
                viewModel = checklistViewModel,
                sessionId = sessionId,
                onNavigateToRecommendation = {
                    navController.navigate(Routes.recommendation(sessionId))
                }
            )
        }

        // Recommendation (preserved)
        composable(
            route = Routes.RECOMMENDATION,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val recViewModel: com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel.Factory(
                        appContainer.recommendationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.recommendation.RecommendationScreen(
                viewModel = recViewModel,
                sessionId = sessionId
            )
        }

        // ===== Trip flow =====

        composable(Routes.TRIP_LIST) {
            val tripViewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripListScreen(
                viewModel = tripViewModel,
                onNavigateToCreate = { navController.navigate(Routes.TRIP_CREATE) },
                onNavigateToDetail = { tripId -> navController.navigate(Routes.tripDetail(tripId)) }
            )
        }

        composable(Routes.TRIP_CREATE) {
            val tripViewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripCreateScreen(
                viewModel = tripViewModel,
                onTripCreated = { tripId ->
                    navController.navigate(Routes.tripDetail(tripId)) {
                        popUpTo(Routes.TRIP_LIST) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val tripViewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripDetailScreen(
                viewModel = tripViewModel,
                tripId = tripId,
                onNavigateToFlightEdit = { flightId ->
                    navController.navigate(Routes.flightEdit(tripId, flightId))
                },
                onNavigateToHotelEdit = {
                    navController.navigate(Routes.hotelEdit(tripId))
                },
                onNavigateToVisitReservation = { storeId, storeName ->
                    navController.navigate(Routes.visitReservation(tripId, storeId, storeName))
                }
            )
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
            val tripViewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.FlightEditScreen(
                viewModel = tripViewModel,
                flightId = flightId,
                tripId = tripId,
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.HOTEL_EDIT,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val tripViewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.HotelEditScreen(
                viewModel = tripViewModel,
                tripId = tripId,
                onSaved = { navController.popBackStack() }
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
            val storeName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("storeName") ?: "", "UTF-8"
            )
            val reservationViewModel: com.gryffindor.smartshopping.feature.reservation.VisitReservationViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.reservation.VisitReservationViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.storeRepository
                    )
                )
            com.gryffindor.smartshopping.feature.reservation.VisitReservationScreen(
                viewModel = reservationViewModel,
                tripId = tripId,
                storeId = storeId,
                storeName = storeName,
                onReservationCreated = {
                    navController.popBackStack()
                },
                onNavigateToReservationList = {
                    navController.navigate(Routes.reservationList(tripId)) {
                        popUpTo(Routes.VISIT_RESERVATION) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RESERVATION_LIST,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val reservationViewModel: com.gryffindor.smartshopping.feature.reservation.VisitReservationViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.reservation.VisitReservationViewModel.Factory(
                        appContainer.tripRepository
                    )
                )

            LaunchedEffect(tripId) {
                reservationViewModel.loadReservations(tripId)
            }

            val listState by reservationViewModel.listState.collectAsState()

            com.gryffindor.smartshopping.feature.reservation.ReservationListScreen(
                reservations = listState.reservations,
                isLoading = listState.isLoading,
                error = listState.error,
                cancellingIds = listState.cancellingIds,
                onCancelReservation = { reservationId ->
                    reservationViewModel.cancelReservation(reservationId, tripId)
                },
                onRetry = { reservationViewModel.loadReservations(tripId) }
            )
        }

        // MyPage (legacy route)
        composable(route = Routes.MY_PAGE) {
            val myPageViewModel: com.gryffindor.smartshopping.feature.mypage.MyPageViewModel =
                viewModel(
                    factory = com.gryffindor.smartshopping.feature.mypage.MyPageViewModel.Factory(
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.mypage.MyPageScreen(
                viewModel = myPageViewModel,
                onNavigateToTripDetail = { tripId ->
                    navController.navigate(Routes.tripDetail(tripId))
                },
                onNavigateToReservationList = { tripId ->
                    navController.navigate(Routes.reservationList(tripId))
                }
            )
        }
    }
}
