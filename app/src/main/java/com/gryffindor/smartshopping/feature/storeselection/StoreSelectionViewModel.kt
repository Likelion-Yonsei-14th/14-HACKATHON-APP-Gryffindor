package com.gryffindor.smartshopping.feature.storeselection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.model.Store
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Store Selection screen.
 */
data class StoreSelectionUiState(
    val stores: List<Store> = emptyList(),
    val selectedStoreId: String? = null,
    val isLoading: Boolean = false,
    val isCreatingSession: Boolean = false,
    val errorMessage: String? = null,
    /** Set when session creation succeeds — triggers navigation. */
    val sessionCreated: SessionCreatedEvent? = null
)

/**
 * One-shot event carrying the newly-created session info for navigation.
 */
data class SessionCreatedEvent(
    val sessionId: String,
    val currency: String
)

class StoreSelectionViewModel(
    private val storeRepository: StoreRepository,
    private val sessionRepository: SessionRepository,
    private val currency: String
) : ViewModel() {

    companion object {
        private const val TAG = "StoreSelectionVM"
    }

    private val _uiState = MutableStateFlow(StoreSelectionUiState())
    val uiState: StateFlow<StoreSelectionUiState> = _uiState.asStateFlow()

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val stores = storeRepository.getStores()
                _uiState.update { it.copy(stores = stores, isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load stores", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "매장 목록을 불러올 수 없습니다."
                    )
                }
            }
        }
    }

    fun selectStore(storeId: String) {
        _uiState.update { it.copy(selectedStoreId = storeId) }
    }

    fun confirmSelection() {
        val storeId = _uiState.value.selectedStoreId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true, errorMessage = null) }
            try {
                val session = sessionRepository.createSession(currency, storeId)
                Log.d(TAG, "Session created: ${session.sessionId}")
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        sessionCreated = SessionCreatedEvent(
                            sessionId = session.sessionId,
                            currency = session.currency
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create session", e)
                _uiState.update {
                    it.copy(
                        isCreatingSession = false,
                        errorMessage = e.message ?: "세션 생성에 실패했습니다."
                    )
                }
            }
        }
    }

    fun consumeSessionCreatedEvent() {
        _uiState.update { it.copy(sessionCreated = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val storeRepository: StoreRepository,
        private val sessionRepository: SessionRepository,
        private val currency: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StoreSelectionViewModel(storeRepository, sessionRepository, currency) as T
        }
    }
}
