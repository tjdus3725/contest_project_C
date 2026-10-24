package com.contest.dongheng.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 차량 가로 화면 기준 레이아웃: 좌측 내비게이션 레일(홈·장소·SOS·가족·MY) + 본문.
 * SOS는 가운데에 크고 붉게 두고 어느 화면에서든 한 번에 진입한다.
 */
@Composable
fun App(s: AppState, requestLocation: () -> Unit) {
    DonghengTheme {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Rail(s)
            Box(Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                when (s.tab) {
                    Tab.HOME -> HomeScreen(s) { id -> s.sosGuide = id; s.tab = Tab.SOS }
                    Tab.PLACES -> PlacesScreen(s)
                    Tab.SOS -> SosScreen(s)
                    Tab.FAMILY -> FamilyScreen(s)
                    Tab.MY -> MyScreen(s, requestLocation)
                }
            }
        }
    }
}

@Composable
private fun Rail(s: AppState) {
    Column(
        Modifier.width(120.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Tab.values().forEach { t ->
            if (t == Tab.SOS) SosButton(selected = s.tab == t) { s.tab = t }
            else RailItem(t, s.tab == t) { s.tab = t }
        }
    }
}

@Composable
private fun RailItem(t: Tab, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    Column(
        Modifier.width(100.dp).clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(t.icon, fontSize = 30.sp)
        Text(
            t.label, fontSize = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SosButton(selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(if (selected) 104.dp else 96.dp).clip(CircleShape).background(SosRed).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("SOS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
    }
}
