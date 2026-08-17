package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketStore
import com.gryffindor.smartshopping.core.ui.component.LooketStoreCard
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

// 실제 매장 검색/등록 API 연결 전까지의 더미 목록. "+" 매장 직접추가 버튼은 이번 단계에서 제외.
private val dummyStores = listOf(
    LooketStore(
        id = "1",
        name = "MCM 신세계백화점 본점",
        address = "서울특별시 중구 퇴계로 77 9F 신세계면세점 본점",
    ),
    LooketStore(
        id = "2",
        name = "MCM HAUS",
        address = "서울 강남구 압구정로 412 MCM HAUS",
    ),
    LooketStore(
        id = "3",
        name = "MCM 여의도 더현대점",
        address = "서울 영등포구 여의대로 108 더현대서울 4F",
    ),
)

/**
 * 마이페이지 > RECEIPT — 영수증에 연결할 매장을 선택한다.
 * Figma 마이페이지_Receipt_매장 선택/추가(376:5408) 기준, "+" 매장 직접추가 버튼은 제외.
 */
@Composable
fun MyPageReceiptScreen(
    selectedStoreId: String?,
    onStoreSelected: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LooketTopBar(title = stringResource(R.string.mypage_receipt), onBackClick = onBackClick)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.mypage_receipt_select_store),
                    style = LooketTextStyles.titleTwo,
                    color = LooketColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
                Spacer(Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    dummyStores.forEach { store ->
                        LooketStoreCard(
                            store = store,
                            selected = store.id == selectedStoreId,
                            onClick = { onStoreSelected(store.id) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            LooketPrimaryButton(
                text = stringResource(R.string.common_confirm),
                enabled = selectedStoreId != null,
                onClick = onConfirmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun MyPageReceiptScreenPreview() {
    var selectedStoreId by remember { mutableStateOf<String?>(null) }
    LooketTheme {
        MyPageReceiptScreen(
            selectedStoreId = selectedStoreId,
            onStoreSelected = { selectedStoreId = it },
            onConfirmClick = {},
            onBackClick = {},
            selectedTab = BottomNavTab.MY_PAGE,
            onTabSelected = {},
        )
    }
}
