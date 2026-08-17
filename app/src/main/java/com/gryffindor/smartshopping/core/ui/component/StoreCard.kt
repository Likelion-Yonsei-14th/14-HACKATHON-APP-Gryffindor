package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

data class LooketStore(
    val id: String,
    val name: String,
    val address: String,
)

/**
 * Figma "매장 찾기" 카드. 마이페이지 영수증 매장 선택과 쇼핑-실시간 매장 선택 화면에서 공용으로 쓴다.
 * 실제 매장 사진 대신 회색 자리표시자를 쓴다 — 사진 에셋이 준비되면 image 파라미터를 추가해 교체한다.
 */
@Composable
fun LooketStoreCard(
    store: LooketStore,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) LooketColors.BrandPrimary else LooketColors.BorderDisabled

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(LooketColors.Surface)
            .clickable(onClick = onClick),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .background(LooketColors.BorderDisabled),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = store.name, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = store.address, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
        }
    }
}
