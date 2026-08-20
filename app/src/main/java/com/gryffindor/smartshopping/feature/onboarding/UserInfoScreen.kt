package com.gryffindor.smartshopping.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gryffindor.smartshopping.R

private val TextPrimary = Color(0xFF1B1A22)
private val BrandPrimary = Color(0xFF616AF3)
private val BorderDefault = Color(0xFFD7D6E1)
private val BorderDisabled = Color(0xFFEEEDF1)

private data class DropdownOption(val label: String, val value: String)

private val languageOptions = listOf(
    DropdownOption("ENGLISH", "en"),
    DropdownOption("한국어", "ko"),
    DropdownOption("中國語", "zh"),
)

private val currencyOptions = listOf(
    DropdownOption("$", "usd"),
    DropdownOption("₩", "krw"),
    DropdownOption("(CN)¥", "cny"),
)

@Composable
fun UserInfoScreen(onComplete: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var language by remember { mutableStateOf<DropdownOption?>(null) }
    var currency by remember { mutableStateOf<DropdownOption?>(null) }
    var showTerms by remember { mutableStateOf(false) }

    val isReady = nickname.isNotBlank() && language != null && currency != null

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Text(
                "Welcome!\n사용자 정보를 입력해주세요.",
                modifier = Modifier.padding(top = 64.dp, bottom = 10.dp),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 31.sp,
            )

            // 닉네임
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text("닉네임", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TextPrimary))
            }

            // 언어
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text("언어", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField(
                    placeholder = "LANGUAGE",
                    options = languageOptions,
                    selected = language,
                    onSelect = { language = it },
                )
            }

            // 사용 화폐
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text("사용 화폐", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                DropdownField(
                    placeholder = "CURRENCY",
                    options = currencyOptions,
                    selected = currency,
                    onSelect = { currency = it },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isReady) BrandPrimary else BorderDisabled)
                    .clickable(enabled = isReady) { showTerms = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("다음", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTerms) {
        TermsBottomSheet(
            onDismiss = { showTerms = false },
            onStart = {
                showTerms = false
                onComplete()
            },
        )
    }
}

@Composable
private fun DropdownField(
    placeholder: String,
    options: List<DropdownOption>,
    selected: DropdownOption?,
    onSelect: (DropdownOption) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Column(modifier = Modifier.widthIn(max = 180.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = if (open) BrandPrimary else BorderDefault,
                    shape = RoundedCornerShape(10.dp),
                )
                .clickable { open = !open }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(selected?.label ?: placeholder, color = TextPrimary, fontSize = 14.sp)
            Text("⌄", color = if (open) BrandPrimary else TextPrimary, fontSize = 16.sp)
        }

        if (open) {
            Column(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(width = 1.dp, color = BrandPrimary, shape = RoundedCornerShape(10.dp)),
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) Color(0xFFE0E8FF) else Color.White)
                            .clickable {
                                onSelect(option)
                                open = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(option.label, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private enum class TermsId(val label: String, val fullText: String) {
    PRIVACY_POLICY("개인정보 처리방침", PRIVACY_POLICY_TEXT),
    TERMS_OF_SERVICE("이용약관", TERMS_OF_SERVICE_TEXT),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsBottomSheet(onDismiss: () -> Unit, onStart: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var checkedTerms by remember { mutableStateOf(setOf<TermsId>()) }
    var detailTerm by remember { mutableStateOf<TermsId?>(null) }
    val allChecked = TermsId.entries.all { it in checkedTerms }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val shownDetail = detailTerm
        if (shownDetail != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { detailTerm = null }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("‹", color = TextPrimary, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(shownDetail.label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TermsBody(text = shownDetail.fullText, textColor = TextPrimary)
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "앱 사용을 위해\n정보수집에 동의해주세요.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 31.sp,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TermsId.entries.forEach { term ->
                        TermsRow(
                            label = term.label,
                            checked = term in checkedTerms,
                            onToggle = {
                                checkedTerms = if (term in checkedTerms) checkedTerms - term else checkedTerms + term
                            },
                            onDetailClick = { detailTerm = term },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (allChecked) BrandPrimary else BorderDisabled)
                        .clickable(enabled = allChecked, onClick = onStart),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("시작하기", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TermsRow(label: String, checked: Boolean, onToggle: () -> Unit, onDetailClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = if (checked) R.drawable.check_active else R.drawable.check_inactive),
                contentDescription = if (checked) "체크됨" else "체크 안 됨",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextPrimary, fontSize = 14.sp)
        }
        Text(
            "›",
            color = TextPrimary,
            fontSize = 18.sp,
            modifier = Modifier.clickable(onClick = onDetailClick).padding(8.dp),
        )
    }
}