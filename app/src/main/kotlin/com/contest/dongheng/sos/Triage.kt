package com.contest.dongheng.sos

import com.contest.dongheng.data.Facility

/**
 * SOS 상황 분류. 규칙 기반이며 질병·고장 원인을 진단하지 않는다.
 * 안내 문구는 일반적으로 알려진 안전 수칙을 담은 시안이며, 실서비스 전 소방·도로 전문가 검토가 필요하다.
 * 위험 신호가 확인되면 추가 문답 없이 119 연결을 우선한다.
 */
enum class Call(val label: String, val number: String) {
    FIRE119("119 (소방·구급)", "119"),
    POLICE112("112 (경찰)", "112"),
    HIGHWAY("도로공사 콜센터", "1588-2504"),
}

data class Scenario(
    val id: String,
    val title: String,
    val icon: String,
    val keywords: List<String>,
    /** 하나라도 매칭되면 문답 없이 즉시 도움 권고 */
    val immediateKeywords: List<String>,
    /** 한 번에 하나씩 묻는 위험 신호 질문. 하나라도 '예'면 즉시 도움 권고 */
    val screening: List<String>,
    /** 항상 즉시 도움 권고(화재·침수 등) */
    val alwaysImmediate: Boolean,
    val steps: List<String>,
    val cautions: List<String>,
    val primaryCall: Call,
    val useInsurer: Boolean = false,
)

object Triage {
    val scenarios = listOf(
        Scenario(
            "passenger", "동승자 이상", "🧒",
            keywords = listOf("아프", "아파", "토했", "구토", "열이", "열나", "속이안", "배탈", "어지", "울음", "이상해"),
            immediateKeywords = listOf("의식", "숨", "호흡", "경련", "쓰러", "기절", "피가", "피를", "출혈", "반응이 없", "안 깨"),
            screening = listOf(
                "의식이 흐리거나 부르는 말에 반응이 없나요?",
                "숨쉬기 힘들어하거나 얼굴·입술색이 변했나요?",
                "경련을 하거나 심한 출혈·큰 외상이 있나요?",
            ),
            alwaysImmediate = false,
            steps = listOf(
                "가까운 안전한 곳에 차를 세우세요. 갓길 정차는 비상등을 켜고, 가능하면 휴게소·졸음쉼터를 이용하세요.",
                "동승자를 편안한 자세로 두고 곁에서 상태를 계속 살펴보세요.",
                "상태가 나빠지거나 걱정되면 바로 119에 연결하세요. 통화 중에는 상담원의 안내를 따르세요.",
                "증상이 가볍다면 장소 찾기에서 필요한 시설이 있는 휴식 장소를 찾을 수 있어요.",
            ),
            cautions = listOf("임의로 약이나 음식을 먹이지 마세요.", "상태가 바뀌면 이 화면에서 다시 확인하세요."),
            primaryCall = Call.FIRE119,
        ),
        Scenario(
            "accident", "사고", "💥",
            keywords = listOf("사고", "추돌", "충돌", "박았", "부딪"),
            immediateKeywords = listOf("다쳤", "부상", "의식", "피가", "끼였"),
            screening = listOf(
                "다친 사람이 있나요?",
                "차가 도로 위에 멈춰 있어 2차 사고 위험이 큰가요?",
            ),
            alwaysImmediate = false,
            steps = listOf(
                "비상등을 켜고 안전하다면 차를 갓길 등 안전한 곳으로 옮기세요.",
                "엔진을 끄고, 탑승자는 안전하게 가드레일 밖 등 차량에서 떨어진 곳으로 대피하세요.",
                "고속도로에서는 차량 후방 멀리에 안전삼각대를 설치하세요. 주변 차량 흐름을 먼저 확인하세요.",
                "부상자가 있거나 도로가 위험하면 119/112에 연결하고 위치를 알리세요.",
                "보험사에 사고 접수를 하세요.",
            ),
            cautions = listOf("부상자를 함부로 옮기지 마세요.", "도로 위에서 오래 서 있지 마세요."),
            primaryCall = Call.FIRE119,
            useInsurer = true,
        ),
        Scenario(
            "breakdown", "고장", "🔧",
            keywords = listOf("고장", "시동", "멈췄", "펑크", "타이어", "배터리", "경고등"),
            immediateKeywords = emptyList(),
            screening = listOf("차가 도로 한가운데나 차선 위에 멈춰 있어 위험한가요?"),
            alwaysImmediate = false,
            steps = listOf(
                "비상등을 켜고 가능하면 갓길·안전한 곳으로 천천히 이동하세요.",
                "탑승자는 안전한 곳(가드레일 밖 등)으로 대피하세요.",
                "차량 후방 멀리에 안전삼각대를 설치하세요.",
                "보험사 긴급출동 또는 도로공사 콜센터에 연락하세요.",
            ),
            cautions = listOf("고속도로 갓길에서는 차 안에 머무르지 마세요."),
            primaryCall = Call.HIGHWAY,
            useInsurer = true,
        ),
        Scenario(
            "fire", "연기·화재", "🔥",
            keywords = listOf("연기", "불이", "불났", "화재", "타는 냄새", "탄내"),
            immediateKeywords = emptyList(),
            screening = emptyList(),
            alwaysImmediate = true,
            steps = listOf(
                "즉시 안전한 곳에 차를 세우고 시동을 끄세요.",
                "모든 탑승자를 차에서 내리게 하고 차량에서 멀리 떨어지세요.",
                "119에 신고하고 위치를 알리세요.",
            ),
            cautions = listOf("보닛을 함부로 열지 마세요.", "짐을 챙기려고 차로 돌아가지 마세요."),
            primaryCall = Call.FIRE119,
        ),
        Scenario(
            "flood", "침수", "🌊",
            keywords = listOf("침수", "물이 차", "물에 잠", "잠겼", "물이 들어"),
            immediateKeywords = emptyList(),
            screening = emptyList(),
            alwaysImmediate = true,
            steps = listOf(
                "물이 차오르면 지체하지 말고 탑승자 전원이 즉시 탈출하세요.",
                "문이 안 열리면 창문을 열거나 비상 탈출 도구로 창문을 깨고 나가세요.",
                "높은 곳으로 이동한 뒤 119에 신고하세요.",
            ),
            cautions = listOf("차 안에서 시동을 다시 걸려고 하지 마세요."),
            primaryCall = Call.FIRE119,
        ),
    )

    /** 장소 찾기로 바로 연결되는 일상 요청 키워드 */
    private val placeRequests = listOf(
        listOf("화장실", "소변", "쉬", "응가") to Facility.RESTROOM,
        listOf("기저귀") to Facility.DIAPER,
        listOf("수유", "젖") to Facility.NURSING,
        listOf("밥", "식사", "배고", "배고파") to Facility.FOOD,
    )

    sealed interface Match {
        data class Emergency(val scenario: Scenario, val immediate: Boolean) : Match
        data class Place(val facility: Facility) : Match
        object Unknown : Match
    }

    fun classify(text: String): Match {
        val t = text.replace(" ", "")
        // 위험 신호가 있으면 어떤 일상 요청보다 우선한다.
        scenarios.firstOrNull { s -> s.immediateKeywords.any { t.contains(it.replace(" ", "")) } }
            ?.let { return Match.Emergency(it, true) }
        scenarios.firstOrNull { s -> s.keywords.any { t.contains(it.replace(" ", "")) } }
            ?.let { return Match.Emergency(it, it.alwaysImmediate) }
        placeRequests.firstOrNull { (keys, _) -> keys.any { t.contains(it) } }
            ?.let { return Match.Place(it.second) }
        return Match.Unknown
    }
}
