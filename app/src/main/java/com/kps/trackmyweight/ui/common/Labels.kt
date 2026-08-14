package com.kps.trackmyweight.ui.common

import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.EquipmentCategory
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.domain.calc.DistributionQuality
import com.kps.trackmyweight.domain.calc.VolumeStatus
import com.kps.trackmyweight.domain.calc.VolumeVerdict

/**
 * Libellés français des enums métier.
 *
 * Ces constantes ne doivent jamais être affichées brutes : `SHOULDERS_FRONT`
 * ou `BAR` n'ont aucun sens pour l'utilisateur.
 *
 * Toutes les surcharges de `labelFr` vivent ici, dans le même package : un seul
 * import les couvre toutes, et la résolution se fait sur le type du receveur.
 * Les éparpiller obligeait à jongler avec des alias d'import.
 */
fun MuscleGroup.labelFr(): String = when (this) {
    MuscleGroup.CHEST -> "Pectoraux"
    MuscleGroup.BACK_LATS -> "Dorsaux"
    MuscleGroup.BACK_UPPER -> "Haut du dos"
    MuscleGroup.LOWER_BACK -> "Lombaires"
    MuscleGroup.TRAPS -> "Trapèzes"
    MuscleGroup.SHOULDERS_FRONT -> "Épaules avant"
    MuscleGroup.SHOULDERS_SIDE -> "Épaules latérales"
    MuscleGroup.SHOULDERS_REAR -> "Épaules arrière"
    MuscleGroup.BICEPS -> "Biceps"
    MuscleGroup.TRICEPS -> "Triceps"
    MuscleGroup.FOREARMS -> "Avant-bras"
    MuscleGroup.QUADS -> "Quadriceps"
    MuscleGroup.HAMSTRINGS -> "Ischio-jambiers"
    MuscleGroup.GLUTES -> "Fessiers"
    MuscleGroup.CALVES -> "Mollets"
    MuscleGroup.ABS -> "Abdominaux"
    MuscleGroup.OBLIQUES -> "Obliques"
    MuscleGroup.NECK -> "Nuque"
}

fun ExerciseMechanics.labelFr(): String = when (this) {
    ExerciseMechanics.COMPOUND -> "Polyarticulaire"
    ExerciseMechanics.ISOLATION -> "Isolation"
}

fun SetType.labelFr(): String = when (this) {
    SetType.WORKING -> "Normale"
    SetType.WARMUP -> "Échauffement"
    SetType.DROP -> "Dégressive"
    SetType.FAILURE -> "Échec"
    SetType.BACKOFF -> "Back-off"
    SetType.AMRAP -> "AMRAP"
}

/** Explication courte, affichée sous le libellé au moment du choix. */
fun SetType.descriptionFr(): String = when (this) {
    SetType.WORKING -> "Série de travail classique"
    SetType.WARMUP -> "Ne compte ni dans le volume ni dans les records"
    SetType.DROP -> "Charge réduite enchaînée sans repos"
    SetType.FAILURE -> "Menée jusqu'à l'échec musculaire"
    SetType.BACKOFF -> "Série allégée après la série lourde"
    SetType.AMRAP -> "Autant de répétitions que possible"
}

fun EquipmentCategory.labelFr(): String = when (this) {
    EquipmentCategory.BAR -> "Barres et disques"
    EquipmentCategory.DUMBBELL -> "Haltères"
    EquipmentCategory.KETTLEBELL -> "Kettlebells"
    EquipmentCategory.MACHINE -> "Machines et bancs"
    EquipmentCategory.CABLE -> "Poulies"
    EquipmentCategory.BODYWEIGHT -> "Poids du corps"
    EquipmentCategory.CARDIO -> "Cardio"
    EquipmentCategory.ACCESSORY -> "Accessoires"
}

fun ExerciseForce.labelFr(): String = when (this) {
    ExerciseForce.PUSH -> "Poussée"
    ExerciseForce.PULL -> "Tirage"
    ExerciseForce.HINGE -> "Charnière de hanche"
    ExerciseForce.SQUAT -> "Flexion de jambes"
    ExerciseForce.CARRY -> "Port de charge"
    ExerciseForce.STATIC -> "Gainage"
    ExerciseForce.ROTATION -> "Rotation"
}

fun CardioType.labelFr() = when (this) {
    CardioType.WALK -> "Marche"
    CardioType.RUN -> "Course"
    CardioType.LISS -> "LISS"
    CardioType.BIKE -> "Vélo"
    CardioType.ROWER -> "Rameur"
    CardioType.ELLIPTICAL -> "Elliptique"
    CardioType.JUMP_ROPE -> "Corde à sauter"
    CardioType.HIIT -> "HIIT"
    CardioType.SWIM -> "Natation"
    CardioType.BATTLE_ROPES -> "Battle ropes"
    CardioType.JUMPING_JACKS -> "Jumping jacks"
    CardioType.BURPEES -> "Burpees"
    CardioType.MOUNTAIN_CLIMBERS -> "Mountain climbers"
    CardioType.STAIR_MASTER -> "Stair master"
    CardioType.OTHER -> "Autre"
}

/**
 * Repères de volume hebdomadaire (Renaissance Periodization).
 * MEV = minimum efficace, MAV = zone optimale, MRV = maximum récupérable.
 */
fun VolumeStatus.labelFr(): String = when (this) {
    VolumeStatus.UNDER_MEV -> "Sous le minimum"
    VolumeStatus.WITHIN_RANGE -> "Dans la zone"
    VolumeStatus.AT_MAV -> "Volume optimal"
    VolumeStatus.OVER_MRV -> "Au-delà du récupérable"
}

/** Conseil associé, formulé à la deuxième personne comme le reste de l'app. */
fun VolumeVerdict.adviceFr(): String = when (status) {
    VolumeStatus.UNDER_MEV -> "Ajoute ${suggestedSetsDelta} séries pour progresser"
    VolumeStatus.WITHIN_RANGE -> "Tu peux monter de ${suggestedSetsDelta} séries"
    VolumeStatus.AT_MAV -> "Maintiens ce volume"
    VolumeStatus.OVER_MRV -> "Retire ${-suggestedSetsDelta} séries, tu ne récupères plus"
}

fun DistributionQuality.labelFr(): String = when (this) {
    DistributionQuality.EXCELLENT -> "Excellente"
    DistributionQuality.GOOD -> "Correcte"
    DistributionQuality.UNBALANCED -> "Déséquilibrée"
    DistributionQuality.INSUFFICIENT -> "Insuffisante"
}

fun PainArea.labelFr(): String = when (this) {
    PainArea.SHOULDER_L -> "Épaule gauche"
    PainArea.SHOULDER_R -> "Épaule droite"
    PainArea.ELBOW_L -> "Coude gauche"
    PainArea.ELBOW_R -> "Coude droit"
    PainArea.WRIST_L -> "Poignet gauche"
    PainArea.WRIST_R -> "Poignet droit"
    PainArea.LOWER_BACK -> "Bas du dos"
    PainArea.KNEE_L -> "Genou gauche"
    PainArea.KNEE_R -> "Genou droit"
    PainArea.HIP_L -> "Hanche gauche"
    PainArea.HIP_R -> "Hanche droite"
    PainArea.ANKLE_L -> "Cheville gauche"
    PainArea.ANKLE_R -> "Cheville droite"
    PainArea.NECK -> "Nuque"
    PainArea.UPPER_BACK -> "Haut du dos"
    PainArea.OTHER -> "Autre"
}

/** Qualification d'une intensité sur 10, pour éviter d'afficher un nombre nu. */
fun painIntensityLabelFr(intensity: Int): String = when {
    intensity <= 2 -> "Gêne légère"
    intensity <= 4 -> "Inconfort"
    intensity <= 6 -> "Douleur nette"
    intensity <= 8 -> "Douleur forte"
    else -> "Douleur sévère"
}
