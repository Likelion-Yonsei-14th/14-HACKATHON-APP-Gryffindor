package com.gryffindor.smartshopping.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val products: List<SessionProduct> = emptyList(),
    val isSessionActive: Boolean = true
)

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ShoppingUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ShoppingUiState>> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    fun loadProducts(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = shoppingRepository.getProducts(sessionId)
                _uiState.value = UiState.Success(
                    ShoppingUiState(products = products, isSessionActive = true)
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "상품 목록을 불러올 수 없습니다.")
            }
        }
    }

    fun endShopping(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepository.completeSession(sessionId)
                val currentState = (_uiState.value as? UiState.Success)?.data
                if (currentState != null) {
                    _uiState.value = UiState.Success(currentState.copy(isSessionActive = false))
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "쇼핑 종료에 실패했습니다.")
            }
        }
    }

    fun retry() {
        currentSessionId?.let { loadProducts(it) }
    }

    class Factory(
        private val shoppingRepository: ShoppingRepository,
        private val sessionRepository: SessionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingViewModel(shoppingRepository, sessionRepository) as T
        }
    }
}
