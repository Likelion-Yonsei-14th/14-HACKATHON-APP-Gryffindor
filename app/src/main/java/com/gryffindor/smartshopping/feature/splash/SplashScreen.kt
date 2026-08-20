package com.gryffindor.smartshopping.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.theme.GradientEnd
import com.gryffindor.smartshopping.core.ui.theme.GradientStart
import kotlinx.coroutines.delay

/**
 * Splash screen — Figma node 376:5147 (스플래시)
 *
 * Gradient background (133deg, #616AF3 → #3B36CC)
 * White favicon logo centered (100x26 in 412-wide frame)
 * No additional text on splash.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
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
        // Favicon logo — Figma: 100x26 at center of 412x917 frame
        // Ratio: width ≈ 24% of screen width. Using fixed dp matching design intent.
        Image(
            painter = painterResource(R.drawable.logo_looket_white),
            contentDescription = "LOOKET",
            modifier = Modifier
                .width(100.dp)
                .height(26.dp),
            contentScale = ContentScale.Fit
        )
    }
}
