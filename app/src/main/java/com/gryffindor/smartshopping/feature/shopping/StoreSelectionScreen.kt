package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketStore
import com.gryffindor.smartshopping.core.ui.component.LooketStoreSelectionScreen
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

// 실제 매장 검색 API 연결 전까지의 더미 목록.
internal val dummyShoppingStores = listOf(
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
        name = "MCM 롯데백화점 본점",
        address = "서울 송파구 송파대로 521 롯데백화점잠실점 1F",
    ),
)

/**
 * 쇼핑 - 실시간 > 매장 선택 — 실시간 쇼핑 세션을 시작할 매장을 고른다.
 * 카드 탭은 선택(보라 테두리)만 하고, 확인 버튼은 매장 선택 시에만 하단
 * 네비게이션 바 위에 팝업으로 뜬다 — 마이페이지 영수증 매장 선택과 동일한 로직
 * ([LooketStoreSelectionScreen] 공유). Figma 쇼핑-실시간-매장선택(376:5275) 기준.
 */
@Composable
fun ShoppingStoreSelectionScreen(
    selectedStoreId: String?,
    onStoreSelected: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LooketStoreSelectionScreen(
        title = stringResource(R.string.shopping_store_selection_title),
        stores = dummyShoppingStores,
        selectedStoreId = selectedStoreId,
        onStoreSelected = onStoreSelected,
        onConfirmClick = onConfirmClick,
        onBackClick = onBackClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun ShoppingStoreSelectionScreenPreview() {
    var selectedStoreId by remember { mutableStateOf<String?>(null) }
    LooketTheme {
        ShoppingStoreSelectionScreen(
            selectedStoreId = selectedStoreId,
            onStoreSelected = { selectedStoreId = it },
            onConfirmClick = {},
            onBackClick = {},
            selectedTab = BottomNavTab.SHOP,
            onTabSelected = {},
        )
    }
}
