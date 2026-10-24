package com.contest.dongheng.data

import com.contest.dongheng.data.Facility.*

/**
 * 시연용 샘플 데이터. 장소명은 경부고속도로 휴게소를 따랐지만 좌표·거리·시설 정보는
 * 근사/가상 값이며 실제 시설 현황이 아니다. 실서비스에서는 공공데이터·운영사 API로 교체한다.
 */
object PlaceRepository {
    private const val SRC = "시연용 샘플 데이터"

    val destinations = listOf(
        Destination("대전", 36.3504, 127.3845, 160f, "대전광역시"),
        Destination("대구", 35.8714, 128.6014, 290f, "대구광역시"),
        Destination("부산", 35.1796, 129.0756, 420f, "부산광역시"),
    )

    val places = listOf(
        Place("jukjeon", "죽전휴게소(하행)", 37.3250, 127.1050, 27f, "경기 용인시 수지구",
            setOf(RESTROOM, DIAPER, FOOD, STROLLER), hours = "24시간", detourMin = 4, source = SRC, verifiedAt = "2026-09-01"),
        Place("giheung", "기흥휴게소(하행)", 37.2650, 127.1000, 36f, "경기 용인시 기흥구",
            setOf(RESTROOM, FOOD), notAvailable = setOf(DIAPER), hours = "24시간", detourMin = 5, source = SRC, verifiedAt = "2026-09-01"),
        Place("anseong", "안성휴게소(하행)", 36.9900, 127.2100, 70f, "경기 안성시",
            setOf(RESTROOM, DIAPER, NURSING, FOOD, STROLLER), hours = "24시간", detourMin = 6, source = SRC, verifiedAt = "2026-08-20"),
        Place("cheonan", "천안휴게소(하행)", 36.8300, 127.2000, 85f, "충남 천안시",
            setOf(RESTROOM, FOOD), hours = "06:00-24:00", detourMin = 5, source = SRC, verifiedAt = "2026-07-15"),
        Place("oksan", "옥산휴게소(하행)", 36.6500, 127.5000, 150f, "충북 청주시",
            setOf(RESTROOM, FOOD, STROLLER), detourMin = 5, source = SRC),
        Place("geumgang", "금강휴게소(하행)", 36.3000, 127.6200, 190f, "충북 옥천군",
            setOf(RESTROOM, DIAPER, NURSING, FOOD), hours = "24시간", detourMin = 7, source = SRC, verifiedAt = "2026-08-02"),
        Place("chupungnyeong", "추풍령휴게소(하행)", 36.2200, 127.9900, 230f, "충북 영동군",
            setOf(RESTROOM, DIAPER, FOOD), hours = "24시간", detourMin = 4, source = SRC, verifiedAt = "2026-06-10"),
        Place("chilgok", "칠곡휴게소(하행)", 36.0500, 128.4000, 285f, "경북 칠곡군",
            setOf(RESTROOM, DIAPER, NURSING, FOOD, STROLLER), hours = "24시간", detourMin = 6, source = SRC, verifiedAt = "2026-09-12"),
        Place("gyeongju", "경주휴게소(하행)", 35.8400, 129.2200, 340f, "경북 경주시",
            setOf(RESTROOM, FOOD), hours = "24시간", detourMin = 5, source = SRC, verifiedAt = "2026-05-30"),
        Place("eonyang", "언양휴게소(하행)", 35.5600, 129.1200, 385f, "울산 울주군",
            setOf(RESTROOM, DIAPER, FOOD), hours = "24시간", detourMin = 5, source = SRC),
    )

    private const val AVG_SPEED_KMH = 85f

    fun etaMin(distanceKm: Float) = (distanceKm / AVG_SPEED_KMH * 60f).toInt().coerceAtLeast(1)

    /**
     * 현재 위치([progressKm]) 앞쪽, 목적지([destKm]) 이전의 후보를 추천한다.
     * - 필수 시설은 확인됨일 때만 충족으로 본다. 미확인은 충족으로 처리하지 않는다.
     * - 없음이 확인된 필수 시설이 있으면 후보에서 제외한다.
     * - 필수 조건은 자동으로 완화하지 않는다(범위 확대·조건 변경은 사용자가 선택).
     */
    fun recommend(
        required: Set<Facility>,
        preferred: Set<Facility>,
        progressKm: Float,
        destKm: Float,
        rangeKm: Float,
    ): List<Recommendation> = places
        .filter { it.km > progressKm && it.km < destKm && it.km - progressKm <= rangeKm }
        .filter { p -> required.none { p.statusOf(it) == FacilityStatus.UNAVAILABLE } }
        .map { p ->
            val d = p.km - progressKm
            val ok = required.all { p.statusOf(it) == FacilityStatus.CONFIRMED }
            val hit = preferred.count { p.statusOf(it) == FacilityStatus.CONFIRMED }
            val have = (required + preferred).filter { p.statusOf(it) == FacilityStatus.CONFIRMED }
            val reason = if (have.isEmpty()) "조건에 맞는 시설 정보가 아직 확인되지 않았어요"
            else have.joinToString(", ") { it.label } + " 이용 가능으로 확인됐어요"
            Recommendation(p, d, etaMin(d), ok, hit, reason)
        }
        .sortedWith(
            compareByDescending<Recommendation> { it.requiredOk }
                .thenByDescending { it.preferredHit }
                .thenBy { it.distanceKm }
        )
}
