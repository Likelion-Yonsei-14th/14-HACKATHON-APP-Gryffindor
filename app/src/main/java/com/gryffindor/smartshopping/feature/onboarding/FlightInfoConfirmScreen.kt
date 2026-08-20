package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.background
import com.gryffindor.smartshopping.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.core.ui.component.BottomNavBar
import com.gryffindor.smartshopping.core.ui.component.BottomNavTab
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTheme

// 수정할 수 있도록 value를 변경 가능한 상태로 관리하기 위해 수정
data class FlightInfoField(
    val id: String,
    val label: String,
    val value: String
)

@Composable
fun FlightInfoConfirmScreen(
    fields: List<FlightInfoField>,
    onBackClick: () -> Unit,
    onConfirmClick: (List<FlightInfoField>) -> Unit, // 수정 완료 후 전달용
) {
    // 각 필드의 값을 상태로 관리
    var editableFields by remember { mutableStateOf(fields) }

    Scaffold(
        bottomBar = { BottomNavBar(selectedTab = BottomNavTab.MY_PAGE, onTabSelected = {}) },
        containerColor = LooketColors.Surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 52.dp)
                    .size(48.dp)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = LooketColors.TextPrimary, fontSize = 28.sp)
            }

            Text(
                "항공편 정보가 맞는지\n다시 한번 확인해주세요.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = LooketColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 31.sp,
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                editableFields.forEachIndexed { index, field ->
                    FlightInfoFieldRow(
                        field = field,
                        onValueChange = { newValue ->
                            editableFields = editableFields.toMutableList().also {
                                it[index] = field.copy(value = newValue)
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LooketColors.BrandPrimary)
                    .clickable(onClick = { onConfirmClick(editableFields) }),
                contentAlignment = Alignment.Center,
            ) {
                Text("확인 완료", color = LooketColors.TextInverse, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FlightInfoFieldRow(
    field: FlightInfoField,
    onValueChange: (String) -> Unit
) {
    // 수정 모드 상태 (true면 입력 가능 및 포커스)
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // 수정 모드로 바뀔 때 자동으로 커서 포커스 요청
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isEditing = true } // 행을 누르면 수정 모드로 전환
    ) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
            Text(field.label, color = LooketColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isEditing) {
                // 수정 중일 때 나타나는 TextField
                BasicTextField(
                    value = field.value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true
                )
            } else {
                // 평소에는 텍스트로 표시
                Text(
                    text = field.value,
                    modifier = Modifier.weight(1f),
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // TODO: 프로젝트에 등록된 실제 피그마 아이콘 리소스명으로 교체 필요 (예: R.drawable.ic_edit 등)
            // 아이콘을 눌러도 수정 모드로 진입 가능
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { isEditing = !isEditing },
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    painter = painterResource(id = R.drawable.icon_edit),
                    contentDescription = "수정",
                    tint = LooketColors.TextPrimary
                )

            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LooketColors.TextPrimary))
    }
}

private val previewFields = listOf(
    FlightInfoField("1", "출발지", "BEJ"),
    FlightInfoField("2", "도착지", "ICN"),
    FlightInfoField("3", "터미널", "인천공항 T2"),
    FlightInfoField("4", "출발 시간", "2026.08.21 10:00"),
    FlightInfoField("5", "도착 시간", "2026.08.25 19:00"),
    FlightInfoField("6", "공항 도착 예정시간", "2026.08.25 15:00"),
)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun FlightInfoConfirmScreenPreview() {
    LooketTheme {
        FlightInfoConfirmScreen(fields = previewFields, onBackClick = {}, onConfirmClick = {})
    }
}