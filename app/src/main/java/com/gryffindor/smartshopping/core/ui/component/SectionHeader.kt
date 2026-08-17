package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * Figma "Frame 94(뒤로가기) + section guide" 조합. 가운데 정렬 타이틀을 쓰는
 * [LooketTopBar]와 달리 좌측 정렬 큰 제목(title-1, 24sp Bold)을 쓰는 화면에서 사용한다.
 * 온보딩/영수증 등록 계열 화면에서 반복되는 패턴이라 공용 컴포넌트로 뺐다.
 */
@Composable
fun LooketSectionHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        IconButton(onClick = onBackClick, modifier = Modifier.height(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = LooketColors.TextPrimary,
            )
        }
        Text(
            text = title,
            style = LooketTextStyles.titleOne,
            color = LooketColors.TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
