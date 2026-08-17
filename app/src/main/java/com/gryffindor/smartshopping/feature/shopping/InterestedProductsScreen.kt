package com.gryffindor.smartshopping.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

/**
 * 추천/관심 후보 상품 하나. [id]는 나중에 실제 찜(위시리스트) DB의 기본키가 될 값이라,
 * 선택 여부는 이 데이터 클래스 안에 넣지 않고 [InterestedProductsScreen]의
 * `selectedProductIds`(찜 테이블에 실제로 존재하는 id 집합에 대응)로 따로 관리한다.
 */
data class RecommendedProduct(
    val id: String,
    val name: String,
    val storeName: String,
    val price: String,
)

/**
 * 쇼핑 결과 기록 > 관심 상품 등록 — 구매 상품 확인에서 "확인"을 누르면 진입.
 * Figma 쇼핑 결과 기록_관심제품 등록(376:5110) 기준. 카드를 탭하면 보라 테두리로
 * 선택/해제되고(추천 기반 찜 등록), "완료"를 누르면 다음 단계로 넘어간다.
 */
@Composable
fun InterestedProductsScreen(
    products: List<RecommendedProduct>,
    selectedProductIds: Set<String>,
    onToggleProduct: (String) -> Unit,
    onCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            LooketTopBar(title = stringResource(R.string.shopping_result_interested_title), onBackClick = onBackClick)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.shopping_result_interested_heading),
                    style = LooketTextStyles.titleOne,
                    color = LooketColors.TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.shopping_result_interested_caption),
                    style = LooketTextStyles.bodyThree,
                    color = LooketColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                products.chunked(2).forEach { rowProducts ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        rowProducts.forEach { product ->
                            RecommendedProductCard(
                                product = product,
                                selected = product.id in selectedProductIds,
                                onClick = { onToggleProduct(product.id) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            LooketPrimaryButton(
                text = stringResource(R.string.common_done),
                onClick = onCompleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecommendedProductCard(
    product: RecommendedProduct,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) LooketColors.BrandPrimary else LooketColors.Surface
    Column(
        modifier = Modifier
            .width(180.dp)
            .border(1.dp, borderColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(LooketColors.SurfaceEmphasized),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LooketColors.Surface)
                .padding(10.dp),
        ) {
            Text(text = product.name, style = LooketTextStyles.bodyTwo, color = LooketColors.TextPrimary)
            Text(text = product.storeName, style = LooketTextStyles.bodyThree, color = LooketColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = product.price, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
        }
    }
}

@Preview(showBackground = true, heightDp = 917, widthDp = 412)
@Composable
private fun InterestedProductsScreenPreview() {
    var selectedIds by remember { mutableStateOf(setOf("p1")) }
    LooketTheme {
        InterestedProductsScreen(
            products = dummyRecommendedProducts,
            selectedProductIds = selectedIds,
            onToggleProduct = { id ->
                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
            },
            onCompleteClick = {},
            onBackClick = {},
            selectedTab = BottomNavTab.SHOP,
            onTabSelected = {},
        )
    }
}

internal val dummyRecommendedProducts = listOf(
    RecommendedProduct("p1", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000"),
    RecommendedProduct("p2", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000"),
    RecommendedProduct("p3", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000"),
    RecommendedProduct("p4", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000"),
    RecommendedProduct("p5", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000"),
    RecommendedProduct("p6", "Aren 비세토스 E/W 숄더백", "신세계면세점 본점", "₩ 1,090,000"),
)
