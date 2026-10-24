package com.leanlog.app.domain

import com.leanlog.app.core.AdaptiveTdeeResult
import com.leanlog.app.core.CardioType
import com.leanlog.app.core.Gender
import com.leanlog.app.core.LeanLogMath
import com.leanlog.app.core.WeightPoint
import com.leanlog.app.data.ProfileEntity
import com.leanlog.app.data.WeightLogEntity
import org.json.JSONObject

/**
 * 把 LeanLogMath 的纯公式接到真实数据上。
 * 界面只调用这里，不直接调公式，避免出现第二套换算。
 */
object Figures {

    fun gender(profile: ProfileEntity): Gender =
        if (profile.gender == "FEMALE") Gender.FEMALE else Gender.MALE

    fun bmr(profile: ProfileEntity): Double =
        LeanLogMath.bmr(gender(profile), profile.weightKg, profile.heightCm, profile.age)

    /** 不含训练的日常消耗，训练消耗单独加。 */
    fun tdeeBase(profile: ProfileEntity): Double =
        LeanLogMath.tdeeBase(bmr(profile), profile.activityFactor)

    fun proteinTarget(profile: ProfileEntity): Int =
        LeanLogMath.proteinTarget(profile.weightKg, profile.proteinAuto, profile.proteinCustomG)

    fun targetDeficit(profile: ProfileEntity): Int =
        LeanLogMath.targetDeficit(profile.weeklyRateKg)

    // ---------------------------------------------------------------- 力量

    data class StrengthCalc(val minutes: Double, val gross: Double, val net: Double)

    fun strengthCalc(
        exerciseMet: Double,
        weightKg: Double,
        weightValue: Double,
        reps: Int,
        sets: Int,
        bodyweight: Boolean,
        bmr: Double,
        restSec: Int = 90,
    ): StrengthCalc {
        val minutes = LeanLogMath.strengthMinutes(sets, reps, restSec)
        // 自重的强度由体重决定；负重的强度按 1RM 比例粗略修正（重量越大 MET 越高）
        val effectiveMet = if (bodyweight) {
            exerciseMet
        } else {
            exerciseMet * (1.0 + (weightValue / 100.0) * 0.25)
        }
        val gross = LeanLogMath.grossKcal(effectiveMet, weightKg, minutes)
        return StrengthCalc(minutes = minutes, gross = gross, net = LeanLogMath.netKcal(gross, bmr, minutes))
    }

    // ---------------------------------------------------------------- 有氧

    data class CardioCalc(val minutes: Double, val met: Double, val gross: Double, val net: Double)

    fun cardioCalc(
        type: CardioType,
        params: Map<String, Double>,
        enumValue: String?,
        bmr: Double,
        weightKg: Double,
    ): CardioCalc {
        val met = type.met(params, enumValue)
        val minutes = params["min"] ?: 30.0
        val gross = LeanLogMath.grossKcal(met, weightKg, minutes)
        return CardioCalc(minutes, met, gross, LeanLogMath.netKcal(gross, bmr, minutes))
    }

    // ---------------------------------------------------------------- 自适应 TDEE

    fun adaptive(
        weights: List<WeightLogEntity>,
        intakeByDay: Map<Int, Double>,
        previous: Double? = null,
    ): AdaptiveTdeeResult = LeanLogMath.adaptiveTdee(
        points = weights.map { WeightPoint(it.day, it.kg) },
        intakeByDay = intakeByDay,
        previousEstimate = previous,
    )

    /**
     * 公式值 vs 实测值。偏差 > 800 时取中位数（PRD §2.6），
     * 否则一条水肿数据就能把整月日历染红。
     */
    data class TdeeCompare(val formula: Double, val adaptive: Double?, val used: Double, val anomaly: Boolean, val note: String?)

    fun compareTdee(profile: ProfileEntity, adaptive: AdaptiveTdeeResult): TdeeCompare {
        val formula = tdeeBase(profile)
        val value = adaptive.value ?: return TdeeCompare(formula, null, formula, false, adaptive.reason)
        val reconciled = LeanLogMath.reconcileTdee(value, formula)
        return TdeeCompare(formula, value, reconciled.value, reconciled.anomaly, reconciled.note)
    }
}

/** 有氧参数快照的存取。不同项目字段差异太大，不适合拍平成列。 */
object CardioParams {

    fun encode(params: Map<String, Double>): String {
        val json = JSONObject()
        params.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    fun decode(raw: String?): Map<String, Double> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.optDouble(it, 0.0) }
        }.getOrDefault(emptyMap())
    }

    /** 参数摘要，列表里显示「坡度 10% · 速度 4.5 km/h · 35 分钟」 */
    fun summary(type: CardioType, params: Map<String, Double>, enumValue: String?): String {
        val knobs = type.params.mapNotNull { spec ->
            val v = params[spec.key] ?: return@mapNotNull null
            val shown = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            "${spec.label} $shown${spec.unit}"
        }
        return (listOfNotNull(enumValue) + knobs).joinToString(" · ")
    }
}
