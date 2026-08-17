package com.gryffindor.smartshopping.core.ui.component

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
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.theme.LooketColors

/**
 * 큰 제목(매장명 또는 안내문) + 영수증 사진 placeholder + 하단 버튼 하나로 이루어진
 * 화면 템플릿. 마이페이지 영수증 등록/보기(Figma 376:5433, 376:5422)와 쇼핑 결과 기록
 * 영수증 등록(Figma 376:5047)이 전부 이 구조를 그대로 공유한다 — 제목과 버튼 텍스트만 다르다.
 */
@Composable
fun LooketReceiptPhotoScreen(
    title: String,
    buttonText: String,
    onBackClick: () -> Unit,
    onButtonClick: () -> Unit,
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
                    text = buttonText,
                    onClick = onButtonClick,
                )
            }
        }
    }
}
