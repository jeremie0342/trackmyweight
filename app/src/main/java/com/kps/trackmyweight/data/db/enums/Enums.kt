package com.kps.trackmyweight.data.db.enums

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────
// User & config
// ─────────────────────────────────────────────────────────────

@Serializable enum class Sex { MALE, FEMALE }

@Serializable enum class UnitSystem { METRIC, IMPERIAL }

@Serializable
enum class ActivityLevel {
    SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE,
}

@Serializable enum class GoalPhase { CUT, RECOMP, BULK, MAINTENANCE }

// ─────────────────────────────────────────────────────────────
// Equipment & muscles
// ─────────────────────────────────────────────────────────────

@Serializable
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

@Serializable enum class ExerciseMechanics { COMPOUND, ISOLATION }
@Serializable enum class ExerciseForce { PUSH, PULL, HINGE, SQUAT, CARRY, STATIC, ROTATION }

// ─────────────────────────────────────────────────────────────
// Body tracking
// ─────────────────────────────────────────────────────────────

@Serializable enum class WeightSource { MANUAL, HEALTH_CONNECT, IMPORT }
@Serializable enum class PhotoAngle { FRONT, SIDE_LEFT, SIDE_RIGHT, BACK }
@Serializable enum class BodyFatMethod { NAVY, IMPEDANCE, MANUAL, ESTIMATED }

// ─────────────────────────────────────────────────────────────
// Workout & cardio
// ─────────────────────────────────────────────────────────────

@Serializable enum class SetType { WORKING, WARMUP, DROP, FAILURE, BACKOFF, AMRAP }

@Serializable
enum class PrKind {
    ONE_RM_EST, THREE_RM, FIVE_RM,
    MAX_REPS_AT_WEIGHT, MAX_VOLUME_SESSION, MAX_WEIGHT_ANY_REPS,
}

@Serializable
enum class CardioType {
    RUN, BIKE, ROWER, ELLIPTICAL, JUMP_ROPE, WALK, HIIT, LISS, SWIM,
    BATTLE_ROPES, JUMPING_JACKS, BURPEES, MOUNTAIN_CLIMBERS, STAIR_MASTER,
    OTHER,
}

@Serializable enum class CardioSource { MANUAL, HEALTH_CONNECT, GPS }

@Serializable
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

@Serializable enum class FoodRegion { BENIN, WEST_AFRICA, INTERNATIONAL, CUSTOM }

@Serializable
enum class FoodCategory {
    GRAIN, PROTEIN_ANIMAL, PROTEIN_PLANT, VEGETABLE, FRUIT,
    DAIRY, FAT, SAUCE, BEVERAGE, SNACK, COMPOSED_DISH, CONDIMENT, SUPPLEMENT,
}

@Serializable
enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT
}

@Serializable
enum class PortionMode {
    PRECISE_G, PALM, FIST, THUMB, CUPPED_HAND, HANDFUL,
    LADLE_SMALL, LADLE_LARGE, SPOON_TEA, SPOON_TABLE, UNIT, SERVING,
    CUP, GLASS, BOWL, PIECE, PLATE, SLICE,
}

/**
 * Mode de cuisson d'une portion. Impacte les kcal/lipides (huile absorbée pour la friture).
 *  - RAW : cru
 *  - BOILED : bouilli / à l'eau
 *  - STEAMED : à la vapeur
 *  - GRILLED : grillé / braisé (pas d'ajout d'huile)
 *  - BAKED : au four
 *  - SAUTEED : sauté à la poêle avec un peu d'huile (~5%)
 *  - FRIED : frit / bain d'huile (~10% du poids en huile absorbée)
 */
@Serializable
enum class CookingMethod {
    RAW, BOILED, STEAMED, GRILLED, BAKED, SAUTEED, FRIED,
}

@Serializable enum class WaterSource { MANUAL, HEALTH_CONNECT }

@Serializable
enum class DietPhaseKind {
    CUT_MODERATE, CUT_AGGRESSIVE, RECOMP, MAINTENANCE,
    BULK_LEAN, BULK_STANDARD, REFEED, DIET_BREAK,
}

// ─────────────────────────────────────────────────────────────
// Habits & recovery
// ─────────────────────────────────────────────────────────────

@Serializable enum class SleepSource { MANUAL, HEALTH_CONNECT, SLEEP_AS_ANDROID }
@Serializable enum class StepsSource { HEALTH_CONNECT, MANUAL, PEDOMETER }
@Serializable enum class HeartRateContext { REST_MORNING, POST_WORKOUT, MANUAL, DURING_ACTIVITY }
@Serializable enum class HeartRateSource { MANUAL, CAMERA_PPG, WEARABLE, HEALTH_CONNECT }

// ─────────────────────────────────────────────────────────────
// Analytics
// ─────────────────────────────────────────────────────────────

@Serializable enum class CorrelationPeriod { LAST_30D, LAST_90D, ALL }

// ─────────────────────────────────────────────────────────────
// Meta
// ─────────────────────────────────────────────────────────────

@Serializable enum class BackupDestination { LOCAL, DRIVE, SHARE }
