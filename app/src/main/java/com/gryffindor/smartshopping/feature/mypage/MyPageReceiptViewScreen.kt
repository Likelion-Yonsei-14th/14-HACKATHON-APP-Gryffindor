package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 마이페이지 > RECEIPT > 매장 카드 확인 — 그 매장에서 등록한 영수증 사진을 보여준다.
 * Figma 마이페이지_Receipt_영수증 편집(376:5422) 기준(제목이 매장명이라는 점만
 * [MyPageReceiptRegisterScreen]과 다르고 레이아웃은 동일해 [ReceiptPhotoScreen]을 공유한다).
 */
@Composable
fun MyPageReceiptViewScreen(
    storeName: String,
    onBackClick: () -> Unit,
    onRetakePhotoClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiptPhotoScreen(
        title = storeName,
        onBackClick = onBackClick,
        onRetakePhotoClick = onRetakePhotoClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun MyPageReceiptViewScreenPreview() {
    LooketTheme {
        MyPageReceiptViewScreen(
            storeName = "MCM 신세계백화점 본점",
            onBackClick = {},
            onRetakePhotoClick = {},
            selectedTab = BottomNavTab.MY_PAGE,
            onTabSelected = {},
        )
    }
}
