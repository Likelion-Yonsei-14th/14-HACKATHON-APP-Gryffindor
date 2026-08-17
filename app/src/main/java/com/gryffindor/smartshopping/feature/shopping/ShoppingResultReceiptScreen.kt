package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketReceiptPhotoScreen
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 쇼핑 결과 기록 > 영수증 등록 — 실시간 쇼핑 종료(문 버튼) 직후 진입.
 * Figma 쇼핑 결과 기록_영수증 등록(376:5047) 기준. 카메라 촬영/OCR 연동은 아직 없고,
 * [onRegisterClick]만 콜백으로 열어둔 상태(화면 우선 제작 단계).
 */
@Composable
fun ShoppingResultReceiptScreen(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LooketReceiptPhotoScreen(
        title = stringResource(R.string.shopping_result_receipt_guide),
        buttonText = stringResource(R.string.shopping_result_register),
        onBackClick = onBackClick,
        onButtonClick = onRegisterClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun ShoppingResultReceiptScreenPreview() {
    LooketTheme {
        ShoppingResultReceiptScreen(
            onBackClick = {},
            onRegisterClick = {},
            selectedTab = BottomNavTab.SHOP,
            onTabSelected = {},
        )
    }
}
