package com.gryffindor.smartshopping.feature.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme
import com.gryffindor.smartshopping.domain.model.ChecklistItem

private val StatusGreen = Color(0xFF00D96C)

@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel,
    sessionId: String,
    onNavigateToRecommendation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadChecklist(sessionId)
    }

    when (val state = uiState) {
        is UiState.Loading -> ChecklistLoadingContent()
        is UiState.Error -> ChecklistErrorContent(message = state.message, onRetry = { viewModel.retry() })
        is UiState.Success -> ChecklistSuccessContent(
            items = state.data.items,
            checkedIds = state.data.checkedIds,
            onToggle = { itemId -> viewModel.toggleChecked(itemId) },
            onNavigateToRecommendation = onNavigateToRecommendation,
        )
    }
}

@Composable
private fun ChecklistLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ChecklistErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun ChecklistSuccessContent(
    items: List<ChecklistItem>,
    checkedIds: Set<String>,
    onToggle: (String) -> Unit,
    onNavigateToRecommendation: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(LooketColors.Surface)) {
        Text(
            "환급 체크리스트",
            color = LooketColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(items, key = { it.id }) { item ->
                ChecklistRow(
                    item = item,
                    isChecked = item.id in checkedIds,
                    onToggle = { onToggle(item.id) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(
                onClick = onNavigateToRecommendation,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LooketColors.BrandPrimary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("다음", color = LooketColors.TextInverse, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    isChecked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LooketColors.Surface)
            .border(width = 1.dp, color = LooketColors.BrandPrimary, shape = RoundedCornerShape(8.dp))
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 제목: 좌우16 상하10 — Figma "px-4 py-2.5"
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
            // 설명: 좌우16, 텍스트 자체는 상하10 — Figma "px-4" 안쪽에 "py-2.5" 줄
            // TODO: 실제로는 "시간:~15:00 / 장소:T1 3층" 처럼 두 줄일 수 있음 — description 포맷 확인되면 분리
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(item.description, fontSize = 12.sp, color = Color.Black)
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isChecked) StatusGreen else LooketColors.BorderDisabled)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- Preview용 목데이터 ----------

private val previewItems = listOf(
    ChecklistItem(id = "1", title = "세관 신고하기", description = "시간: ~15:00 / 장소: T1 3층", required = true),
    ChecklistItem(id = "2", title = "미환급 물품 환급받기", description = "시간: ~17:30 / 장소: 면세구역 내 환급 카운터", required = true),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ChecklistSuccessPreview() {
    LooketTheme {
        ChecklistSuccessContent(
            items = previewItems,
            checkedIds = setOf("1"),
            onToggle = {},
            onNavigateToRecommendation = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ChecklistLoadingPreview() {
    LooketTheme {
        ChecklistLoadingContent()
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun ChecklistErrorPreview() {
    LooketTheme {
        ChecklistErrorContent(message = "체크리스트를 불러올 수 없습니다.", onRetry = {})
    }
}