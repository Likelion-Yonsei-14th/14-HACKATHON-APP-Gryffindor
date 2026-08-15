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
import com.gryffindor.smartshopping.feature.home.HomeScreen
import com.gryffindor.smartshopping.feature.home.HomeViewModel
import com.gryffindor.smartshopping.feature.recommendation.RecommendationScreen
import com.gryffindor.smartshopping.feature.recommendation.RecommendationViewModel
import com.gryffindor.smartshopping.feature.review.ReviewScreen
import com.gryffindor.smartshopping.feature.review.ReviewViewModel
import com.gryffindor.smartshopping.feature.shopping.ShoppingScreen
import com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel
import com.gryffindor.smartshopping.feature.travel.TravelScreen
import com.gryffindor.smartshopping.feature.travel.TravelViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    appContainer.sessionRepository,
                    appContainer.cameraFrameProvider
                )
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToShopping = { sessionId ->
                    navController.navigate(Routes.shopping(sessionId))
                }
            )
        }

        composable(
            route = Routes.SHOPPING,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val viewModel: ShoppingViewModel = viewModel(
                factory = ShoppingViewModel.Factory(
                    appContainer.shoppingRepository,
                    appContainer.sessionRepository,
                    appContainer.cameraFrameProvider
                )
            )
            ShoppingScreen(
                viewModel = viewModel,
                sessionId = sessionId,
                onNavigateToReview = {
                    navController.navigate(Routes.review(sessionId))
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
    }
}
