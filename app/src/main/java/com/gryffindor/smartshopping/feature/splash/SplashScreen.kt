package com.gryffindor.smartshopping.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.core.ui.theme.GradientEnd
import com.gryffindor.smartshopping.core.ui.theme.GradientStart
import com.gryffindor.smartshopping.core.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val colors = LocalAppColors.current

    LaunchedEffect(Unit) {
        delay(1500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LOOKET",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textInverse
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "look. pocket. repeat.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textInverse.copy(alpha = 0.8f)
            )
        }
    }
}
