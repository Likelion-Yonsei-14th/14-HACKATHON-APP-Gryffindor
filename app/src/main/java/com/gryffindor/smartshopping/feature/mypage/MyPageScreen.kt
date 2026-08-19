package com.gryffindor.smartshopping.feature.mypage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.PurchasedProduct
import com.gryffindor.smartshopping.domain.model.TripSummary

@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel,
    onNavigateToTripDetail: (tripId: String) -> Unit = {},
    onNavigateToReservationList: (tripId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Load on first composition
    LaunchedEffect(Unit) {
        viewModel.loadMyPage()
    }

    // Show receipt message as snackbar
    LaunchedEffect(uiState.receiptMessage) {
        uiState.receiptMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearReceiptMessage()
        }
    }

    // Photo picker for receipt
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                viewModel.analyzeReceipt(bytes)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.myPage == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.myPage == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadMyPage() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            uiState.myPage != null -> {
                val myPage = uiState.myPage!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Header
                    Text(
                        text = "My Page",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // User section
                    UserSection(userName = myPage.user.name)
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    // Wishlist section
                    WishlistSection(
                        wishlist = myPage.wishlist,
                        onRemove = { productId -> viewModel.removeFromWishlist(productId) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    // Purchased Products section
                    PurchasedProductsSection(
                        products = myPage.purchasedProducts,
                        isAnalyzing = uiState.isAnalyzingReceipt,
                        onAnalyzeReceipt = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    // Trip section
                    TripSection(
                        trips = myPage.trips,
                        onTripClick = onNavigateToTripDetail
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    // Reservation section
                    ReservationSection(
                        trips = myPage.trips,
                        onViewReservations = onNavigateToReservationList
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ===== Sections =====

@Composable
private fun UserSection(userName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "👤",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "사용자",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WishlistSection(
    wishlist: List<Product>,
    onRemove: (String) -> Unit
) {
    Text(
        text = "Wishlist",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (wishlist.isEmpty()) {
        Text(
            text = "위시리스트가 비어있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        wishlist.forEach { product ->
            WishlistItemCard(product = product, onRemove = { onRemove(product.productId) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WishlistItemCard(product: Product, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onRemove) {
                Text(
                    text = "삭제",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PurchasedProductsSection(
    products: List<PurchasedProduct>,
    isAnalyzing: Boolean,
    onAnalyzeReceipt: () -> Unit
) {
    Text(
        text = "구매한 상품",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Receipt upload button
    Button(
        onClick = onAnalyzeReceipt,
        enabled = !isAnalyzing,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isAnalyzing) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("구매 상품을 확인하고 있어요...")
        } else {
            Text("영수증으로 구매 상품 등록")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (products.isEmpty()) {
        Text(
            text = "등록된 구매 상품이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        products.forEach { purchased ->
            PurchasedProductCard(purchased)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PurchasedProductCard(purchased: PurchasedProduct) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (purchased.product != null) {
                // Catalog matched
                val product = purchased.product
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Unmatched — fallback name
                Text(
                    text = purchased.fallbackProductName ?: "상품명 없음",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "상품 정보 미연결",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Store & price info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                purchased.storeName?.let { store ->
                    Text(
                        text = store,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (purchased.price != null) {
                    val priceText = if (purchased.currency != null) {
                        "${purchased.currency} ${String.format("%,d", purchased.price)}"
                    } else {
                        String.format("%,d", purchased.price)
                    }
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Purchase date
            purchased.purchasedAt?.let { date ->
                Text(
                    text = date.take(10), // Show date portion only
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TripSection(
    trips: List<TripSummary>,
    onTripClick: (String) -> Unit
) {
    Text(
        text = "여행 일정",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (trips.isEmpty()) {
        Text(
            text = "등록된 여행이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        trips.forEach { trip ->
            TripCard(trip = trip, onClick = { onTripClick(trip.id) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TripCard(trip: TripSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = trip.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (trip.startsAt != null || trip.endsAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${trip.startsAt?.take(10) ?: "?"} ~ ${trip.endsAt?.take(10) ?: "?"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReservationSection(
    trips: List<TripSummary>,
    onViewReservations: (String) -> Unit
) {
    Text(
        text = "방문 예약",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (trips.isEmpty()) {
        Text(
            text = "여행을 먼저 등록해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        trips.forEach { trip ->
            OutlinedButton(
                onClick = { onViewReservations(trip.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${trip.title} — 방문 예약 보기")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
