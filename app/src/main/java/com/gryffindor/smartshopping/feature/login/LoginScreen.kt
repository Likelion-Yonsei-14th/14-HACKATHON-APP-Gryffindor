package com.gryffindor.smartshopping.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

/**
 * Login screen — Figma node 376:5161 (로그인)
 *
 * Layout:
 * - Top bar area (68dp top padding, placeholder back/forward arrows — not functional)
 * - Center: slogan "look. pocket. repeat." + favicon logo
 * - Bottom: Kakao login (#FBE300) + Guest login (#E0E8FF)
 * - 256dp gap between top bar and center content (Figma: gap=256px between groups)
 */
@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit,
    onGuestLogin: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Logo area — Figma: slogan text above favicon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "look. pocket. repeat.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif, // Clash Grotesk fallback
                color = colors.brandPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Favicon — Figma: 100x26
            Image(
                painter = painterResource(R.drawable.logo_looket),
                contentDescription = "LOOKET",
                modifier = Modifier
                    .width(100.dp)
                    .height(26.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Login buttons — Figma: gap=16dp, each 380 wide x 56 tall, radius 6dp
        Column(
            modifier = Modifier.padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kakao Login — Figma: bg #FBE300, text #3B1E1E, body-1
            Button(
                onClick = onKakaoLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBE300)
                )
            ) {
                Text(
                    text = "카카오 로그인",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3B1E1E)
                )
            }

            // Guest Login — Figma: bg BrandPrimarySubtle (#E0E8FF), text TextBrand (#616AF3), body-1
            Button(
                onClick = onGuestLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimarySubtle
                )
            ) {
                Text(
                    text = "게스트로 로그인",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textBrand
                )
            }
        }
    }
}
