package com.gryffindor.smartshopping.feature.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.model.Flight
import com.gryffindor.smartshopping.domain.model.HotelStay
import com.gryffindor.smartshopping.domain.model.Purchase
import com.gryffindor.smartshopping.domain.model.RefundChecklist
import com.gryffindor.smartshopping.domain.model.RefundMethod
import com.gryffindor.smartshopping.domain.model.Trip
import com.gryffindor.smartshopping.domain.model.TripDetail
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import com.gryffindor.smartshopping.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UI State ---

data class TripListUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TripCreateUiState(
    val title: String = "",
    val city: String = "",
    val country: String = "",
    val startsAt: String = "",
    val endsAt: String = "",
    val isCreating: Boolean = false,
    val createdTripId: String? = null,
    val error: String? = null
)

data class TripDetailUiState(
    val tripDetail: TripDetail? = null,
    val isLoading: Boolean = false,
    val isAnalyzingFlight: Boolean = false,
    val isSavingFlight: Boolean = false,
    val isSavingHotel: Boolean = false,
    val error: String? = null,
    val flightError: String? = null,
    val hotelError: String? = null,
    val flightSaved: Boolean = false,
    val hotelSaved: Boolean = false,
    // B7 Refund Flow
    val purchases: List<Purchase> = emptyList(),
    val isLoadingPurchases: Boolean = false,
    val isAnalyzingReceipt: Boolean = false,
    val receiptMessage: String? = null,
    val updatingRefundMethodIds: Set<String> = emptySet(),
    val refundChecklist: RefundChecklist? = null,
    val isLoadingRefundChecklist: Boolean = false,
    val refundChecklistError: String? = null,
    val checkedChecklistIds: Set<String> = emptySet()
)

// --- ViewModel ---

class TripViewModel(
    private val tripRepository: TripRepository,
    private val personalizationRepository: PersonalizationRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(TripListUiState())
    val listState: StateFlow<TripListUiState> = _listState.asStateFlow()

    private val _createState = MutableStateFlow(TripCreateUiState())
    val createState: StateFlow<TripCreateUiState> = _createState.asStateFlow()

    private val _detailState = MutableStateFlow(TripDetailUiState())
    val detailState: StateFlow<TripDetailUiState> = _detailState.asStateFlow()

    // ========== Trip List ==========

    fun loadTrips() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }
            try {
                val trips = tripRepository.getTrips()
                _listState.update { it.copy(trips = trips, isLoading = false) }
            } catch (e: Exception) {
                _listState.update {
                    it.copy(isLoading = false, error = e.message ?: "여행 목록을 불러오지 못했습니다.")
                }
            }
        }
    }

    // ========== Trip Create ==========

    fun updateCreateTitle(value: String) {
        _createState.update { it.copy(title = value) }
    }

    fun updateCreateCity(value: String) {
        _createState.update { it.copy(city = value) }
    }

    fun updateCreateCountry(value: String) {
        _createState.update { it.copy(country = value) }
    }

    fun updateCreateStartsAt(value: String) {
        _createState.update { it.copy(startsAt = value) }
    }

    fun updateCreateEndsAt(value: String) {
        _createState.update { it.copy(endsAt = value) }
    }

    fun createTrip() {
        val state = _createState.value
        if (state.title.isBlank()) {
            _createState.update { it.copy(error = "여행 이름을 입력해주세요.") }
            return
        }
        viewModelScope.launch {
            _createState.update { it.copy(isCreating = true, error = null) }
            try {
                val trip = tripRepository.createTrip(
                    title = state.title,
                    destinationCity = state.city.ifBlank { null },
                    destinationCountry = state.country.ifBlank { null },
                    startsAt = state.startsAt.ifBlank { null },
                    endsAt = state.endsAt.ifBlank { null }
                )
                _createState.update { it.copy(isCreating = false, createdTripId = trip.id) }
            } catch (e: Exception) {
                _createState.update {
                    it.copy(isCreating = false, error = e.message ?: "여행 생성에 실패했습니다.")
                }
            }
        }
    }

    fun resetCreateState() {
        _createState.value = TripCreateUiState()
    }

    // ========== Trip Detail ==========

    fun loadTripDetail(tripId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            try {
                val detail = tripRepository.getTrip(tripId)
                _detailState.update { it.copy(tripDetail = detail, isLoading = false) }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(isLoading = false, error = e.message ?: "여행 상세를 불러오지 못했습니다.")
                }
            }
        }
        // Load B7 data in parallel
        loadPurchases(tripId)
        loadRefundChecklist(tripId)
    }

    // ========== Flight - Analyze (OCR) ==========

    fun analyzeFlight(imageBytes: ByteArray, tripId: String) {
        if (_detailState.value.isAnalyzingFlight) return // prevent duplicate
        viewModelScope.launch {
            _detailState.update { it.copy(isAnalyzingFlight = true, flightError = null) }
            try {
                val flight = personalizationRepository.analyzeFlight(imageBytes, tripId)
                // Refresh the full detail to get updated flights list
                val detail = tripRepository.getTrip(tripId)
                _detailState.update {
                    it.copy(tripDetail = detail, isAnalyzingFlight = false)
                }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isAnalyzingFlight = false,
                        flightError = e.message ?: "항공권 분석에 실패했습니다."
                    )
                }
            }
        }
    }

    // ========== Flight - Update ==========

    fun updateFlight(
        flightId: String,
        tripId: String,
        departureAirport: String?,
        arrivalAirport: String?,
        terminal: String?,
        flightNumber: String?,
        departureAt: String?,
        arrivalAt: String?,
        airportArrivalAt: String?
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isSavingFlight = true, flightError = null, flightSaved = false) }
            try {
                personalizationRepository.updateFlight(
                    flightId = flightId,
                    departureAirport = departureAirport?.ifBlank { null },
                    arrivalAirport = arrivalAirport?.ifBlank { null },
                    terminal = terminal?.ifBlank { null },
                    flightNumber = flightNumber?.ifBlank { null },
                    departureAt = departureAt?.ifBlank { null },
                    arrivalAt = arrivalAt?.ifBlank { null },
                    airportArrivalAt = airportArrivalAt?.ifBlank { null }
                )
                // Refresh detail
                val detail = tripRepository.getTrip(tripId)
                _detailState.update {
                    it.copy(tripDetail = detail, isSavingFlight = false, flightSaved = true)
                }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isSavingFlight = false,
                        flightError = e.message ?: "항공편 수정에 실패했습니다."
                    )
                }
            }
        }
    }

    fun clearFlightSaved() {
        _detailState.update { it.copy(flightSaved = false) }
    }

    // ========== Hotel - Upsert ==========

    fun upsertHotel(
        tripId: String,
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isSavingHotel = true, hotelError = null, hotelSaved = false) }
            try {
                tripRepository.upsertHotel(
                    tripId = tripId,
                    name = name,
                    address = address?.ifBlank { null },
                    latitude = latitude,
                    longitude = longitude
                )
                // Refresh detail
                val detail = tripRepository.getTrip(tripId)
                _detailState.update {
                    it.copy(tripDetail = detail, isSavingHotel = false, hotelSaved = true)
                }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isSavingHotel = false,
                        hotelError = e.message ?: "숙소 저장에 실패했습니다."
                    )
                }
            }
        }
    }

    fun clearHotelSaved() {
        _detailState.update { it.copy(hotelSaved = false) }
    }

    // ========== Visit Reservation ==========

    fun cancelReservation(reservationId: String, tripId: String) {
        viewModelScope.launch {
            try {
                tripRepository.cancelVisitReservation(reservationId)
                // Refresh detail to get updated reservation statuses
                val detail = tripRepository.getTrip(tripId)
                _detailState.update { it.copy(tripDetail = detail) }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(error = e.message ?: "예약 취소에 실패했습니다.")
                }
            }
        }
    }

    fun resetDetailState() {
        _detailState.value = TripDetailUiState()
    }

    // ========== B7: Purchases ==========

    private fun loadPurchases(tripId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoadingPurchases = true) }
            try {
                val allPurchases = personalizationRepository.getPurchases()
                val tripPurchases = allPurchases.filter { it.tripId == tripId }
                _detailState.update { it.copy(purchases = tripPurchases, isLoadingPurchases = false) }
            } catch (e: Exception) {
                _detailState.update { it.copy(isLoadingPurchases = false) }
            }
        }
    }

    // ========== B7: Receipt OCR ==========

    fun analyzeReceipt(imageBytes: ByteArray, tripId: String) {
        if (_detailState.value.isAnalyzingReceipt) return
        viewModelScope.launch {
            _detailState.update { it.copy(isAnalyzingReceipt = true, receiptMessage = null) }
            try {
                personalizationRepository.analyzeReceipt(imageBytes, tripId)
                _detailState.update { it.copy(isAnalyzingReceipt = false, receiptMessage = "영수증이 등록되었습니다.") }
                // Refresh purchases and checklist
                loadPurchases(tripId)
                loadRefundChecklist(tripId)
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isAnalyzingReceipt = false,
                        receiptMessage = "영수증 분석에 실패했습니다."
                    )
                }
            }
        }
    }

    fun clearReceiptMessage() {
        _detailState.update { it.copy(receiptMessage = null) }
    }

    // ========== B7: Refund Method ==========

    fun updateRefundMethod(purchaseId: String, tripId: String, refundMethod: RefundMethod) {
        if (purchaseId in _detailState.value.updatingRefundMethodIds) return
        viewModelScope.launch {
            _detailState.update { it.copy(updatingRefundMethodIds = it.updatingRefundMethodIds + purchaseId) }
            try {
                personalizationRepository.updatePurchaseRefundMethod(purchaseId, refundMethod)
                // Refresh purchases and checklist
                loadPurchases(tripId)
                loadRefundChecklist(tripId)
            } catch (_: Exception) {
                // Silent fail — UI will remain with old value
            } finally {
                _detailState.update { it.copy(updatingRefundMethodIds = it.updatingRefundMethodIds - purchaseId) }
            }
        }
    }

    // ========== B7: Refund Checklist ==========

    private fun loadRefundChecklist(tripId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoadingRefundChecklist = true, refundChecklistError = null) }
            try {
                val checklist = tripRepository.getRefundChecklist(tripId)
                _detailState.update {
                    // Remove checked IDs that no longer exist in the response
                    val validIds = checklist.items.map { item -> item.id }.toSet()
                    it.copy(
                        refundChecklist = checklist,
                        isLoadingRefundChecklist = false,
                        checkedChecklistIds = it.checkedChecklistIds.intersect(validIds)
                    )
                }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isLoadingRefundChecklist = false,
                        refundChecklistError = "체크리스트를 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun toggleChecklistItem(itemId: String) {
        _detailState.update {
            val newChecked = if (itemId in it.checkedChecklistIds) {
                it.checkedChecklistIds - itemId
            } else {
                it.checkedChecklistIds + itemId
            }
            it.copy(checkedChecklistIds = newChecked)
        }
    }

    // ========== Factory ==========

    class Factory(
        private val tripRepository: TripRepository,
        private val personalizationRepository: PersonalizationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripViewModel(tripRepository, personalizationRepository) as T
        }
    }
}
