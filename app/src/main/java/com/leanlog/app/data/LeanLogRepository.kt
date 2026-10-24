package com.leanlog.app.data

import com.leanlog.app.core.FoodSeed
import com.leanlog.app.core.SeedData

/**
 * 数据入口。所有写操作都在这里，ViewModel 不直接碰 DAO。
 * 种子的灌入也放这里（Room 的 onCreate 回调里拿不到 suspend，逻辑也不该散在回调里）。
 */
class LeanLogRepository(private val dao: LeanLogDao) {

    // ---------------------------------------------------------------- 初始化

    suspend fun ensureSeeded() {
        if (dao.exerciseCount() == 0) {
            dao.insertMuscleGroups(SeedData.muscleGroups.map { MuscleGroupEntity(it.id, it.name, it.sortOrder) })
            dao.insertExercises(SeedData.exercises.map {
                ExerciseEntity(it.id, it.name, it.met, it.bodyweightDefault, custom = false)
            })
            dao.insertExerciseGroups(
                SeedData.exercises.flatMap { ex ->
                    ex.groups.map { ExerciseGroupEntity(ex.id, it) }
                }
            )
        }
        if (dao.foodCount() == 0) {
            dao.insertFoods(SeedData.foods.map { it.toEntity() })
        }
    }

    private fun FoodSeed.toEntity() = FoodEntity(
        id = id,
        name = name,
        alias = alias,
        kcalPer100g = kcalPer100g,
        proteinPer100g = proteinPer100g,
        carbPer100g = carbPer100g,
        fatPer100g = fatPer100g,
        servingUnit = servingUnit,
        servingGrams = servingGrams,
        emoji = emoji,
        source = "BUILTIN",
    )

    // ---------------------------------------------------------------- 用户

    suspend fun profile(): ProfileEntity {
        dao.profile()?.let { return it }
        val fresh = ProfileEntity()
        dao.upsertProfile(fresh)
        return fresh
    }

    suspend fun saveProfile(profile: ProfileEntity) = dao.upsertProfile(profile)

    // ---------------------------------------------------------------- 体重

    suspend fun weights(sinceDay: Int): List<WeightLogEntity> = dao.weightsSince(sinceDay)

    suspend fun weightOn(day: Int): WeightLogEntity? = dao.weightOn(day)

    suspend fun recordWeight(day: Int, kg: Double) {
        dao.upsertWeight(WeightLogEntity(day = day, kg = kg))
        // 个人资料里的体重始终等于"最近一天"的体重，BMR 才不会用到过期的数
        val latest = dao.allWeights().lastOrNull() ?: return
        val current = profile()
        if (current.weightKg != latest.kg) dao.upsertProfile(current.copy(weightKg = latest.kg))
    }

    // ---------------------------------------------------------------- 器械

    suspend fun exercisesWithGroups(): List<ExerciseWithGroups> = dao.exercisesWithGroups()

    suspend fun muscleGroups(): List<MuscleGroupEntity> = dao.muscleGroups()

    suspend fun exercise(id: String): ExerciseEntity? = dao.exerciseById(id)

    suspend fun addCustomExercise(name: String, met: Double, groupIds: List<String>): ExerciseEntity {
        val id = "custom_" + System.currentTimeMillis()
        val entity = ExerciseEntity(id, name, met, bodyweightDefault = false, custom = true)
        dao.insertExercise(entity)
        groupIds.forEach { dao.insertExerciseGroup(ExerciseGroupEntity(id, it)) }
        return entity
    }

    // ---------------------------------------------------------------- 力量

    suspend fun strengthOn(day: Int): List<StrengthRecordEntity> = dao.strengthOn(day)

    suspend fun strengthById(id: Long): StrengthRecordEntity? = dao.strengthById(id)

    suspend fun addStrength(record: StrengthRecordEntity): Long = dao.insertStrength(record)

    suspend fun updateStrength(record: StrengthRecordEntity) = dao.updateStrength(record)

    suspend fun deleteStrength(id: Long) = dao.deleteStrengthById(id)

    suspend fun lastStrengthFor(exerciseId: String): StrengthRecordEntity? = dao.lastStrengthFor(exerciseId)

    suspend fun recentExerciseIds(limit: Int = 12): List<String> =
        dao.recentExercises(limit).map { it.exerciseId }

    // ---------------------------------------------------------------- 有氧

    suspend fun cardioOn(day: Int): List<CardioRecordEntity> = dao.cardioOn(day)

    suspend fun cardioById(id: Long): CardioRecordEntity? = dao.cardioById(id)

    suspend fun addCardio(record: CardioRecordEntity): Long = dao.insertCardio(record)

    suspend fun updateCardio(record: CardioRecordEntity) = dao.updateCardio(record)

    suspend fun deleteCardio(id: Long) = dao.deleteCardioById(id)

    // ---------------------------------------------------------------- 食物

    suspend fun searchFoods(query: String): List<FoodEntity> = dao.searchFoods(query)

    suspend fun allFoods(): List<FoodEntity> = dao.allFoods()

    suspend fun foodById(id: String): FoodEntity? = dao.foodById(id)

    suspend fun upsertFood(food: FoodEntity) = dao.upsertFood(food)

    suspend fun recentFoods(limit: Int = 10): List<FoodEntity> = dao.recentFoods(limit)

    // ---------------------------------------------------------------- 饮食记录

    suspend fun mealsOn(day: Int): List<MealEntryEntity> = dao.mealsOn(day)

    suspend fun addMeal(entry: MealEntryEntity): Long = dao.insertMeal(entry)

    suspend fun updateMeal(entry: MealEntryEntity) = dao.updateMeal(entry)

    suspend fun deleteMeal(id: Long) = dao.deleteMealById(id)

    // ---------------------------------------------------------------- 按天聚合

    suspend fun intakeByDay(): Map<Int, DayIntakeRow> = dao.intakeByDay().associateBy { it.day }

    suspend fun strengthBurnByDay(): Map<Int, DayBurnRow> = dao.strengthByDay().associateBy { it.day }

    suspend fun cardioBurnByDay(): Map<Int, DayBurnRow> = dao.cardioByDay().associateBy { it.day }

    // ---------------------------------------------------------------- 导出

    suspend fun exportBundle(): ExportBundle = ExportBundle(
        profile = profile(),
        weights = dao.allWeights(),
        meals = dao.allMeals(),
        strength = dao.allStrength(),
        cardio = dao.allCardio(),
        exercises = dao.allExercises(),
        foods = dao.allFoods(),
    )
}

data class ExportBundle(
    val profile: ProfileEntity,
    val weights: List<WeightLogEntity>,
    val meals: List<MealEntryEntity>,
    val strength: List<StrengthRecordEntity>,
    val cardio: List<CardioRecordEntity>,
    val exercises: List<ExerciseEntity>,
    val foods: List<FoodEntity>,
)
