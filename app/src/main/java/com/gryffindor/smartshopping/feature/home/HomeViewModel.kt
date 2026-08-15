package com.gryffindor.smartshopping.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val sessionId: String? = null,
    val isSessionActive: Boolean = false,
    val isStarting: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val sessionRepository: SessionRepository,
    private val cameraFrameProvider: CameraFrameProvider
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun startShopping() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            try {
                val session = sessionRepository.createSession("KRW")
                _uiState.update {
                    it.copy(
                        sessionId = session.sessionId,
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSessionNavigation() {
        _uiState.update { it.copy(sessionId = null, isSessionActive = false) }
    }

    class Factory(
        private val sessionRepository: SessionRepository,
        private val cameraFrameProvider: CameraFrameProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(sessionRepository, cameraFrameProvider) as T
        }
    }
}
