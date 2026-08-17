package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

private val TextPrimary = Color(0xFF1B1A22)
private val TextSecondary = Color(0xFF47455F)
private val BrandPrimary = Color(0xFF616AF3)
private val Disabled = Color(0xFFEEEDF1)

private enum class Authority(val label: String, val description: String, val iconRes: Int) {
    NOTIFICATION("알림", "알림 메시지 발송", R.drawable.icon_bell),
    META("Meta 계정", "글래스 시선 및 제품 인식 데이터 가져오기", R.drawable.icon_meta),
    LOCATION("위치", "매장 위치 정보 인식", R.drawable.icon_location),
}

@Composable
fun PermissionScreen(onNext: () -> Unit) {
    var granted by remember { mutableStateOf(setOf<Authority>()) }
    val allGranted = granted.size == Authority.entries.size

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Text(
            "앱 사용을 위해\n접근 권한을 허용해주세요.",
            modifier = Modifier.padding(top = 64.dp, bottom = 10.dp, start = 16.dp, end = 16.dp),
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 31.sp,
        )

        Column(
            modifier = Modifier.padding(top = 70.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Authority.entries.forEach { authority ->
                AuthorityRow(
                    authority = authority,
                    onClick = {
                        // TODO: 실제 권한 요청 연동 (notification/meta 계정 연동/location)
                        granted = granted + authority
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (allGranted) BrandPrimary else Disabled)
                    .clickable(enabled = allGranted, onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Text("다음", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthorityRow(authority: Authority, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = authority.iconRes),
                contentDescription = authority.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(authority.label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(authority.description, color = TextSecondary, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PermissionScreenPreview() {
    LooketTheme {
        PermissionScreen(onNext = {})
    }
}