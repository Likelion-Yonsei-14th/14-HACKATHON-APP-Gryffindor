package com.gryffindor.smartshopping.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.camera.GlassesUpdateResult
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.model.FeedRecommendation
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.PurchasedProduct
import com.gryffindor.smartshopping.domain.model.RefundChecklist
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val sessionId: String? = null,
    // 쇼핑 화면으로 이동하자마자 sessionId는 resetSessionNavigation()으로 null이 되므로,
    // 홈에 머무는 동안에도 체크리스트로 이동할 수 있도록 가장 최근 세션 ID를 따로 보관한다.
    val lastSessionId: String? = null,
    val isSessionActive: Boolean = false,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
    val datUpdateRequired: Boolean = false,
    val datUpdateError: String? = null,
    // Backend-driven home data
    val purchasedProducts: List<PurchasedProduct> = emptyList(),
    val recommendations: List<FeedRecommendation> = emptyList(),
    val wishlistProducts: List<Product> = emptyList(),
    val refundChecklist: RefundChecklist? = null,
    // FOR YOU 추천에서 상품을 눌러 방문 예약으로 들어갈 때 필요 — 유저의 첫 번째 여행.
    // 여러 여행 중 하나를 고르는 UI는 아직 없어서 첫 번째 여행으로 고정.
    val currentTripId: String? = null,
    val isChecklistLoading: Boolean = false,
    val isHomeDataLoading: Boolean = false,
    val homeDataLoaded: Boolean = false,
)

class HomeViewModel(
    private val sessionRepository: SessionRepository,
    private val cameraFrameProvider: CameraFrameProvider,
    private val personalizationRepository: PersonalizationRepository,
    private val tripRepository: TripRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe camera state to detect DAT update requirement
        viewModelScope.launch {
            cameraFrameProvider.cameraState.collect { state ->
                when (state) {
                    is CameraState.DatUpdateRequired -> {
                        _uiState.update {
                            it.copy(
                                datUpdateRequired = true,
                                datUpdateError = null,
                                errorMessage = state.message
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(datUpdateRequired = false, datUpdateError = null)
                        }
                    }
                }
            }
        }

        // Load real Backend data on creation
        loadHomeData()
    }

    /**
     * Fetches user's purchased products and trip feed recommendations from Backend.
     * Called on init and can be re-called to refresh (e.g. after returning from Review).
     */
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHomeDataLoading = true, isChecklistLoading = true) }

            // Fetch MyPage (purchased products + trips list)
            var purchasedProducts: List<PurchasedProduct> = emptyList()
            var tripId: String? = null
            try {
                val myPage = personalizationRepository.getMyPage()
                purchasedProducts = myPage.purchasedProducts
                // Use the first available trip for feed recommendations
                tripId = myPage.trips.firstOrNull()?.id
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load MyPage data", e)
            }

            // Fallback: if MyPage didn't return trips, try fetching trips directly
            if (tripId == null) {
                try {
                    tripId = tripRepository.getTrips().firstOrNull()?.id
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load trips as fallback", e)
                }
            }

            // Fetch trip feed recommendations if we have a tripId
            var recommendations: List<FeedRecommendation> = emptyList()
            if (tripId != null) {
                try {
                    val feed = tripRepository.getTripFeed(tripId)
                    recommendations = feed.recommendations
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load trip feed", e)
                }
            }

            // Fetch refund checklist if we have a tripId
            var refundChecklist: RefundChecklist? = null
            if (tripId != null) {
                try {
                    refundChecklist = tripRepository.getRefundChecklist(tripId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load refund checklist", e)
                }
            }

            // Fetch wishlist for the MY LOOKET "관심" section
            val wishlistProducts = try {
                personalizationRepository.getWishlist()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load wishlist", e)
                emptyList()
            }

            _uiState.update {
                it.copy(
                    purchasedProducts = purchasedProducts,
                    recommendations = recommendations,
                    wishlistProducts = wishlistProducts,
                    refundChecklist = refundChecklist,
                    currentTripId = tripId,
                    isChecklistLoading = false,
                    isHomeDataLoading = false,
                    homeDataLoaded = true,
                )
            }
        }
    }

    /**
     * MY LOOKET "관심" 카드의 제거 버튼에서 호출. 낙관적으로 로컬 상태에서 먼저 지우고,
     * 실패하면 다음 loadHomeData() 새로고침 때 다시 나타난다.
     */
    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(wishlistProducts = it.wishlistProducts.filterNot { p -> p.productId == productId }) }
            try {
                personalizationRepository.deleteWishlist(productId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove $productId from wishlist", e)
                loadHomeData()
            }
        }
    }

    fun startShopping(storeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            try {
                // TODO: Backend는 USD/CNY만 허용. 통화 선택 UI 연결 시 교체 필요.
                val session = sessionRepository.createSession("USD", storeId)
                _uiState.update {
                    it.copy(
                        sessionId = session.sessionId,
                        lastSessionId = session.sessionId,
                        isSessionActive = true,
                        isStarting = false
                    )
                }

                // Camera startup is additive — failure does NOT roll back the session.
                // Camera errors are observable through CameraState, not HomeUiState.
                launch {
                    try {
                        cameraFrameProvider.startCamera()
                    } catch (e: Exception) {
                        Log.w(TAG, "Camera start failed (shopping session unaffected)", e)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStarting = false,
                        errorMessage = e.message ?: "세션 생성에 실패했습니다."
                    )
                }
            }
        }
    }

    fun requestGlassesUpdate() {
        viewModelScope.launch {
            val result = cameraFrameProvider.openGlassesUpdate()
            when (result) {
                is GlassesUpdateResult.Success -> {
                    Log.i(TAG, "Glasses update flow launched successfully")
                    // User will update externally, then tap retry
                }
                is GlassesUpdateResult.Failed -> {
                    Log.w(TAG, "Glasses update flow failed: ${result.reason}")
                    _uiState.update { it.copy(datUpdateError = result.reason) }
                }
                is GlassesUpdateResult.Unsupported -> {
                    Log.w(TAG, "Glasses update not supported")
                    _uiState.update {
                        it.copy(datUpdateError = "업데이트 기능을 사용할 수 없습니다")
                    }
                }
            }
        }
    }

    fun retryCamera() {
        viewModelScope.launch {
            _uiState.update { it.copy(datUpdateRequired = false, datUpdateError = null) }
            try {
                cameraFrameProvider.startCamera()
            } catch (e: Exception) {
                Log.w(TAG, "Camera retry failed", e)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, datUpdateError = null) }
    }

    fun resetSessionNavigation() {
        _uiState.update { it.copy(sessionId = null, isSessionActive = false) }
    }

    class Factory(
        private val sessionRepository: SessionRepository,
        private val cameraFrameProvider: CameraFrameProvider,
        private val personalizationRepository: PersonalizationRepository,
        private val tripRepository: TripRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(sessionRepository, cameraFrameProvider, personalizationRepository, tripRepository) as T
        }
    }
}
