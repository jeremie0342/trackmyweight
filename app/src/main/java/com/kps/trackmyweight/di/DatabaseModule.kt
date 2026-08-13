package com.kps.trackmyweight.di

import android.content.Context
import androidx.room.Room
import com.kps.trackmyweight.data.db.ALL_MIGRATIONS
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.AnalyticsMetaDao
import com.kps.trackmyweight.data.db.dao.BodyDao
import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.HabitDao
import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): TrackMyWeightDatabase =
        Room.databaseBuilder(
            context,
            TrackMyWeightDatabase::class.java,
            TrackMyWeightDatabase.DB_NAME,
        )
            // Foreign keys sont activées automatiquement par Room, mais on est explicite.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(*ALL_MIGRATIONS)
            // Le repli destructif est volontairement limité aux versions pré-5.
            //
            // Il était auparavant global : n'importe quel bump de schéma effaçait
            // silencieusement toutes les données de l'utilisateur. Pire, il masquait
            // les migrations boguées — Room se rabattait sur l'effacement au lieu de
            // signaler l'erreur. Désormais, à partir de la v5, une migration absente
            // ou incorrecte provoque un crash explicite : bruyant, mais non destructif.
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
            .build()

    @Provides fun provideUserDao(db: TrackMyWeightDatabase): UserDao = db.userDao()
    @Provides fun provideBodyDao(db: TrackMyWeightDatabase): BodyDao = db.bodyDao()
    @Provides fun provideExerciseDao(db: TrackMyWeightDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideWorkoutDao(db: TrackMyWeightDatabase): WorkoutDao = db.workoutDao()
    @Provides fun provideNutritionDao(db: TrackMyWeightDatabase): NutritionDao = db.nutritionDao()
    @Provides fun provideHabitDao(db: TrackMyWeightDatabase): HabitDao = db.habitDao()
    @Provides fun provideAnalyticsMetaDao(db: TrackMyWeightDatabase): AnalyticsMetaDao = db.analyticsMetaDao()
}
