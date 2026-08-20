package com.gryffindor.smartshopping.feature.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.model.Flight
import com.gryffindor.smartshopping.domain.model.Trip
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import com.gryffindor.smartshopping.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates onboarding flow:
 * 1. Flight image → analyzeFlight() → get Flight
 * 2. Auto-create Trip from Flight data
 * 3. Attach Flight to Trip via updateFlight(tripId)
 */
class OnboardingViewModel(
    private val personalizationRepository: PersonalizationRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var analyzedFlight: Flight? = null
    private var createdTrip: Trip? = null

    fun createTripFromFlight(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading

            try {
                // Step 1: OCR the flight ticket
                val flight = personalizationRepository.analyzeFlight(imageBytes, tripId = null)
                analyzedFlight = flight
                Log.d(TAG, "analyzeFlight success: flightId=${flight.id} arrival=${flight.arrivalAirport}")

                // Step 2: Generate trip title from flight data
                val tripTitle = flight.arrivalAirport?.let { "$it 여행" }
                    ?: flight.flightNumber?.let { "$it 여행" }
                    ?: "내 여행"

                // Step 3: Create Trip
                val trip = tripRepository.createTrip(
                    title = tripTitle,
                    destinationCity = null,
                    destinationCountry = null,
                    startsAt = flight.departureAt,
                    endsAt = flight.arrivalAt
                )
                createdTrip = trip
                Log.d(TAG, "createTrip success: tripId=${trip.id} title=${trip.title}")

                // Step 4: Attach flight to trip
                personalizationRepository.updateFlight(
                    flightId = flight.id,
                    tripId = trip.id
                )
                Log.d(TAG, "updateFlight success: flight ${flight.id} attached to trip ${trip.id}")

                _uiState.value = OnboardingUiState.FlightAnalyzed(flight, trip)

            } catch (e: Exception) {
                Log.e(TAG, "createTripFromFlight failed", e)
                _uiState.value = OnboardingUiState.Error(
                    e.message ?: "항공권 분석에 실패했습니다."
                )
            }
        }
    }

    fun getAnalyzedFlight(): Flight? = analyzedFlight
    fun getCreatedTrip(): Trip? = createdTrip

    class Factory(
        private val personalizationRepository: PersonalizationRepository,
        private val tripRepository: TripRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(personalizationRepository, tripRepository) as T
        }
    }

    companion object {
        private const val TAG = "OnboardingViewModel"
    }
}

sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState
    data object Loading : OnboardingUiState
    data class FlightAnalyzed(val flight: Flight, val trip: Trip) : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}
