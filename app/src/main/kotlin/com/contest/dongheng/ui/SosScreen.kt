package com.contest.dongheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.contest.dongheng.sos.Call
import com.contest.dongheng.sos.Scenario
import com.contest.dongheng.sos.Triage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Phase(val label: String) {
    SCREENING("상황 확인 중"), IMMEDIATE("즉시 도움 권고"), GUIDE("안내 제공 중")
}

private class Flow(val scenario: Scenario, immediate: Boolean, val browseOnly: Boolean) {
    var phase by mutableStateOf(
        when {
            browseOnly -> Phase.GUIDE
            immediate -> Phase.IMMEDIATE
            scenario.screening.isNotEmpty() -> Phase.SCREENING
            else -> Phase.GUIDE
        }
    )
    var q by mutableIntStateOf(0)
    var step by mutableIntStateOf(0)
    val log = mutableStateListOf<String>()

    fun answer(yes: Boolean) {
        log += scenario.screening[q] + " → " + if (yes) "예" else "아니요"
        if (yes) phase = Phase.IMMEDIATE
        else if (++q >= scenario.screening.size) phase = Phase.GUIDE
    }

    fun recheck() {
        q = 0; step = 0; log += "(상태 변화 알림 - 다시 확인)"
        phase = if (scenario.screening.isNotEmpty()) Phase.SCREENING else Phase.IMMEDIATE
    }
}

@Composable
fun SosScreen(s: AppState) {
    var text by remember { mutableStateOf("") }
    var flow by remember { mutableStateOf<Flow?>(null) }
    var notice by remember { mutableStateOf("") }

    // 홈에서 넘어온 열람 전용 가이드
    LaunchedEffect(s.sosGuide) {
        s.sosGuide?.let { id ->
            Triage.scenarios.firstOrNull { it.id == id }?.let { flow = Flow(it, false, true) }
            s.sosGuide = null
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("SOS", style = MaterialTheme.typography.headlineMedium, color = SosRed)
            CallPanel(s)

            Panel(title = "상황 말하기") {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 아이가 토했어요 / 차에서 연기가 나요") },
                    singleLine = true,
                )
                BigButton("상황 확인", {
                    notice = ""
                    when (val m = Triage.classify(text)) {
                        is Triage.Match.Emergency -> flow = Flow(m.scenario, m.immediate, false)
                        is Triage.Match.Place -> {
                            flow = null
                            notice = "${m.facility.label}이(가) 필요하시군요. 장소 찾기로 연결할게요."
                            s.requestFacility(m.facility)
                        }
                        Triage.Match.Unknown -> {
                            flow = null
                            notice = "상황을 정확히 이해하지 못했어요. 아래에서 직접 선택해 주세요."
                        }
                    }
                }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
                if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodyLarge)
                Hint("음성 입력은 SpeechToText SDK 연동 후 제공 예정입니다. 지금은 문장 입력 또는 아래 버튼을 사용하세요.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Triage.scenarios.take(3).forEach { sc ->
                        BigButton("${sc.icon} ${sc.title}", { flow = Flow(sc, sc.alwaysImmediate, false) },
                            Modifier.weight(1f), outlined = true)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Triage.scenarios.drop(3).forEach { sc ->
                        BigButton("${sc.icon} ${sc.title}", { flow = Flow(sc, sc.alwaysImmediate, false) },
                            Modifier.weight(1f), outlined = true)
                    }
                }
            }
            flow?.let { GuidePanel(s, it) }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReportPanel(s, flow)
        }
    }
}

@Composable
private fun CallPanel(s: AppState) {
    Panel(title = "긴급 전화") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("📞 119", { s.dial(Call.FIRE119.number) }, Modifier.weight(1f), color = SosRed)
            BigButton("📞 112", { s.dial(Call.POLICE112.number) }, Modifier.weight(1f), color = SosRed)
        }
        val v = s.vehicle
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(
                if (v.insurerPhone.isBlank()) "보험사 (MY에서 등록)" else "🚗 ${v.insurer.ifBlank { "보험사" }} 긴급출동",
                { s.dial(v.insurerPhone) }, Modifier.weight(1f), enabled = v.insurerPhone.isNotBlank(),
            )
            BigButton("🛣️ 도로공사 ${Call.HIGHWAY.number}", { s.dial(Call.HIGHWAY.number) }, Modifier.weight(1f), outlined = true)
        }
        if (s.callState != CallState.NONE) Text(s.callState.label, style = MaterialTheme.typography.bodyMedium, color = WarnAmber)
        Hint("누르면 전화 앱의 발신 화면이 열려요. 앱은 신고 접수나 구조대 출동 여부를 알 수 없어요.")
    }
}

@Composable
private fun GuidePanel(s: AppState, f: Flow) {
    val sc = f.scenario
    Panel(title = "${sc.icon} ${sc.title}") {
        Text(
            if (f.browseOnly) "미리 보기 (긴급 상황이 아닐 때 읽어두는 가이드)" else f.phase.label,
            color = if (f.phase == Phase.IMMEDIATE && !f.browseOnly) SosRed else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
        )

        if (f.phase == Phase.IMMEDIATE && !f.browseOnly) {
            Text("바로 119 등 도움 연결을 권해요. 아래 안내는 연결과 함께 따라 하세요.", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("📞 ${sc.primaryCall.label}", { s.dial(sc.primaryCall.number) }, Modifier.weight(1f), color = SosRed)
                if (sc.useInsurer && s.vehicle.insurerPhone.isNotBlank()) {
                    BigButton("🚗 보험사", { s.dial(s.vehicle.insurerPhone) }, Modifier.weight(1f))
                }
            }
        }

        if (f.phase == Phase.SCREENING) {
            Text(sc.screening[f.q], style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("예", { f.answer(true) }, Modifier.weight(1f), color = SosRed)
                BigButton("아니요", { f.answer(false) }, Modifier.weight(1f), outlined = true)
            }
            Hint("질문 ${f.q + 1}/${sc.screening.size} · 걱정되면 질문에 답하지 말고 바로 119에 전화하세요.")
        } else {
            // 한 화면에 한 단계
            Text("${f.step + 1}/${sc.steps.size}", style = MaterialTheme.typography.bodyMedium)
            Text(sc.steps[f.step], style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("이전", { f.step-- }, Modifier.weight(1f), enabled = f.step > 0, outlined = true)
                BigButton("다음", { f.step++ }, Modifier.weight(1f), enabled = f.step < sc.steps.lastIndex)
            }
            if (f.step == sc.steps.lastIndex) {
                sc.cautions.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodyLarge, color = WarnAmber) }
            }
            if (sc.id == "passenger" && f.phase == Phase.GUIDE && !f.browseOnly) {
                BigButton("쉴 곳 찾기", { s.requestFacility(null) }, Modifier.fillMaxWidth(), outlined = true)
            }
            if (!f.browseOnly) {
                BigButton("상태가 바뀌었어요 (다시 확인)", { f.recheck() }, Modifier.fillMaxWidth(), outlined = true)
            }
        }
        Hint("임시 시안: 안내 문구는 소방·도로 전문가 검토 전 버전입니다. 앱은 원인을 진단하지 않습니다.")
    }
}

@Composable
private fun ReportPanel(s: AppState, f: Flow?) {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    Panel(title = "신고용 요약 (전화 연결 후 읽어주세요)") {
        Text("시각: ${fmt.format(Date())}", style = MaterialTheme.typography.bodyLarge)
        val loc = s.location
        val at = s.locationAt
        if (loc != null && at != null) {
            Text("위치: 위도 %.5f, 경도 %.5f".format(Locale.US, loc.latitude, loc.longitude), style = MaterialTheme.typography.bodyLarge)
            Hint("위치 확인 시각 ${fmt.format(Date(at))}")
        } else {
            Text("위치: 확인되지 않음", style = MaterialTheme.typography.bodyLarge, color = WarnAmber)
            Hint("MY에서 위치 권한을 확인하세요. 도로 표지판·이정표를 직접 말해 주세요.")
        }
        Text("상황: ${f?.scenario?.title ?: "(아직 선택 안 함)"}", style = MaterialTheme.typography.bodyLarge)
        Text("탑승 인원: ${s.riders.size}명 (${s.riders.joinToString(", ") { it.name + " " + it.ageGroup }})",
            style = MaterialTheme.typography.bodyLarge)
        if (f != null && f.log.isNotEmpty()) {
            Text("확인한 내용", style = MaterialTheme.typography.bodyMedium)
            f.log.forEach { Hint("· $it") }
        }
    }
    Panel(title = "동승자 등록 정보 (사용자 입력)") {
        val withInfo = s.riders.filter { it.hasEmergencyInfo || it.contactPhone.isNotBlank() }
        if (withInfo.isEmpty()) Hint("등록된 응급 참고 정보가 없어요. 등록하지 않아도 SOS는 사용할 수 있어요.")
        withInfo.forEach { m ->
            Text("${m.name} (${m.ageGroup})", style = MaterialTheme.typography.titleMedium)
            if (m.allergies.isNotBlank()) Text("알레르기: ${m.allergies}", style = MaterialTheme.typography.bodyMedium)
            if (m.meds.isNotBlank()) Text("복용약: ${m.meds}", style = MaterialTheme.typography.bodyMedium)
            if (m.conditions.isNotBlank()) Text("기저질환: ${m.conditions}", style = MaterialTheme.typography.bodyMedium)
            if (m.contactPhone.isNotBlank()) {
                BigButton("📞 ${m.contactName.ifBlank { "보호자" }} ${m.contactPhone}", { s.dial(m.contactPhone) },
                    Modifier.fillMaxWidth(), outlined = true)
            }
            Hint("마지막 수정 ${m.updatedAt.ifBlank { "기록 없음" }}")
        }
    }
}
