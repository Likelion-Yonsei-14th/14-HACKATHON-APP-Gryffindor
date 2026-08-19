package com.gryffindor.smartshopping.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

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

        // Logo area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LOOKET",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textBrand
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "look. pocket. repeat.",
                style = MaterialTheme.typography.titleMedium,
                color = colors.brandPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Login buttons
        Column(
            modifier = Modifier.padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kakao Login
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

            // Guest Login
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
