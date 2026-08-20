package com.gryffindor.smartshopping.feature.reservation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketStore
import com.gryffindor.smartshopping.core.ui.component.LooketStoreSelectionScreen
import com.gryffindor.smartshopping.domain.model.Store
import com.gryffindor.smartshopping.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VisitReservationStoreSelectionUiState(
    val isLoading: Boolean = false,
    val stores: List<Store> = emptyList(),
    val selectedStoreId: String? = null,
    val error: String? = null,
)

/**
 * 방문 예약 > 매장 선택 전용 로더. [com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel]은
 * 실시간 쇼핑 세션 생성까지 같이 들고 있어서(currency/sessionRepository 필요) 이 화면엔
 * 안 맞아 별도로 둔다 — 여긴 매장 하나 골라서 다음 화면(정보 입력)으로 storeId/storeName만
 * 넘기면 된다.
 */
class VisitReservationStoreSelectionViewModel(
    private val storeRepository: StoreRepository
) : ViewModel() {

    companion object {
        private const val TAG = "VisitResStoreSelectVM"
    }

    private val _uiState = MutableStateFlow(VisitReservationStoreSelectionUiState())
    val uiState: StateFlow<VisitReservationStoreSelectionUiState> = _uiState.asStateFlow()

    fun loadStores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val stores = storeRepository.getStores()
                _uiState.update { it.copy(isLoading = false, stores = stores) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load stores", e)
                _uiState.update { it.copy(isLoading = false, error = "매장 목록을 불러오지 못했습니다.") }
            }
        }
    }

    fun selectStore(storeId: String) {
        _uiState.update { it.copy(selectedStoreId = storeId) }
    }

    class Factory(
        private val storeRepository: StoreRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VisitReservationStoreSelectionViewModel(storeRepository) as T
        }
    }
}

@Composable
fun VisitReservationStoreSelectionScreen(
    stores: List<LooketStore>,
    selectedStoreId: String?,
    onStoreSelected: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LooketStoreSelectionScreen(
        title = stringResource(R.string.reservation_title),
        stores = stores,
        selectedStoreId = selectedStoreId,
        onStoreSelected = onStoreSelected,
        onConfirmClick = onConfirmClick,
        onBackClick = onBackClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
