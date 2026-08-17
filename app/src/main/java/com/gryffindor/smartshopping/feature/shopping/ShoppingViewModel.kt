package com.gryffindor.smartshopping.feature.shopping

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.PurchaseState
import com.gryffindor.smartshopping.domain.model.RecognitionResult
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
    private val detectionResultProvider: DetectionResultProvider,
    private val attentionCandidateProvider: AttentionCandidateProvider
) : ViewModel() {

    companion object {
        private const val TAG = "ShoppingVM"
    }

    private val _uiState = MutableStateFlow<UiState<ShoppingUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ShoppingUiState>> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    /** Tracks productIds already added to the card list — prevents duplicates. */
    private val recognizedProductIds = mutableSetOf<String>()

    /** Whether the shopping session is still active. Guards recognition requests. */
    private var sessionActive: Boolean = false

    init {
        // Observe detection pipeline — ensures the lazy DetectionPipeline is accessed
        // and starts collecting when camera goes Streaming.
        viewModelScope.launch {
            detectionResultProvider.detections.collect { result ->
                Log.d(TAG, "Detection received: ts=${result.frameTimestampUs}, count=${result.detections.size}")
            }
        }

        // A4: Collect attention candidates and send to Backend for recognition.
        viewModelScope.launch {
            attentionCandidateProvider.candidates.collect { candidate ->
                handleAttentionCandidate(candidate)
            }
        }
    }

    fun loadProducts(sessionId: String) {
        currentSessionId = sessionId
        sessionActive = true
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = shoppingRepository.getProducts(sessionId)
                // Seed dedup set with already-known products
                products.forEach { recognizedProductIds.add(it.product.productId) }
                _uiState.value = UiState.Success(
                    ShoppingUiState(products = products, isSessionActive = true)
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "상품 목록을 불러올 수 없습니다.")
            }
        }
    }

    fun endShopping(sessionId: String) {
        // Immediately stop sending recognition requests.
        sessionActive = false

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

    /**
     * A4 core: sends AttentionCandidate to Backend /recognize,
     * adds MATCHED products to the product card list (dedup by productId).
     */
    private suspend fun handleAttentionCandidate(candidate: AttentionCandidate) {
        val sessionId = currentSessionId
        if (sessionId == null || !sessionActive) {
            Log.d(TAG, "Candidate ignored: session=${sessionId}, active=$sessionActive")
            return
        }

        Log.i(TAG, buildString {
            append("[A4] AttentionCandidate received: ")
            append("trackingId=${candidate.trackingId} ")
            append("triggerType=${candidate.triggerType.name} ")
            append("occupancy=${"%.3f".format(candidate.occupancyRatio)} ")
            append("dwell=${candidate.dwellMs}ms ")
            append("jpeg=${candidate.jpegBytes.size} bytes")
        })

        try {
            Log.d(TAG, "[A4] recognize request start: sessionId=$sessionId")
            val result = shoppingRepository.recognize(sessionId, candidate)

            when (result) {
                is RecognitionResult.Matched -> handleMatched(result)
                is RecognitionResult.Ambiguous -> {
                    Log.i(TAG, "[A4] recognize result=AMBIGUOUS candidates=${result.candidateProductIds}")
                }
                is RecognitionResult.Unknown -> {
                    Log.i(TAG, "[A4] recognize result=UNKNOWN")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[A4] recognize network error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    private fun handleMatched(result: RecognitionResult.Matched) {
        val productId = result.observedProduct.product.productId
        val isNew = result.isNew

        Log.i(TAG, buildString {
            append("[A4] recognize result=MATCHED ")
            append("productId=$productId ")
            append("name=${result.observedProduct.product.name} ")
            append("isNew=$isNew")
        })

        // Dedup: skip if already in our product card list
        if (!recognizedProductIds.add(productId)) {
            Log.i(TAG, "[A4] duplicate ignored: productId=$productId")
            return
        }

        // Convert ObservedProduct → SessionProduct for the existing Product Card UI
        val sessionProduct = SessionProduct(
            product = result.observedProduct.product,
            pricing = result.observedProduct.pricing,
            purchaseState = PurchaseState.UNSET,
            interested = false
        )

        // Append to existing product list
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        val updatedProducts = currentState.products + sessionProduct
        _uiState.value = UiState.Success(currentState.copy(products = updatedProducts))

        Log.i(TAG, "[A4] product card added: productId=$productId")
    }

    class Factory(
        private val shoppingRepository: ShoppingRepository,
        private val sessionRepository: SessionRepository,
        private val cameraFrameProvider: CameraFrameProvider,
        private val detectionResultProvider: DetectionResultProvider,
        private val attentionCandidateProvider: AttentionCandidateProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingViewModel(
                shoppingRepository,
                sessionRepository,
                cameraFrameProvider,
                detectionResultProvider,
                attentionCandidateProvider
            ) as T
        }
    }
}
