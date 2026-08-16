package com.gryffindor.smartshopping.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R

private val BrandPrimary = Color(0xFF616AF3)
private val BrandGradientEnd = Color(0xFF3B36CC)

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(BrandPrimary, BrandGradientEnd))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_white),
            contentDescription = "looket",
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(100.dp).height(25.dp),
        )
    }
}