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

// ─────── Profile ───────
fun UserProfileEntity.toB() = BProfile(sex, birthDate.toString(), heightCm, preferredUnit, currency, locale, activityLevel, coachModeEnabled)
fun BProfile.toEntity(now: Instant = Clock.System.now()) = UserProfileEntity(
    sex = sex, birthDate = LocalDate.parse(birthDate), heightCm = heightCm,
    preferredUnit = preferredUnit, currency = currency, locale = locale,
    activityLevel = activityLevel, coachModeEnabled = coachModeEnabled,
    createdAt = now, updatedAt = now,
)

// ─────── Goal ───────
fun GoalEntity.toB() = BGoal(targetWeightKg, targetDate.toString(), phase, startedAt.toString(), endedAt?.toString(), notes)
fun BGoal.toEntity(now: Instant = Clock.System.now()) = GoalEntity(
    targetWeightKg = targetWeightKg, targetDate = LocalDate.parse(targetDate),
    phase = phase, isActive = true, startedAt = LocalDate.parse(startedAt),
    endedAt = endedAt?.let(LocalDate::parse), notes = notes,
    createdAt = now, updatedAt = now,
)

// ─────── Weight ───────
fun WeightEntryEntity.toB() = BWeight(date.toString(), weightKg, source, recordedAt.toString(), note)
fun BWeight.toEntity() = WeightEntryEntity(
    date = LocalDate.parse(date), weightKg = weightKg, source = source,
    recordedAt = Instant.parse(recordedAt), note = note,
    createdAt = Instant.parse(recordedAt),
)

// ─────── Measurement ───────
fun BodyMeasurementSessionEntity.toB() = BMeasurement(
    date = date.toString(), neckCm = neckCm, shoulderCm = shoulderCm, chestCm = chestCm,
    waistCm = waistCm, hipCm = hipCm,
    armLeftCm = armLeftCm, armRightCm = armRightCm,
    forearmLeftCm = forearmLeftCm, forearmRightCm = forearmRightCm,
    thighLeftCm = thighLeftCm, thighRightCm = thighRightCm,
    calfLeftCm = calfLeftCm, calfRightCm = calfRightCm,
    wristCm = wristCm, notes = notes,
)
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
fun FoodEntity.toB() = BFood(
    name = name, brand = brand, region = region, category = category,
    kcalPer100g = kcalPer100g, proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g, fatsPer100g = fatsPer100g, fiberPer100g = fiberPer100g,
    defaultServingG = defaultServingG, servingLabel = servingLabel, barcode = barcode,
)
fun BFood.toEntity(now: Instant = Clock.System.now()) = FoodEntity(
    name = name, brand = brand, region = region, category = category,
    kcalPer100g = kcalPer100g, proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g, fatsPer100g = fatsPer100g, fiberPer100g = fiberPer100g,
    defaultServingG = defaultServingG, servingLabel = servingLabel, barcode = barcode,
    isCustom = true, isVerified = false,
    createdAt = now, updatedAt = now,
)

// ─────── Meal ───────
fun MealEntity.toB(entries: List<MealEntryEntity>, foodsById: Map<Long, FoodEntity>): BMeal = BMeal(
    date = date.toString(), mealType = mealType, eatenAt = eatenAt.toString(), notes = notes,
    entries = entries.map { e ->
        BMealEntry(
            foodName = foodsById[e.foodId]?.name ?: "?",
            portionMode = e.portionMode, portionQuantity = e.portionQuantity,
            resolvedGrams = e.resolvedGrams,
            kcal = e.snapKcal, proteinG = e.snapProteinG, carbsG = e.snapCarbsG,
            fatsG = e.snapFatsG, fiberG = e.snapFiberG,
        )
    },
)
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
fun FavoriteMealEntity.toB(entries: List<FavoriteMealEntryEntity>, foodsById: Map<Long, FoodEntity>) = BFavoriteMeal(
    name = name, mealTypeHint = mealTypeHint,
    entries = entries.map { e ->
        BFavoriteMealEntry(
            foodName = foodsById[e.foodId]?.name ?: "?",
            portionMode = e.portionMode, portionQuantity = e.portionQuantity,
        )
    },
)
fun BFavoriteMeal.toEntity(now: Instant = Clock.System.now()) = FavoriteMealEntity(
    name = name, mealTypeHint = mealTypeHint, createdAt = now,
)
fun BFavoriteMealEntry.toEntity(favoriteMealId: Long, foodId: Long) = FavoriteMealEntryEntity(
    favoriteMealId = favoriteMealId, foodId = foodId,
    portionMode = portionMode, portionQuantity = portionQuantity,
)

// ─────── Workout session ───────
fun WorkoutSessionEntity.toB(
    performed: List<PerformedExerciseEntity>,
    setsByPerformed: Map<PerformedExerciseEntity, List<PerformedSetEntity>>,
) = BWorkoutSession(
    date = date.toString(), startedAt = startedAt.toString(), endedAt = endedAt?.toString(),
    templateName = null, sessionRpe = sessionRpe, notes = notes, totalVolumeKg = totalVolumeKg,
    performedExercises = performed.map { pe ->
        BPerformedExercise(
            exerciseName = pe.exerciseNameSnapshot, orderIndex = pe.orderIndex, notes = pe.notes,
            sets = setsByPerformed[pe].orEmpty().map { s ->
                BPerformedSet(s.setNumber, s.weightKg, s.reps, s.rpe, s.type, s.restBeforeSec)
            },
        )
    },
)
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
)
fun BPerformedSet.toEntity(performedExerciseId: Long) = PerformedSetEntity(
    performedExerciseId = performedExerciseId, setNumber = setNumber,
    weightKg = weightKg, reps = reps, rpe = rpe, type = type,
    restBeforeSec = restBeforeSec, isPrCandidate = false, createdAt = Clock.System.now(),
)

// ─────── Cardio ───────
fun CardioSessionEntity.toB() = BCardio(
    date = date.toString(), startedAt = startedAt.toString(), endedAt = endedAt?.toString(),
    type = type, durationSec = durationSec, distanceM = distanceM, avgSpeedKmh = avgSpeedKmh,
    avgRpe = avgRpe, caloriesEstimated = caloriesEstimated, source = source, notes = notes,
)
fun BCardio.toEntity() = CardioSessionEntity(
    date = LocalDate.parse(date), startedAt = Instant.parse(startedAt),
    endedAt = endedAt?.let(Instant::parse), type = type, durationSec = durationSec,
    distanceM = distanceM, avgSpeedKmh = avgSpeedKmh, avgRpe = avgRpe,
    caloriesEstimated = caloriesEstimated, source = source, notes = notes,
    createdAt = Clock.System.now(),
)

// ─────── Sleep/Steps/Water/DailyLog/Habit ───────
fun SleepEntryEntity.toB() = BSleep(date.toString(), bedtime.toString(), wakeTime.toString(), durationMin, qualityRating, source)
fun BSleep.toEntity() = SleepEntryEntity(
    date = LocalDate.parse(date), bedtime = Instant.parse(bedtime), wakeTime = Instant.parse(wakeTime),
    durationMin = durationMin, qualityRating = qualityRating, source = source,
    createdAt = Clock.System.now(),
)
fun StepsEntryEntity.toB() = BSteps(date.toString(), count, source, correctionFactor, adjustedCount)
fun BSteps.toEntity() = StepsEntryEntity(
    date = LocalDate.parse(date), count = count, source = source,
    correctionFactor = correctionFactor, adjustedCount = adjustedCount,
    updatedAt = Clock.System.now(),
)
fun WaterEntryEntity.toB() = BWater(date.toString(), timestamp.toString(), volumeMl, source)
fun BWater.toEntity() = WaterEntryEntity(
    date = LocalDate.parse(date), timestamp = Instant.parse(timestamp),
    volumeMl = volumeMl, source = source,
)
fun DailyLogEntity.toB() = BDailyLog(
    date.toString(), readinessSleep, readinessEnergy, readinessSoreness, readinessMood,
    readinessScore, restingHrBpm, restingHrSource, freeNote,
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
fun DietPhaseEntity.toB() = BDietPhase(
    startDate.toString(), endDate?.toString(), phase,
    targetKcal, targetProteinG, targetCarbsG, targetFatsG, notes,
)
fun BDietPhase.toEntity(now: Instant = Clock.System.now()) = DietPhaseEntity(
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    phase = phase, targetKcal = targetKcal, targetProteinG = targetProteinG,
    targetCarbsG = targetCarbsG, targetFatsG = targetFatsG,
    notes = notes, isActive = true, createdAt = now,
)
