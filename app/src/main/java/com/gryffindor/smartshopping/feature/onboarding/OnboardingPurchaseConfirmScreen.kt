package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketSectionHeader
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * 목록에 표시할 구매 물품 한 건. 실제 인식/영수증 데이터 연동 전까지는
 * [FlightInfoField]처럼 이미 포맷된 표시용 문자열을 그대로 들고 있는다.
 */
data class PurchaseConfirmItem(
    val id: String,
    val name: String,
    val storeName: String,
    val priceLabel: String,
    val refundLabel: String,
)

/**
 * 온보딩 영수증 등록 다음 단계 — 인식된 구매 물품 목록이 영수증과 일치하는지
 * 사용자가 눈으로 확인하고 틀린 항목은 제거한다.
 * Figma 온보딩_구매상품 정보 확인(376:5461) 기준. 실제 인식 결과 연동 전까지
 * [items]는 화면 진입 전 상위에서 채워 넣는다.
 */
@Composable
fun OnboardingPurchaseConfirmScreen(
    items: List<PurchaseConfirmItem>,
    onBackClick: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LooketColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column {
            Spacer(Modifier.height(52.dp))
            LooketSectionHeader(
                title = stringResource(R.string.onboarding_purchase_confirm_guide),
                onBackClick = onBackClick,
            )
        }

        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PurchaseConfirmRow(item)
                LooketPrimaryButton(
                    text = stringResource(R.string.common_remove),
                    onClick = { onRemoveItem(item.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        LooketPrimaryButton(
            text = stringResource(R.string.common_confirm),
            onClick = onConfirmClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun PurchaseConfirmRow(item: PurchaseConfirmItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = LooketColors.BorderDefault)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // TODO: 실제 인식된 제품 이미지로 교체
        Spacer(
            modifier = Modifier
                .size(width = 80.dp, height = 126.dp)
                .background(LooketColors.SurfaceEmphasized),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = item.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
                    Text(text = item.storeName, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = item.priceLabel, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
                    Text(text = item.refundLabel, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(41.dp).width(28.dp))
        }
    }
}
