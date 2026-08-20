package com.gryffindor.smartshopping.feature.reservation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gryffindor.smartshopping.R
import com.gryffindor.smartshopping.core.ui.component.LooketDropdown
import com.gryffindor.smartshopping.core.ui.component.LooketIconButton
import com.gryffindor.smartshopping.core.ui.component.LooketPrimaryButton
import com.gryffindor.smartshopping.core.ui.component.LooketTopBar
import com.gryffindor.smartshopping.core.ui.theme.LooketColors
import com.gryffindor.smartshopping.core.ui.theme.LooketTextStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val visitTimeOptions = listOf("11:00-12:00", "14:00-15:00", "15:00-16:00")
internal val visitPurposeOptions = listOf("관심 제품 구매", "컬렉션 탐색", "교환 & 반품")
private val reservationDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 방문 예약 > 정보 입력. Figma "홈-제품-방문예약-정보 입력" 기준.
 *
 * [selectedPurpose]/[visitorName]/[note]는 백엔드 TripRepository.createVisitReservation이
 * 아직 방문 목적/예약자 성함/기타 요청사항 필드를 받지 않아서(scheduledAt/productIds만 지원)
 * 지금은 화면에만 반영되고 실제로 서버에 저장되진 않는다 — API가 확장되면 여기서 그대로
 * createReservation 호출부에 실어 보내면 된다.
 */
@Composable
fun VisitReservationFormScreen(
    date: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    selectedTimeRange: String?,
    onTimeRangeSelected: (String) -> Unit,
    selectedPurpose: String?,
    onPurposeSelected: (String) -> Unit,
    visitorName: String,
    onVisitorNameChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LooketColors.Surface)
            .navigationBarsPadding(),
    ) {
        LooketTopBar(title = stringResource(R.string.reservation_title), onBackClick = onBackClick)

        DateNavigator(date = date, onPreviousDay = onPreviousDay, onNextDay = onNextDay)

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LooketDropdown(
                label = stringResource(R.string.reservation_visit_time),
                options = visitTimeOptions,
                selectedOption = selectedTimeRange,
                onOptionSelected = onTimeRangeSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            LooketDropdown(
                label = stringResource(R.string.reservation_visit_purpose),
                options = visitPurposeOptions,
                selectedOption = selectedPurpose,
                onOptionSelected = onPurposeSelected,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            EditableFieldRow(
                label = stringResource(R.string.reservation_visitor_name),
                value = visitorName,
                onValueChange = onVisitorNameChange,
            )
            EditableFieldRow(
                label = stringResource(R.string.reservation_note),
                value = note,
                onValueChange = onNoteChange,
            )
        }

        Spacer(Modifier.weight(1f))

        LooketPrimaryButton(
            text = stringResource(R.string.common_done),
            onClick = onCompleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
                .width(144.dp)
                .height(5.dp)
                .background(LooketColors.TextPrimary, RoundedCornerShape(100.dp)),
        )
    }
}

@Composable
private fun DateNavigator(date: LocalDate, onPreviousDay: () -> Unit, onNextDay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LooketIconButton(onClick = onPreviousDay) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "이전 날짜",
                tint = LooketColors.TextPrimary,
            )
        }
        Text(
            text = date.format(reservationDateFormatter),
            style = LooketTextStyles.titleTwo,
            color = LooketColors.TextPrimary,
        )
        LooketIconButton(onClick = onNextDay) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = "다음 날짜",
                tint = LooketColors.TextPrimary,
            )
        }
    }
}

/** 라벨을 누르거나 연필 아이콘을 누르면 편집 모드로 바뀌는 입력행 — [FlightInfoConfirmScreen]과 같은 패턴. */
@Composable
private fun EditableFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { isEditing = true },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = LooketTextStyles.bodyOne, color = LooketColors.TextPrimary)
            Icon(
                painter = painterResource(R.drawable.icon_edit),
                contentDescription = "수정",
                tint = LooketColors.TextSecondary,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { isEditing = !isEditing },
            )
        }
        Spacer(Modifier.height(10.dp))
        if (isEditing) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LooketTextStyles.bodyTwo.copy(color = LooketColors.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
            )
        } else {
            Text(
                text = value,
                style = LooketTextStyles.bodyTwo,
                color = if (value.isBlank()) LooketColors.TextDisabled else LooketColors.TextPrimary,
                modifier = Modifier.height(20.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LooketColors.BorderDefault),
        )
    }
}
