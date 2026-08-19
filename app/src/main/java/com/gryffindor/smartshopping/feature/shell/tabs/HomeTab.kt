package com.gryffindor.smartshopping.feature.shell.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gryffindor.smartshopping.app.AppContainer
import com.gryffindor.smartshopping.app.navigation.Routes
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

@Composable
fun HomeTab(
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
    ) {
        // Top Bar area with logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOOKET",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textBrand
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shopping start section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "쇼핑 시작",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "쇼핑 시작",
                onClick = {
                    navController.navigate(Routes.storeSelection("KRW"))
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Trip section placeholder
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "내 여행",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "등록된 여행이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                text = "여행 등록하기",
                onClick = {
                    navController.navigate(Routes.TRIP_LIST)
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recommendation placeholder
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "당신만을 위한 오늘의 셀렉션",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "쇼핑을 시작하면 맞춤 제품을 추천해드립니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
