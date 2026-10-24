package com.leanlog.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface LeanLogDao {

    // ---------------------------------------------------------------- 用户

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profile(): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    // ---------------------------------------------------------------- 体重

    @Query("SELECT * FROM weight_log ORDER BY day")
    suspend fun allWeights(): List<WeightLogEntity>

    @Query("SELECT * FROM weight_log WHERE day >= :fromDay ORDER BY day")
    suspend fun weightsSince(fromDay: Int): List<WeightLogEntity>

    @Query("SELECT * FROM weight_log WHERE day = :day LIMIT 1")
    suspend fun weightOn(day: Int): WeightLogEntity?

    @Upsert
    suspend fun upsertWeight(weight: WeightLogEntity)

    @Query("DELETE FROM weight_log WHERE day = :day")
    suspend fun deleteWeight(day: Int)

    // ---------------------------------------------------------------- 器械

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun exerciseCount(): Int

    @Query("SELECT * FROM exercise ORDER BY name")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    suspend fun exerciseById(id: String): ExerciseEntity?

    @Query(
        """
        SELECT e.id AS id, e.name AS name, e.met AS met, e.bodyweightDefault AS bodyweightDefault, g.groupId AS groupId
        FROM exercise e INNER JOIN exercise_group g ON e.id = g.exerciseId
        ORDER BY e.name
        """
    )
    suspend fun exercisesWithGroups(): List<ExerciseWithGroups>

    @Query("SELECT * FROM muscle_group ORDER BY sortOrder")
    suspend fun muscleGroups(): List<MuscleGroupEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMuscleGroups(items: List<MuscleGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseGroups(items: List<ExerciseGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseGroup(item: ExerciseGroupEntity)

    @Query("DELETE FROM exercise WHERE id = :id AND custom = 1")
    suspend fun deleteCustomExercise(id: String)

    // ---------------------------------------------------------------- 力量记录

    @Query("SELECT * FROM strength_record WHERE day = :day ORDER BY createdAt")
    suspend fun strengthOn(day: Int): List<StrengthRecordEntity>

    @Query("SELECT * FROM strength_record WHERE id = :id LIMIT 1")
    suspend fun strengthById(id: Long): StrengthRecordEntity?

    @Insert
    suspend fun insertStrength(record: StrengthRecordEntity): Long

    @Update
    suspend fun updateStrength(record: StrengthRecordEntity)

    @Delete
    suspend fun deleteStrength(record: StrengthRecordEntity)

    @Query("DELETE FROM strength_record WHERE id = :id")
    suspend fun deleteStrengthById(id: Long)

    @Query("SELECT * FROM strength_record WHERE exerciseId = :exerciseId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastStrengthFor(exerciseId: String): StrengthRecordEntity?

    @Query(
        """
        SELECT exerciseId AS exerciseId, MAX(createdAt) AS lastUsed
        FROM strength_record GROUP BY exerciseId ORDER BY lastUsed DESC LIMIT :limit
        """
    )
    suspend fun recentExercises(limit: Int): List<RecentExerciseRow>

    @Query(
        """
        SELECT day AS day, SUM(netKcal) AS netKcal, COUNT(*) AS entries
        FROM strength_record GROUP BY day
        """
    )
    suspend fun strengthByDay(): List<DayBurnRow>

    @Query("SELECT COUNT(*) FROM strength_record WHERE day = :day")
    suspend fun strengthCountOn(day: Int): Int

    // ---------------------------------------------------------------- 有氧记录

    @Query("SELECT * FROM cardio_record WHERE day = :day ORDER BY createdAt")
    suspend fun cardioOn(day: Int): List<CardioRecordEntity>

    @Query("SELECT * FROM cardio_record WHERE id = :id LIMIT 1")
    suspend fun cardioById(id: Long): CardioRecordEntity?

    @Insert
    suspend fun insertCardio(record: CardioRecordEntity): Long

    @Update
    suspend fun updateCardio(record: CardioRecordEntity)

    @Query("DELETE FROM cardio_record WHERE id = :id")
    suspend fun deleteCardioById(id: Long)

    @Query(
        """
        SELECT day AS day, SUM(netKcal) AS netKcal, COUNT(*) AS entries
        FROM cardio_record GROUP BY day
        """
    )
    suspend fun cardioByDay(): List<DayBurnRow>

    @Query("SELECT COUNT(*) FROM cardio_record WHERE day = :day")
    suspend fun cardioCountOn(day: Int): Int

    // ---------------------------------------------------------------- 食物库

    @Query("SELECT COUNT(*) FROM food")
    suspend fun foodCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(items: List<FoodEntity>)

    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Query("SELECT * FROM food WHERE id = :id LIMIT 1")
    suspend fun foodById(id: String): FoodEntity?

    @Query("SELECT * FROM food ORDER BY name")
    suspend fun allFoods(): List<FoodEntity>

    @Query(
        """
        SELECT * FROM food
        WHERE name LIKE '%' || :q || '%' OR IFNULL(alias, '') LIKE '%' || :q || '%'
        ORDER BY (name = :q) DESC, LENGTH(name) LIMIT 30
        """
    )
    suspend fun searchFoods(q: String): List<FoodEntity>

    @Query("DELETE FROM food WHERE id = :id AND source = 'CUSTOM'")
    suspend fun deleteCustomFood(id: String)

    // ---------------------------------------------------------------- 饮食记录

    @Query("SELECT * FROM meal_entry WHERE day = :day ORDER BY createdAt")
    suspend fun mealsOn(day: Int): List<MealEntryEntity>

    @Insert
    suspend fun insertMeal(entry: MealEntryEntity): Long

    @Update
    suspend fun updateMeal(entry: MealEntryEntity)

    @Query("DELETE FROM meal_entry WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query(
        """
        SELECT day AS day, SUM(kcal) AS kcal, SUM(protein) AS protein, COUNT(*) AS entries
        FROM meal_entry GROUP BY day
        """
    )
    suspend fun intakeByDay(): List<DayIntakeRow>

    @Query("SELECT COUNT(*) FROM meal_entry WHERE day = :day")
    suspend fun mealCountOn(day: Int): Int

    @Query(
        """
        SELECT f.* FROM food f
        INNER JOIN (SELECT foodId, MAX(createdAt) AS t FROM meal_entry GROUP BY foodId) m
        ON f.id = m.foodId
        ORDER BY m.t DESC LIMIT :limit
        """
    )
    suspend fun recentFoods(limit: Int): List<FoodEntity>

    // ---------------------------------------------------------------- 导出

    @Query("SELECT * FROM meal_entry ORDER BY day, createdAt")
    suspend fun allMeals(): List<MealEntryEntity>

    @Query("SELECT * FROM strength_record ORDER BY day, createdAt")
    suspend fun allStrength(): List<StrengthRecordEntity>

    @Query("SELECT * FROM cardio_record ORDER BY day, createdAt")
    suspend fun allCardio(): List<CardioRecordEntity>
}
