package com.leanlog.app.core

import kotlin.math.max

/**
 * 全局唯一的计算口径，对应 PRD §2。
 * 纯 Kotlin，无 Android 依赖，可直接用 JVM 单元测试验证。
 *
 * 注意：界面上任何数字都必须经由本文件计算，不允许出现第二套算法。
 */
object LeanLogMath {

    const val KCAL_PER_KG_FAT = 7700.0
    private const val MINUTES_PER_DAY = 1440.0

    // ---------------------------------------------------------------- 2.1 BMR

    /** Mifflin-St Jeor。体重取最近一条体重记录。 */
    fun bmr(gender: Gender, weightKg: Double, heightCm: Double, age: Int): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return if (gender == Gender.MALE) base + 5.0 else base - 161.0
    }

    /** 2.2 活动系数只描述「训练之外的日常活动」，训练消耗单独加，不重复计入。 */
    fun tdeeBase(bmr: Double, activityFactor: Double): Double = bmr * activityFactor

    // ------------------------------------------------------------ 2.3 训练消耗

    /**
     * 单条力量记录的时长（分钟）。
     * perEntrySec = sets × (reps × tempo + rest) − rest  // 最后一组结束后不再休息
     * 下限 60 秒。
     */
    fun strengthMinutes(
        sets: Int,
        reps: Int,
        restSec: Int,
        tempoSecPerRep: Int = 3,
    ): Double {
        val sec = sets * (reps * tempoSecPerRep + restSec) - restSec
        return max(60, sec).toDouble() / 60.0
    }

    /** 毛消耗：MET × 3.5 × 体重kg / 200 × 分钟。有氧与力量共用。 */
    fun grossKcal(met: Double, weightKg: Double, minutes: Double): Double =
        met * 3.5 * weightKg / 200.0 * minutes

    /**
     * 净消耗 = 毛消耗 − BMR/1440 × 分钟。
     * 因为 TDEE 已经用「BMR × 活动系数」覆盖了全天 24 小时的静息消耗，
     * 训练那一小时的静息部分必须扣掉，否则算了两遍（PRD §1.1 ②）。
     */
    fun netKcal(gross: Double, bmr: Double, minutes: Double): Double =
        max(0.0, gross - bmr / MINUTES_PER_DAY * minutes)

    // ------------------------------------------------------------ 2.4 每日缺口

    /** 缺口 = 消耗 − 摄入。正数=亏，负数=超。 */
    fun deficit(tdeeBase: Double, netWorkoutBurn: Double, intakeKcal: Double): Int =
        kotlin.math.round(tdeeBase + netWorkoutBurn - intakeKcal).toInt()

    /** 目标缺口 = 每周减重 × 7700 / 7。例：0.5kg/周 → 550 kcal/天。 */
    fun targetDeficit(weeklyRateKg: Double): Int =
        kotlin.math.round(weeklyRateKg * KCAL_PER_KG_FAT / 7.0).toInt()

    // -------------------------------------------------------- 2.5 日历状态判定

    /**
     * 日历评级只认饮食记录（PRD §1.1 ①）。
     * 只记训练没记饮食的日子：返回 EMPTY 之外的 EMPTY_TRAINED，数字灰色、无描边。
     */
    fun calendarStatus(
        isFuture: Boolean,
        hasFoodRecord: Boolean,
        hasWorkout: Boolean,
        deficit: Int,
    ): CalendarStatus = when {
        isFuture -> CalendarStatus.FUTURE
        !hasFoodRecord -> if (hasWorkout) CalendarStatus.TRAINED_ONLY else CalendarStatus.EMPTY
        kotlin.math.abs(deficit) <= 50 -> CalendarStatus.EVEN
        deficit > 0 -> CalendarStatus.DEFICIT
        else -> CalendarStatus.SURPLUS
    }

    /** 关闭「热量缺口警示」后的中性状态：中间三档合并。 */
    fun plainStatus(isFuture: Boolean, hasFoodRecord: Boolean, hasWorkout: Boolean): CalendarStatus =
        when {
            isFuture -> CalendarStatus.FUTURE
            !hasFoodRecord -> if (hasWorkout) CalendarStatus.TRAINED_ONLY else CalendarStatus.EMPTY
            else -> CalendarStatus.PLAIN
        }

    // -------------------------------------------------------- 2.6 自适应 TDEE

    /**
     * 用「摄入 − 体重变化」反推真实消耗。
     *
     * 窗口 30 天；体重记录 ≥ 8 天；饮食完整度 ≥ 60%。
     * 斜率用线性回归，不用首尾两点（首尾两点对单日水肿极其敏感）。
     * 平滑：0.6 × 上次 + 0.4 × 新值。
     */
    fun adaptiveTdee(
        points: List<WeightPoint>,
        intakeByDay: Map<Int, Double>,
        previousEstimate: Double? = null,
        windowDays: Int = 30,
    ): AdaptiveTdeeResult {
        if (points.size < MIN_WEIGHT_POINTS) {
            return AdaptiveTdeeResult(null, "体重记录不足 $MIN_WEIGHT_POINTS 天（当前 ${points.size} 天）")
        }
        val sorted = points.sortedBy { it.day }
        val span = sorted.last().day - sorted.first().day + 1
        if (span > windowDays) {
            return adaptiveTdee(sorted.filter { it.day > sorted.last().day - windowDays }, intakeByDay, previousEstimate, windowDays)
        }
        if (span < MIN_SPAN_DAYS) {
            return AdaptiveTdeeResult(null, "时间跨度不足 $MIN_SPAN_DAYS 天（当前 $span 天）")
        }

        val observed = intakeByDay.filterKeys { it >= sorted.first().day && it <= sorted.last().day }
        val requiredDays = (span * MIN_INTAKE_COVERAGE).toInt()
        if (observed.size < requiredDays) {
            return AdaptiveTdeeResult(null, "饮食记录完整度不足（$span 天里只有 ${observed.size} 天，需 $requiredDays 天）")
        }

        val avgIntake = observed.values.average()
        val slope = linearSlope(sorted) // kg / day，负数=在掉秤
        val raw = avgIntake - slope * KCAL_PER_KG_FAT
        val smoothed = previousEstimate?.let { 0.6 * it + 0.4 * raw } ?: raw

        return AdaptiveTdeeResult(
            value = smoothed,
            reason = null,
            sampleDays = span,
            avgIntake = avgIntake,
            slopeKgPerDay = slope,
            rawValue = raw,
        )
    }

    /** 最小二乘斜率。x = 天数，y = 体重。 */
    internal fun linearSlope(points: List<WeightPoint>): Double {
        val n = points.size
        if (n < 2) return 0.0
        val meanX = points.sumOf { it.day.toDouble() } / n
        val meanY = points.sumOf { it.kg } / n
        var num = 0.0
        var den = 0.0
        for (p in points) {
            val dx = p.day - meanX
            num += dx * (p.kg - meanY)
            den += dx * dx
        }
        return if (den == 0.0) 0.0 else num / den
    }

    /**
     * 自适应值与公式值差异过大时取中位数，避免一条水肿数据把整月日历染红。
     */
    fun reconcileTdee(adaptive: Double, formula: Double): Reconciliation {
        val diff = adaptive - formula
        return if (kotlin.math.abs(diff) > ANOMALY_THRESHOLD) {
            Reconciliation(
                value = (adaptive + formula) / 2.0,
                anomaly = true,
                note = "数据波动较大，已取两者中位数",
            )
        } else {
            Reconciliation(value = adaptive, anomaly = false, note = null)
        }
    }

    // -------------------------------------------------------- 2.7 蛋白目标

    fun proteinTarget(weightKg: Double, auto: Boolean, customG: Int): Int =
        if (auto) kotlin.math.round(weightKg * PROTEIN_AUTO_FACTOR).toInt() else customG

    /**
     * 摄入多少克才算出「蛋白质达标」的常用区间，仅供文案参考。
     * 1.2 维持 / 1.6 减脂 / 2.0 高强度。
     */
    fun proteinPreset(weightKg: Double, gPerKg: Double): Int =
        kotlin.math.round(weightKg * gPerKg).toInt()

    // -------------------------------------------------------- 周汇总

    /** 本周平均缺口，只统计已评级（有饮食记录）的日期。 */
    fun averageDeficit(defcits: List<Int>): Int? =
        if (defcits.isEmpty()) null else kotlin.math.round(defcits.average()).toInt()

    /** 本周达标天数：缺口在目标 ±100 内。 */
    fun targetHitDays(defcits: List<Int>, target: Int): Int =
        defcits.count { kotlin.math.abs(it - target) <= 100 }

    // -------------------------------------------------------- 常量

    const val PROTEIN_AUTO_FACTOR = 1.8
    const val MIN_WEIGHT_POINTS = 8
    const val MIN_SPAN_DAYS = 14
    const val MIN_INTAKE_COVERAGE = 0.6
    const val ANOMALY_THRESHOLD = 800.0
}

/** 横轴统一用「距今天的天数」，避免时区与 LocalDate 运算成本。 */
data class WeightPoint(val day: Int, val kg: Double)

data class AdaptiveTdeeResult(
    val value: Double?,
    val reason: String?,
    val sampleDays: Int = 0,
    val avgIntake: Double = 0.0,
    val slopeKgPerDay: Double = 0.0,
    val rawValue: Double = 0.0,
) {
    val available: Boolean get() = value != null
}

data class Reconciliation(val value: Double, val anomaly: Boolean, val note: String?)
