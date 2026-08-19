package com.gryffindor.smartshopping.feature.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.location.LocationProvider
import com.gryffindor.smartshopping.domain.model.Trip
import com.gryffindor.smartshopping.domain.model.TripFeed
import com.gryffindor.smartshopping.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- UI State ---

data class FeedUiState(
    val isLoadingTrips: Boolean = false,
    val isLoadingFeed: Boolean = false,
    val trips: List<Trip> = emptyList(),
    val selectedTrip: Trip? = null,
    val feed: TripFeed? = null,
    val currentLocationAvailable: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = feed != null && feed.recommendations.isEmpty()

    val isSuccess: Boolean
        get() = feed != null && feed.recommendations.isNotEmpty()
}

// --- ViewModel ---

class FeedViewModel(
    private val tripRepository: TripRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    companion object {
        private const val TAG = "FeedViewModel"
    }

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Prevent duplicate feed requests for the same trip
    private var lastLoadedTripId: String? = null

    /**
     * Load trips and automatically fetch feed for the first trip.
     * Called once when the Home screen enters composition.
     */
    fun loadTripsAndFeed() {
        // Prevent re-loading if already loaded
        if (_uiState.value.trips.isNotEmpty() || _uiState.value.isLoadingTrips) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTrips = true, error = null) }
            try {
                val trips = tripRepository.getTrips()
                val firstTrip = trips.firstOrNull()
                _uiState.update {
                    it.copy(
                        trips = trips,
                        selectedTrip = firstTrip,
                        isLoadingTrips = false
                    )
                }
                // Auto-load feed for first trip
                if (firstTrip != null) {
                    loadFeed(firstTrip.id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load trips", e)
                _uiState.update {
                    it.copy(
                        isLoadingTrips = false,
                        error = "여행 정보를 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    /**
     * Load feed for a specific trip. Fetches current location if available.
     */
    fun loadFeed(tripId: String) {
        // Prevent duplicate requests for the same trip
        if (lastLoadedTripId == tripId && _uiState.value.feed != null) return
        if (_uiState.value.isLoadingFeed) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFeed = true, error = null, feed = null) }

            try {
                // One-shot location (returns null if permission denied or unavailable)
                val location = locationProvider.getCurrentLocation()
                val locationAvailable = location != null

                Log.d(TAG, "Fetching feed for trip=$tripId, location=$location")

                val feed = tripRepository.getTripFeed(
                    tripId = tripId,
                    latitude = location?.latitude,
                    longitude = location?.longitude
                )

                lastLoadedTripId = tripId
                _uiState.update {
                    it.copy(
                        isLoadingFeed = false,
                        feed = feed,
                        currentLocationAvailable = locationAvailable
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load feed for trip=$tripId", e)
                _uiState.update {
                    it.copy(
                        isLoadingFeed = false,
                        error = "추천을 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    /**
     * Retry loading feed for the currently selected trip.
     */
    fun retry() {
        lastLoadedTripId = null
        val tripId = _uiState.value.selectedTrip?.id ?: return
        loadFeed(tripId)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val tripRepository: TripRepository,
        private val locationProvider: LocationProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedViewModel(tripRepository, locationProvider) as T
        }
    }
}
