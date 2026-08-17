package com.gryffindor.smartshopping.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles

/**
 * Figma "Frame 94(뒤로가기) + section guide" 조합. 가운데 정렬 타이틀을 쓰는
 * [LooketTopBar]와 달리 좌측 정렬 큰 제목(title-1, 24sp Bold)을 쓰는 화면에서 사용한다.
 * 온보딩/영수증 등록 계열 화면에서 반복되는 패턴이라 공용 컴포넌트로 뺐다.
 * [onSkipClick]을 넘기면 뒤로가기 버튼과 같은 줄 오른쪽에 "건너뛰기" 텍스트 버튼이 나타난다
 * (Figma 온보딩_항공편 등록/영수증 등록, 건너뛰기 버튼 추가본 기준).
 */
@Composable
fun LooketSectionHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LooketIconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = null,
                    tint = LooketColors.TextPrimary,
                )
            }
            if (onSkipClick != null) {
                Text(
                    text = stringResource(R.string.common_skip),
                    style = LooketTextStyles.titleTwo,
                    color = LooketColors.TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onSkipClick)
                        .padding(10.dp),
                )
            }
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
