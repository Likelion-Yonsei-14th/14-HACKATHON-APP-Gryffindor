package com.gryffindor.smartshopping.feature.mypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.model.MyPage
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageUiState(
    val isLoading: Boolean = false,
    val myPage: MyPage? = null,
    val error: String? = null,
    val isAnalyzingReceipt: Boolean = false,
    val receiptMessage: String? = null
)

class MyPageViewModel(
    private val personalizationRepository: PersonalizationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MyPageVM"
    }

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    fun loadMyPage() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val myPage = personalizationRepository.getMyPage()
                _uiState.update { it.copy(isLoading = false, myPage = myPage) }
                Log.d(TAG, "MyPage loaded: ${myPage.purchasedProducts.size} purchases, ${myPage.wishlist.size} wishlist")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load MyPage", e)
                _uiState.update { it.copy(isLoading = false, error = "마이페이지를 불러오지 못했습니다.") }
            }
        }
    }

    fun analyzeReceipt(imageBytes: ByteArray) {
        if (_uiState.value.isAnalyzingReceipt) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingReceipt = true, receiptMessage = null) }
            try {
                val receipt = personalizationRepository.analyzeReceipt(imageBytes)
                Log.d(TAG, "Receipt analyzed: ${receipt.items.size} items, store=${receipt.storeName}")
                _uiState.update { it.copy(isAnalyzingReceipt = false, receiptMessage = "구매 상품이 등록되었습니다.") }
                // Refresh MyPage to show new purchases
                refreshAfterReceipt()
            } catch (e: Exception) {
                Log.w(TAG, "Receipt analysis failed", e)
                _uiState.update {
                    it.copy(
                        isAnalyzingReceipt = false,
                        receiptMessage = "영수증을 확인하지 못했습니다.\n다른 이미지를 사용해 주세요."
                    )
                }
            }
        }
    }

    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            try {
                personalizationRepository.deleteWishlist(productId)
                // Optimistic update
                _uiState.update { state ->
                    state.copy(
                        myPage = state.myPage?.copy(
                            wishlist = state.myPage.wishlist.filter { it.productId != productId }
                        )
                    )
                }
                Log.d(TAG, "Removed from wishlist: $productId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove wishlist item: $productId", e)
            }
        }
    }

    fun clearReceiptMessage() {
        _uiState.update { it.copy(receiptMessage = null) }
    }

    private suspend fun refreshAfterReceipt() {
        try {
            val myPage = personalizationRepository.getMyPage()
            _uiState.update { it.copy(myPage = myPage) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh after receipt", e)
        }
    }

    class Factory(
        private val personalizationRepository: PersonalizationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageViewModel(personalizationRepository) as T
        }
    }
}
