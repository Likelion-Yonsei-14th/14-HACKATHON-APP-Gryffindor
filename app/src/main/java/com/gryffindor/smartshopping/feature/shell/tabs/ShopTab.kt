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
import com.gryffindor.smartshopping.app.navigation.ProductionRoutes
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

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "매장 선택하기",
            onClick = {
                navController.navigate(ProductionRoutes.SHOP_STORE_SELECTION)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "쇼핑 흐름",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Flow description
        val steps = listOf(
            "1. 매장 선택",
            "2. 기기 연결 확인",
            "3. 쇼핑 시작",
            "4. 제품 자동 인식",
            "5. 쇼핑 리스트 확인"
        )
        steps.forEach { step ->
            Text(
                text = step,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
