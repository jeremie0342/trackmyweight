package com.kps.trackmyweight.data.backup

import com.kps.trackmyweight.data.db.entity.BodyMeasurementSessionEntity
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.DailyLogEntity
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntryEntity
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.MealEntity
import com.kps.trackmyweight.data.db.entity.MealEntryEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.SleepEntryEntity
import com.kps.trackmyweight.data.db.entity.StepsEntryEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.entity.WaterEntryEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.CardioSource
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.data.db.enums.FoodCategory
import com.kps.trackmyweight.data.db.enums.FoodRegion
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.HeartRateSource
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.db.enums.SleepSource
import com.kps.trackmyweight.data.db.enums.StepsSource
import com.kps.trackmyweight.data.db.enums.UnitSystem
import com.kps.trackmyweight.data.db.enums.WaterSource
import com.kps.trackmyweight.data.db.enums.WeightSource
import kotlinx.serialization.Serializable

/**
 * Racine de la sauvegarde. Portable, sans dépendance Room.
 * Tous les timestamps sont stockés en ISO-8601 strings pour être robustes cross-plateforme.
 */
@Serializable
data class BackupRoot(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: String,
    val profile: BProfile? = null,
    val activeGoal: BGoal? = null,
    val weights: List<BWeight> = emptyList(),
    val measurements: List<BMeasurement> = emptyList(),
    val meals: List<BMeal> = emptyList(),
    val favorites: List<BFavoriteMeal> = emptyList(),
    val customFoods: List<BFood> = emptyList(),
    val workoutSessions: List<BWorkoutSession> = emptyList(),
    val cardioSessions: List<BCardio> = emptyList(),
    val sleep: List<BSleep> = emptyList(),
    val steps: List<BSteps> = emptyList(),
    val water: List<BWater> = emptyList(),
    val dailyLogs: List<BDailyLog> = emptyList(),
    val habitCompletions: List<BHabitCompletion> = emptyList(),
    val activePhase: BDietPhase? = null,
    /** Métadonnées des photos ; les fichiers JPG sont dans photos/{fileKey}.jpg du zip. */
    val photos: List<BPhoto> = emptyList(),
) {
    companion object { const val SCHEMA_VERSION = 2 }
}

@Serializable data class BPhoto(
    /** Clé unique dans le zip : photos/{fileKey}.jpg */
    val fileKey: String,
    val date: String,
    val angle: String,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val createdAt: String,
)

@Serializable data class BProfile(
    val sex: Sex, val birthDate: String, val heightCm: Float,
    val preferredUnit: UnitSystem, val currency: String, val locale: String,
    val activityLevel: ActivityLevel, val coachModeEnabled: Boolean,
)

@Serializable data class BGoal(
    val targetWeightKg: Float, val targetDate: String, val phase: GoalPhase,
    val startedAt: String, val endedAt: String? = null, val notes: String? = null,
)

@Serializable data class BWeight(
    val date: String, val weightKg: Float, val source: WeightSource,
    val recordedAt: String, val note: String? = null,
)

@Serializable data class BMeasurement(
    val date: String,
    val neckCm: Float? = null, val shoulderCm: Float? = null, val chestCm: Float? = null,
    val waistCm: Float? = null, val hipCm: Float? = null,
    val armLeftCm: Float? = null, val armRightCm: Float? = null,
    val forearmLeftCm: Float? = null, val forearmRightCm: Float? = null,
    val thighLeftCm: Float? = null, val thighRightCm: Float? = null,
    val calfLeftCm: Float? = null, val calfRightCm: Float? = null,
    val wristCm: Float? = null, val notes: String? = null,
)

@Serializable data class BFood(
    val name: String, val brand: String? = null, val region: FoodRegion, val category: FoodCategory,
    val kcalPer100g: Float, val proteinPer100g: Float, val carbsPer100g: Float, val fatsPer100g: Float,
    val fiberPer100g: Float = 0f, val defaultServingG: Float, val servingLabel: String? = null,
    val barcode: String? = null,
)

@Serializable data class BMealEntry(
    val foodName: String, val portionMode: PortionMode, val portionQuantity: Float,
    val resolvedGrams: Float,
    val kcal: Float, val proteinG: Float, val carbsG: Float, val fatsG: Float, val fiberG: Float,
)

@Serializable data class BMeal(
    val date: String, val mealType: MealType, val eatenAt: String, val notes: String? = null,
    val entries: List<BMealEntry>,
)

@Serializable data class BFavoriteMeal(
    val name: String, val mealTypeHint: MealType? = null,
    val entries: List<BFavoriteMealEntry>,
)

@Serializable data class BFavoriteMealEntry(
    val foodName: String, val portionMode: PortionMode, val portionQuantity: Float,
)

@Serializable data class BWorkoutSession(
    val date: String, val startedAt: String, val endedAt: String? = null,
    val templateName: String? = null, val sessionRpe: Float? = null,
    val notes: String? = null, val totalVolumeKg: Float,
    val performedExercises: List<BPerformedExercise>,
)

@Serializable data class BPerformedExercise(
    val exerciseName: String, val orderIndex: Int, val notes: String? = null,
    val sets: List<BPerformedSet>,
)

@Serializable data class BPerformedSet(
    val setNumber: Int, val weightKg: Float, val reps: Int, val rpe: Float? = null,
    val type: SetType, val restBeforeSec: Int? = null,
)

@Serializable data class BCardio(
    val date: String, val startedAt: String, val endedAt: String? = null,
    val type: CardioType, val durationSec: Int, val distanceM: Float? = null,
    val avgSpeedKmh: Float? = null, val avgRpe: Float? = null,
    val caloriesEstimated: Float, val source: CardioSource, val notes: String? = null,
)

@Serializable data class BSleep(
    val date: String, val bedtime: String, val wakeTime: String,
    val durationMin: Int, val qualityRating: Int? = null, val source: SleepSource,
)

@Serializable data class BSteps(
    val date: String, val count: Int, val source: StepsSource,
    val correctionFactor: Float, val adjustedCount: Int,
)

@Serializable data class BWater(
    val date: String, val timestamp: String, val volumeMl: Int, val source: WaterSource,
)

@Serializable data class BDailyLog(
    val date: String, val readinessSleep: Int? = null, val readinessEnergy: Int? = null,
    val readinessSoreness: Int? = null, val readinessMood: Int? = null,
    val readinessScore: Float? = null, val restingHrBpm: Int? = null,
    val restingHrSource: HeartRateSource? = null, val freeNote: String? = null,
)

@Serializable data class BHabitCompletion(
    val habitKey: String, val date: String, val isDone: Boolean, val valueNumeric: Float? = null,
)

@Serializable data class BDietPhase(
    val startDate: String, val endDate: String? = null, val phase: DietPhaseKind,
    val targetKcal: Int, val targetProteinG: Int, val targetCarbsG: Int, val targetFatsG: Int,
    val notes: String? = null,
)
