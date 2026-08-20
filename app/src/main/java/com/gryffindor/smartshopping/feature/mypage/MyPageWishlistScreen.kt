package com.gryffindor.smartshopping.feature.mypage

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketSmallButton
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageWishlistUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val removingIds: Set<String> = emptySet(),
    val error: String? = null,
)

/**
 * 마이페이지 "위시리스트" 화면 전용 ViewModel. [com.gryffindor.smartshopping.feature.wishlist.WishlistViewModel]은
 * Feed/Shopping 상품 카드의 찜 토글 표시용으로 productId 집합만 들고 있어서, 이름/브랜드까지
 * 보여줘야 하는 이 목록 화면에는 맞지 않아 별도로 둔다.
 */
class MyPageWishlistViewModel(
    private val personalizationRepository: PersonalizationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MyPageWishlistVM"
    }

    private val _uiState = MutableStateFlow(MyPageWishlistUiState())
    val uiState: StateFlow<MyPageWishlistUiState> = _uiState.asStateFlow()

    fun loadWishlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val products = personalizationRepository.getWishlist()
                _uiState.update { it.copy(isLoading = false, products = products) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load wishlist", e)
                _uiState.update { it.copy(isLoading = false, error = "위시리스트를 불러오지 못했습니다.") }
            }
        }
    }

    fun removeFromWishlist(productId: String) {
        if (_uiState.value.removingIds.contains(productId)) return
        viewModelScope.launch {
            _uiState.update { it.copy(removingIds = it.removingIds + productId) }
            try {
                personalizationRepository.deleteWishlist(productId)
                _uiState.update {
                    it.copy(
                        products = it.products.filterNot { product -> product.productId == productId },
                        removingIds = it.removingIds - productId,
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove $productId from wishlist", e)
                _uiState.update {
                    it.copy(removingIds = it.removingIds - productId, error = "삭제에 실패했습니다.")
                }
            }
        }
    }

    class Factory(
        private val personalizationRepository: PersonalizationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageWishlistViewModel(personalizationRepository) as T
        }
    }
}

@Composable
fun MyPageWishlistScreen(
    uiState: MyPageWishlistUiState,
    onRemoveClick: (productId: String) -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = { BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface),
        ) {
            LooketTopBar(title = stringResource(R.string.mypage_wishlist), onBackClick = onBackClick)

            when {
                uiState.isLoading && uiState.products.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LooketColors.BrandPrimary)
                    }
                }
                uiState.products.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.mypage_wishlist_empty),
                            style = LooketTextStyles.bodyTwo,
                            color = LooketColors.TextSecondary,
                        )
                    }
                }
                else -> {
                    LazyColumn {
                        items(uiState.products, key = { it.productId }) { product ->
                            WishlistItemRow(
                                product = product,
                                onRemoveClick = { onRemoveClick(product.productId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistItemRow(product: Product, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val color = LooketColors.BorderDefault
                drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (product.imageUrl != null) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(width = 80.dp, height = 80.dp)
                    .background(LooketColors.SurfaceEmphasized),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 80.dp)
                    .background(LooketColors.SurfaceEmphasized),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(text = product.brand, style = LooketTextStyles.bodyThree, color = LooketColors.TextSecondary)
                Text(text = product.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
            }
            LooketSmallButton(
                text = stringResource(R.string.shopping_result_wishlist_heart),
                backgroundColor = LooketColors.RedLight,
                contentColor = LooketColors.Red,
                onClick = onRemoveClick,
            )
        }
    }
}
