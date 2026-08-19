package com.gryffindor.smartshopping.feature.trip

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
 * Handles the flight-based trip registration flow:
 * 1. Select flight image -> OCR analyze
 * 2. Display analyzed flight for user confirmation/edit
 * 3. On confirm -> create Trip -> attach Flight to Trip
 *
 * Unlike OnboardingViewModel, this does NOT create a Trip before user confirms.
 */
class TripRegistrationViewModel(
    private val personalizationRepository: PersonalizationRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripRegistrationUiState>(TripRegistrationUiState.Idle)
    val uiState: StateFlow<TripRegistrationUiState> = _uiState.asStateFlow()

    private var analyzedFlight: Flight? = null

    /**
     * Step 1: OCR the flight ticket image.
     * Does NOT create a Trip — only analyzes and holds the Flight data.
     */
    fun analyzeFlight(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = TripRegistrationUiState.Analyzing

            try {
                val flight = personalizationRepository.analyzeFlight(imageBytes, tripId = null)
                analyzedFlight = flight
                Log.d(TAG, "analyzeFlight success: flightId=${flight.id}")
                _uiState.value = TripRegistrationUiState.FlightReady(flight)
            } catch (e: Exception) {
                Log.e(TAG, "analyzeFlight failed", e)
                _uiState.value = TripRegistrationUiState.Error(
                    e.message ?: "항공권 분석에 실패했습니다."
                )
            }
        }
    }

    /**
     * Step 2: User confirmed the flight data.
     * Now we create the Trip and attach the Flight.
     */
    fun confirmAndCreateTrip() {
        val flight = analyzedFlight ?: return
        viewModelScope.launch {
            _uiState.value = TripRegistrationUiState.Creating

            try {
                // Generate trip title from flight data
                val tripTitle = flight.arrivalAirport?.let { "$it 여행" }
                    ?: flight.flightNumber?.let { "$it 여행" }
                    ?: "내 여행"

                // Create Trip
                val trip = tripRepository.createTrip(
                    title = tripTitle,
                    destinationCity = null,
                    destinationCountry = null,
                    startsAt = flight.departureAt,
                    endsAt = flight.arrivalAt
                )
                Log.d(TAG, "createTrip success: tripId=${trip.id}")

                // Attach flight to trip
                personalizationRepository.updateFlight(
                    flightId = flight.id,
                    tripId = trip.id
                )
                Log.d(TAG, "updateFlight success: flight ${flight.id} attached to trip ${trip.id}")

                _uiState.value = TripRegistrationUiState.Completed(flight, trip)
            } catch (e: Exception) {
                Log.e(TAG, "confirmAndCreateTrip failed", e)
                _uiState.value = TripRegistrationUiState.Error(
                    e.message ?: "여행 생성에 실패했습니다."
                )
            }
        }
    }

    fun getAnalyzedFlight(): Flight? = analyzedFlight

    fun resetState() {
        _uiState.value = TripRegistrationUiState.Idle
        analyzedFlight = null
    }

    class Factory(
        private val personalizationRepository: PersonalizationRepository,
        private val tripRepository: TripRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripRegistrationViewModel(personalizationRepository, tripRepository) as T
        }
    }

    companion object {
        private const val TAG = "TripRegistrationVM"
    }
}

sealed interface TripRegistrationUiState {
    data object Idle : TripRegistrationUiState
    data object Analyzing : TripRegistrationUiState
    data class FlightReady(val flight: Flight) : TripRegistrationUiState
    data object Creating : TripRegistrationUiState
    data class Completed(val flight: Flight, val trip: Trip) : TripRegistrationUiState
    data class Error(val message: String) : TripRegistrationUiState
}
