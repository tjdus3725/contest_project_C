package com.contest.dongheng.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** SharedPreferences(JSON) 기반 로컬 저장소. 서버 전송 없음. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dongheng", Context.MODE_PRIVATE)

    fun loadMembers(): List<FamilyMember> {
        val raw = sp.getString("members", null) ?: return seed()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { memberFrom(arr.getJSONObject(it)) }
        }.getOrElse { seed() }
    }

    fun saveMembers(list: List<FamilyMember>) {
        sp.edit().putString("members", JSONArray(list.map { memberTo(it) }).toString()).apply()
    }

    fun loadRiders(all: List<FamilyMember>): Set<String> {
        val raw = sp.getString("riders", null) ?: return all.map { it.id }.toSet()
        return runCatching { JSONArray(raw).let { a -> List(a.length()) { a.getString(it) }.toSet() } }
            .getOrDefault(emptySet())
    }

    fun saveRiders(ids: Set<String>) {
        sp.edit().putString("riders", JSONArray(ids.toList()).toString()).apply()
    }

    fun loadVehicle(): Vehicle {
        val raw = sp.getString("vehicle", null) ?: return Vehicle()
        return runCatching {
            val o = JSONObject(raw)
            Vehicle(o.optString("nickname"), o.optString("plate"), o.optString("insurer"), o.optString("insurerPhone"))
        }.getOrDefault(Vehicle())
    }

    fun saveVehicle(v: Vehicle) {
        val o = JSONObject().put("nickname", v.nickname).put("plate", v.plate)
            .put("insurer", v.insurer).put("insurerPhone", v.insurerPhone)
        sp.edit().putString("vehicle", o.toString()).apply()
    }

    fun clearAll() = sp.edit().clear().apply()

    private fun seed(): List<FamilyMember> {
        val today = LocalDate.now().toString()
        return listOf(
            FamilyMember("m1", "보호자", "성인", updatedAt = today),
            FamilyMember(
                "m2", "아이", "유아(0~3세)",
                needs = setOf(Facility.DIAPER, Facility.STROLLER), restIntervalMin = 90,
                packing = listOf("기저귀", "물티슈", "여벌 옷", "물"), updatedAt = today,
            ),
        )
    }

    private fun memberTo(m: FamilyMember) = JSONObject()
        .put("id", m.id).put("name", m.name).put("ageGroup", m.ageGroup)
        .put("needs", JSONArray(m.needs.map { it.name }))
        .put("rest", m.restIntervalMin ?: JSONObject.NULL)
        .put("allergies", m.allergies).put("meds", m.meds).put("conditions", m.conditions)
        .put("contactName", m.contactName).put("contactPhone", m.contactPhone)
        .put("packing", JSONArray(m.packing)).put("updatedAt", m.updatedAt)

    private fun memberFrom(o: JSONObject): FamilyMember {
        val needs = o.optJSONArray("needs")?.let { a ->
            List(a.length()) { a.getString(it) }
                .mapNotNull { n -> Facility.values().firstOrNull { it.name == n } }.toSet()
        } ?: emptySet()
        val pack = o.optJSONArray("packing")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList()
        return FamilyMember(
            o.getString("id"), o.getString("name"), o.optString("ageGroup"), needs,
            if (o.isNull("rest")) null else o.optInt("rest"),
            o.optString("allergies"), o.optString("meds"), o.optString("conditions"),
            o.optString("contactName"), o.optString("contactPhone"), pack, o.optString("updatedAt"),
        )
    }
}
