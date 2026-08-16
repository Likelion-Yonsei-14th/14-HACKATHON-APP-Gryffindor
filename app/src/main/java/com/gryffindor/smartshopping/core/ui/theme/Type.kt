package com.gryffindor.smartshopping.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

// Figma 텍스트 토큰(서비스명/텍스트/*)과 1:1로 대응되는 스타일.
// Material3 Typography 슬롯 이름과 어긋나는 값들은 여기서 직접 참조한다.
object LooketTextStyles {
    val titleTwo = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
    )
    val bodyOne = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    )
    val bodyThree = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
}

val LooketTypography = Typography(
    titleLarge = LooketTextStyles.titleTwo,
    titleMedium = LooketTextStyles.titleTwo,
    bodyLarge = LooketTextStyles.bodyOne,
    bodyMedium = LooketTextStyles.bodyOne,
    bodySmall = LooketTextStyles.bodyThree,
    labelSmall = LooketTextStyles.bodyThree,
)
