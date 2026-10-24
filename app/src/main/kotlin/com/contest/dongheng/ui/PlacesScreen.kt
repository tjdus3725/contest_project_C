package com.contest.dongheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.contest.dongheng.data.Facility
import com.contest.dongheng.data.FacilityStatus
import com.contest.dongheng.data.PlaceRepository
import com.contest.dongheng.data.Recommendation
import com.contest.dongheng.data.statusOf

@Composable
fun PlacesScreen(s: AppState) {
    val dest = s.destination
    var rangeKm by remember { mutableStateOf(80f) }
    var showAll by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("장소 찾기", style = MaterialTheme.typography.headlineMedium) }

        if (dest == null) {
            item {
                Panel {
                    Text("먼저 홈에서 이번 이동을 시작해 주세요.", style = MaterialTheme.typography.bodyLarge)
                    BigButton("홈으로", { s.tab = Tab.HOME })
                }
            }
            return@LazyColumn
        }

        item {
            Panel(title = "조건") {
                Text("꼭 필요한 시설 (필수)", style = MaterialTheme.typography.bodyLarge)
                FacilityChips(Facility.values().toList(), s.required) { f ->
                    s.required = if (f in s.required) s.required - f else s.required + f
                    s.preferred = s.preferred - f
                }
                Text("있으면 좋은 시설 (선호)", style = MaterialTheme.typography.bodyLarge)
                FacilityChips(Facility.values().filter { it !in s.required }, s.preferred) { f ->
                    s.preferred = if (f in s.preferred) s.preferred - f else s.preferred + f
                }
                Hint("동승자 조건이 필수로 자동 적용돼요. 필수 시설 정보가 없는 장소는 조건을 충족한 것으로 처리하지 않아요.")
            }
        }

        val recs = PlaceRepository.recommend(s.required, s.preferred, s.progressKm, dest.km, rangeKm)
        val ok = recs.filter { it.requiredOk }
        val unknown = recs.filter { !it.requiredOk }

        item {
            Text(
                "현재 서울 기점 ${s.progressKm.toInt()}km · 앞쪽 ${rangeKm.toInt()}km 이내 후보",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (s.naviMessage.isNotBlank()) item { Hint("내비 요청 결과: ${s.naviMessage}") }

        val shown = if (showAll) ok else ok.take(3)
        if (shown.isEmpty()) {
            item {
                Panel {
                    Text("조건을 확인된 장소가 이 범위에 없어요.", style = MaterialTheme.typography.bodyLarge)
                    Hint("필수 조건은 자동으로 풀지 않아요. 아래에서 직접 선택해 주세요.")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (rangeKm < 200f) BigButton("검색 범위 200km로 넓히기", { rangeKm = 200f }, Modifier.weight(1f))
                        BigButton("필수 조건 바꾸기", { s.required = emptySet() }, Modifier.weight(1f), outlined = true)
                    }
                }
            }
        }
        items(shown, key = { it.place.id }) { PlaceCard(it, s) }
        if (ok.size > 3 && !showAll) {
            item { BigButton("다른 후보 ${ok.size - 3}곳 더 보기", { showAll = true }, outlined = true) }
        }
        if (unknown.isNotEmpty()) {
            item {
                Text("정보 미확인 후보 (필수 시설 확인 안 됨)", style = MaterialTheme.typography.titleMedium,
                    color = WarnAmber)
            }
            items(unknown.take(3), key = { "u-" + it.place.id }) { PlaceCard(it, s) }
        }
    }
}

@Composable
private fun FacilityChips(list: List<Facility>, selected: Set<Facility>, toggle: (Facility) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { f ->
            FilterChip(selected = f in selected, onClick = { toggle(f) }, label = { Text("${f.icon} ${f.label}") })
        }
    }
}

@Composable
private fun PlaceCard(r: Recommendation, s: AppState) {
    val p = r.place
    val chosen = s.plannedStop?.id == p.id
    Panel(title = p.name) {
        Text(r.reason, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("약 ${r.distanceKm.toInt()}km · 약 ${r.etaMin}분 후 도착", style = MaterialTheme.typography.bodyMedium)
            Text(
                p.detourMin?.let { "추가 주행 약 ${it}분 (추정)" } ?: "추가 주행 시간 계산 불가",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            s.required.forEach { f -> StatusTag(f, p.statusOf(f)) }
        }
        Hint("운영시간 ${p.hours ?: "미확인"} · ${p.address}")
        Hint("출처: ${p.source} · 마지막 확인 ${p.verifiedAt ?: "확인 이력 없음"}")
        BigButton(
            if (chosen) "안내 요청함 ✔" else "이곳으로 안내",
            { s.guideTo(p) },
            Modifier.fillMaxWidth(),
            enabled = !chosen,
        )
    }
}

@Composable
private fun StatusTag(f: Facility, st: FacilityStatus) {
    val (mark, color) = when (st) {
        FacilityStatus.CONFIRMED -> "확인됨" to OkGreen
        FacilityStatus.UNKNOWN -> "미확인" to WarnAmber
        FacilityStatus.UNAVAILABLE -> "없음" to MaterialTheme.colorScheme.error
    }
    Text("${f.label}: $mark", color = color, style = MaterialTheme.typography.bodyMedium)
}
