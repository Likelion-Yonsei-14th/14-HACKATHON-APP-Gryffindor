package com.gryffindor.smartshopping.feature.mypage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketConfirmDialog
import com.gryffindor.smartshopping.core.ui.component.LooketDropdown
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.domain.model.PurchasedProduct

private val languageOptions = listOf("ENGLISH", "한국어", "中國語")
private val currencyOptions = listOf("$", "₩", "(CN)¥")

@Composable
fun MyPageScreen(
    nickname: String,
    purchasedProducts: List<PurchasedProduct>,
    selectedLanguage: String?,
    selectedCurrency: String?,
    onLanguageSelected: (String) -> Unit,
    onCurrencySelected: (String) -> Unit,
    onTravelClick: () -> Unit,
    onReceiptClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onLogoutClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        bottomBar = { BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(LooketColors.Surface)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LooketTopBar(title = stringResource(R.string.mypage_title))
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(72.dp),
            ) {
                ProfileSection(
                    nickname = nickname,
                    selectedLanguage = selectedLanguage,
                    selectedCurrency = selectedCurrency,
                    onLanguageSelected = onLanguageSelected,
                    onCurrencySelected = onCurrencySelected,
                )

                Column(verticalArrangement = Arrangement.spacedBy(63.dp)) {
                    MyPageMenuRow(text = stringResource(R.string.mypage_travel), onClick = onTravelClick)
                    MyPageMenuRow(text = stringResource(R.string.mypage_receipt), onClick = onReceiptClick)
                    MyPageMenuRow(text = stringResource(R.string.mypage_wishlist), onClick = onWishlistClick)
                    MyPageMenuRow(text = stringResource(R.string.mypage_logout), onClick = { showLogoutDialog = true })
                }

                if (purchasedProducts.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    PurchasedProductsSection(purchasedProducts = purchasedProducts)
                }
            }
        }
    }

    if (showLogoutDialog) {
        LooketConfirmDialog(
            message = stringResource(R.string.mypage_logout_confirm_message),
            onConfirm = {
                showLogoutDialog = false
                onLogoutClick()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun ProfileSection(
    nickname: String,
    selectedLanguage: String?,
    selectedCurrency: String?,
    onLanguageSelected: (String) -> Unit,
    onCurrencySelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProfileAvatar()
            Text(text = nickname, style = LooketTextStyles.titleTwo, color = LooketColors.TextPrimary)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LooketDropdown(
                label = stringResource(R.string.mypage_language_label),
                options = languageOptions,
                selectedOption = selectedLanguage,
                onOptionSelected = onLanguageSelected,
            )
            LooketDropdown(
                label = stringResource(R.string.mypage_currency_label),
                options = currencyOptions,
                selectedOption = selectedCurrency,
                onOptionSelected = onCurrencySelected,
            )
        }
    }
}

// Figma: linear-gradient(92.37deg, BrandPrimary 7.015%, BrandGradientEnd 113.15%)
// — 거의 수평(왼쪽→오른쪽)이라 92.37°를 90°로 근사해도 80dp 원 안에서는 육안상 차이가 없다.
@Composable
private fun ProfileAvatar() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(CircleShape)
            .drawWithCache {
                val brush = Brush.linearGradient(
                    colors = listOf(LooketColors.BrandPrimary, LooketColors.BrandGradientEnd),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

@Composable
private fun MyPageMenuRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = LooketTextStyles.titleTwo, color = LooketColors.TextPrimary)
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = LooketColors.TextPrimary,
        )
    }
}

@Composable
private fun PurchasedProductsSection(purchasedProducts: List<PurchasedProduct>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "구매 상품",
            style = LooketTextStyles.titleTwo,
            color = LooketColors.TextPrimary,
        )
        purchasedProducts.forEach { item ->
            PurchasedProductRow(item)
        }
    }
}

@Composable
private fun PurchasedProductRow(item: PurchasedProduct) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LooketColors.SurfaceEmphasized, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val imageUrl = item.product?.imageUrl
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.product?.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LooketColors.BorderDefault),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product?.brand ?: item.storeName ?: "",
                style = LooketTextStyles.bodyThree,
                color = LooketColors.TextSecondary,
            )
            Text(
                text = item.product?.name ?: item.fallbackProductName ?: "",
                style = LooketTextStyles.bodyTwo,
                color = LooketColors.TextPrimary,
            )
            if (item.price != null) {
                Text(
                    text = "${item.currency ?: "₩"}${"%,d".format(item.price)}",
                    style = LooketTextStyles.bodyThree,
                    color = LooketColors.TextSecondary,
                )
            }
        }
    }
}
