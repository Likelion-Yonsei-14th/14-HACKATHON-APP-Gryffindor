package com.gryffindor.smartshopping.feature.reservation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.model.Store
import com.gryffindor.smartshopping.domain.model.StoreWishlistProduct
import com.gryffindor.smartshopping.domain.model.VisitReservation
import com.gryffindor.smartshopping.domain.repository.StoreRepository
import com.gryffindor.smartshopping.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UI States ---

data class ReservationCreateUiState(
    val store: Store? = null,
    val isLoadingProducts: Boolean = false,
    val storeWishlistProducts: List<StoreWishlistProduct> = emptyList(),
    val selectedProductIds: Set<String> = emptySet(),
    val scheduledDate: String = "", // yyyy-MM-dd
    val scheduledTime: String = "", // HH:mm
    val isSubmitting: Boolean = false,
    val createdReservation: VisitReservation? = null,
    val error: String? = null
)

data class ReservationListUiState(
    val isLoading: Boolean = false,
    val reservations: List<VisitReservation> = emptyList(),
    val cancellingIds: Set<String> = emptySet(),
    val error: String? = null
)

// --- ViewModel ---

class VisitReservationViewModel(
    private val tripRepository: TripRepository,
    private val storeRepository: StoreRepository? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ReservationVM"
    }

    private val _createState = MutableStateFlow(ReservationCreateUiState())
    val createState: StateFlow<ReservationCreateUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow(ReservationListUiState())
    val listState: StateFlow<ReservationListUiState> = _listState.asStateFlow()

    // ========== Store Lookup ==========

    fun loadStore(storeId: String) {
        if (storeRepository == null) return
        viewModelScope.launch {
            try {
                val stores = storeRepository.getStores()
                val store = stores.find { it.id == storeId }
                _createState.update { it.copy(store = store) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load store info for $storeId", e)
            }
        }
    }

    // ========== Create Flow ==========

    fun loadStoreWishlistProducts(storeId: String) {
        viewModelScope.launch {
            _createState.update { it.copy(isLoadingProducts = true, error = null) }
            try {
                val products = tripRepository.getStoreWishlistProducts(storeId)
                _createState.update {
                    it.copy(
                        isLoadingProducts = false,
                        storeWishlistProducts = products
                    )
                }
                Log.d(TAG, "Store wishlist products loaded: ${products.size} for store=$storeId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load store wishlist products", e)
                _createState.update {
                    it.copy(
                        isLoadingProducts = false,
                        error = "매장 상품 정보를 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun toggleProductSelection(productId: String) {
        _createState.update { state ->
            val current = state.selectedProductIds
            val updated = if (current.contains(productId)) current - productId else current + productId
            state.copy(selectedProductIds = updated)
        }
    }

    fun updateScheduledDate(date: String) {
        _createState.update { it.copy(scheduledDate = date) }
    }

    fun updateScheduledTime(time: String) {
        _createState.update { it.copy(scheduledTime = time) }
    }

    fun createReservation(tripId: String, storeId: String) {
        val state = _createState.value
        if (state.isSubmitting) return

        if (state.scheduledDate.isBlank() || state.scheduledTime.isBlank()) {
            _createState.update { it.copy(error = "날짜와 시간을 선택해주세요.") }
            return
        }

        // Build ISO-8601 with system timezone offset
        val scheduledAt = buildScheduledAt(state.scheduledDate, state.scheduledTime)
        if (scheduledAt == null) {
            _createState.update { it.copy(error = "날짜/시간 형식이 올바르지 않습니다.") }
            return
        }

        viewModelScope.launch {
            _createState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val reservation = tripRepository.createVisitReservation(
                    tripId = tripId,
                    storeId = storeId,
                    scheduledAt = scheduledAt,
                    productIds = state.selectedProductIds.toList()
                )
                _createState.update {
                    it.copy(isSubmitting = false, createdReservation = reservation)
                }
                Log.d(TAG, "Reservation created: ${reservation.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create reservation", e)
                _createState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "예약 생성에 실패했습니다."
                    )
                }
            }
        }
    }

    fun resetCreateState() {
        _createState.value = ReservationCreateUiState()
    }

    // ========== List Flow ==========

    fun loadReservations(tripId: String) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }
            try {
                val reservations = tripRepository.getVisitReservations(tripId)
                _listState.update {
                    it.copy(isLoading = false, reservations = reservations)
                }
                Log.d(TAG, "Reservations loaded: ${reservations.size} for trip=$tripId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load reservations", e)
                _listState.update {
                    it.copy(
                        isLoading = false,
                        error = "예약 목록을 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun cancelReservation(reservationId: String, tripId: String) {
        if (_listState.value.cancellingIds.contains(reservationId)) return

        viewModelScope.launch {
            _listState.update { it.copy(cancellingIds = it.cancellingIds + reservationId) }
            try {
                tripRepository.cancelVisitReservation(reservationId)
                // Refresh list to get updated status
                val reservations = tripRepository.getVisitReservations(tripId)
                _listState.update {
                    it.copy(
                        reservations = reservations,
                        cancellingIds = it.cancellingIds - reservationId
                    )
                }
                Log.d(TAG, "Reservation cancelled: $reservationId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cancel reservation $reservationId", e)
                _listState.update {
                    it.copy(
                        cancellingIds = it.cancellingIds - reservationId,
                        error = "예약 취소에 실패했습니다."
                    )
                }
            }
        }
    }

    fun clearError() {
        _createState.update { it.copy(error = null) }
        _listState.update { it.copy(error = null) }
    }

    // --- Helpers ---

    private fun buildScheduledAt(date: String, time: String): String? {
        // date: yyyy-MM-dd, time: HH:mm
        return try {
            val offset = java.util.TimeZone.getDefault().let { tz ->
                val offsetMs = tz.getOffset(System.currentTimeMillis())
                val hours = offsetMs / 3600000
                val minutes = (Math.abs(offsetMs) % 3600000) / 60000
                String.format("%+03d:%02d", hours, minutes)
            }
            "${date}T${time}:00$offset"
        } catch (e: Exception) {
            null
        }
    }

    class Factory(
        private val tripRepository: TripRepository,
        private val storeRepository: StoreRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VisitReservationViewModel(tripRepository, storeRepository) as T
        }
    }
}
