package com.contest.dongheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.contest.dongheng.data.Facility
import com.contest.dongheng.data.PlaceRepository
import com.contest.dongheng.sos.Triage
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(s: AppState, openGuide: (String) -> Unit) {
    val dest = s.destination
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("홈", style = MaterialTheme.typography.headlineMedium) }

        if (dest == null) {
            item { StartTrip(s) }
        } else {
            item { TripSummary(s, now) }
            item { RestTimer(s, now) }
            item { QuickRequests(s) }
            item { Checklist(s) }
        }
        item { GuideShortcuts(openGuide) }
        item {
            Hint("※ 가족 현황은 등록한 정보와 직접 입력한 내용입니다. 센서로 아이의 수면·건강 상태를 자동 감지하지 않습니다.")
        }
    }
}

@Composable
private fun StartTrip(s: AppState) {
    Panel(title = "이번 이동 시작") {
        Text("함께 타는 가족", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            s.members.forEach { m ->
                FilterChip(
                    selected = m.id in s.riderIds,
                    onClick = { s.toggleRider(m.id) },
                    label = { Text("${m.name} (${m.ageGroup})") },
                )
            }
        }
        val needs = s.riderNeeds()
        Hint(
            if (needs.isEmpty()) "이번 이동에 적용되는 시설 조건이 없어요."
            else "적용 조건: " + needs.joinToString(", ") { it.label }
        )
        Text("목적지", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            s.destinations.forEach { d ->
                BigButton("${d.name} 안내 시작", { s.startTrip(d) }, Modifier.weight(1f))
            }
        }
        Hint("시연용 경부선 하행 구간입니다. 내비 연결: ${s.navi.name}")
    }
}

@Composable
private fun TripSummary(s: AppState, now: Long) {
    val d = s.destination ?: return
    val remaining = (d.km - s.progressKm).coerceAtLeast(0f)
    val eta = PlaceRepository.etaMin(remaining)
    Panel(title = "${d.name} 이동 중") {
        Text("남은 거리 약 ${remaining.toInt()}km · 예상 ${eta / 60}시간 ${eta % 60}분 (평균속도 85km/h 가정)",
            style = MaterialTheme.typography.bodyLarge)
        val stop = s.plannedStop
        Text(
            if (stop != null) "다음 정차: ${stop.name}" else "다음 정차: 아직 정하지 않았어요",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (s.naviMessage.isNotBlank()) Hint(s.naviMessage)
        Text("동승자: " + s.riders.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium)
        val needs = s.riderNeeds()
        if (needs.isNotEmpty()) Hint("적용 조건: " + needs.joinToString(", ") { it.label })

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("주행 중", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = s.driving, onCheckedChange = { s.driving = it })
            Hint("시연용 스위치 (실제 차량에서는 Vehicle SDK의 주행 상태 사용)")
        }
        Text("시연용 현재 위치: 서울에서 ${s.progressKm.toInt()}km", style = MaterialTheme.typography.bodyMedium)
        Slider(value = s.progressKm, onValueChange = { s.progressKm = it }, valueRange = 0f..d.km)
        BigButton("이동 종료", { s.endTrip() }, outlined = true)
    }
}

@Composable
private fun RestTimer(s: AppState, now: Long) {
    val sinceMin = ((now - s.lastRestAt) / 60_000).toInt()
    val interval = s.restIntervalMin
    val near = sinceMin >= interval - 15
    Panel(title = "휴식 시점") {
        Text(
            if (near) "설정한 휴식 간격(${interval}분)이 가까워졌어요 · 마지막 휴식 후 ${sinceMin}분"
            else "마지막 휴식 후 ${sinceMin}분 · 설정 간격 ${interval}분",
            style = MaterialTheme.typography.bodyLarge,
            color = if (near) WarnAmber else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (near) BigButton("쉴 곳 찾기", { s.requestFacility(null) }, Modifier.weight(1f))
            BigButton("방금 쉬었어요", { s.rested() }, Modifier.weight(1f), outlined = true)
        }
    }
}

@Composable
private fun QuickRequests(s: AppState) {
    Panel(title = "빠른 요청") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickBtn("🚻 화장실", s, Facility.RESTROOM)
            QuickBtn("👶 기저귀 교환", s, Facility.DIAPER)
            QuickBtn("🍽️ 식사", s, Facility.FOOD)
            QuickBtn("☕ 잠깐 쉬기", s, null)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.QuickBtn(text: String, s: AppState, f: Facility?) =
    BigButton(text, { s.requestFacility(f) }, Modifier.weight(1f))

@Composable
private fun Checklist(s: AppState) {
    val items = (s.riders.flatMap { it.packing } + listOf("차량 서류·지갑", "충전기")).distinct()
    Panel(title = "출발·도착 점검") {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item in s.checked, onCheckedChange = {
                    if (it) s.checked.add(item) else s.checked.remove(item)
                })
                Text(item, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Hint("준비물은 가족 탭에서 동승자별로 등록합니다. 정차·도착 때 두고 내리는 물건이 없는지 확인하세요.")
    }
}

@Composable
private fun GuideShortcuts(openGuide: (String) -> Unit) {
    Panel(title = "안전 가이드 미리 보기") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 2.dp)) {
            Triage.scenarios.forEach { sc ->
                BigButton("${sc.icon} ${sc.title}", { openGuide(sc.id) }, Modifier.weight(1f), outlined = true)
            }
        }
    }
}
