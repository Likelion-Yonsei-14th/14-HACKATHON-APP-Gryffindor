package com.gryffindor.smartshopping.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val products: List<SessionProduct> = emptyList(),
    val purchasedIds: Set<String> = emptySet(),
    val interestedIds: Set<String> = emptySet(),
    val isSubmitting: Boolean = false
)

class ReviewViewModel(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ReviewUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ReviewUiState>> = _uiState.asStateFlow()

    private val _submitSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val submitSuccess: SharedFlow<Unit> = _submitSuccess.asSharedFlow()

    private var currentSessionId: String? = null

    fun loadProducts(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = shoppingRepository.getProducts(sessionId)
                _uiState.value = UiState.Success(
                    ReviewUiState(products = products)
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "상품 목록을 불러올 수 없습니다.")
            }
        }
    }

    fun togglePurchased(productId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        val newPurchasedIds = if (productId in current.purchasedIds) {
            current.purchasedIds - productId
        } else {
            current.purchasedIds + productId
        }
        // PURCHASED takes precedence: remove from interestedIds if purchased
        val newInterestedIds = current.interestedIds - newPurchasedIds
        _uiState.value = UiState.Success(
            current.copy(purchasedIds = newPurchasedIds, interestedIds = newInterestedIds)
        )
    }

    fun toggleInterested(productId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        // Cannot mark as interested if already purchased
        if (productId in current.purchasedIds) return
        val newInterestedIds = if (productId in current.interestedIds) {
            current.interestedIds - productId
        } else {
            current.interestedIds + productId
        }
        _uiState.value = UiState.Success(current.copy(interestedIds = newInterestedIds))
    }

    fun submitReview(sessionId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = UiState.Success(current.copy(isSubmitting = true))
            try {
                // Ensure submitted interestedIds never contain purchasedIds
                val finalInterestedIds = current.interestedIds - current.purchasedIds
                shoppingRepository.submitReview(
                    sessionId = sessionId,
                    purchasedProductIds = current.purchasedIds.toList(),
                    interestedProductIds = finalInterestedIds.toList()
                )
                _uiState.value = UiState.Success(current.copy(isSubmitting = false))
                _submitSuccess.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "리뷰 제출에 실패했습니다.")
            }
        }
    }

    fun retry() {
        currentSessionId?.let { loadProducts(it) }
    }

    class Factory(
        private val shoppingRepository: ShoppingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(shoppingRepository) as T
        }
    }
}
