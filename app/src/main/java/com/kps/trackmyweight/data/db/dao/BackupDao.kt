package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kps.trackmyweight.data.db.entity.AlcoholEntryEntity
import com.kps.trackmyweight.data.db.entity.AppEventEntity
import com.kps.trackmyweight.data.db.entity.BodyCompositionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.BodyMeasurementSessionEntity
import com.kps.trackmyweight.data.db.entity.CardioBlockEntity
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.CorrelationInsightEntity
import com.kps.trackmyweight.data.db.entity.DailyLogEntity
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEquipmentRequirementEntity
import com.kps.trackmyweight.data.db.entity.ExerciseMaxLoadEntity
import com.kps.trackmyweight.data.db.entity.ExerciseSubstitutionEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntryEntity
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.FoodPortionAliasEntity
import com.kps.trackmyweight.data.db.entity.FoodPriceEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.db.entity.GymEquipmentEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
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
import kotlinx.datetime.Instant

/**
 * Accès brut, table par table, pour la sauvegarde complète.
 *
 * Les autres DAO exposent des vues métier (fenêtres de dates, lignes actives,
 * éléments non supprimés) : s'en servir pour exporter revenait à oublier tout
 * ce qui sort de ces vues. Ici chaque lecture renvoie la table entière, et
 * chaque insertion conserve l'identifiant fourni.
 */
@Dao
interface BackupDao {

    // ── Lecture intégrale ─────────────────────────────────────
    @Query("SELECT * FROM user_profile") suspend fun profiles(): List<UserProfileEntity>
    @Query("SELECT * FROM goal") suspend fun goals(): List<GoalEntity>
    @Query("SELECT * FROM gym") suspend fun gyms(): List<GymEntity>
    @Query("SELECT * FROM equipment") suspend fun equipment(): List<EquipmentEntity>
    @Query("SELECT * FROM gym_equipment") suspend fun gymEquipment(): List<GymEquipmentEntity>
    @Query("SELECT * FROM weight_entry") suspend fun weights(): List<WeightEntryEntity>
    @Query("SELECT * FROM body_measurement_session") suspend fun measurements(): List<BodyMeasurementSessionEntity>
    @Query("SELECT * FROM progress_photo") suspend fun photos(): List<ProgressPhotoEntity>
    @Query("SELECT * FROM body_composition_snapshot") suspend fun bodyCompositions(): List<BodyCompositionSnapshotEntity>
    @Query("SELECT * FROM exercise") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT * FROM exercise_equipment_requirement") suspend fun exerciseRequirements(): List<ExerciseEquipmentRequirementEntity>
    @Query("SELECT * FROM exercise_max_load") suspend fun exerciseMaxLoads(): List<ExerciseMaxLoadEntity>
    @Query("SELECT * FROM exercise_substitution") suspend fun exerciseSubstitutions(): List<ExerciseSubstitutionEntity>
    @Query("SELECT * FROM workout_template") suspend fun templates(): List<WorkoutTemplateEntity>
    @Query("SELECT * FROM template_exercise") suspend fun templateExercises(): List<TemplateExerciseEntity>
    @Query("SELECT * FROM template_rotation_group") suspend fun rotationGroups(): List<TemplateRotationGroupEntity>
    @Query("SELECT * FROM template_rotation_member") suspend fun rotationMembers(): List<TemplateRotationMemberEntity>
    @Query("SELECT * FROM program") suspend fun programs(): List<ProgramEntity>
    @Query("SELECT * FROM program_day") suspend fun programDays(): List<ProgramDayEntity>
    @Query("SELECT * FROM workout_session") suspend fun sessions(): List<WorkoutSessionEntity>
    @Query("SELECT * FROM performed_exercise") suspend fun performedExercises(): List<PerformedExerciseEntity>
    @Query("SELECT * FROM performed_set") suspend fun performedSets(): List<PerformedSetEntity>
    @Query("SELECT * FROM personal_record") suspend fun personalRecords(): List<PersonalRecordEntity>
    @Query("SELECT * FROM muscle_group_volume_weekly") suspend fun muscleVolumes(): List<MuscleGroupVolumeWeeklyEntity>
    @Query("SELECT * FROM cardio_session") suspend fun cardioSessions(): List<CardioSessionEntity>
    @Query("SELECT * FROM cardio_block") suspend fun cardioBlocks(): List<CardioBlockEntity>
    @Query("SELECT * FROM pain_log") suspend fun painLogs(): List<PainLogEntity>
    @Query("SELECT * FROM food") suspend fun foods(): List<FoodEntity>
    @Query("SELECT * FROM food_portion_alias") suspend fun foodPortionAliases(): List<FoodPortionAliasEntity>
    @Query("SELECT * FROM food_price") suspend fun foodPrices(): List<FoodPriceEntity>
    @Query("SELECT * FROM meal") suspend fun meals(): List<MealEntity>
    @Query("SELECT * FROM meal_entry") suspend fun mealEntries(): List<MealEntryEntity>
    @Query("SELECT * FROM favorite_meal") suspend fun favoriteMeals(): List<FavoriteMealEntity>
    @Query("SELECT * FROM favorite_meal_entry") suspend fun favoriteMealEntries(): List<FavoriteMealEntryEntity>
    @Query("SELECT * FROM recipe") suspend fun recipes(): List<RecipeEntity>
    @Query("SELECT * FROM recipe_ingredient") suspend fun recipeIngredients(): List<RecipeIngredientEntity>
    @Query("SELECT * FROM water_entry") suspend fun water(): List<WaterEntryEntity>
    @Query("SELECT * FROM alcohol_entry") suspend fun alcohol(): List<AlcoholEntryEntity>
    @Query("SELECT * FROM diet_phase") suspend fun dietPhases(): List<DietPhaseEntity>
    @Query("SELECT * FROM daily_log") suspend fun dailyLogs(): List<DailyLogEntity>
    @Query("SELECT * FROM habit_definition") suspend fun habits(): List<HabitDefinitionEntity>
    @Query("SELECT * FROM habit_completion") suspend fun habitCompletions(): List<HabitCompletionEntity>
    @Query("SELECT * FROM sleep_entry") suspend fun sleep(): List<SleepEntryEntity>
    @Query("SELECT * FROM steps_entry") suspend fun steps(): List<StepsEntryEntity>
    @Query("SELECT * FROM heart_rate_sample") suspend fun heartRates(): List<HeartRateSampleEntity>
    @Query("SELECT * FROM weekly_review") suspend fun weeklyReviews(): List<WeeklyReviewEntity>
    @Query("SELECT * FROM correlation_insight") suspend fun correlations(): List<CorrelationInsightEntity>
    @Query("SELECT * FROM projection_snapshot") suspend fun projections(): List<ProjectionSnapshotEntity>
    @Query("SELECT * FROM app_event") suspend fun appEvents(): List<AppEventEntity>

    // ── Référentiels : ajout d'un élément absent ──────────────
    // Identifiant à 0 : c'est la base qui l'attribue, celui de la sauvegarde
    // pouvant être déjà pris par un autre élément du catalogue.
    @Insert suspend fun insertEquipment(item: EquipmentEntity): Long
    @Insert suspend fun insertExercise(item: ExerciseEntity): Long
    @Insert suspend fun insertFood(item: FoodEntity): Long
    @Insert suspend fun insertHabit(item: HabitDefinitionEntity): Long
    @Update suspend fun updateHabit(item: HabitDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseRequirements(items: List<ExerciseEquipmentRequirementEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseSubstitutions(items: List<ExerciseSubstitutionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodPortionAliases(items: List<FoodPortionAliasEntity>)

    // ── Données utilisateur : identifiants conservés ──────────
    // ABORT (par défaut) : la table vient d'être vidée, un conflit signale une
    // sauvegarde incohérente et doit annuler toute la restauration.
    @Insert suspend fun insertProfiles(items: List<UserProfileEntity>)
    @Insert suspend fun insertGoals(items: List<GoalEntity>)
    @Insert suspend fun insertGyms(items: List<GymEntity>)
    @Insert suspend fun insertGymEquipment(items: List<GymEquipmentEntity>)
    @Insert suspend fun insertWeights(items: List<WeightEntryEntity>)
    @Insert suspend fun insertMeasurements(items: List<BodyMeasurementSessionEntity>)
    @Insert suspend fun insertPhotos(items: List<ProgressPhotoEntity>)
    @Insert suspend fun insertBodyCompositions(items: List<BodyCompositionSnapshotEntity>)
    @Insert suspend fun insertExerciseMaxLoads(items: List<ExerciseMaxLoadEntity>)
    @Insert suspend fun insertTemplates(items: List<WorkoutTemplateEntity>)
    @Insert suspend fun insertTemplateExercises(items: List<TemplateExerciseEntity>)
    @Insert suspend fun insertRotationGroups(items: List<TemplateRotationGroupEntity>)
    @Insert suspend fun insertRotationMembers(items: List<TemplateRotationMemberEntity>)
    @Insert suspend fun insertPrograms(items: List<ProgramEntity>)
    @Insert suspend fun insertProgramDays(items: List<ProgramDayEntity>)
    @Insert suspend fun insertSessions(items: List<WorkoutSessionEntity>)
    @Insert suspend fun insertPerformedExercises(items: List<PerformedExerciseEntity>)
    @Insert suspend fun insertPerformedSets(items: List<PerformedSetEntity>)
    @Insert suspend fun insertPersonalRecords(items: List<PersonalRecordEntity>)
    @Insert suspend fun insertMuscleVolumes(items: List<MuscleGroupVolumeWeeklyEntity>)
    @Insert suspend fun insertCardioSessions(items: List<CardioSessionEntity>)
    @Insert suspend fun insertCardioBlocks(items: List<CardioBlockEntity>)
    @Insert suspend fun insertPainLogs(items: List<PainLogEntity>)
    @Insert suspend fun insertFoodPrices(items: List<FoodPriceEntity>)
    @Insert suspend fun insertMeals(items: List<MealEntity>)
    @Insert suspend fun insertMealEntries(items: List<MealEntryEntity>)
    @Insert suspend fun insertFavoriteMeals(items: List<FavoriteMealEntity>)
    @Insert suspend fun insertFavoriteMealEntries(items: List<FavoriteMealEntryEntity>)
    @Insert suspend fun insertRecipes(items: List<RecipeEntity>)
    @Insert suspend fun insertRecipeIngredients(items: List<RecipeIngredientEntity>)
    @Insert suspend fun insertWater(items: List<WaterEntryEntity>)
    @Insert suspend fun insertAlcohol(items: List<AlcoholEntryEntity>)
    @Insert suspend fun insertDietPhases(items: List<DietPhaseEntity>)
    @Insert suspend fun insertDailyLogs(items: List<DailyLogEntity>)
    @Insert suspend fun insertHabitCompletions(items: List<HabitCompletionEntity>)
    @Insert suspend fun insertSleep(items: List<SleepEntryEntity>)
    @Insert suspend fun insertSteps(items: List<StepsEntryEntity>)
    @Insert suspend fun insertHeartRates(items: List<HeartRateSampleEntity>)
    @Insert suspend fun insertWeeklyReviews(items: List<WeeklyReviewEntity>)
    @Insert suspend fun insertCorrelations(items: List<CorrelationInsightEntity>)
    @Insert suspend fun insertProjections(items: List<ProjectionSnapshotEntity>)
    @Insert suspend fun insertAppEvents(items: List<AppEventEntity>)

    @Query("UPDATE progress_photo SET overlayReferencePhotoId = :referenceId WHERE id = :photoId")
    suspend fun setPhotoOverlay(photoId: Long, referenceId: Long)

    // ── Import de l'ancien format (v2) : détection des doublons ──
    @Query("SELECT id FROM exercise WHERE name = :name COLLATE NOCASE ORDER BY isCustom, id LIMIT 1")
    suspend fun exerciseIdByName(name: String): Long?

    @Query("SELECT id FROM food WHERE name = :name COLLATE NOCASE ORDER BY isCustom DESC, id LIMIT 1")
    suspend fun foodIdByName(name: String): Long?

    @Query("SELECT id FROM food WHERE name = :name AND isCustom = 1 LIMIT 1")
    suspend fun customFoodIdByName(name: String): Long?

    @Query("SELECT COUNT(*) FROM meal WHERE date = :date AND mealType = :mealType AND eatenAt = :eatenAt")
    suspend fun countMeals(date: String, mealType: String, eatenAt: Instant): Int

    @Query("SELECT COUNT(*) FROM favorite_meal WHERE name = :name")
    suspend fun countFavorites(name: String): Int

    @Query("SELECT COUNT(*) FROM workout_session WHERE startedAt = :startedAt")
    suspend fun countSessionsStartedAt(startedAt: Instant): Int

    @Query("SELECT COUNT(*) FROM cardio_session WHERE startedAt = :startedAt")
    suspend fun countCardioStartedAt(startedAt: Instant): Int

    @Query("SELECT COUNT(*) FROM water_entry WHERE date = :date")
    suspend fun countWaterOn(date: String): Int

    @Query(
        "SELECT COUNT(*) FROM goal WHERE isActive = 1 AND targetWeightKg = :targetWeightKg " +
            "AND targetDate = :targetDate AND startedAt = :startedAt",
    )
    suspend fun countSameActiveGoal(targetWeightKg: Float, targetDate: String, startedAt: String): Int
}
