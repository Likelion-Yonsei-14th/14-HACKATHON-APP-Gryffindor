package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * "매장 선택하기" 화면 공용 템플릿 — 매장 카드 목록에서 하나를 고르면 보라 테두리로
 * 표시되고, 하단 네비게이션 바로 위에 확인 버튼이 팝업처럼 뜬다. 마이페이지 영수증
 * 매장 선택과 쇼핑-실시간 매장 선택이 이 구조를 그대로 공유한다(Figma 376:5408, 376:5275).
 * [onAddClick]이 null이 아니면 목록 아래에 "+" 버튼도 표시한다.
 */
@Composable
fun LooketStoreSelectionScreen(
    title: String,
    stores: List<LooketStore>,
    selectedStoreId: String?,
    onStoreSelected: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = selectedStoreId != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                ) {
                    LooketPrimaryButton(
                        text = stringResource(R.string.common_confirm),
                        onClick = onConfirmClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LooketColors.Surface)
                            .padding(16.dp),
                    )
                }
                BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LooketTopBar(title = title, onBackClick = onBackClick)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.store_selection_section_title),
                    style = LooketTextStyles.titleTwo,
                    color = LooketColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
                Spacer(Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    stores.forEach { store ->
                        LooketStoreCard(
                            store = store,
                            selected = store.id == selectedStoreId,
                            onClick = { onStoreSelected(store.id) },
                        )
                    }
                }
            }

            if (onAddClick != null) {
                Spacer(Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = stringResource(R.string.mypage_receipt_add),
                            tint = LooketColors.TextPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
