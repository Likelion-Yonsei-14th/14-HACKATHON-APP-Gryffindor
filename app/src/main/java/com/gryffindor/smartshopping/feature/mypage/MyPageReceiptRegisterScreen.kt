package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 마이페이지 > RECEIPT > "+" — 영수증 사진을 찍어 매장 정보를 등록한다.
 * Figma 마이페이지_Receipt_영수증 등록(376:5433) 기준. 실제 카메라 촬영/OCR 연동은
 * 아직 없고, [onRetakePhotoClick]만 콜백으로 열어둔 상태(화면 우선 제작 단계).
 */
@Composable
fun MyPageReceiptRegisterScreen(
    onBackClick: () -> Unit,
    onRetakePhotoClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiptPhotoScreen(
        title = stringResource(R.string.mypage_receipt_register_guide),
        onBackClick = onBackClick,
        onRetakePhotoClick = onRetakePhotoClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun MyPageReceiptRegisterScreenPreview() {
    LooketTheme {
        MyPageReceiptRegisterScreen(
            onBackClick = {},
            onRetakePhotoClick = {},
            selectedTab = BottomNavTab.MY_PAGE,
            onTabSelected = {},
        )
    }
}
