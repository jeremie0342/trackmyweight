package com.kps.trackmyweight.data.backup

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
import kotlinx.serialization.Serializable

/**
 * Sauvegarde complète (format v3) : une copie de chaque table, ligne pour ligne.
 *
 * L'ancien format ([LegacyBackupRoot]) passait par des objets de transfert
 * écrits à la main, table par table. Chaque table oubliée était une donnée
 * perdue à la restauration — programmes, salles, records, prix des aliments
 * l'étaient tous. Sérialiser directement les entités Room ferme cette porte :
 * une colonne ajoutée à une entité part dans la sauvegarde sans qu'on y pense.
 *
 * Conséquence à respecter : tout champ ajouté à une entité doit avoir une
 * valeur par défaut Kotlin, sans quoi les sauvegardes antérieures ne se
 * décoderaient plus. C'est la même contrainte que pour une migration.
 *
 * Les identifiants sont conservés tels quels. Seuls les référentiels partagés
 * avec le catalogue de l'app (exercices, équipements, aliments, habitudes)
 * sont recalés à l'import, par leur clé naturelle — voir `BackupService`.
 *
 * Exclus volontairement : l'index plein texte (dérivé de `food`), l'état de
 * synchronisation Health Connect et l'historique des sauvegardes, propres à
 * l'appareil.
 */
@Serializable
data class BackupRoot(
    val schemaVersion: Int = SCHEMA_VERSION,
    /** Version du schéma Room au moment de l'export, à titre de diagnostic. */
    val databaseVersion: Int,
    val exportedAt: String,

    // Référentiels
    val equipment: List<EquipmentEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val exerciseRequirements: List<ExerciseEquipmentRequirementEntity> = emptyList(),
    val exerciseSubstitutions: List<ExerciseSubstitutionEntity> = emptyList(),
    val foods: List<FoodEntity> = emptyList(),
    val foodPortionAliases: List<FoodPortionAliasEntity> = emptyList(),
    val habits: List<HabitDefinitionEntity> = emptyList(),

    // Profil & configuration
    val profiles: List<UserProfileEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val gyms: List<GymEntity> = emptyList(),
    val gymEquipment: List<GymEquipmentEntity> = emptyList(),

    // Corps
    val weights: List<WeightEntryEntity> = emptyList(),
    val measurements: List<BodyMeasurementSessionEntity> = emptyList(),
    /** Chemins propres à l'appareil d'origine : réécrits à l'import. */
    val photos: List<ProgressPhotoEntity> = emptyList(),
    val bodyCompositions: List<BodyCompositionSnapshotEntity> = emptyList(),

    // Entraînement
    val exerciseMaxLoads: List<ExerciseMaxLoadEntity> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val templateExercises: List<TemplateExerciseEntity> = emptyList(),
    val rotationGroups: List<TemplateRotationGroupEntity> = emptyList(),
    val rotationMembers: List<TemplateRotationMemberEntity> = emptyList(),
    val programs: List<ProgramEntity> = emptyList(),
    val programDays: List<ProgramDayEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val performedExercises: List<PerformedExerciseEntity> = emptyList(),
    val performedSets: List<PerformedSetEntity> = emptyList(),
    val personalRecords: List<PersonalRecordEntity> = emptyList(),
    val muscleVolumes: List<MuscleGroupVolumeWeeklyEntity> = emptyList(),
    val cardioSessions: List<CardioSessionEntity> = emptyList(),
    val cardioBlocks: List<CardioBlockEntity> = emptyList(),
    val painLogs: List<PainLogEntity> = emptyList(),

    // Nutrition
    val foodPrices: List<FoodPriceEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val mealEntries: List<MealEntryEntity> = emptyList(),
    val favoriteMeals: List<FavoriteMealEntity> = emptyList(),
    val favoriteMealEntries: List<FavoriteMealEntryEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val recipeIngredients: List<RecipeIngredientEntity> = emptyList(),
    val water: List<WaterEntryEntity> = emptyList(),
    val alcohol: List<AlcoholEntryEntity> = emptyList(),
    val dietPhases: List<DietPhaseEntity> = emptyList(),

    // Habitudes & récupération
    val dailyLogs: List<DailyLogEntity> = emptyList(),
    val habitCompletions: List<HabitCompletionEntity> = emptyList(),
    val sleep: List<SleepEntryEntity> = emptyList(),
    val steps: List<StepsEntryEntity> = emptyList(),
    val heartRates: List<HeartRateSampleEntity> = emptyList(),

    // Analyses
    val weeklyReviews: List<WeeklyReviewEntity> = emptyList(),
    val correlations: List<CorrelationInsightEntity> = emptyList(),
    val projections: List<ProjectionSnapshotEntity> = emptyList(),
    val appEvents: List<AppEventEntity> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 3
    }
}
