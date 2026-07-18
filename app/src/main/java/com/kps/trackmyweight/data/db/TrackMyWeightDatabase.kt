package com.kps.trackmyweight.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kps.trackmyweight.data.db.converters.Converters
import com.kps.trackmyweight.data.db.dao.AnalyticsMetaDao
import com.kps.trackmyweight.data.db.dao.BodyDao
import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.HabitDao
import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import com.kps.trackmyweight.data.db.entity.AlcoholEntryEntity
import com.kps.trackmyweight.data.db.entity.AppEventEntity
import com.kps.trackmyweight.data.db.entity.BackupRecordEntity
import com.kps.trackmyweight.data.db.entity.BodyCompositionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.BodyMeasurementSessionEntity
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.CorrelationInsightEntity
import com.kps.trackmyweight.data.db.entity.DailyLogEntity
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEquipmentRequirementEntity
import com.kps.trackmyweight.data.db.entity.ExerciseSubstitutionEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntryEntity
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.FoodFtsEntity
import com.kps.trackmyweight.data.db.entity.FoodPortionAliasEntity
import com.kps.trackmyweight.data.db.entity.FoodPriceEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.db.entity.GymEquipmentEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
import com.kps.trackmyweight.data.db.entity.HealthConnectSyncStateEntity
import com.kps.trackmyweight.data.db.entity.HeartRateSampleEntity
import com.kps.trackmyweight.data.db.entity.MealEntity
import com.kps.trackmyweight.data.db.entity.MealEntryEntity
import com.kps.trackmyweight.data.db.entity.MuscleGroupVolumeWeeklyEntity
import com.kps.trackmyweight.data.db.entity.PainLogEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.ProgramDayEntity
import com.kps.trackmyweight.data.db.entity.ProgramEntity
import com.kps.trackmyweight.data.db.entity.ProgressPhotoEntity
import com.kps.trackmyweight.data.db.entity.ProjectionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.RecipeEntity
import com.kps.trackmyweight.data.db.entity.RecipeIngredientEntity
import com.kps.trackmyweight.data.db.entity.SleepEntryEntity
import com.kps.trackmyweight.data.db.entity.StepsEntryEntity
import com.kps.trackmyweight.data.db.entity.TemplateExerciseEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationMemberEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.entity.WaterEntryEntity
import com.kps.trackmyweight.data.db.entity.WeeklyReviewEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity

@Database(
    entities = [
        // User & config
        UserProfileEntity::class,
        GoalEntity::class,
        GymEntity::class,
        EquipmentEntity::class,
        GymEquipmentEntity::class,
        // Body
        WeightEntryEntity::class,
        BodyMeasurementSessionEntity::class,
        ProgressPhotoEntity::class,
        BodyCompositionSnapshotEntity::class,
        // Exercises
        ExerciseEntity::class,
        ExerciseEquipmentRequirementEntity::class,
        ExerciseSubstitutionEntity::class,
        // Workout
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        TemplateRotationGroupEntity::class,
        TemplateRotationMemberEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        WorkoutSessionEntity::class,
        PerformedExerciseEntity::class,
        PerformedSetEntity::class,
        PersonalRecordEntity::class,
        MuscleGroupVolumeWeeklyEntity::class,
        CardioSessionEntity::class,
        com.kps.trackmyweight.data.db.entity.CardioBlockEntity::class,
        PainLogEntity::class,
        // Nutrition
        FoodEntity::class,
        FoodFtsEntity::class,
        FoodPortionAliasEntity::class,
        FoodPriceEntity::class,
        MealEntity::class,
        MealEntryEntity::class,
        FavoriteMealEntity::class,
        FavoriteMealEntryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        WaterEntryEntity::class,
        AlcoholEntryEntity::class,
        DietPhaseEntity::class,
        // Habits & recovery
        DailyLogEntity::class,
        HabitDefinitionEntity::class,
        HabitCompletionEntity::class,
        SleepEntryEntity::class,
        StepsEntryEntity::class,
        HeartRateSampleEntity::class,
        // Analytics & meta
        WeeklyReviewEntity::class,
        CorrelationInsightEntity::class,
        ProjectionSnapshotEntity::class,
        HealthConnectSyncStateEntity::class,
        BackupRecordEntity::class,
        AppEventEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TrackMyWeightDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bodyDao(): BodyDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun habitDao(): HabitDao
    abstract fun analyticsMetaDao(): AnalyticsMetaDao

    companion object {
        const val DB_NAME = "trackmyweight.db"
    }
}
