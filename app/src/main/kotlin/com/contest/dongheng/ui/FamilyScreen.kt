package com.contest.dongheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.contest.dongheng.data.FamilyMember
import com.contest.dongheng.data.Facility
import java.time.LocalDate

private const val STALE_DAYS = 180L

@Composable
fun FamilyScreen(s: AppState) {
    var editing by remember { mutableStateOf<FamilyMember?>(null) }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("가족", style = MaterialTheme.typography.headlineMedium) }
        if (s.driving) {
            item { Panel { Text("🔒 주행 중에는 편집할 수 없어요. 정차 후 이용해 주세요.", color = WarnAmber,
                style = MaterialTheme.typography.bodyLarge) } }
        }
        items(s.members, key = { it.id }) { m ->
            Panel(title = "${m.name} · ${m.ageGroup}") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = m.id in s.riderIds, onClick = { s.toggleRider(m.id) },
                        label = { Text(if (m.id in s.riderIds) "이번 이동에 탑승" else "이번 이동 제외") })
                }
                Text("시설 조건: " + (m.needs.joinToString(", ") { it.label }.ifBlank { "없음" }),
                    style = MaterialTheme.typography.bodyLarge)
                Text("휴식 간격: " + (m.restIntervalMin?.let { "${it}분" } ?: "기본(120분)"), style = MaterialTheme.typography.bodyLarge)
                if (m.packing.isNotEmpty()) Text("준비물: " + m.packing.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                if (m.hasEmergencyInfo) {
                    Hint("응급 참고: 알레르기 ${m.allergies.ifBlank { "-" }} / 복용약 ${m.meds.ifBlank { "-" }} / 기저질환 ${m.conditions.ifBlank { "-" }}")
                }
                if (m.contactPhone.isNotBlank()) Hint("연락처: ${m.contactName} ${m.contactPhone}")
                val stale = runCatching {
                    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(m.updatedAt), LocalDate.now()) > STALE_DAYS
                }.getOrDefault(false)
                Hint("마지막 수정 ${m.updatedAt.ifBlank { "기록 없음" }}" + if (stale && m.hasEmergencyInfo) " · 오래된 정보예요. 확인해 주세요." else "",
                    color = if (stale && m.hasEmergencyInfo) WarnAmber else MaterialTheme.colorScheme.onSurfaceVariant)
                BigButton("편집", { editing = m }, enabled = !s.driving, outlined = true)
            }
        }
        item {
            BigButton("＋ 가족 추가", {
                editing = FamilyMember("m" + System.currentTimeMillis(), "", "성인")
            }, Modifier.fillMaxWidth(), enabled = !s.driving)
        }
        item { Hint("응급 참고 정보는 SOS의 신고용 요약에서만 쓰이고, 장소 검색에는 사용되지 않아요. 이 기기에만 저장돼요.") }
    }

    editing?.let { m ->
        MemberDialog(m, isNew = s.members.none { it.id == m.id },
            onDismiss = { editing = null },
            onDelete = { s.deleteMember(m.id); editing = null },
            onSave = { s.upsertMember(it.copy(updatedAt = LocalDate.now().toString())); editing = null })
    }
}

@Composable
private fun MemberDialog(
    m: FamilyMember, isNew: Boolean,
    onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (FamilyMember) -> Unit,
) {
    var name by remember { mutableStateOf(m.name) }
    var age by remember { mutableStateOf(m.ageGroup) }
    var needs by remember { mutableStateOf(m.needs) }
    var rest by remember { mutableStateOf(m.restIntervalMin?.toString() ?: "") }
    var allergies by remember { mutableStateOf(m.allergies) }
    var meds by remember { mutableStateOf(m.meds) }
    var cond by remember { mutableStateOf(m.conditions) }
    var cName by remember { mutableStateOf(m.contactName) }
    var cPhone by remember { mutableStateOf(m.contactPhone) }
    var packing by remember { mutableStateOf(m.packing.joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "가족 추가" else "가족 편집") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("이름 또는 별칭") }, singleLine = true)
                OutlinedTextField(age, { age = it }, label = { Text("연령대 (예: 유아(0~3세))") }, singleLine = true)
                Text("필요한 시설", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Facility.values().filter { it != Facility.FOOD }.forEach { f ->
                        FilterChip(f in needs, { needs = if (f in needs) needs - f else needs + f },
                            label = { Text(f.label) })
                    }
                }
                OutlinedTextField(rest, { rest = it.filter(Char::isDigit).take(3) },
                    label = { Text("선호 휴식 간격(분)") }, singleLine = true)
                OutlinedTextField(packing, { packing = it }, label = { Text("준비물 (쉼표로 구분)") })
                Text("응급 참고 정보 (선택)", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(allergies, { allergies = it }, label = { Text("알레르기") }, singleLine = true)
                OutlinedTextField(meds, { meds = it }, label = { Text("복용약") }, singleLine = true)
                OutlinedTextField(cond, { cond = it }, label = { Text("기저질환") }, singleLine = true)
                OutlinedTextField(cName, { cName = it }, label = { Text("보호자·긴급 연락처 이름") }, singleLine = true)
                OutlinedTextField(cPhone, { cPhone = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("연락처") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    m.copy(
                        name = name.ifBlank { "이름 없음" }, ageGroup = age, needs = needs,
                        restIntervalMin = rest.toIntOrNull()?.takeIf { it in 20..300 },
                        allergies = allergies, meds = meds, conditions = cond,
                        contactName = cName, contactPhone = cPhone,
                        packing = packing.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    )
                )
            }) { Text("저장") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("삭제", color = SosRed) }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        },
    )
}
