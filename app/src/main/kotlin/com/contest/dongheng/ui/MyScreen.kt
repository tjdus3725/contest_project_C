package com.contest.dongheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
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
import com.contest.dongheng.data.Vehicle

@Composable
fun MyScreen(s: AppState, requestLocation: () -> Unit) {
    var editVehicle by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("MY", style = MaterialTheme.typography.headlineMedium) }

        item {
            Panel(title = "차량·보험") {
                val v = s.vehicle
                Text("차량: ${v.nickname.ifBlank { "미등록" }} ${v.plate}", style = MaterialTheme.typography.bodyLarge)
                Text("보험사: ${v.insurer.ifBlank { "미등록" }} ${v.insurerPhone}", style = MaterialTheme.typography.bodyLarge)
                Hint("보험사 긴급출동 번호는 SOS 화면에서 바로 발신 화면을 열 수 있어요.")
                BigButton("편집", { editVehicle = true }, enabled = !s.driving, outlined = true)
            }
        }
        item {
            Panel(title = "내비게이션 연결") {
                Text(s.navi.name, style = MaterialTheme.typography.bodyLarge)
                Text("상태: ${s.naviStatus}", style = MaterialTheme.typography.bodyMedium)
                if (s.naviMessage.isNotBlank()) Hint("최근 요청: ${s.naviMessage}")
            }
        }
        item {
            Panel(title = "권한") {
                Text("위치: " + if (s.locationGranted) "허용됨 (SOS 신고용 요약에 사용)" else "허용 안 됨", style = MaterialTheme.typography.bodyLarge)
                if (!s.locationGranted) BigButton("위치 권한 요청", requestLocation)
                Hint("마이크 권한은 음성 입력 기능을 추가할 때 요청합니다.")
            }
        }
        item {
            Panel(title = "개인정보") {
                Text("가족·차량 정보는 이 기기에만 저장되며 서버로 전송하지 않아요.", style = MaterialTheme.typography.bodyLarge)
                Text("위치는 SOS 요약 표시에만 쓰고 기록으로 저장하지 않아요.", style = MaterialTheme.typography.bodyLarge)
                BigButton("저장된 정보 모두 삭제", { confirmWipe = true }, enabled = !s.driving, color = SosRed)
            }
        }
        item {
            Panel(title = "앱 정보") {
                Text("동행 0.1.0 (공모전 시제품)", style = MaterialTheme.typography.bodyLarge)
                Hint("장소 데이터는 시연용 샘플이며 실제 시설 현황이 아닙니다. 대처 가이드는 앱에 포함되어 통신 없이도 열람되지만, 전문가 검토 전 시안입니다.")
            }
        }
    }

    if (editVehicle) VehicleDialog(s.vehicle, { editVehicle = false }) { s.saveVehicle(it); editVehicle = false }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("모두 삭제할까요?") },
            text = { Text("가족·차량 정보가 삭제되고 되돌릴 수 없어요.") },
            confirmButton = { TextButton({ s.wipeAll(); confirmWipe = false }) { Text("삭제", color = SosRed) } },
            dismissButton = { TextButton({ confirmWipe = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun VehicleDialog(v: Vehicle, onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    var nick by remember { mutableStateOf(v.nickname) }
    var plate by remember { mutableStateOf(v.plate) }
    var ins by remember { mutableStateOf(v.insurer) }
    var phone by remember { mutableStateOf(v.insurerPhone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("차량·보험 정보") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nick, { nick = it }, label = { Text("차량 별칭") }, singleLine = true)
                OutlinedTextField(plate, { plate = it }, label = { Text("차량번호") }, singleLine = true)
                OutlinedTextField(ins, { ins = it }, label = { Text("보험사") }, singleLine = true)
                OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("긴급출동 연락처") }, singleLine = true)
            }
        },
        confirmButton = { TextButton({ onSave(Vehicle(nick, plate, ins, phone)) }) { Text("저장") } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}
