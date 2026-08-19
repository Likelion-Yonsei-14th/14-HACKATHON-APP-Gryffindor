package com.gryffindor.smartshopping.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.feature.checklist.ChecklistScreen
import com.gryffindor.smartshopping.feature.checklist.ChecklistViewModel
import com.gryffindor.smartshopping.feature.feed.FeedViewModel
import com.gryffindor.smartshopping.feature.home.HomeScreen
import com.gryffindor.smartshopping.feature.home.HomeViewModel
import com.gryffindor.smartshopping.feature.recommendation.RecommendationScreen
import com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel
import com.gryffindor.smartshopping.feature.review.ReviewScreen
import com.gryffindor.smartshopping.feature.review.ReviewViewModel
import com.gryffindor.smartshopping.feature.shopping.ShoppingScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel
import com.gryffindor.smartshopping.feature.storeselection.StoreSelectionScreen
import com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel
import com.gryffindor.smartshopping.feature.travel.TravelScreen
import com.gryffindor.smartshopping.feature.travel.TravelViewModel
import com.gryffindor.smartshopping.feature.trip.FlightEditScreen
import com.gryffindor.smartshopping.feature.trip.HotelEditScreen
import com.gryffindor.smartshopping.feature.trip.TripCreateScreen
import com.gryffindor.smartshopping.feature.trip.TripDetailScreen
import com.gryffindor.smartshopping.feature.trip.TripListScreen
import com.gryffindor.smartshopping.feature.trip.TripViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    appContainer.cameraFrameProvider
                )
            )
            val feedViewModel: FeedViewModel = viewModel(
                factory = FeedViewModel.Factory(
                    appContainer.tripRepository,
                    appContainer.locationProvider
                )
            )
            HomeScreen(
                viewModel = viewModel,
                feedViewModel = feedViewModel,
                onNavigateToStoreSelection = { currency ->
                    navController.navigate(Routes.storeSelection(currency))
                },
                onNavigateToTripList = {
                    navController.navigate(Routes.TRIP_LIST)
                },
                onNavigateToStore = { _ ->
                    // Store detail / reservation navigation — to be connected in future work
                }
            )
        }

        composable(
            route = Routes.STORE_SELECTION,
            arguments = listOf(
                navArgument("currency") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val currency = backStackEntry.arguments?.getString("currency") ?: "USD"
            val viewModel: StoreSelectionViewModel = viewModel(
                factory = StoreSelectionViewModel.Factory(
                    appContainer.storeRepository,
                    appContainer.sessionRepository,
                    currency
                )
            )
            StoreSelectionScreen(
                viewModel = viewModel,
                onNavigateToShopping = { sessionId, sessionCurrency ->
                    navController.navigate(Routes.shopping(sessionId, sessionCurrency)) {
                        popUpTo(Routes.HOME) { inclusive = false }
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

        // ===== Trip flow =====

        composable(Routes.TRIP_LIST) {
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(
                    appContainer.tripRepository,
                    appContainer.personalizationRepository
                )
            )
            TripListScreen(
                viewModel = viewModel,
                onNavigateToCreate = {
                    navController.navigate(Routes.TRIP_CREATE)
                },
                onNavigateToDetail = { tripId ->
                    navController.navigate(Routes.tripDetail(tripId))
                }
            )
        }

        composable(Routes.TRIP_CREATE) {
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(
                    appContainer.tripRepository,
                    appContainer.personalizationRepository
                )
            )
            TripCreateScreen(
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
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(
                    appContainer.tripRepository,
                    appContainer.personalizationRepository
                )
            )
            TripDetailScreen(
                viewModel = viewModel,
                tripId = tripId,
                onNavigateToFlightEdit = { flightId ->
                    navController.navigate(Routes.flightEdit(tripId, flightId))
                },
                onNavigateToHotelEdit = {
                    navController.navigate(Routes.hotelEdit(tripId))
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
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(
                    appContainer.tripRepository,
                    appContainer.personalizationRepository
                )
            )
            // Ensure detail is loaded so FlightEditScreen can find the flight
            FlightEditScreen(
                viewModel = viewModel,
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
            val viewModel: TripViewModel = viewModel(
                factory = TripViewModel.Factory(
                    appContainer.tripRepository,
                    appContainer.personalizationRepository
                )
            )
            HotelEditScreen(
                viewModel = viewModel,
                tripId = tripId,
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
