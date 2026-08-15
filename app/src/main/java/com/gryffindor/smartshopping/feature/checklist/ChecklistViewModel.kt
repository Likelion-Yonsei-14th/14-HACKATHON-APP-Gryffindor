package com.gryffindor.smartshopping.feature.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.model.ChecklistItem
import com.gryffindor.smartshopping.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val items: List<ChecklistItem> = emptyList(),
    val checkedIds: Set<String> = emptySet()
)

class ChecklistViewModel(
    private val checklistRepository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ChecklistUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ChecklistUiState>> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    fun loadChecklist(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val items = checklistRepository.getRefundChecklist(sessionId)
                _uiState.value = UiState.Success(ChecklistUiState(items = items))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "체크리스트를 불러올 수 없습니다.")
            }
        }
    }

    fun toggleChecked(itemId: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        val newCheckedIds = if (itemId in current.checkedIds) {
            current.checkedIds - itemId
        } else {
            current.checkedIds + itemId
        }
        _uiState.value = UiState.Success(current.copy(checkedIds = newCheckedIds))
    }

    fun retry() {
        currentSessionId?.let { loadChecklist(it) }
    }

    class Factory(
        private val checklistRepository: ChecklistRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChecklistViewModel(checklistRepository) as T
        }
    }
}
