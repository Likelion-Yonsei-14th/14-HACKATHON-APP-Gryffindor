package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

data class FlightInfoField(val label: String, val value: String)

@Composable
fun FlightInfoConfirmScreen(
    fields: List<FlightInfoField>,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    Scaffold(
        bottomBar = { BottomNavBar(selectedTab = BottomNavTab.MY_PAGE, onTabSelected = {}) },
        containerColor = LooketColors.Surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 52.dp)
                    .size(48.dp)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = LooketColors.TextPrimary, fontSize = 28.sp)
            }

            Text(
                "항공편 정보가 맞는지\n다시 한번 확인해주세요.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = LooketColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 31.sp,
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                fields.forEach { field -> FlightInfoFieldRow(field) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LooketColors.BrandPrimary)
                    .clickable(onClick = onEditClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("편집하기", color = LooketColors.TextInverse, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FlightInfoFieldRow(field: FlightInfoField) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
            Text(field.label, color = LooketColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            Text(field.value, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LooketColors.TextPrimary))
    }
}

// TODO: 실제로는 항공편 등록 결과(FlightRegisterScreen)에서 파싱된 값으로 채워야 함
private val previewFields = listOf(
    FlightInfoField("출발지", "BEJ"),
    FlightInfoField("도착지", "ICN"),
    FlightInfoField("터미널", "인천공항 T2"),
    FlightInfoField("출발 시간", "2026.08.21 10:00"),
    FlightInfoField("도착 시간", "2026.08.25 19:00"),
    FlightInfoField("공항 도착 예정시간", "2026.08.25 15:00"),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun FlightInfoConfirmScreenPreview() {
    LooketTheme {
        FlightInfoConfirmScreen(fields = previewFields, onBackClick = {}, onEditClick = {})
    }
}