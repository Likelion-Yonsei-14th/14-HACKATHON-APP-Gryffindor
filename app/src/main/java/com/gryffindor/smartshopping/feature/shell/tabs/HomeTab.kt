package com.gryffindor.smartshopping.feature.shell.tabs

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.app.navigation.ProductionRoutes
import com.gryffindor.smartshopping.app.navigation.Routes
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.GradientEnd
import com.gryffindor.smartshopping.core.ui.theme.GradientStart
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.feature.feed.FeedSection
import com.gryffindor.smartshopping.feature.feed.FeedViewModel
import com.gryffindor.smartshopping.feature.trip.TripViewModel
import com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel

@Composable
fun HomeTab(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val colors = LocalAppColors.current

    val tripViewModel: TripViewModel = viewModel(
        factory = TripViewModel.Factory(
            appContainer.tripRepository,
            appContainer.personalizationRepository
        )
    )
    val tripUiState by tripViewModel.listState.collectAsState()

    val wishlistViewModel: WishlistViewModel = viewModel(
        factory = WishlistViewModel.Factory(appContainer.personalizationRepository)
    )
    val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()

    // Feed ViewModel — loads trips + AI recommendations with store cards
    val feedViewModel: FeedViewModel = viewModel(
        factory = FeedViewModel.Factory(
            appContainer.tripRepository,
            appContainer.locationProvider
        )
    )
    val feedUiState by feedViewModel.uiState.collectAsState()

    // Snackbar state for "trip required" message
    var showTripRequiredMessage by remember { mutableStateOf(false) }

    // Location permission request — needed for Feed distance calculation
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Re-trigger feed load with location now available
            feedViewModel.retry()
        }
    }

    LaunchedEffect(Unit) {
        tripViewModel.loadTrips()
        wishlistViewModel.loadWishlist()

        // Request location permission if not yet granted
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        feedViewModel.loadTripsAndFeed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(68.dp))

        // Logo row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_looket),
                contentDescription = "LOOKET",
                modifier = Modifier
                    .width(80.dp)
                    .height(21.dp),
                contentScale = ContentScale.Fit
            )
            Icon(
                painter = painterResource(R.drawable.ic_nav_home),
                contentDescription = "알림",
                modifier = Modifier.size(24.dp),
                tint = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Greeting
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "안녕하세요!",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "오늘도 즐거운 쇼핑 되세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 환급 금액 카드 — Figma: 환급_금액 (324:1246)
        RefundAmountCard()

        Spacer(modifier = Modifier.height(24.dp))

        // Trip section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "내 여행",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val trips = tripUiState.trips
            if (trips.isEmpty()) {
                Text(
                    text = "등록된 여행이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                val currentTrip = trips.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.backgroundEmphasized),
                    onClick = {
                        navController.navigate(
                            com.gryffindor.smartshopping.app.navigation.Routes.tripDetail(currentTrip.id)
                        )
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = currentTrip.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary
                        )
                        currentTrip.startsAt?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            PrimaryButton(
                text = "여행 등록하기",
                onClick = {
                    navController.navigate(ProductionRoutes.TRIP_FLIGHT_REGISTER)
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Wishlist section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "찜한 브랜드",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (wishlistIds.isEmpty()) {
                Text(
                    text = "아직 찜한 제품이 없습니다.\n쇼핑하면서 마음에 드는 제품을 찜해보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            } else {
                Text(
                    text = "${wishlistIds.size}개의 제품이 위시리스트에 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- AI Feed Section: 추천 Store + 방문 예약 CTA ---
        if (feedUiState.trips.isNotEmpty() || feedUiState.isLoadingTrips || feedUiState.isLoadingFeed) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "추천 매장",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            FeedSection(
                uiState = feedUiState,
                onRetry = { feedViewModel.retry() },
                onStoreClick = { storeId ->
                    // Navigate to store detail if available, otherwise no-op
                },
                onWishlistToggle = { productId -> wishlistViewModel.toggleWishlist(productId) },
                onVisitReservation = { storeId, storeName ->
                    val tripId = feedUiState.selectedTrip?.id
                    if (tripId != null) {
                        navController.navigate(Routes.visitReservation(tripId, storeId, storeName))
                    } else {
                        // No trip — prompt user to register a trip first
                        showTripRequiredMessage = true
                        navController.navigate(ProductionRoutes.TRIP_FLIGHT_REGISTER)
                    }
                },
                wishlistIds = wishlistIds,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Trip required snackbar
        if (showTripRequiredMessage) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("방문 예약을 위해 여행을 먼저 등록해주세요.")
            }
            LaunchedEffect(showTripRequiredMessage) {
                kotlinx.coroutines.delay(3000)
                showTripRequiredMessage = false
            }
        }
    }
}

/**
 * 환급 금액 카드 — Figma: 환급_금액 (324:1246)
 *
 * Gradient bg (#616AF3→#3B36CC), radius 8dp, padding 16dp
 * - "OO님의 환급 금액" (title-2, white)
 * - "총 구매 금액 ₩ 0 중" (body-2, white)
 * - "₩ 0" (display: 28/ExtraBold, white)
 * - Bottom: 완료 N건 (green) / 진행중 N건 (orange)
 */
@Composable
private fun RefundAmountCard() {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "OO님의 환급 금액",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textInverse
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 총 구매 금액
            Text(
                text = "총 구매 금액 ₩ 0 중",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textInverse
            )

            // 환급 금액
            Text(
                text = "₩ 0",
                style = MaterialTheme.typography.displaySmall, // 28/ExtraBold
                color = colors.textInverse,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 완료/진행중 row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 완료
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.semanticGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "완료 0건",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textInverse
                        )
                    }
                    Text(
                        text = "₩ 0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textInverse
                    )
                }

                // 진행중
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.brandSecondary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "진행중 0건",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textInverse
                        )
                    }
                    Text(
                        text = "₩ 0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textInverse
                    )
                }
            }
        }
    }
}
