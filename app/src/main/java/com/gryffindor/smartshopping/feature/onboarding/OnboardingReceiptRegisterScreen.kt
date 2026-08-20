package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketSectionHeader
import com.gryffindor.smartshopping.core.ui.theme.LooketColors

/**
 * 온보딩 마지막 단계 — 여행 중 받은 구매 영수증 사진을 등록한다.
 * Figma 온보딩_영수증 등록(376:5058) / 온보딩_영수증 편집(376:5068) 기준.
 * 두 화면은 사진 유무([hasPhoto])에 따라 CTA 문구만 다른 동일 레이아웃이라
 * [FlightRegisterScreen]과 같은 방식으로 하나의 컴포저블에서 토글한다.
 * 실제 카메라 촬영/갤러리 선택 연동은 아직 없다.
 */
@Composable
fun OnboardingReceiptRegisterScreen(
    hasPhoto: Boolean,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LooketColors.Surface)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(52.dp))
        LooketSectionHeader(
            title = stringResource(R.string.mypage_receipt_register_guide),
            onBackClick = onBackClick,
            onSkipClick = onSkipClick,
        )
        Spacer(Modifier.height(29.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // TODO: 실제 촬영/갤러리 선택 사진으로 교체 (hasPhoto=true일 때 여기 보여줘야 함)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(570.dp)
                    .background(LooketColors.SurfaceEmphasized),
            )
            Spacer(Modifier.height(24.dp))
            LooketPrimaryButton(
                text = stringResource(
                    if (hasPhoto) R.string.onboarding_receipt_retake_cta
                    else R.string.onboarding_receipt_register_cta
                ),
                onClick = onCaptureClick,
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
