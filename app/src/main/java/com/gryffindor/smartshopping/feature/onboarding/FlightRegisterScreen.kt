package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketSectionHeader
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

@Composable
fun FlightRegisterScreen(
    hasPhoto: Boolean,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LooketColors.Surface)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(52.dp))
        LooketSectionHeader(
            title = "이번 여행의 항공편 사진을\n등록해주세요.",
            onBackClick = onBackClick,
            onSkipClick = onSkipClick,
        )
        Spacer(Modifier.height(29.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // TODO: 실제 촬영/갤러리 선택 사진으로 교체 (hasPhoto=true일 때 여기 보여줘야 함)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(570.dp)
                    .background(LooketColors.SurfaceEmphasized),
            )

            LooketPrimaryButton(
                text = if (hasPhoto) "다시찍기" else "등록하기",
                onClick = onCaptureClick,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun FlightRegisterScreenPreview() {
    LooketTheme {
        FlightRegisterScreen(hasPhoto = false, onBackClick = {}, onCaptureClick = {}, onSkipClick = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun FlightRetakeScreenPreview() {
    LooketTheme {
        FlightRegisterScreen(hasPhoto = true, onBackClick = {}, onCaptureClick = {}, onSkipClick = {})
    }
}
