package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketSectionHeader
import com.gryffindor.smartshopping.core.ui.theme.LooketColors

/**
 * [MyPageReceiptRegisterScreen](새 영수증 등록, Figma 376:5433)와
 * [MyPageReceiptViewScreen](매장 선택 후 기존 영수증 보기, Figma 376:5422)이
 * 공유하는 레이아웃 — 제목만 다르고 사진 placeholder/재촬영 버튼은 동일하다.
 */
@Composable
internal fun ReceiptPhotoScreen(
    title: String,
    onBackClick: () -> Unit,
    onRetakePhotoClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = { BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(52.dp))
            LooketSectionHeader(title = title, onBackClick = onBackClick)
            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(517.dp)
                        .background(LooketColors.SurfaceEmphasized),
                )
                Spacer(Modifier.height(24.dp))
                LooketPrimaryButton(
                    text = stringResource(R.string.mypage_receipt_retake_photo),
                    onClick = onRetakePhotoClick,
                )
            }
        }
    }
}
