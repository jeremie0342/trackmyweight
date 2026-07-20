package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import com.kps.trackmyweight.data.db.entity.CardioBlockEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.TemplateExerciseEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.db.enums.CardioSource
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.PrKind
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.domain.calc.MetCalories
import com.kps.trackmyweight.domain.calc.OneRepMax
import com.kps.trackmyweight.domain.calc.PrDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class TemplateWithExercises(
    val template: WorkoutTemplateEntity,
    val exercises: List<TemplateExerciseWithMeta>,
)

data class TemplateExerciseWithMeta(
    val templateExercise: TemplateExerciseEntity,
    val exerciseName: String,
)

@Singleton
class WorkoutRepository @Inject constructor(
    private val db: TrackMyWeightDatabase,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
) {
    fun observeTemplates(): Flow<List<WorkoutTemplateEntity>> = workoutDao.observeTemplates()
    fun observeRecentSessions(limit: Int = 30) = workoutDao.observeRecentSessions(limit)
    fun observeRecentPrs(limit: Int = 20) = workoutDao.observeRecentPrs(limit)

    suspend fun getTemplate(id: Long): TemplateWithExercises? {
        val t = workoutDao.getTemplate(id) ?: return null
        val list = workoutDao.getTemplateExercises(id).map { te ->
            TemplateExerciseWithMeta(te, exerciseDao.getById(te.exerciseId)?.name ?: "?")
        }
        return TemplateWithExercises(t, list)
    }

    suspend fun saveTemplate(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>,
    ): Long = db.withTransaction {
        val templateId = workoutDao.upsertTemplate(template)
        workoutDao.clearTemplateExercises(templateId)
        val prepared = exercises.mapIndexed { i, te -> te.copy(templateId = templateId, orderIndex = i, id = 0) }
        if (prepared.isNotEmpty()) workoutDao.insertTemplateExercises(prepared)
        templateId
    }

    // ─────── Session lifecycle ───────

    /**
     * Démarre une nouvelle séance. Renvoie l'id de session créée.
     */
    suspend fun startSession(templateId: Long?, gymId: Long?): Long {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val template = templateId?.let { workoutDao.getTemplate(it) }
        return workoutDao.insertSession(
            WorkoutSessionEntity(
                date = date,
                startedAt = now,
                endedAt = null,
                templateId = templateId,
                gymId = gymId,
                totalVolumeKg = 0f,
                totalCalories = 0f,
                isCoachProgram = false,
            )
        ).also { sessionId ->
            // Pré-remplir les exercices du template
            if (template != null && templateId != null) {
                val teList = workoutDao.getTemplateExercises(templateId)
                teList.forEach { te ->
                    val exName = exerciseDao.getById(te.exerciseId)?.name ?: "?"
                    workoutDao.insertPerformedExercise(
                        PerformedExerciseEntity(
                            sessionId = sessionId,
                            exerciseId = te.exerciseId,
                            exerciseNameSnapshot = exName,
                            orderIndex = te.orderIndex,
                        )
                    )
                }
            }
        }
    }

    suspend fun endSession(sessionId: Long, sessionRpe: Float?, notes: String?) {
        val session = workoutDao.getSession(sessionId) ?: return
        val performed = workoutDao.getPerformedExercises(sessionId)
        var totalVolume = 0f
        performed.forEach { pe ->
            workoutDao.getSetsFor(pe.id).forEach { s ->
                if (s.type == SetType.WORKING || s.type == SetType.BACKOFF || s.type == SetType.FAILURE) {
                    totalVolume += s.weightKg * s.reps
                }
            }
        }
        workoutDao.updateSession(
            session.copy(
                endedAt = Clock.System.now(),
                sessionRpe = sessionRpe,
                notes = notes,
                totalVolumeKg = totalVolume,
            )
        )
    }

    suspend fun getOrCreatePerformedExercise(sessionId: Long, exerciseId: Long, order: Int): PerformedExerciseEntity {
        val existing = workoutDao.getPerformedExercises(sessionId).firstOrNull { it.exerciseId == exerciseId }
        if (existing != null) return existing
        val exName = exerciseDao.getById(exerciseId)?.name ?: "?"
        val id = workoutDao.insertPerformedExercise(
            PerformedExerciseEntity(
                sessionId = sessionId, exerciseId = exerciseId,
                exerciseNameSnapshot = exName, orderIndex = order,
            )
        )
        return workoutDao.getPerformedExercises(sessionId).first { it.id == id }
    }

    /**
     * Enregistre une série et détecte les PRs éventuels.
     */
    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        performedExerciseId: Long,
        setNumber: Int,
        weightKg: Float,
        reps: Int,
        rpe: Float?,
        type: SetType = SetType.WORKING,
        restBeforeSec: Int?,
    ): PerformedSetEntity = db.withTransaction {
        val now = Clock.System.now()

        // PR detection : on regarde les meilleurs actuels
        val currentMaxWeight = workoutDao.getCurrentPr(exerciseId, PrKind.MAX_WEIGHT_ANY_REPS)?.value
        val currentOneRm = workoutDao.getCurrentPr(exerciseId, PrKind.ONE_RM_EST)?.value

        val setId = workoutDao.insertPerformedSet(
            PerformedSetEntity(
                performedExerciseId = performedExerciseId,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                type = type,
                restBeforeSec = restBeforeSec,
                isPrCandidate = false,
                createdAt = now,
            )
        )

        val prs = PrDetector.detect(
            newWeightKg = weightKg, newReps = reps,
            currentMaxWeight = currentMaxWeight,
            currentOneRm = currentOneRm,
            currentMaxRepsAtWeight = null,
        )
        prs.forEach { pr ->
            workoutDao.insertPr(
                PersonalRecordEntity(
                    exerciseId = exerciseId,
                    kind = pr.kind,
                    value = pr.value,
                    referenceValue = pr.referenceValue,
                    achievedAt = now,
                    sessionId = sessionId,
                    setId = setId,
                )
            )
        }

        workoutDao.getSetsFor(performedExerciseId).first { it.id == setId }
    }

    /**
     * Auto-fill : dernière série effectuée pour cet exercice, quel que soit la séance.
     */
    suspend fun lastSetForExercise(exerciseId: Long): PerformedSetEntity? =
        workoutDao.getLastSetsForExercise(exerciseId, limit = 1).firstOrNull()

    suspend fun setsForPerformedExercise(peId: Long): List<PerformedSetEntity> =
        workoutDao.getSetsFor(peId)

    suspend fun performedExercisesForSession(sessionId: Long): List<PerformedExerciseEntity> =
        workoutDao.getPerformedExercises(sessionId)

    suspend fun getActiveOrLatestSession(): WorkoutSessionEntity? =
        workoutDao.observeRecentSessions(1).first().firstOrNull()

    suspend fun getSession(id: Long): WorkoutSessionEntity? = workoutDao.getSession(id)

    /**
     * Log un échauffement cardio lié à la séance de muscu et met à jour
     * `warmupCardioSessionId`. Si un warmup existait déjà, l'ancienne CardioSession
     * est laissée telle quelle (garde l'historique) et remplacée par la nouvelle.
     */
    suspend fun logWarmupCardio(
        sessionId: Long,
        type: CardioType,
        durationMin: Int,
        bodyWeightKg: Float,
        rpe: Float? = null,
    ): Long = db.withTransaction {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val kcal = MetCalories.estimate(type, durationMin * 60, bodyWeightKg, rpe)
        val cardioId = workoutDao.insertCardio(
            CardioSessionEntity(
                date = date,
                startedAt = now,
                endedAt = now,
                type = type,
                durationSec = durationMin * 60,
                avgRpe = rpe,
                caloriesEstimated = kcal.toFloat(),
                source = CardioSource.MANUAL,
                notes = "Échauffement séance #$sessionId",
                createdAt = now,
            )
        )
        workoutDao.insertCardioBlock(
            CardioBlockEntity(
                sessionId = cardioId,
                orderIndex = 0,
                type = type,
                durationSec = durationMin * 60,
                avgRpe = rpe,
                caloriesEstimated = kcal.toFloat(),
            )
        )
        workoutDao.setWarmupCardio(sessionId, cardioId)
        cardioId
    }

    suspend fun getCardio(id: Long): CardioSessionEntity? = workoutDao.getCardioSession(id)

    // ─────── Rotation resolution ───────

    /** Résout le prochain template à faire pour un groupe de rotation. */
    suspend fun nextTemplateInRotation(groupId: Long): WorkoutTemplateEntity? {
        val id = workoutDao.nextTemplateInRotation(groupId) ?: return null
        return workoutDao.getTemplate(id)
    }

    // ─────── PR helpers ───────
    fun oneRmFor(weightKg: Float, reps: Int): Float = OneRepMax.average(weightKg, reps)

    // ─────── Export texte pour coach ───────
    suspend fun formatSessionForCoach(sessionId: Long): String {
        val session = workoutDao.getSession(sessionId) ?: return ""
        val performed = workoutDao.getPerformedExercises(sessionId)
        val sb = StringBuilder()
        sb.appendLine("Séance du ${session.date}")
        session.sessionRpe?.let { sb.appendLine("RPE global : $it") }
        session.notes?.takeIf { it.isNotBlank() }?.let { sb.appendLine("Notes : $it") }
        sb.appendLine()
        performed.forEach { pe ->
            sb.appendLine("• ${pe.exerciseNameSnapshot}")
            workoutDao.getSetsFor(pe.id).forEach { s ->
                sb.appendLine("    S${s.setNumber} : ${"%.1f".format(s.weightKg)} kg × ${s.reps}${s.rpe?.let { " @RPE$it" }.orEmpty()}")
            }
        }
        return sb.toString()
    }

    private suspend fun formatTs(t: Instant): String = t.toString()

    private suspend fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
