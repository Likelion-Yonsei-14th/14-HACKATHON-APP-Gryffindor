package com.gryffindor.smartshopping.feature.shell.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.app.navigation.Routes
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import com.gryffindor.smartshopping.domain.model.Store
import com.gryffindor.smartshopping.feature.storeselection.StoreSelectionViewModel

@Composable
fun ShopTab(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val colors = LocalAppColors.current
    val viewModel: StoreSelectionViewModel = viewModel(
        factory = StoreSelectionViewModel.Factory(
            appContainer.storeRepository,
            appContainer.sessionRepository,
            "KRW"
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStores()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "SHOP",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "매장을 선택하고 스마트글래스로\n쇼핑을 시작하세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "매장 선택하기",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Store list
        val stores = uiState.stores
        if (stores.isEmpty() && uiState.isLoading) {
            CircularProgressIndicator(color = colors.brandPrimary)
        } else {
            stores.forEach { store ->
                StoreCard(
                    store = store,
                    isSelected = store.id == uiState.selectedStoreId,
                    onClick = { viewModel.selectStore(store.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "확인",
            onClick = {
                viewModel.confirmSelection()
            },
            enabled = uiState.selectedStoreId != null && !uiState.isCreatingSession
        )

        // Navigate when session created
        uiState.sessionCreated?.let { event ->
            LaunchedEffect(event.sessionId) {
                navController.navigate(Routes.shopping(event.sessionId, event.currency))
                viewModel.consumeSessionCreatedEvent()
            }
        }
    }
}

@Composable
private fun StoreCard(
    store: Store,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundSurface
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, colors.brandPrimary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, colors.borderDisabled)
        }
    ) {
        Column {
            // Store image placeholder
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(colors.backgroundEmphasized)
            )
            // Store info
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${store.city} · ${store.brand}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
