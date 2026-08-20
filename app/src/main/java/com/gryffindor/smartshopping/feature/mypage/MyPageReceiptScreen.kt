package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketStore
import com.gryffindor.smartshopping.core.ui.component.LooketStoreSelectionScreen

/**
 * 마이페이지 > RECEIPT — 등록된 영수증(매장) 목록에서 하나를 선택한다.
 * "+"는 새 매장을 추가하는 버튼이 아니라 영수증을 새로 등록하는 버튼이다
 * ([onAddReceiptClick] -> MyPageReceiptRegisterScreen으로 이동, 카메라로 찍으면
 * 매장 정보가 인식되어 목록에 새 항목으로 추가되는 방식).
 * Figma 마이페이지_Receipt_매장 선택/추가(376:5408) 기준.
 */
@Composable
fun MyPageReceiptScreen(
    stores: List<LooketStore>,
    selectedStoreId: String?,
    onStoreSelected: (String) -> Unit,
    onAddReceiptClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LooketStoreSelectionScreen(
        title = stringResource(R.string.mypage_receipt),
        stores = stores,
        selectedStoreId = selectedStoreId,
        onStoreSelected = onStoreSelected,
        onConfirmClick = onConfirmClick,
        onBackClick = onBackClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
        onAddClick = onAddReceiptClick,
    )
}
