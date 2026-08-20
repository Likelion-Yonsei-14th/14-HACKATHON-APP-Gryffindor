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
import com.gryffindor.smartshopping.domain.model.Store

/**
 * 쇼핑 - 실시간 > 매장 선택 — 실시간 쇼핑 세션을 시작할 매장을 고른다.
 * 카드 탭은 선택(보라 테두리)만 하고, 확인 버튼은 매장 선택 시에만 하단
 * 네비게이션 바 위에 팝업으로 뜬다 — 마이페이지 영수증 매장 선택과 동일한 로직
 * ([LooketStoreSelectionScreen] 공유). Figma 쇼핑-실시간-매장선택(376:5275) 기준.
 *
 * [stores]는 실제 매장 DB(StoreRepository.getStores())에서 불러온 값을 [toLooketStore]로
 * 변환해서 넘긴다 — 로딩/실제 데이터 연결은 AppNavGraph의 StoreSelectionViewModel이 맡는다.
 */
@Composable
fun ShoppingStoreSelectionScreen(
    stores: List<LooketStore>,
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
        stores = stores,
        selectedStoreId = selectedStoreId,
        onStoreSelected = onStoreSelected,
        onConfirmClick = onConfirmClick,
        onBackClick = onBackClick,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

/** 매장 도메인 모델 -> [LooketStoreCard]가 쓰는 UI 모델 변환. */
fun Store.toLooketStore(): LooketStore = LooketStore(
    id = id,
    name = name,
    address = address ?: listOfNotNull(city, country).joinToString(", "),
)

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun ShoppingStoreSelectionScreenPreview() {
    var selectedStoreId by remember { mutableStateOf<String?>(null) }
    val previewStores = listOf(
        LooketStore(id = "1", name = "MCM 신세계백화점 본점", address = "서울특별시 중구 퇴계로 77 9F 신세계면세점 본점"),
        LooketStore(id = "2", name = "MCM HAUS", address = "서울 강남구 압구정로 412 MCM HAUS"),
    )
    LooketTheme {
        ShoppingStoreSelectionScreen(
            stores = previewStores,
            selectedStoreId = selectedStoreId,
            onStoreSelected = { selectedStoreId = it },
            onConfirmClick = {},
            onBackClick = {},
            selectedTab = BottomNavTab.SHOP,
            onTabSelected = {},
        )
    }
}
