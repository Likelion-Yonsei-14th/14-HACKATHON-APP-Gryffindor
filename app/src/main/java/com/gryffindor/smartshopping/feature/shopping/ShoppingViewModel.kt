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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Which currency the product card prices are currently displayed in.
 */
enum class DisplayCurrency { KRW, CONVERTED }

data class PricingSummary(
    val totalRetailKrw: Long = 0,
    val totalEstimatedRefundKrw: Long = 0,
    val totalConvertedRetail: String? = null,
    val totalConvertedRefund: String? = null,
    val convertedCurrency: String? = null
)

data class ShoppingUiState(
    val products: List<SessionProduct> = emptyList(),
    val isSessionActive: Boolean = true,
    val sessionCurrency: String = "USD",
    val displayCurrency: DisplayCurrency = DisplayCurrency.KRW,
    val summary: PricingSummary = PricingSummary(),
    /** Whether all products have converted pricing available */
    val convertedAvailable: Boolean = false
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
        private const val TIMING_TAG = "AttentionTiming"

        /** Slow fallback must not block one independent fast-path request. */
        private const val RECOGNITION_MAX_CONCURRENT_REQUESTS = 2
    }

    private val _uiState = MutableStateFlow<UiState<ShoppingUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ShoppingUiState>> = _uiState.asStateFlow()

    private var currentSessionId: String? = null
    private var currentCurrency: String = "USD"

    /** Tracks productIds already added to the card list — prevents duplicates. */
    private val recognizedProductIds = mutableSetOf<String>()

    /** Whether the shopping session is still active. Guards recognition requests. */
    @Volatile
    private var sessionActive: Boolean = false

    // --- Bounded recognition concurrency state ---

    /** Serializes recognition registration with session shutdown. Never held across suspension. */
    private val recognitionStateLock = Any()

    /** Two non-blocking permits: a third candidate is dropped instead of queued. */
    private val recognitionSlots = Semaphore(RECOGNITION_MAX_CONCURRENT_REQUESTS)

    /** Every recognition Job registered for the active session. */
    private val recognitionJobs = mutableSetOf<Job>()

    /** Suppresses concurrent requests for the same safely identifiable tracked object. */
    private val activeRecognitionTrackingIds = mutableSetOf<String>()

    init {
        // Observe detection pipeline — ensures the lazy DetectionPipeline is accessed
        // and starts collecting when camera goes Streaming.
        viewModelScope.launch {
            detectionResultProvider.detections.collect { result ->
                Log.d(TAG, "Detection received: ts=${result.frameTimestampUs}, count=${result.detections.size}")
            }
        }

        // A4: Collect attention candidates — non-blocking consumer that drops when busy.
        viewModelScope.launch {
            attentionCandidateProvider.candidates.collect { candidate ->
                dispatchRecognition(candidate)
            }
        }
    }

    fun loadProducts(sessionId: String, currency: String) {
        synchronized(recognitionStateLock) {
            currentSessionId = sessionId
            currentCurrency = currency
            sessionActive = true
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = shoppingRepository.getProducts(sessionId)
                // Seed dedup set with already-known products
                products.forEach { recognizedProductIds.add(it.product.productId) }
                _uiState.value = UiState.Success(
                    ShoppingUiState(
                        products = products,
                        isSessionActive = true,
                        sessionCurrency = currency,
                        displayCurrency = DisplayCurrency.KRW,
                        summary = computeSummary(products),
                        convertedAvailable = products.all { hasConvertedPricing(it) }
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "상품 목록을 불러올 수 없습니다.")
            }
        }
    }

    fun toggleCurrency() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (!current.convertedAvailable && current.displayCurrency == DisplayCurrency.KRW) {
            // Can't switch to converted if data is unavailable
            return
        }
        val newDisplay = when (current.displayCurrency) {
            DisplayCurrency.KRW -> DisplayCurrency.CONVERTED
            DisplayCurrency.CONVERTED -> DisplayCurrency.KRW
        }
        _uiState.value = UiState.Success(current.copy(displayCurrency = newDisplay))
    }

    private fun computeSummary(products: List<SessionProduct>): PricingSummary {
        val totalRetailKrw = products.sumOf { it.pricing.retailPriceKrw }
        val totalRefundKrw = products.sumOf { it.pricing.estimatedRefundKrw }

        // Sum converted values (all must be non-null for a valid total)
        val allConverted = products.all { hasConvertedPricing(it) }
        val totalConvertedRetail = if (allConverted && products.isNotEmpty()) {
            sumConvertedStrings(products.map { it.pricing.convertedRetailPrice!! })
        } else null
        val totalConvertedRefund = if (allConverted && products.isNotEmpty()) {
            sumConvertedStrings(products.map { it.pricing.convertedEstimatedRefund!! })
        } else null
        val currency = products.firstOrNull()?.pricing?.convertedCurrency

        return PricingSummary(
            totalRetailKrw = totalRetailKrw,
            totalEstimatedRefundKrw = totalRefundKrw,
            totalConvertedRetail = totalConvertedRetail,
            totalConvertedRefund = totalConvertedRefund,
            convertedCurrency = currency
        )
    }

    private fun hasConvertedPricing(product: SessionProduct): Boolean {
        val p = product.pricing
        return p.convertedRetailPrice != null &&
            p.convertedEstimatedRefund != null &&
            p.convertedCurrency != null
    }

    private fun sumConvertedStrings(values: List<String>): String {
        val total = values.sumOf { it.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
        return total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
    }

    fun endShopping(sessionId: String) {
        // Registration and shutdown share a lock, so no recognition Job can escape cancellation.
        val jobsToCancel = synchronized(recognitionStateLock) {
            sessionActive = false
            recognitionJobs.toList()
        }
        jobsToCancel.forEach(Job::cancel)
        if (jobsToCancel.isNotEmpty()) {
            Log.i(TAG, "[A4] recognitions cancelled: session completed count=${jobsToCancel.size}")
        }

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
        currentSessionId?.let { loadProducts(it, currentCurrency) }
    }

    /**
     * Non-blocking dispatcher: accepts up to two recognition requests and drops a candidate
     * immediately when both slots are occupied or the same tracked object is already active.
     * Independent tracks can reuse a slot immediately after a response.
     * The collector itself never suspends on network I/O — backpressure is resolved by dropping.
     */
    private fun dispatchRecognition(candidate: AttentionCandidate) {
        val dispatchWallMs = System.currentTimeMillis()

        val recognitionJob = synchronized(recognitionStateLock) {
            val dispatchSessionId = currentSessionId
            if (dispatchSessionId == null || !sessionActive) {
                Log.i(TIMING_TAG, buildString {
                    append("[Dispatch] DROP trackingId=${candidate.trackingId}")
                    append(" | reason=session_inactive")
                    append(" | dwellMs=${candidate.dwellMs}")
                    append(" | occupancy=${"%.4f".format(candidate.occupancyRatio)}")
                    append(" | wallMs=$dispatchWallMs")
                })
                Log.d(TAG, "Candidate ignored: session=$dispatchSessionId, active=$sessionActive")
                return
            }

            val trackingId = candidate.trackingId
            if (trackingId != null && !activeRecognitionTrackingIds.add(trackingId)) {
                Log.i(TIMING_TAG, buildString {
                    append("[Dispatch] DROP trackingId=$trackingId")
                    append(" | reason=same_trackingId_inflight")
                    append(" | dwellMs=${candidate.dwellMs}")
                    append(" | occupancy=${"%.4f".format(candidate.occupancyRatio)}")
                    append(" | wallMs=$dispatchWallMs")
                })
                Log.d(TAG, "[A4] candidate dropped: trackingId=$trackingId already in-flight")
                return
            }

            if (!recognitionSlots.tryAcquire()) {
                trackingId?.let(activeRecognitionTrackingIds::remove)
                Log.i(TIMING_TAG, buildString {
                    append("[Dispatch] DROP trackingId=$trackingId")
                    append(" | reason=concurrency_full")
                    append(" | dwellMs=${candidate.dwellMs}")
                    append(" | occupancy=${"%.4f".format(candidate.occupancyRatio)}")
                    append(" | wallMs=$dispatchWallMs")
                })
                Log.d(TAG, "[A4] candidate dropped: max concurrent recognitions reached")
                return
            }

            Log.i(TIMING_TAG, buildString {
                append("[Dispatch] ACCEPTED trackingId=$trackingId")
                append(" | dwellMs=${candidate.dwellMs}")
                append(" | occupancy=${"%.4f".format(candidate.occupancyRatio)}")
                append(" | capturedAt=${candidate.capturedAt}")
                append(" | dispatchWallMs=$dispatchWallMs")
            })

            // The fallback cleanup handles cancellation after registration but before coroutine entry.
            val slotReleased = AtomicBoolean(false)
            lateinit var job: Job
            job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    handleAttentionCandidate(candidate, dispatchSessionId, dispatchWallMs)
                } finally {
                    releaseRecognitionResources(
                        trackingId = trackingId,
                        slotReleased = slotReleased
                    )
                }
            }

            recognitionJobs.add(job)
            job.invokeOnCompletion {
                releaseRecognitionResources(
                    trackingId = trackingId,
                    slotReleased = slotReleased
                )
                synchronized(recognitionStateLock) {
                    recognitionJobs.remove(job)
                }
            }
            job
        }

        recognitionJob.start()
    }

    /**
     * Releases a permit and tracking reservation exactly once.
     */
    private fun releaseRecognitionResources(
        trackingId: String?,
        slotReleased: AtomicBoolean
    ) {
        if (!slotReleased.compareAndSet(false, true)) return

        synchronized(recognitionStateLock) {
            recognitionSlots.release()
            trackingId?.let(activeRecognitionTrackingIds::remove)
        }
    }

    /**
     * A4 core: sends AttentionCandidate to Backend /recognize,
     * adds MATCHED products to the product card list (dedup by productId).
     */
    private suspend fun handleAttentionCandidate(
        candidate: AttentionCandidate,
        dispatchSessionId: String,
        dispatchWallMs: Long
    ) {
        Log.i(TAG, buildString {
            append("[A4] AttentionCandidate received: ")
            append("trackingId=${candidate.trackingId} ")
            append("triggerType=${candidate.triggerType.name} ")
            append("occupancy=${"%.3f".format(candidate.occupancyRatio)} ")
            append("dwell=${candidate.dwellMs}ms ")
            append("jpeg=${candidate.jpegBytes.size} bytes")
        })

        try {
            val recognizeStartMs = System.currentTimeMillis()
            Log.i(TIMING_TAG, buildString {
                append("[Recognize] START trackingId=${candidate.trackingId}")
                append(" | sessionId=$dispatchSessionId")
                append(" | latencySinceDispatchMs=${recognizeStartMs - dispatchWallMs}")
            })
            Log.d(TAG, "[A4] recognize request start: sessionId=$dispatchSessionId")
            val result = shoppingRepository.recognize(dispatchSessionId, candidate)
            val recognizeEndMs = System.currentTimeMillis()

            Log.i(TIMING_TAG, buildString {
                append("[Recognize] END trackingId=${candidate.trackingId}")
                append(" | networkMs=${recognizeEndMs - recognizeStartMs}")
                append(" | totalSinceDispatchMs=${recognizeEndMs - dispatchWallMs}")
                append(" | result=${result::class.simpleName}")
            })

            // Session lifecycle guard: ignore stale results
            val isCurrentSession = synchronized(recognitionStateLock) {
                sessionActive && currentSessionId == dispatchSessionId
            }
            if (!isCurrentSession) {
                Log.i(TIMING_TAG, "[Recognize] STALE trackingId=${candidate.trackingId} | result ignored (session ended)")
                Log.i(TAG, "[A4] stale recognition result ignored")
                return
            }

            when (result) {
                is RecognitionResult.Matched -> handleMatched(result)
                is RecognitionResult.Ambiguous -> {
                    Log.i(TAG, "[A4] recognize result=AMBIGUOUS candidates=${result.candidateProductIds}")
                }
                is RecognitionResult.Unknown -> {
                    Log.i(TAG, "[A4] recognize result=UNKNOWN")
                }
            }
        } catch (e: CancellationException) {
            Log.i(TIMING_TAG, "[Recognize] CANCELLED trackingId=${candidate.trackingId}")
            Log.i(TAG, "[A4] recognize cancelled")
            throw e
        } catch (e: Exception) {
            Log.i(TIMING_TAG, buildString {
                append("[Recognize] ERROR trackingId=${candidate.trackingId}")
                append(" | error=${e.javaClass.simpleName}: ${e.message}")
                append(" | elapsedMs=${System.currentTimeMillis() - dispatchWallMs}")
            })
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
        val newConvertedAvailable = updatedProducts.all { hasConvertedPricing(it) }
        // If converted becomes unavailable, reset display to KRW
        val newDisplayCurrency = if (!newConvertedAvailable && currentState.displayCurrency == DisplayCurrency.CONVERTED) {
            DisplayCurrency.KRW
        } else {
            currentState.displayCurrency
        }
        _uiState.value = UiState.Success(
            currentState.copy(
                products = updatedProducts,
                summary = computeSummary(updatedProducts),
                convertedAvailable = newConvertedAvailable,
                displayCurrency = newDisplayCurrency
            )
        )

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
