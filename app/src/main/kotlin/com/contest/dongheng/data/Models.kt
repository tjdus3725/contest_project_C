package com.contest.dongheng.data

enum class Facility(val label: String, val icon: String) {
    RESTROOM("화장실", "🚻"),
    DIAPER("기저귀 교환대", "👶"),
    NURSING("수유실", "🍼"),
    STROLLER("유모차 접근", "🛒"),
    FOOD("식사", "🍽️"),
}

data class FamilyMember(
    val id: String,
    val name: String,
    val ageGroup: String,
    val needs: Set<Facility> = emptySet(),
    val restIntervalMin: Int? = null,
    val allergies: String = "",
    val meds: String = "",
    val conditions: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val packing: List<String> = emptyList(),
    val updatedAt: String = "",
) {
    val hasEmergencyInfo get() = allergies.isNotBlank() || meds.isNotBlank() || conditions.isNotBlank()
}

data class Vehicle(
    val nickname: String = "",
    val plate: String = "",
    val insurer: String = "",
    val insurerPhone: String = "",
)

data class Destination(val name: String, val lat: Double, val lng: Double, val km: Float, val address: String)

/** 경부선 하행 기준 시연용 장소. [km]는 서울 기점 누적거리(근사). */
data class Place(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val km: Float,
    val address: String,
    /** 직접 확인된 시설 */
    val confirmed: Set<Facility>,
    /** 없음이 확인된 시설. confirmed·notAvailable 둘 다 아니면 미확인 */
    val notAvailable: Set<Facility> = emptySet(),
    val hours: String? = null,
    /** 본선 이탈 후 복귀까지 추가 주행 시간(분). 없으면 계산 불가 */
    val detourMin: Int? = null,
    val source: String,
    val verifiedAt: String? = null,
)

enum class FacilityStatus { CONFIRMED, UNKNOWN, UNAVAILABLE }

fun Place.statusOf(f: Facility) = when (f) {
    in confirmed -> FacilityStatus.CONFIRMED
    in notAvailable -> FacilityStatus.UNAVAILABLE
    else -> FacilityStatus.UNKNOWN
}

data class Recommendation(
    val place: Place,
    val distanceKm: Float,
    val etaMin: Int,
    val requiredOk: Boolean,
    val preferredHit: Int,
    val reason: String,
)
