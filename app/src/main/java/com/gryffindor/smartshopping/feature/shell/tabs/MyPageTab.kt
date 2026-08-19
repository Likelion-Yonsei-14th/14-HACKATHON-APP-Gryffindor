package com.gryffindor.smartshopping.feature.shell.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.app.navigation.ProductionRoutes
import com.gryffindor.smartshopping.core.ui.component.ConfirmDialog
import com.gryffindor.smartshopping.core.ui.theme.AppColors
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.feature.mypage.MyPageViewModel

@Composable
fun MyPageTab(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val colors = LocalAppColors.current
    val viewModel: MyPageViewModel = viewModel(
        factory = MyPageViewModel.Factory(appContainer.personalizationRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMyPage()
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "로그아웃하시겠습니까?",
            onConfirm = {
                showLogoutDialog = false
                navController.navigate(ProductionRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MY PAGE",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.brandPrimary)
            )
            Column {
                val nickname = uiState.myPage?.user?.name ?: "User"
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ENGLISH",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "KRW",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Menu items per Frame 46: Wishlist, Recent Viewed, Logout
        MyPageMenuItem(label = "WISHLIST", colors = colors) {
            navController.navigate(ProductionRoutes.MY_PAGE_WISHLIST)
        }
        MyPageMenuItem(label = "RECENT VIEWED", colors = colors) {
            navController.navigate(ProductionRoutes.MY_PAGE_RECENT)
        }
        MyPageMenuItem(label = "LOGOUT", colors = colors) {
            showLogoutDialog = true
        }
    }
}

@Composable
private fun MyPageMenuItem(
    label: String,
    colors: AppColors,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary
            )
        }
        HorizontalDivider(color = colors.borderDisabled)
    }
}
