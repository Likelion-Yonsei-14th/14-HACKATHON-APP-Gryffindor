package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

enum class BottomNavDestination(val label: String, val iconRes: Int) {
    HOME("HOME", R.drawable.ic_nav_home),
    SHOP("SHOP", R.drawable.ic_nav_shop),
    MY_PAGE("MY PAGE", R.drawable.ic_nav_mypage)
}

/**
 * Bottom navigation bar — Figma: nav bar component (171:456)
 *
 * Specs: padding 8px 16px 24px, height 80dp, surface bg,
 * top border 1px #EEEDF1, 3 items (HOME/SHOP/MY PAGE)
 */
@Composable
fun BottomNavBar(
    currentDestination: BottomNavDestination,
    onDestinationSelected: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    NavigationBar(
        modifier = modifier,
        containerColor = colors.backgroundSurface,
        tonalElevation = 0.dp
    ) {
        BottomNavDestination.entries.forEach { destination ->
            val selected = destination == currentDestination
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = destination.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.textPrimary,
                    selectedTextColor = colors.textPrimary,
                    unselectedIconColor = colors.textDisabled,
                    unselectedTextColor = colors.textDisabled,
                    indicatorColor = colors.backgroundSurface
                )
            )
        }
    }
}
