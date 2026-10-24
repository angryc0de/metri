package com.leanlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        WeightLogEntity::class,
        MuscleGroupEntity::class,
        ExerciseEntity::class,
        ExerciseGroupEntity::class,
        StrengthRecordEntity::class,
        CardioRecordEntity::class,
        FoodEntity::class,
        MealEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LeanLogDatabase : RoomDatabase() {

    abstract fun dao(): LeanLogDao

    companion object {
        @Volatile
        private var instance: LeanLogDatabase? = null

        fun get(context: Context): LeanLogDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LeanLogDatabase::class.java,
                "leanlog.db",
            ).build().also { instance = it }
        }
    }
}
