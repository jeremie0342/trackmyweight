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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

// Conversion de l'ancien format de sauvegarde (v1-v2) vers les entités, pour
// l'import uniquement : ce format n'est plus produit.

// ─────── Profile ───────
fun BProfile.toEntity(now: Instant = Clock.System.now()) = UserProfileEntity(
    sex = sex, birthDate = LocalDate.parse(birthDate), heightCm = heightCm,
    preferredUnit = preferredUnit, currency = currency, locale = locale,
    activityLevel = activityLevel, coachModeEnabled = coachModeEnabled,
    createdAt = now, updatedAt = now,
)

// ─────── Goal ───────
fun BGoal.toEntity(now: Instant = Clock.System.now()) = GoalEntity(
    targetWeightKg = targetWeightKg, targetDate = LocalDate.parse(targetDate),
    phase = phase, isActive = true, startedAt = LocalDate.parse(startedAt),
    endedAt = endedAt?.let(LocalDate::parse), notes = notes,
    createdAt = now, updatedAt = now,
)

// ─────── Weight ───────
fun BWeight.toEntity() = WeightEntryEntity(
    date = LocalDate.parse(date), weightKg = weightKg, source = source,
    recordedAt = Instant.parse(recordedAt), note = note,
    createdAt = Instant.parse(recordedAt),
)

// ─────── Measurement ───────
fun BMeasurement.toEntity(now: Instant = Clock.System.now()) = BodyMeasurementSessionEntity(
    date = LocalDate.parse(date),
    neckCm = neckCm, shoulderCm = shoulderCm, chestCm = chestCm,
    waistCm = waistCm, hipCm = hipCm,
    armLeftCm = armLeftCm, armRightCm = armRightCm,
    forearmLeftCm = forearmLeftCm, forearmRightCm = forearmRightCm,
    thighLeftCm = thighLeftCm, thighRightCm = thighRightCm,
    calfLeftCm = calfLeftCm, calfRightCm = calfRightCm,
    wristCm = wristCm, notes = notes,
    createdAt = now, updatedAt = now,
)

// ─────── Food ───────
fun BFood.toEntity(now: Instant = Clock.System.now()) = FoodEntity(
    name = name, brand = brand, region = region, category = category,
    kcalPer100g = kcalPer100g, proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g, fatsPer100g = fatsPer100g, fiberPer100g = fiberPer100g,
    defaultServingG = defaultServingG, servingLabel = servingLabel, barcode = barcode,
    isCustom = true, isVerified = false,
    createdAt = now, updatedAt = now,
)

// ─────── Meal ───────
fun BMeal.toEntity(now: Instant = Clock.System.now()) = MealEntity(
    date = LocalDate.parse(date), mealType = mealType,
    eatenAt = Instant.parse(eatenAt), notes = notes, createdAt = now,
)
fun BMealEntry.toEntity(mealId: Long, foodId: Long) = MealEntryEntity(
    mealId = mealId, foodId = foodId, portionMode = portionMode, portionQuantity = portionQuantity,
    resolvedGrams = resolvedGrams,
    snapKcal = kcal, snapProteinG = proteinG, snapCarbsG = carbsG, snapFatsG = fatsG, snapFiberG = fiberG,
)

// ─────── Favorites ───────
fun BFavoriteMeal.toEntity(now: Instant = Clock.System.now()) = FavoriteMealEntity(
    name = name, mealTypeHint = mealTypeHint, createdAt = now,
)
fun BFavoriteMealEntry.toEntity(favoriteMealId: Long, foodId: Long) = FavoriteMealEntryEntity(
    favoriteMealId = favoriteMealId, foodId = foodId,
    portionMode = portionMode, portionQuantity = portionQuantity,
)

// ─────── Workout session ───────
fun BWorkoutSession.toEntity() = WorkoutSessionEntity(
    date = LocalDate.parse(date),
    startedAt = Instant.parse(startedAt),
    endedAt = endedAt?.let(Instant::parse),
    templateId = null, programId = null, gymId = null,
    sessionRpe = sessionRpe, mood = null, notes = notes,
    totalVolumeKg = totalVolumeKg, totalCalories = 0f, isCoachProgram = false,
)
fun BPerformedExercise.toEntity(sessionId: Long, exerciseId: Long) = PerformedExerciseEntity(
    sessionId = sessionId, exerciseId = exerciseId,
    exerciseNameSnapshot = exerciseName, orderIndex = orderIndex, notes = notes,
    targetSets = targetSets, targetRepsMin = targetRepsMin, targetRepsMax = targetRepsMax,
    targetRpe = targetRpe, targetWeightKg = targetWeightKg, restSecOverride = restSecOverride,
    supersetGroup = supersetGroup,
)
fun BPerformedSet.toEntity(performedExerciseId: Long) = PerformedSetEntity(
    performedExerciseId = performedExerciseId, setNumber = setNumber,
    weightKg = weightKg, reps = reps, rpe = rpe, type = type,
    restBeforeSec = restBeforeSec, isPrCandidate = false, createdAt = Clock.System.now(),
)

// ─────── Cardio ───────
fun BCardio.toEntity() = CardioSessionEntity(
    date = LocalDate.parse(date), startedAt = Instant.parse(startedAt),
    endedAt = endedAt?.let(Instant::parse), type = type, durationSec = durationSec,
    distanceM = distanceM, avgSpeedKmh = avgSpeedKmh, avgRpe = avgRpe,
    caloriesEstimated = caloriesEstimated, source = source, notes = notes,
    createdAt = Clock.System.now(),
)

// ─────── Sleep/Steps/Water/DailyLog/Habit ───────
fun BSleep.toEntity() = SleepEntryEntity(
    date = LocalDate.parse(date), bedtime = Instant.parse(bedtime), wakeTime = Instant.parse(wakeTime),
    durationMin = durationMin, qualityRating = qualityRating, source = source,
    createdAt = Clock.System.now(),
)
fun BSteps.toEntity() = StepsEntryEntity(
    date = LocalDate.parse(date), count = count, source = source,
    correctionFactor = correctionFactor, adjustedCount = adjustedCount,
    updatedAt = Clock.System.now(),
)
fun BWater.toEntity() = WaterEntryEntity(
    date = LocalDate.parse(date), timestamp = Instant.parse(timestamp),
    volumeMl = volumeMl, source = source,
)
fun BDailyLog.toEntity(now: Instant = Clock.System.now()) = DailyLogEntity(
    date = LocalDate.parse(date),
    readinessSleep = readinessSleep, readinessEnergy = readinessEnergy,
    readinessSoreness = readinessSoreness, readinessMood = readinessMood,
    readinessScore = readinessScore, restingHrBpm = restingHrBpm,
    restingHrSource = restingHrSource, freeNote = freeNote,
    createdAt = now, updatedAt = now,
)
fun BHabitCompletion.toEntity(habitId: Long) = HabitCompletionEntity(
    habitId = habitId, date = LocalDate.parse(date), isDone = isDone, valueNumeric = valueNumeric,
)

// ─────── DietPhase ───────
fun BDietPhase.toEntity(now: Instant = Clock.System.now()) = DietPhaseEntity(
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    phase = phase, targetKcal = targetKcal, targetProteinG = targetProteinG,
    targetCarbsG = targetCarbsG, targetFatsG = targetFatsG,
    notes = notes, isActive = true, createdAt = now,
)
