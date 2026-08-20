package com.gryffindor.smartshopping.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.camera.GlassesUpdateResult
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.repository.SessionRepository
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
    val datUpdateError: String? = null
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
    }

    fun startShopping(storeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            try {
                val session = sessionRepository.createSession("KRW", storeId)
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
        private val cameraFrameProvider: CameraFrameProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(sessionRepository, cameraFrameProvider) as T
        }
    }
}
