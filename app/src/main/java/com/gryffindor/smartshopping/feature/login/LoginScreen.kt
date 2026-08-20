package com.gryffindor.smartshopping.feature.login

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R

private val BrandPrimary = Color(0xFF616AF3)
private val BrandPrimarySubtle = Color(0xFFE0E8FF)
private val KakaoYellow = Color(0xFFFBE300)
private val KakaoTextBrown = Color(0xFF3B1E1E)

@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit,
    onGuestLogin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // 브랜드 영역: 태그라인 + 로고 (RN 때 저장해둔 PNG 그대로 사용)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.login_text),
                contentDescription = "look. pocket. repeat.",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(160.dp).height(24.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.logo_indigo),
                contentDescription = "looket",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(100.dp).height(26.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 버튼 영역
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KakaoLoginButton(onClick = onKakaoLogin)
            GuestLoginButton(onClick = onGuestLogin)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun KakaoLoginButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(KakaoYellow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.logo_kakao),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(21.dp).height(19.3.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("카카오 로그인", color = KakaoTextBrown, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuestLoginButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(BrandPrimarySubtle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("게스트로 로그인", color = BrandPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}