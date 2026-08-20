package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketReceiptPhotoScreen

/**
 * 마이페이지 > RECEIPT > 매장 카드 확인 — 그 매장에서 등록한 영수증 사진을 보여준다.
 * Figma 마이페이지_Receipt_영수증 편집(376:5422) 기준(제목이 매장명이라는 점만
 * [MyPageReceiptRegisterScreen]과 다르고 레이아웃은 동일해 [LooketReceiptPhotoScreen]을 공유한다).
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
    LooketReceiptPhotoScreen(
        title = storeName,
        buttonText = stringResource(R.string.mypage_receipt_retake_photo),
        onBackClick = onBackClick,
        onButtonClick = onRetakePhotoClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
