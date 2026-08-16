package com.gryffindor.smartshopping.feature.shopping

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
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
    private val sessionRepository: SessionRepository,
    private val cameraFrameProvider: CameraFrameProvider,
    private val detectionResultProvider: DetectionResultProvider
) : ViewModel() {

    companion object {
        private const val TAG = "ShoppingViewModel"
    }

    private val _uiState = MutableStateFlow<UiState<ShoppingUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ShoppingUiState>> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    init {
        // Observe detection pipeline — ensures the lazy DetectionPipeline is accessed
        // and starts collecting when camera goes Streaming.
        viewModelScope.launch {
            detectionResultProvider.detections.collect { result ->
                // Detection results are logged by DetectionPipeline itself.
                // Future: feed into AttentionPolicy / UI.
                Log.d(TAG, "Detection received: ts=${result.frameTimestampUs}, count=${result.detections.size}")
            }
        }
    }

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
            // Camera stop is best-effort — failure does NOT block session completion.
            try {
                cameraFrameProvider.stopCamera()
            } catch (e: Exception) {
                Log.w(TAG, "Camera stop failed (session completion unaffected)", e)
            }

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
        private val sessionRepository: SessionRepository,
        private val cameraFrameProvider: CameraFrameProvider,
        private val detectionResultProvider: DetectionResultProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingViewModel(shoppingRepository, sessionRepository, cameraFrameProvider, detectionResultProvider) as T
        }
    }
}
