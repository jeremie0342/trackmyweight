package com.kps.trackmyweight.data.db.enums

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────
// User & config
// ─────────────────────────────────────────────────────────────

@Serializable
enum class Sex { MALE, FEMALE }

enum class UnitSystem { METRIC, IMPERIAL }

enum class ActivityLevel {
    SEDENTARY,        // 1.2
    LIGHTLY_ACTIVE,   // 1.375
    MODERATELY_ACTIVE,// 1.55
    VERY_ACTIVE,      // 1.725
    EXTRA_ACTIVE,     // 1.9
}

enum class GoalPhase { CUT, RECOMP, BULK, MAINTENANCE }

// ─────────────────────────────────────────────────────────────
// Equipment & muscles
// ─────────────────────────────────────────────────────────────

enum class EquipmentCategory {
    BAR, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, CARDIO, ACCESSORY, KETTLEBELL
}

@Serializable
enum class MuscleGroup {
    CHEST,
    BACK_LATS, BACK_UPPER, LOWER_BACK, TRAPS,
    SHOULDERS_FRONT, SHOULDERS_SIDE, SHOULDERS_REAR,
    BICEPS, TRICEPS, FOREARMS,
    QUADS, HAMSTRINGS, GLUTES, CALVES,
    ABS, OBLIQUES, NECK,
}

enum class ExerciseMechanics { COMPOUND, ISOLATION }

enum class ExerciseForce { PUSH, PULL, HINGE, SQUAT, CARRY, STATIC, ROTATION }

// ─────────────────────────────────────────────────────────────
// Body tracking
// ─────────────────────────────────────────────────────────────

enum class WeightSource { MANUAL, HEALTH_CONNECT, IMPORT }

enum class PhotoAngle { FRONT, SIDE_LEFT, SIDE_RIGHT, BACK }

enum class BodyFatMethod { NAVY, IMPEDANCE, MANUAL, ESTIMATED }

// ─────────────────────────────────────────────────────────────
// Workout & cardio
// ─────────────────────────────────────────────────────────────

enum class SetType { WORKING, WARMUP, DROP, FAILURE, BACKOFF, AMRAP }

enum class PrKind {
    ONE_RM_EST, THREE_RM, FIVE_RM,
    MAX_REPS_AT_WEIGHT, MAX_VOLUME_SESSION, MAX_WEIGHT_ANY_REPS,
}

enum class CardioType {
    RUN, BIKE, ROWER, ELLIPTICAL, JUMP_ROPE, WALK, HIIT, LISS, SWIM, OTHER
}

enum class CardioSource { MANUAL, HEALTH_CONNECT, GPS }

enum class PainArea {
    SHOULDER_L, SHOULDER_R,
    ELBOW_L, ELBOW_R,
    WRIST_L, WRIST_R,
    LOWER_BACK,
    KNEE_L, KNEE_R,
    HIP_L, HIP_R,
    ANKLE_L, ANKLE_R,
    NECK, UPPER_BACK, OTHER,
}

// ─────────────────────────────────────────────────────────────
// Nutrition
// ─────────────────────────────────────────────────────────────

enum class FoodRegion { BENIN, WEST_AFRICA, INTERNATIONAL, CUSTOM }

enum class FoodCategory {
    GRAIN, PROTEIN_ANIMAL, PROTEIN_PLANT, VEGETABLE, FRUIT,
    DAIRY, FAT, SAUCE, BEVERAGE, SNACK, COMPOSED_DISH, CONDIMENT, SUPPLEMENT,
}

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT
}

enum class PortionMode {
    PRECISE_G,      // grammes exacts
    PALM,           // paume (protéine ~100-120g)
    FIST,           // poing (féculent ~150g cuit)
    THUMB,          // pouce (matière grasse ~10-15g)
    CUPPED_HAND,    // main en coupe (~30g fruits secs, ~80g légumes)
    HANDFUL,        // poignée (~30g)
    LADLE_SMALL,    // petite louche (~80ml)
    LADLE_LARGE,    // grande louche (~150ml)
    SPOON_TEA,      // c. à café (~5g)
    SPOON_TABLE,    // c. à soupe (~15g)
    UNIT,           // à l'unité
    SERVING,        // portion standard de l'aliment
}

enum class WaterSource { MANUAL, HEALTH_CONNECT }

enum class DietPhaseKind {
    CUT_MODERATE, CUT_AGGRESSIVE, RECOMP, MAINTENANCE,
    BULK_LEAN, BULK_STANDARD, REFEED, DIET_BREAK,
}

// ─────────────────────────────────────────────────────────────
// Habits & recovery
// ─────────────────────────────────────────────────────────────

enum class SleepSource { MANUAL, HEALTH_CONNECT, SLEEP_AS_ANDROID }

enum class StepsSource { HEALTH_CONNECT, MANUAL, PEDOMETER }

enum class HeartRateContext { REST_MORNING, POST_WORKOUT, MANUAL, DURING_ACTIVITY }

enum class HeartRateSource { MANUAL, CAMERA_PPG, WEARABLE, HEALTH_CONNECT }

// ─────────────────────────────────────────────────────────────
// Analytics
// ─────────────────────────────────────────────────────────────

enum class CorrelationPeriod { LAST_30D, LAST_90D, ALL }

// ─────────────────────────────────────────────────────────────
// Meta
// ─────────────────────────────────────────────────────────────

enum class BackupDestination { LOCAL, DRIVE, SHARE }
