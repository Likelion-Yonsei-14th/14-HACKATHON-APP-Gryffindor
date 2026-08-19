package com.gryffindor.smartshopping.feature.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavDestination
import com.gryffindor.smartshopping.feature.shell.tabs.HomeTab
import com.gryffindor.smartshopping.feature.shell.tabs.MyPageTab
import com.gryffindor.smartshopping.feature.shell.tabs.ShopTab

/**
 * Main shell with bottom navigation.
 * Hosts HOME / SHOP / MY_PAGE tabs.
 * Uses parent navController for navigating to full-screen flows.
 */
@Composable
fun MainShellScreen(
    navController: NavHostController,
    appContainer: AppContainer
) {
    var currentTab by rememberSaveable { mutableStateOf(BottomNavDestination.HOME) }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentDestination = currentTab,
                onDestinationSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        when (currentTab) {
            BottomNavDestination.HOME -> HomeTab(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                appContainer = appContainer
            )
            BottomNavDestination.SHOP -> ShopTab(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                appContainer = appContainer
            )
            BottomNavDestination.MY_PAGE -> MyPageTab(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                appContainer = appContainer
            )
        }
    }
}
