package com.contest.dongheng.navi

import android.content.Context
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.helper.NaviHelper
import com.contest.dongheng.data.Destination
import com.contest.dongheng.data.Place

/**
 * Pleos Connect NaviHelper SDK(3.0.5) 연동.
 * 흐름: NaviHelper 생성 -> initialize(onConnected/onFailed) -> requestRoute / addWaypoint -> release.
 *
 * 주의: 경유지 반영 여부는 NaviHelperEventListener 콜백(onWaypointChanged 등)으로 확정된다.
 * 현재는 '요청 접수'까지만 표시하며, 콜백 연결은 에뮬레이터에서 시그니처 확인 후 추가한다.
 * import 패키지 경로(data/constants)는 공식 문서 예제에 명시되지 않아 SDK API Reference로 확인이 필요하다.
 */
class PleosNaviGateway(ctx: Context) : NaviGateway {
    override val name = "Pleos NaviHelper 3.0.5"
    private val helper = NaviHelper(ctx)
    @Volatile private var connected = false

    override fun connect(onResult: (NaviResult) -> Unit) {
        helper.initialize(
            onConnected = {
                connected = true
                onResult(NaviResult(true, "내비 서비스 연결됨"))
            },
            onFailed = { t ->
                connected = false
                onResult(NaviResult(false, "내비 연결 실패: ${t.message ?: "원인 미확인"}"))
            },
        )
    }

    override fun setDestination(d: Destination): NaviResult {
        if (!connected) return NaviResult(false, "내비가 연결되지 않았어요")
        return runCatching {
            helper.requestRoute(
                RouteInfo(
                    latitude = d.lat,
                    longitude = d.lng,
                    displayLongitude = null,
                    displayLatitude = null,
                    poiName = d.name,
                    poiId = "demo-${d.name}",
                    poiSubId = "0",
                    address = d.address,
                )
            )
            NaviResult(true, "목적지 '${d.name}' 경로 탐색 요청됨 (완료는 내비에서 확인)")
        }.getOrElse { NaviResult(false, "목적지 요청 실패: ${it.message}") }
    }

    override fun addWaypoint(p: Place): NaviResult {
        if (!connected) return NaviResult(false, "내비가 연결되지 않았어요")
        return runCatching {
            helper.addWaypoint(
                RequestWaypointInfo(
                    latitude = p.lat,
                    longitude = p.lng,
                    waypointIndex = WaypointIndex.FIRST,
                    poiId = "demo-${p.id}",
                    poiName = p.name,
                    poiSubId = "0",
                    address = p.address,
                )
            )
            NaviResult(true, "경유지 '${p.name}' 등록 요청됨 (내비 반영은 내비 화면에서 확인)")
        }.getOrElse { NaviResult(false, "경유지 요청 실패: ${it.message}") }
    }

    override fun removeWaypoints(): NaviResult = runCatching {
        helper.removeWaypoint(WaypointIndex.ALL)
        NaviResult(true, "경유지 삭제 요청됨")
    }.getOrElse { NaviResult(false, "경유지 삭제 실패: ${it.message}") }

    override fun release() {
        runCatching { helper.release() }
    }
}
