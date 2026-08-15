package com.gryffindor.smartshopping.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.model.Recommendation
import com.gryffindor.smartshopping.domain.model.RecommendationType
import com.gryffindor.smartshopping.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecommendationUiState(
    val crossSellItems: List<Recommendation> = emptyList(),
    val reminderItems: List<Recommendation> = emptyList()
)

class RecommendationViewModel(
    private val recommendationRepository: RecommendationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<RecommendationUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<RecommendationUiState>> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    fun loadRecommendations(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val recommendations = recommendationRepository.getRecommendations(sessionId)
                val crossSell = recommendations.filter { it.type == RecommendationType.CROSS_SELL }
                val reminder = recommendations.filter { it.type == RecommendationType.REMINDER }
                _uiState.value = UiState.Success(
                    RecommendationUiState(
                        crossSellItems = crossSell,
                        reminderItems = reminder
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "추천 목록을 불러올 수 없습니다.")
            }
        }
    }

    fun retry() {
        currentSessionId?.let { loadRecommendations(it) }
    }

    class Factory(
        private val recommendationRepository: RecommendationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecommendationViewModel(recommendationRepository) as T
        }
    }
}
