package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import com.kps.trackmyweight.data.db.entity.ExerciseMaxLoadEntity
import com.kps.trackmyweight.data.db.entity.MonthlyTonnageRow
import com.kps.trackmyweight.data.db.entity.MuscleGroupVolumeWeeklyEntity
import com.kps.trackmyweight.data.db.enums.MaxLoadSource
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.domain.calc.ExerciseSetsSummary
import com.kps.trackmyweight.domain.calc.IsoWeek
import com.kps.trackmyweight.domain.calc.OneRepMax
import com.kps.trackmyweight.domain.calc.VolumeBucketing
import com.kps.trackmyweight.domain.calc.VolumeVerdict
import com.kps.trackmyweight.domain.calc.WorkingLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/** Charge maximale courante d'un exercice, avec sa progression sur la période. */
data class MaxLoadSummary(
    val exerciseId: Long,
    val exerciseName: String,
    val current: ExerciseMaxLoadEntity,
    /** Meilleure valeur connue au début de la fenêtre d'observation. */
    val referenceKg: Float?,
) {
    val progressionPercent: Float?
        get() = WorkingLoad.progressionPercent(referenceKg, current.oneRmKg)

    val deltaKg: Float? get() = referenceKg?.let { current.oneRmKg - it }
}

/**
 * Force et progression : charge maximale de référence par exercice, et volume
 * soulevé au fil des mois.
 *
 * Séparé de [WorkoutRepository], qui gère le déroulé d'une séance. Ici on ne
 * regarde plus une séance mais une trajectoire.
 */
@Singleton
class StrengthRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
) {

    // ─────── Charge maximale ───────

    fun observeMaxLoadHistory(exerciseId: Long): Flow<List<ExerciseMaxLoadEntity>> =
        workoutDao.observeMaxLoadHistory(exerciseId)

    suspend fun getLatestMaxLoad(exerciseId: Long): ExerciseMaxLoadEntity? =
        workoutDao.getLatestMaxLoad(exerciseId)

    /** Saisie manuelle : un vrai test à 1 répétition. */
    suspend fun recordTestedMax(exerciseId: Long, weightKg: Float, notes: String? = null): Long =
        insert(exerciseId, weightKg, MaxLoadSource.TESTED, weightKg, 1, notes)

    /** Saisie manuelle d'une valeur connue, sans test réalisé dans l'app. */
    suspend fun recordDeclaredMax(exerciseId: Long, oneRmKg: Float, notes: String? = null): Long =
        insert(exerciseId, oneRmKg, MaxLoadSource.DECLARED, null, null, notes)

    /**
     * Estime le max depuis une série réelle et l'enregistre.
     *
     * Utilisé aussi bien depuis l'UI (« calculer depuis ma meilleure série »)
     * que automatiquement à la détection d'un record de 1RM estimé.
     */
    suspend fun recordEstimatedMax(
        exerciseId: Long,
        weightKg: Float,
        reps: Int,
        at: Instant = Clock.System.now(),
        notes: String? = null,
    ): Long? {
        val oneRm = OneRepMax.average(weightKg, reps)
        if (oneRm <= 0f) return null
        return insert(exerciseId, oneRm, MaxLoadSource.ESTIMATED, weightKg, reps, notes, at)
    }

    suspend fun deleteMaxLoad(id: Long) = workoutDao.deleteMaxLoad(id)

    private suspend fun insert(
        exerciseId: Long,
        oneRmKg: Float,
        source: MaxLoadSource,
        sourceWeightKg: Float?,
        sourceReps: Int?,
        notes: String?,
        at: Instant = Clock.System.now(),
    ): Long = workoutDao.insertMaxLoad(
        ExerciseMaxLoadEntity(
            exerciseId = exerciseId,
            oneRmKg = oneRmKg,
            source = source,
            sourceWeightKg = sourceWeightKg,
            sourceReps = sourceReps,
            measuredAt = at,
            notes = notes,
        )
    )

    /**
     * Tous les exercices ayant une charge de référence, avec leur progression
     * sur les [windowDays] derniers jours.
     */
    fun observeMaxLoadSummaries(windowDays: Int = 90): Flow<List<MaxLoadSummary>> =
        combine(
            workoutDao.observeLatestMaxLoads(),
            exerciseDao.observeAll(),
        ) { latest, exercises ->
            val nameById = exercises.associate { it.id to it.name }
            val since = Clock.System.now() - windowDays.days
            latest.mapNotNull { entry ->
                val name = nameById[entry.exerciseId] ?: return@mapNotNull null
                MaxLoadSummary(
                    exerciseId = entry.exerciseId,
                    exerciseName = name,
                    current = entry,
                    referenceKg = workoutDao.bestMaxLoadBefore(entry.exerciseId, since),
                )
            }.sortedByDescending { it.current.measuredAt }
        }

    // ─────── Volume hebdomadaire par groupe musculaire ───────

    /**
     * Recalcule et persiste le volume de la semaine en cours, groupe musculaire
     * par groupe musculaire, confronté aux repères MEV/MAV/MRV.
     *
     * [VolumeBucketing] et la table `muscle_group_volume_weekly` existaient
     * depuis le début sans qu'aucun code ne les alimente : la fonctionnalité
     * annoncée n'était donc jamais calculée.
     *
     * Les muscles secondaires comptent pour moitié : un développé couché
     * sollicite réellement les triceps, mais pas autant que les pectoraux.
     */
    suspend fun refreshWeeklyVolume(reference: LocalDate = todayLocal()): List<VolumeVerdict> {
        val monday = reference.minus(DatePeriod(days = reference.dayOfWeek.isoDayNumber - 1))
        val sunday = monday.plus(DatePeriod(days = 6))
        val isoWeek = IsoWeek.of(reference)

        val rows = workoutDao.setsPerExerciseBetween(monday, sunday)
        if (rows.isEmpty()) return emptyList()

        val exercises = exerciseDao.observeAll().first().associateBy { it.id }
        val summaries = rows.mapNotNull { row ->
            val exercise = exercises[row.exerciseId] ?: return@mapNotNull null
            ExerciseSetsSummary(
                exerciseId = row.exerciseId,
                primaryMuscle = exercise.primaryMuscle,
                secondaryMuscles = exercise.secondaryMuscles,
                totalSets = row.totalSets,
            )
        }

        val setsByMuscle = VolumeBucketing.aggregate(summaries)
        val repsByMuscle = mutableMapOf<MuscleGroup, Int>()
        val volumeByMuscle = mutableMapOf<MuscleGroup, Float>()
        rows.forEach { row ->
            val exercise = exercises[row.exerciseId] ?: return@forEach
            repsByMuscle.merge(exercise.primaryMuscle, row.totalReps, Int::plus)
            volumeByMuscle.merge(exercise.primaryMuscle, row.totalVolumeKg, Float::plus)
        }

        val now = Clock.System.now()
        val entities = setsByMuscle.map { (muscle, sets) ->
            val landmarks = VolumeBucketing.landmarksFor(muscle)
            MuscleGroupVolumeWeeklyEntity(
                isoWeek = isoWeek,
                muscleGroup = muscle,
                totalSets = sets,
                totalReps = repsByMuscle[muscle] ?: 0,
                totalVolumeKg = volumeByMuscle[muscle] ?: 0f,
                mev = landmarks.mev,
                mav = landmarks.mav,
                mrv = landmarks.mrv,
                computedAt = now,
            )
        }
        workoutDao.upsertMuscleGroupVolumeAll(entities)

        return setsByMuscle.map { (muscle, sets) -> VolumeBucketing.verdictFor(muscle, sets) }
            .sortedByDescending { it.currentSets }
    }

    /**
     * Historique du volume hebdomadaire, semaine par semaine.
     *
     * C'est ce qui justifie de persister l'agrégat plutôt que de toujours le
     * recalculer : les séries des semaines passées restent lisibles même si
     * l'exercice a changé de muscle principal depuis, puisque les repères sont
     * figés au moment du calcul.
     */
    fun observeWeeklyVolumeHistory(weeks: Int = 8): Flow<List<MuscleGroupVolumeWeeklyEntity>> {
        val since = IsoWeek.of(todayLocal().minus(DatePeriod(days = weeks * 7)))
        return workoutDao.observeWeeklyVolumeSince(since)
    }

    /** Volume déjà calculé pour une semaine donnée, sans recalcul. */
    suspend fun weeklyVolumeFor(week: LocalDate = todayLocal()): List<MuscleGroupVolumeWeeklyEntity> =
        workoutDao.getWeeklyVolume(IsoWeek.of(week))

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // ─────── Tonnage ───────

    fun observeMonthlyTonnage(months: Int = 12): Flow<List<MonthlyTonnageRow>> =
        workoutDao.observeMonthlyTonnage(months).map { it.reversed() }

    fun observeMonthlyTonnageForExercise(exerciseId: Long, months: Int = 12): Flow<List<MonthlyTonnageRow>> =
        workoutDao.observeMonthlyTonnageForExercise(exerciseId, months).map { it.reversed() }
}
