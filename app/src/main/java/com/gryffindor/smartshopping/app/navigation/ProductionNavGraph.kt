package com.gryffindor.smartshopping.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.feature.login.LoginScreen
import com.gryffindor.smartshopping.feature.onboarding.FlightInfoConfirmScreen
import com.gryffindor.smartshopping.feature.onboarding.FlightRegisterScreen
import com.gryffindor.smartshopping.feature.onboarding.OnboardingViewModel
import com.gryffindor.smartshopping.feature.onboarding.PermissionScreen
import com.gryffindor.smartshopping.feature.onboarding.UserInfoScreen
import com.gryffindor.smartshopping.feature.shell.MainShellScreen
import com.gryffindor.smartshopping.feature.splash.SplashScreen

/**
 * Production UI navigation graph.
 * Wraps all Production screens and delegates to existing AppNavGraph routes
 * for detailed flows (shopping live, trip detail, etc.).
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
                    // Demo: go directly to main shell
                    navController.navigate(ProductionRoutes.MAIN_SHELL) {
                        popUpTo(ProductionRoutes.LOGIN) { inclusive = true }
                    }
                },
                onGuestLogin = {
                    // Demo: go directly to main shell
                    navController.navigate(ProductionRoutes.MAIN_SHELL) {
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
                    navController.navigate(ProductionRoutes.ONBOARDING_FLIGHT_REGISTER)
                }
            )
        }

        composable(ProductionRoutes.ONBOARDING_FLIGHT_REGISTER) {
            val viewModel: OnboardingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = OnboardingViewModel.Factory(
                    appContainer.personalizationRepository,
                    appContainer.tripRepository
                )
            )
            FlightRegisterScreen(
                viewModel = viewModel,
                onFlightAnalyzed = { flightId, tripId ->
                    navController.navigate(
                        ProductionRoutes.onboardingFlightConfirm(flightId)
                    ) {
                        popUpTo(ProductionRoutes.ONBOARDING_FLIGHT_REGISTER) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(ProductionRoutes.MAIN_SHELL) {
                        popUpTo(ProductionRoutes.ONBOARDING_PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = ProductionRoutes.ONBOARDING_FLIGHT_CONFIRM,
            arguments = listOf(navArgument("flightId") { type = NavType.StringType })
        ) {
            FlightInfoConfirmScreen(
                onConfirm = {
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

        // ===== Existing functional screens (delegated from Production UI) =====
        // These are the existing routes from AppNavGraph that we re-register here
        // so the Production navController can navigate to them directly.

        composable(
            route = Routes.STORE_SELECTION,
            arguments = listOf(navArgument("currency") { type = NavType.StringType })
        ) { backStackEntry ->
            val currency = backStackEntry.arguments?.getString("currency") ?: "KRW"
            val viewModel: com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel.Factory(
                        appContainer.storeRepository,
                        appContainer.sessionRepository,
                        currency
                    )
                )
            com.gryffindor.smartshopping.feature.storeselection.StoreSelectionScreen(
                viewModel = viewModel,
                onNavigateToShopping = { sessionId, sessionCurrency ->
                    navController.navigate(Routes.shopping(sessionId, sessionCurrency)) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
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
            val viewModel: com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel.Factory(
                        appContainer.shoppingRepository,
                        appContainer.sessionRepository,
                        appContainer.cameraFrameProvider,
                        appContainer.detectionResultProvider,
                        appContainer.attentionCandidateProvider
                    )
                )
            com.gryffindor.smartshopping.feature.shopping.ShoppingScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                currency = currency,
                wishlistIds = emptySet(),
                onWishlistToggle = {},
                onNavigateToReview = {
                    navController.navigate(Routes.review(sessionId)) {
                        popUpTo(ProductionRoutes.MAIN_SHELL) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val viewModel: com.gryffindor.smartshopping.feature.review.ReviewViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.review.ReviewViewModel.Factory(
                        appContainer.shoppingRepository
                    )
                )
            com.gryffindor.smartshopping.feature.review.ReviewScreen(
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
            val viewModel: com.gryffindor.smartshopping.feature.travel.TravelViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.travel.TravelViewModel.Factory(
                        appContainer.travelRepository
                    )
                )
            com.gryffindor.smartshopping.feature.travel.TravelScreen(
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
            val viewModel: com.gryffindor.smartshopping.feature.checklist.ChecklistViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.checklist.ChecklistViewModel.Factory(
                        appContainer.checklistRepository
                    )
                )
            com.gryffindor.smartshopping.feature.checklist.ChecklistScreen(
                viewModel = viewModel,
                sessionId = sessionId,
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
            val viewModel: com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel.Factory(
                        appContainer.recommendationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.recommendation.RecommendationScreen(
                viewModel = viewModel,
                sessionId = sessionId
            )
        }

        // ===== Trip flow =====

        composable(Routes.TRIP_LIST) {
            val viewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripListScreen(
                viewModel = viewModel,
                onNavigateToCreate = { navController.navigate(Routes.TRIP_CREATE) },
                onNavigateToDetail = { tripId -> navController.navigate(Routes.tripDetail(tripId)) }
            )
        }

        composable(Routes.TRIP_CREATE) {
            val viewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripCreateScreen(
                viewModel = viewModel,
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
            val viewModel: com.gryffindor.smartshopping.feature.trip.TripViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.trip.TripViewModel.Factory(
                        appContainer.tripRepository,
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.trip.TripDetailScreen(
                viewModel = viewModel,
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
            route = Routes.MY_PAGE
        ) {
            val viewModel: com.gryffindor.smartshopping.feature.mypage.MyPageViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.gryffindor.smartshopping.feature.mypage.MyPageViewModel.Factory(
                        appContainer.personalizationRepository
                    )
                )
            com.gryffindor.smartshopping.feature.mypage.MyPageScreen(
                viewModel = viewModel,
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
