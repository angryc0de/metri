package com.leanlog.app.core

/** 性别决定 BMR 公式的常数项。 */
enum class Gender { MALE, FEMALE }

/**
 * 日历单元格状态（PRD §2.5）。
 * TRAINED_ONLY：只有训练、没有饮食记录 —— 显示圆点，但数字灰色、无描边、不出缺口值。
 */
enum class CalendarStatus {
    FUTURE,
    EMPTY,
    TRAINED_ONLY,
    DEFICIT,
    EVEN,
    SURPLUS,
    PLAIN,
}

enum class MealType(val label: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("其他"),
}

/** 食物来源，决定界面上是否显示「AI」标记。 */
enum class FoodSource { BUILTIN, AI, CUSTOM }

enum class WorkoutKind { STRENGTH, CARDIO }

// ------------------------------------------------------------------ 有氧参数模型

/**
 * 一个有氧项目的数值参数。范围与步长直接驱动参数页的滚轮。
 */
data class ParamSpec(
    val key: String,
    val label: String,
    val unit: String,
    val min: Double,
    val max: Double,
    val step: Double,
    val default: Double,
)

/** 枚举型参数（泳姿、强度），渲染成一行 chip。 */
data class EnumSpec(
    val key: String,
    val label: String,
    val options: List<String>,
    val default: String,
)

/**
 * 有氧项目定义。MET 由参数实时算出，不是固定值 —— 同样 30 分钟，
 * 坡度 10% 和坡度 0 的爬坡走差将近一倍。
 */
data class CardioType(
    val id: String,
    val name: String,
    val params: List<ParamSpec>,
    val enumSpec: EnumSpec? = null,
    val metFn: (params: Map<String, Double>, enumValue: String?) -> Double,
) {
    fun met(params: Map<String, Double>, enumValue: String? = null): Double =
        metFn(params, enumValue).coerceIn(1.0, 20.0)

    /** 用默认值填满参数，用于「上次没有记录」时的初值。 */
    fun defaultParams(): Map<String, Double> = params.associate { it.key to it.default }
}

// ------------------------------------------------------------------ 训练种子

data class ExerciseSeed(
    val id: String,
    val name: String,
    val met: Double,
    val bodyweightDefault: Boolean,
    /** 可挂多个部位：硬拉 = back + legs + glute */
    val groups: List<String>,
)

data class MuscleGroupSeed(val id: String, val name: String, val sortOrder: Int)

// ------------------------------------------------------------------ 食物种子

/**
 * 内置食物。营养值统一按每 100g 存；常用份量（1 碗 = 150g）是必须的，
 * 没有这层映射，用户看到「米饭 116 kcal」根本无法验证也无法记录。
 */
data class FoodSeed(
    val id: String,
    val name: String,
    val alias: String?,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbPer100g: Double?,
    val fatPer100g: Double?,
    val servingUnit: String?,
    val servingGrams: Double?,
    val emoji: String?,
)

// ------------------------------------------------------------------ AI 结果模型

data class FoodEstimateItem(
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carb: Double,
    val fat: Double,
    val confidence: Double,
)

data class FoodEstimate(
    val items: List<FoodEstimateItem>,
    val servingQty: Double,
    val servingUnit: String,
    val servingGrams: Double,
    val assumptions: List<String>,
    /** true = 真 AI 返回；false = 断网/超时后的本地规则降级 */
    val fromAi: Boolean,
) {
    val totalKcal: Int get() = kotlin.math.round(items.sumOf { it.kcal }).toInt()
    val totalProtein: Double get() = items.sumOf { it.protein }
    val totalCarb: Double get() = items.sumOf { it.carb }
    val totalFat: Double get() = items.sumOf { it.fat }
    val totalGrams: Double get() = items.sumOf { it.grams }
}

data class DetectedFood(
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val confidence: Double,
    val emoji: String? = null,
) {
    /** 置信度低于 60% 的项默认不勾选（PRD §8.5）。 */
    val needsConfirm: Boolean get() = confidence < 0.6
}
