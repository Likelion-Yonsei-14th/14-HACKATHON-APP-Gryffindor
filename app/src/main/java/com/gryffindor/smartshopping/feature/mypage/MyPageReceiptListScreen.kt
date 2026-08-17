package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketSectionHeader
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

data class MyPageReceiptItem(
    val id: String,
    val date: String,
    val itemName: String,
    val amount: String,
)

/**
 * 마이페이지 > RECEIPT > 매장 카드 탭 — 그 매장에서 온보딩 단계에 등록해둔 영수증 목록을
 * 읽기 전용으로 보여준다. Figma에 대응하는 화면이 아직 없어 기존 디자인 토큰/컴포넌트
 * 스타일에 맞춰 새로 구성했다.
 */
@Composable
fun MyPageReceiptListScreen(
    storeName: String,
    receipts: List<MyPageReceiptItem>,
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
        ) {
            Spacer(Modifier.height(52.dp))
            LooketSectionHeader(title = storeName, onBackClick = onBackClick)
            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (receipts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.mypage_receipt_list_empty),
                        style = LooketTextStyles.bodyTwo,
                        color = LooketColors.TextDisabled,
                    )
                } else {
                    receipts.forEach { receipt -> ReceiptRow(receipt) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReceiptRow(receipt: MyPageReceiptItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LooketColors.BorderDisabled, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text(text = receipt.date, style = LooketTextStyles.bodyThree, color = LooketColors.TextDisabled)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = receipt.itemName, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
            Text(text = receipt.amount, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
        }
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun MyPageReceiptListScreenPreview() {
    LooketTheme {
        MyPageReceiptListScreen(
            storeName = "MCM 신세계백화점 본점",
            receipts = listOf(
                MyPageReceiptItem("1", "2026.08.15", "MCM 백팩", "₩890,000"),
                MyPageReceiptItem("2", "2026.08.16", "MCM 반지갑", "₩350,000"),
            ),
            onBackClick = {},
            selectedTab = BottomNavTab.MY_PAGE,
            onTabSelected = {},
        )
    }
}
