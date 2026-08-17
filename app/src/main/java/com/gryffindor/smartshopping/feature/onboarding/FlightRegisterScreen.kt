package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

@Composable
fun FlightRegisterScreen(
    hasPhoto: Boolean,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(LooketColors.Surface)) {
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
            "이번 여행의 항공편 사진을\n등록해주세요.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = LooketColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 31.sp,
        )

        Spacer(modifier = Modifier.height(9.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // TODO: 실제 촬영/갤러리 선택 사진으로 교체 (hasPhoto=true일 때 여기 보여줘야 함)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(570.dp)
                    .background(LooketColors.BorderDefault),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LooketColors.BrandPrimary)
                    .clickable(onClick = onCaptureClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (hasPhoto) "다시찍기" else "등록하기",
                    color = LooketColors.TextInverse,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun FlightRegisterScreenPreview() {
    LooketTheme {
        FlightRegisterScreen(hasPhoto = false, onBackClick = {}, onCaptureClick = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun FlightRetakeScreenPreview() {
    LooketTheme {
        FlightRegisterScreen(hasPhoto = true, onBackClick = {}, onCaptureClick = {})
    }
}