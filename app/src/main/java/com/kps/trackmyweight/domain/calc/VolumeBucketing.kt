package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.MuscleGroup

enum class VolumeStatus { UNDER_MEV, WITHIN_RANGE, AT_MAV, OVER_MRV }

data class VolumeLandmarks(val mev: Int, val mav: Int, val mrv: Int)

data class VolumeVerdict(
    val muscleGroup: MuscleGroup,
    val currentSets: Int,
    val landmarks: VolumeLandmarks,
    val status: VolumeStatus,
    val suggestedSetsDelta: Int,
)

/**
 * Landmarks de volume hebdomadaire par groupe musculaire (Renaissance Periodization / Dr. Mike Israetel).
 *
 *  MEV = Minimum Effective Volume : le seuil minimum pour progresser
 *  MAV = Maximum Adaptive Volume : la "sweet spot" pour la plupart des gens
 *  MRV = Maximum Recoverable Volume : au-dessus, tu régresses
 *
 * Ces valeurs sont des points de départ moyens pour un pratiquant intermédiaire.
 */
object VolumeBucketing {

    private val landmarks: Map<MuscleGroup, VolumeLandmarks> = mapOf(
        MuscleGroup.CHEST to VolumeLandmarks(mev = 8, mav = 16, mrv = 22),
        MuscleGroup.BACK_LATS to VolumeLandmarks(mev = 10, mav = 18, mrv = 25),
        MuscleGroup.BACK_UPPER to VolumeLandmarks(mev = 8, mav = 16, mrv = 22),
        MuscleGroup.LOWER_BACK to VolumeLandmarks(mev = 4, mav = 10, mrv = 14),
        MuscleGroup.TRAPS to VolumeLandmarks(mev = 4, mav = 12, mrv = 20),
        MuscleGroup.SHOULDERS_FRONT to VolumeLandmarks(mev = 4, mav = 8, mrv = 12),
        MuscleGroup.SHOULDERS_SIDE to VolumeLandmarks(mev = 8, mav = 16, mrv = 26),
        MuscleGroup.SHOULDERS_REAR to VolumeLandmarks(mev = 6, mav = 14, mrv = 22),
        MuscleGroup.BICEPS to VolumeLandmarks(mev = 8, mav = 14, mrv = 22),
        MuscleGroup.TRICEPS to VolumeLandmarks(mev = 6, mav = 14, mrv = 22),
        MuscleGroup.FOREARMS to VolumeLandmarks(mev = 4, mav = 10, mrv = 20),
        MuscleGroup.QUADS to VolumeLandmarks(mev = 8, mav = 16, mrv = 20),
        MuscleGroup.HAMSTRINGS to VolumeLandmarks(mev = 6, mav = 12, mrv = 20),
        MuscleGroup.GLUTES to VolumeLandmarks(mev = 4, mav = 10, mrv = 16),
        MuscleGroup.CALVES to VolumeLandmarks(mev = 8, mav = 14, mrv = 20),
        MuscleGroup.ABS to VolumeLandmarks(mev = 6, mav = 14, mrv = 20),
        MuscleGroup.OBLIQUES to VolumeLandmarks(mev = 4, mav = 10, mrv = 16),
        MuscleGroup.NECK to VolumeLandmarks(mev = 4, mav = 8, mrv = 12),
    )

    fun landmarksFor(muscleGroup: MuscleGroup): VolumeLandmarks =
        landmarks[muscleGroup] ?: VolumeLandmarks(mev = 6, mav = 12, mrv = 20)

    fun verdictFor(muscleGroup: MuscleGroup, currentSets: Int): VolumeVerdict {
        val l = landmarksFor(muscleGroup)
        val status = when {
            currentSets < l.mev -> VolumeStatus.UNDER_MEV
            currentSets < l.mav -> VolumeStatus.WITHIN_RANGE
            currentSets < l.mrv -> VolumeStatus.AT_MAV
            else -> VolumeStatus.OVER_MRV
        }
        val delta = when (status) {
            VolumeStatus.UNDER_MEV -> (l.mev - currentSets) + 2
            VolumeStatus.WITHIN_RANGE -> 2
            VolumeStatus.AT_MAV -> 0
            VolumeStatus.OVER_MRV -> -(currentSets - l.mav)
        }
        return VolumeVerdict(muscleGroup, currentSets, l, status, delta)
    }

    /**
     * Agrège les séries hebdomadaires par groupe musculaire.
     * `secondaryWeight` : contribution partielle des muscles secondaires (0.5 = compte moitié).
     */
    fun aggregate(
        setsByExercise: List<ExerciseSetsSummary>,
        secondaryWeight: Float = 0.5f,
    ): Map<MuscleGroup, Int> {
        val agg = mutableMapOf<MuscleGroup, Float>()
        setsByExercise.forEach { s ->
            agg.merge(s.primaryMuscle, s.totalSets.toFloat()) { a, b -> a + b }
            s.secondaryMuscles.forEach { m ->
                agg.merge(m, s.totalSets * secondaryWeight) { a, b -> a + b }
            }
        }
        return agg.mapValues { it.value.toInt() }
    }
}

/** Résumé d'un exercice pour l'agrégation hebdomadaire. */
data class ExerciseSetsSummary(
    val exerciseId: Long,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val totalSets: Int,
)
