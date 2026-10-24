package com.contest.dongheng.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.contest.dongheng.data.Destination
import com.contest.dongheng.data.FamilyMember
import com.contest.dongheng.data.Facility
import com.contest.dongheng.data.Place
import com.contest.dongheng.data.PlaceRepository
import com.contest.dongheng.data.Store
import com.contest.dongheng.data.Vehicle
import com.contest.dongheng.navi.NaviFactory
import com.contest.dongheng.navi.NaviGateway
import com.contest.dongheng.navi.NaviResult

enum class Tab(val label: String, val icon: String) {
    HOME("홈", "🏠"), PLACES("장소", "📍"), SOS("SOS", "🆘"), FAMILY("가족", "👨‍👩‍👧"), MY("MY", "⚙️")
}

enum class CallState(val label: String) {
    NONE(""),
    REQUESTED("전화 연결을 요청했어요. 실제 통화 연결 여부는 앱이 확인할 수 없어요."),
    FAILED("전화 앱을 열 수 없어요. 직접 전화해 주세요. (연결 여부 미확인)"),
}

/** 앱 전역 상태. 화면 간 공유되는 이동·가족·내비 상태를 한 곳에서 관리한다. */
class AppState(private val ctx: Context) {
    private val store = Store(ctx)
    val navi: NaviGateway = NaviFactory.create(ctx)

    var tab by mutableStateOf(Tab.HOME)
    var members by mutableStateOf(store.loadMembers())
        private set
    var riderIds by mutableStateOf(store.loadRiders(members))
        private set
    var vehicle by mutableStateOf(store.loadVehicle())
        private set

    // 이동
    var destination by mutableStateOf<Destination?>(null)
    var progressKm by mutableStateOf(0f)
    var plannedStop by mutableStateOf<Place?>(null)
    var driving by mutableStateOf(false)
    var lastRestAt by mutableStateOf(System.currentTimeMillis())
    val checked = mutableStateListOf<String>()

    // 장소 찾기 조건
    var required by mutableStateOf(riderNeeds())
    var preferred by mutableStateOf<Set<Facility>>(emptySet())

    // 내비·전화·위치
    var naviStatus by mutableStateOf("연결 중...")
    var naviMessage by mutableStateOf("")
    var callState by mutableStateOf(CallState.NONE)
    /** 홈의 안전 가이드 바로가기에서 SOS 화면으로 넘기는 시나리오 id (열람 전용) */
    var sosGuide by mutableStateOf<String?>(null)
    var location by mutableStateOf<Location?>(null)
    var locationAt by mutableStateOf<Long?>(null)
    var locationGranted by mutableStateOf(false)
    var micGranted by mutableStateOf(false)
    /** MainActivity가 채워 주는 마이크 권한 요청 동작 */
    var requestMic: () -> Unit = {}
    val voice = VoiceInput(ctx)
    private var locListener: LocationListener? = null

    val riders get() = members.filter { it.id in riderIds }
    fun riderNeeds(): Set<Facility> = riders.flatMap { it.needs }.toSet()
    val restIntervalMin get() = riders.mapNotNull { it.restIntervalMin }.minOrNull() ?: 120

    init {
        navi.connect { r -> naviStatus = if (r.ok) "연결됨 - ${r.message}" else r.message }
    }

    // --- 가족 ---
    fun toggleRider(id: String) {
        riderIds = if (id in riderIds) riderIds - id else riderIds + id
        store.saveRiders(riderIds)
        required = riderNeeds()
    }

    fun upsertMember(m: FamilyMember) {
        members = if (members.any { it.id == m.id }) members.map { if (it.id == m.id) m else it } else members + m
        store.saveMembers(members)
        if (m.id !in riderIds && members.size == 1) toggleRider(m.id)
        required = riderNeeds()
    }

    fun deleteMember(id: String) {
        members = members.filter { it.id != id }
        riderIds = riderIds - id
        store.saveMembers(members)
        store.saveRiders(riderIds)
        required = riderNeeds()
    }

    fun saveVehicle(v: Vehicle) {
        vehicle = v
        store.saveVehicle(v)
    }

    fun wipeAll() {
        store.clearAll()
        members = store.loadMembers()
        riderIds = store.loadRiders(members)
        vehicle = Vehicle()
        endTrip()
    }

    // --- 이동 ---
    fun startTrip(d: Destination) {
        destination = d
        progressKm = 0f
        plannedStop = null
        lastRestAt = System.currentTimeMillis()
        checked.clear()
        required = riderNeeds()
        report(navi.setDestination(d))
    }

    fun endTrip() {
        if (destination != null) navi.removeWaypoints()
        destination = null
        plannedStop = null
        progressKm = 0f
    }

    fun requestFacility(f: Facility?) {
        required = riderNeeds() + listOfNotNull(f)
        tab = Tab.PLACES
    }

    fun guideTo(p: Place) {
        val r = navi.addWaypoint(p)
        report(r)
        if (r.ok) plannedStop = p
    }

    fun rested() {
        lastRestAt = System.currentTimeMillis()
    }

    private fun report(r: NaviResult) {
        naviMessage = r.message
    }

    // --- 전화 ---
    /** 직접 발신하지 않고 다이얼 화면만 연다. 신고 완료로 표시하지 않는다. */
    fun dial(number: String) {
        val clean = number.filter { it.isDigit() || it == '+' }
        if (clean.isBlank()) return
        callState = try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CallState.REQUESTED
        } catch (t: Throwable) {
            CallState.FAILED
        }
    }

    // --- 위치 ---
    @SuppressLint("MissingPermission")
    fun startLocation() {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        runCatching {
            if (locListener != null) return
            val listener = LocationListener { loc -> location = loc; locationAt = System.currentTimeMillis() }
            locListener = listener
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (lm.isProviderEnabled(p)) {
                    lm.getLastKnownLocation(p)?.let { location = it; locationAt = it.time }
                    lm.requestLocationUpdates(p, 10_000L, 20f, listener)
                }
            }
        }
    }

    fun release() {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        locListener?.let { runCatching { lm?.removeUpdates(it) } }
        locListener = null
        voice.release()
        navi.release()
    }

    val destinations get() = PlaceRepository.destinations
}
