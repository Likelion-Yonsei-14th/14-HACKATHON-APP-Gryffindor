package com.gryffindor.smartshopping.feature.travel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.repository.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TravelUiState(
    val airportCode: String = "",
    val flightNumber: String = "",
    val arrivalTime: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null
)

class TravelViewModel(
    private val travelRepository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TravelUiState())
    val uiState: StateFlow<TravelUiState> = _uiState.asStateFlow()

    fun updateAirportCode(code: String) {
        _uiState.update { it.copy(airportCode = code) }
    }

    fun updateFlightNumber(number: String) {
        _uiState.update { it.copy(flightNumber = number) }
    }

    fun updateArrivalTime(time: String) {
        _uiState.update { it.copy(arrivalTime = time) }
    }

    fun submitTravel(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                travelRepository.submitTravel(
                    sessionId = sessionId,
                    airportCode = _uiState.value.airportCode,
                    flightNumber = _uiState.value.flightNumber,
                    airportArrivalAt = _uiState.value.arrivalTime
                )
                _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "출국 정보 제출에 실패했습니다."
                    )
                }
            }
        }
    }

    class Factory(
        private val travelRepository: TravelRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TravelViewModel(travelRepository) as T
        }
    }
}
