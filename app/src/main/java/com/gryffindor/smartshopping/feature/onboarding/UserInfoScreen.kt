package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.component.PrimaryButton
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors

@Composable
fun UserInfoScreen(
    onNext: () -> Unit
) {
    val colors = LocalAppColors.current
    var nickname by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundSurface)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(136.dp))

        Text(
            text = "Welcome!\n사용자 정보를 입력해주세요.",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "닉네임",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            modifier = Modifier.padding(end = 0.dp),
            placeholder = {
                Text(
                    text = "닉네임을 입력해주세요",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = "다음",
            onClick = onNext,
            enabled = nickname.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
