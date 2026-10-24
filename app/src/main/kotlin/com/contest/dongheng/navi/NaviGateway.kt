package com.contest.dongheng.navi

import android.content.Context
import com.contest.dongheng.data.Destination
import com.contest.dongheng.data.Place

/** 내비게이션 요청 결과. 요청 접수와 내비 쪽 확정은 구분해서 표시한다. */
data class NaviResult(val ok: Boolean, val message: String)

/** 앱이 의존하는 내비 추상화. Pleos SDK 의존은 [PleosNaviGateway] 한 곳에만 둔다. */
interface NaviGateway {
    val name: String
    fun connect(onResult: (NaviResult) -> Unit)
    fun setDestination(d: Destination): NaviResult
    fun addWaypoint(p: Place): NaviResult
    fun removeWaypoints(): NaviResult
    fun release()
}

/** SDK가 없는 환경(일반 에뮬레이터 등)에서 동작을 시연하는 가상 내비. 실제 경로는 만들지 않는다. */
class FakeNaviGateway : NaviGateway {
    override val name = "가상 내비(시연용)"
    override fun connect(onResult: (NaviResult) -> Unit) =
        onResult(NaviResult(true, "가상 내비 사용 중 - 실제 경로 안내는 되지 않습니다"))

    override fun setDestination(d: Destination) = NaviResult(true, "[가상] 목적지 '${d.name}' 설정")
    override fun addWaypoint(p: Place) = NaviResult(true, "[가상] 경유지 '${p.name}' 등록 요청 (실제 내비 반영 아님)")
    override fun removeWaypoints() = NaviResult(true, "[가상] 경유지 삭제")
    override fun release() {}
}

object NaviFactory {
    /** Pleos 빌드면 PleosNaviGateway를, 아니거나 로딩에 실패하면 가상 내비를 쓴다. */
    fun create(ctx: Context): NaviGateway = try {
        Class.forName("com.contest.dongheng.navi.PleosNaviGateway")
            .getConstructor(Context::class.java).newInstance(ctx.applicationContext) as NaviGateway
    } catch (t: Throwable) {
        FakeNaviGateway()
    }
}
