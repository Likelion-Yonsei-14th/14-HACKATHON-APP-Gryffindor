package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

enum class HomeTopBarTab(val label: String) {
    REFUND("REFUND"),
    LOOKET("LOOKET"),
}

@Composable
fun HomeTopBar(
    selectedTab: HomeTopBarTab,
    onTabSelected: (HomeTopBarTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LooketColors.Surface)
            // Figma 원본 pt-68은 상태바 포함 값 — SafeArea가 처리한다는 전제로 축소
            .padding(top = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(41.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_indigo),
                contentDescription = "looket",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(100.dp).height(26.dp),
            )
            Image(
                painter = painterResource(id = R.drawable.icon_bell),
                contentDescription = "알림",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HomeTopBarTab.entries.forEach { tab ->
                HomeTopBarTabItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.width(180.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeTopBarTabItem(
    tab: HomeTopBarTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tab.label,
            style = LooketTextStyles.titleTwo,
            color = if (selected) LooketColors.TextBrand else LooketColors.TextPrimary,
        )
        if (selected) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LooketColors.BrandPrimary))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    LooketTheme {
        HomeTopBar(selectedTab = HomeTopBarTab.REFUND, onTabSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarRefundPreview() {
    LooketTheme {
        HomeTopBar(selectedTab = HomeTopBarTab.REFUND, onTabSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarLooketPreview() {
    LooketTheme {
        HomeTopBar(selectedTab = HomeTopBarTab.LOOKET, onTabSelected = {})
    }
}