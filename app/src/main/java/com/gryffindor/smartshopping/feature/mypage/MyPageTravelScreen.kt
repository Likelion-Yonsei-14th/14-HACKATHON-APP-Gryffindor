package com.gryffindor.smartshopping.feature.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 마이페이지 > TRAVEL — 온보딩에서 등록한 여행 정보를 읽기 전용으로 보여준다.
 * (post-shopping 환급 플로우의 feature.travel.TravelScreen과는 별개 화면)
 */
@Composable
fun MyPageTravelScreen(
    departureAirport: String,
    arrivalAirport: String,
    terminal: String,
    departureTime: String,
    arrivalTime: String,
    airportArrivalEstimate: String,
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LooketTopBar(title = stringResource(R.string.mypage_travel), onBackClick = onBackClick)
            Spacer(Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                TravelInfoField(label = stringResource(R.string.mypage_travel_departure_airport), value = departureAirport)
                TravelInfoField(label = stringResource(R.string.mypage_travel_arrival_airport), value = arrivalAirport)
                TravelInfoField(label = stringResource(R.string.mypage_travel_terminal), value = terminal)
                TravelInfoField(label = stringResource(R.string.mypage_travel_departure_time), value = departureTime)
                TravelInfoField(label = stringResource(R.string.mypage_travel_arrival_time), value = arrivalTime)
                TravelInfoField(
                    label = stringResource(R.string.mypage_travel_airport_arrival_estimate),
                    value = airportArrivalEstimate,
                )
            }
        }
    }
}

@Composable
private fun TravelInfoField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = label, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
        }
        Text(
            text = value,
            style = LooketTextStyles.bodyOne,
            color = LooketColors.TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = LooketColors.TextPrimary,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth,
                    )
                }
                .padding(top = 10.dp, end = 10.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun MyPageTravelScreenPreview() {
    LooketTheme {
        MyPageTravelScreen(
            departureAirport = "BEJ",
            arrivalAirport = "ICN",
            terminal = "인천공항 T2",
            departureTime = "2026.08.21 10:00",
            arrivalTime = "2026.08.25 19:00",
            airportArrivalEstimate = "2026.08.25 15:00",
            onBackClick = {},
            selectedTab = BottomNavTab.MY_PAGE,
            onTabSelected = {},
        )
    }
}
