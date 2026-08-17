package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

enum class BottomNavTab(
    val iconRes: Int,
    val labelRes: Int,
) {
    HOME(R.drawable.ic_nav_home, R.string.nav_home),
    SHOP(R.drawable.ic_nav_shop, R.string.nav_shop),
    MY_PAGE(R.drawable.ic_nav_mypage, R.string.nav_mypage),
}

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(BorderStroke(1.dp, LooketColors.BorderDisabled))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(95.dp, Alignment.CenterHorizontally),
    ) {
        BottomNavTab.entries.forEach { tab ->
            BottomNavItem(
                tab = tab,
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomNavTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) LooketColors.TextPrimary else LooketColors.TextDisabled
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = stringResource(tab.labelRes),
            tint = contentColor,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun BottomNavBarPreview() {
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    LooketTheme {
        Surface {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
    }
}
