package com.leanlog.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 所有日期统一用 epochDay（LocalDate.toEpochDay()），不用字符串也不存时间戳。
 * 好处：范围查询是整数比较，跨时区/夏令时不会错位。
 */

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val gender: String = "MALE",
    val age: Int = 28,
    val heightCm: Double = 178.0,
    val weightKg: Double = 78.0,
    val activityFactor: Double = 1.2,
    val targetWeightKg: Double = 72.0,
    val weeklyRateKg: Double = 0.5,
    val proteinAuto: Boolean = true,
    val proteinCustomG: Int = 140,
)

/** 每天一条，重复记录覆盖。体重曲线依赖这张表。 */
@Entity(tableName = "weight_log")
data class WeightLogEntity(
    @PrimaryKey val day: Int,
    val kg: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "muscle_group")
data class MuscleGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(tableName = "exercise", indices = [Index("name")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val met: Double,
    val bodyweightDefault: Boolean,
    val custom: Boolean = false,
)

/** 器械 ↔ 部位 多对多。硬拉同时属于背/腿/臀。 */
@Entity(tableName = "exercise_group", primaryKeys = ["exerciseId", "groupId"], indices = [Index("groupId")])
data class ExerciseGroupEntity(
    val exerciseId: String,
    val groupId: String,
)

/**
 * 力量记录。minutes 与 netKcal 是**记录当时**算好后存下的，
 * 不能每次渲染再算 —— 否则以后体重一变，去年那天的消耗也跟着变，历史就不可信了。
 */
@Entity(tableName = "strength_record", indices = [Index("day"), Index("exerciseId"), Index("createdAt")])
data class StrengthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val sets: Int,
    val bodyweight: Boolean,
    val minutes: Double,
    val netKcal: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 有氧记录。paramsJson 存该项目的参数快照，不同项目字段完全不同，不适合拍平成列。 */
@Entity(tableName = "cardio_record", indices = [Index("day"), Index("createdAt")])
data class CardioRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val typeId: String,
    val typeName: String,
    val paramsJson: String,
    val enumValue: String?,
    val minutes: Double,
    val met: Double,
    val netKcal: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

/** source: BUILTIN / AI / CUSTOM。AI 估算的食物会写回这张表，避免重复请求。 */
@Entity(tableName = "food", indices = [Index("name")])
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val alias: String? = null,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val servingUnit: String? = null,
    val servingGrams: Double? = null,
    val emoji: String? = null,
    val source: String = "BUILTIN",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 饮食条目。kcal/protein 等按**当时**克数算好存下，食物库日后被改也不会污染历史。
 */
@Entity(tableName = "meal_entry", indices = [Index("day"), Index("meal"), Index("foodId")])
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val meal: String,
    val foodId: String,
    val foodName: String,
    val grams: Double,
    val servings: Double? = null,
    val servingUnit: String? = null,
    val kcal: Double,
    val protein: Double,
    val carb: Double? = null,
    val fat: Double? = null,
    val isAi: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

// ------------------------------------------------------------------ 查询投影

data class DayIntakeRow(
    val day: Int,
    val kcal: Double,
    val protein: Double,
    val entries: Int,
)

data class DayBurnRow(
    val day: Int,
    val netKcal: Double,
    val entries: Int,
)

data class RecentExerciseRow(
    val exerciseId: String,
    val lastUsed: Long,
)

data class ExerciseWithGroups(
    val id: String,
    val name: String,
    val met: Double,
    val bodyweightDefault: Boolean,
    val groupId: String,
)
