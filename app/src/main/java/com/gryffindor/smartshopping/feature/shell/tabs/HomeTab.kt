package com.gryffindor.smartshopping.feature.shell.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.app.navigation.ProductionRoutes
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.feature.trip.TripViewModel
import com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel

@Composable
fun HomeTab(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val colors = LocalAppColors.current

    // Trip ViewModel for current trip
    val tripViewModel: TripViewModel = viewModel(
        factory = TripViewModel.Factory(
            appContainer.tripRepository,
            appContainer.personalizationRepository
        )
    )
    val tripUiState by tripViewModel.listState.collectAsState()

    // Wishlist ViewModel
    val wishlistViewModel: WishlistViewModel = viewModel(
        factory = WishlistViewModel.Factory(appContainer.personalizationRepository)
    )
    val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()

    // Device streaming state
    val cameraState by appContainer.metaCameraSource.cameraState.collectAsState()

    LaunchedEffect(Unit) {
        tripViewModel.loadTrips()
        wishlistViewModel.loadWishlist()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .verticalScroll(rememberScrollState())
    ) {
        // Top nav — Figma: top_nav (319:2129), 68dp top padding
        Spacer(modifier = Modifier.height(68.dp))

        // Logo row — Figma: favicon + notification icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_looket),
                contentDescription = "LOOKET",
                modifier = Modifier
                    .width(80.dp)
                    .height(21.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            // Notification bell icon
            Icon(
                painter = painterResource(R.drawable.ic_nav_home), // TODO: bell icon
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

        // Device status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.backgroundEmphasized
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_shop),
                        contentDescription = "Device",
                        modifier = Modifier.size(32.dp),
                        tint = colors.brandPrimary
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Meta Ray-Ban Gen 2",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (cameraState is CameraState.Streaming) "연결됨" else "대기 중",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (cameraState is CameraState.Streaming) colors.brandPrimary else colors.textSecondary
                        )
                    }
                }
            }
        }

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
                // Show first/current trip
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

        // Wishlist / Personalized content section
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

        // Recommendation section placeholder
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "당신만을 위한 오늘의 셀렉션",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "쇼핑을 시작하면 맞춤 제품을 추천해드립니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
