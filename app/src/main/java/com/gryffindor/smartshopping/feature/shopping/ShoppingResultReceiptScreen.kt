package com.gryffindor.smartshopping.feature.shopping

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketReceiptPhotoScreen
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import kotlinx.coroutines.launch

/**
 * 쇼핑 결과 기록 > 영수증 등록 — 실시간 쇼핑 종료(문 버튼) 직후, 리뷰 화면 진입 전.
 * Figma 쇼핑 결과 기록_영수증 등록(376:5047) 기준.
 *
 * PersonalizationRepository.analyzeReceipt(imageBytes, tripId)의 tripId는 선택값이라 —
 * 쇼핑 세션은 특정 여행(Trip)에 묶여 있지 않으므로 tripId 없이 호출한다. 실제 사진 선택/OCR
 * 연결은 AppNavGraph에서 한다(Routes.SHOPPING_RECEIPT) — 이 화면 자체는 다른 화면들과
 * 마찬가지로 "그냥 데이터를 보여주는" 뷰다.
 */
@Composable
fun ShoppingResultReceiptScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    onSkipClick: (() -> Unit)? = null,
) {
    LooketReceiptPhotoScreen(
        title = stringResource(R.string.shopping_result_receipt_guide),
        buttonText = stringResource(R.string.shopping_result_register),
        onBackClick = onBackClick,
        onButtonClick = onRegisterClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
        onSkipClick = onSkipClick,
    )
}

/** 영수증 사진을 골라 [PersonalizationRepository.analyzeReceipt]로 OCR 등록하는 백그라운드 작업. */
class ShoppingReceiptViewModel(
    private val personalizationRepository: PersonalizationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ShoppingReceiptVM"
    }

    fun analyzeReceipt(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val receipt = personalizationRepository.analyzeReceipt(imageBytes)
                Log.d(TAG, "Receipt analyzed: ${receipt.items.size} items, store=${receipt.storeName}")
            } catch (e: Exception) {
                Log.w(TAG, "Receipt analysis failed", e)
            }
        }
    }

    class Factory(
        private val personalizationRepository: PersonalizationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingReceiptViewModel(personalizationRepository) as T
        }
    }
}
