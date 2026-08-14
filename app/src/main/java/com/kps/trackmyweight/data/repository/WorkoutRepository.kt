package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import com.kps.trackmyweight.data.db.entity.CardioBlockEntity
import com.kps.trackmyweight.data.db.entity.ExerciseMaxLoadEntity
import com.kps.trackmyweight.data.db.entity.PainContextRow
import com.kps.trackmyweight.data.db.entity.PainHotspotRow
import com.kps.trackmyweight.data.db.entity.PainLogEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.TemplateExerciseEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationMemberEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.db.enums.CardioSource
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.MaxLoadSource
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.data.db.enums.PrKind
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.domain.calc.MetCalories
import com.kps.trackmyweight.domain.calc.OneRepMax
import com.kps.trackmyweight.domain.calc.PrDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
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

/**
 * Un exercice tel que planifié à l'écran de préparation, avant tout écriture en base.
 *
 * La préparation ne persiste rien : tant que l'utilisateur n'a pas lancé la séance,
 * il n'existe aucune ligne `workout_session`. C'est ce qui évite les séances
 * fantômes créées par un simple appui accidentel sur un template.
 */
data class PlannedExercise(
    val exerciseId: Long,
    val name: String,
    val targetSets: Int? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetRpe: Float? = null,
    val targetWeightKg: Float? = null,
    val restSecOverride: Int? = null,
    /** Exercices partageant ce numéro : enchaînés en superset. */
    val supersetGroup: Int? = null,
)

/** Résultat du ménage effectué au démarrage sur les séances laissées ouvertes. */
data class StaleSessionCleanup(
    val closed: Int = 0,
    val discarded: Int = 0,
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

    /** Historique : séances terminées uniquement. */
    fun observeFinishedSessions(limit: Int = 30) = workoutDao.observeFinishedSessions(limit)

    /** La séance en cours, ou null. Alimente le bandeau de reprise. */
    fun observeActiveSession(): Flow<WorkoutSessionEntity?> = workoutDao.observeActiveSession()

    suspend fun getActiveSession(): WorkoutSessionEntity? = workoutDao.getOpenSessions().firstOrNull()

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
     * Construit le plan par défaut d'un template, à afficher dans l'écran de
     * préparation. Ne touche pas la base.
     */
    suspend fun planFromTemplate(templateId: Long): List<PlannedExercise> =
        workoutDao.getTemplateExercises(templateId).map { te ->
            PlannedExercise(
                exerciseId = te.exerciseId,
                name = exerciseDao.getById(te.exerciseId)?.name ?: "?",
                targetSets = te.targetSets,
                targetRepsMin = te.targetRepsMin,
                targetRepsMax = te.targetRepsMax,
                targetRpe = te.targetRpe,
                targetWeightKg = te.targetWeightKg,
                restSecOverride = te.restSecOverride,
                supersetGroup = te.supersetGroup,
            )
        }

    /**
     * Démarre une séance à partir d'un plan validé par l'utilisateur.
     * Renvoie l'id de la séance créée.
     *
     * C'est le **seul** point qui crée une `workout_session`. Tant que
     * l'utilisateur n'a pas confirmé, rien n'est écrit.
     */
    suspend fun startSession(
        templateId: Long?,
        gymId: Long?,
        plan: List<PlannedExercise>,
    ): Long = db.withTransaction {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val sessionId = workoutDao.insertSession(
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
        )
        plan.forEachIndexed { index, planned ->
            workoutDao.insertPerformedExercise(
                PerformedExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = planned.exerciseId,
                    exerciseNameSnapshot = planned.name,
                    orderIndex = index,
                    targetSets = planned.targetSets,
                    targetRepsMin = planned.targetRepsMin,
                    targetRepsMax = planned.targetRepsMax,
                    targetRpe = planned.targetRpe,
                    targetWeightKg = planned.targetWeightKg,
                    restSecOverride = planned.restSecOverride,
                    supersetGroup = planned.supersetGroup,
                )
            )
        }
        sessionId
    }

    /** Clôture la séance : elle bascule dans l'historique. */
    suspend fun endSession(sessionId: Long, sessionRpe: Float?, notes: String?) {
        val session = workoutDao.getSession(sessionId) ?: return
        workoutDao.updateSession(
            session.copy(
                endedAt = Clock.System.now(),
                sessionRpe = sessionRpe,
                notes = notes,
                totalVolumeKg = workoutDao.computeSessionVolume(sessionId),
            )
        )
    }

    /**
     * Abandonne la séance : soft delete. Elle disparaît de l'historique et du
     * bandeau de reprise, mais les lignes restent en base — rien n'est perdu
     * définitivement, et les PR déjà détectés gardent une session de référence
     * valide.
     */
    suspend fun abandonSession(sessionId: Long) {
        workoutDao.softDeleteSession(sessionId, Clock.System.now())
    }

    /**
     * Ménage au démarrage des séances restées ouvertes.
     *
     * Avant la refonte du cycle de vie, quitter l'écran de séance laissait la
     * ligne ouverte indéfiniment : un utilisateur existant peut donc en avoir
     * accumulé plusieurs. On ne touche qu'aux séances ouvertes depuis plus de
     * [staleAfterHours] :
     *  - sans aucune série loguée → abandonnées (elles n'ont rien à raconter) ;
     *  - avec des séries → clôturées à l'instant de la dernière série, ce qui
     *    les fait entrer dans l'historique avec leur volume réel.
     *
     * La séance ouverte la plus récente est toujours préservée si elle est
     * récente : c'est celle que l'utilisateur peut légitimement vouloir reprendre.
     */
    suspend fun closeStaleSessions(staleAfterHours: Int = 12): StaleSessionCleanup {
        val now = Clock.System.now()
        val cutoff = now.minus(staleAfterHours.hours)
        var closed = 0
        var discarded = 0
        workoutDao.getOpenSessions().forEach { session ->
            if (session.startedAt >= cutoff) return@forEach
            val setCount = workoutDao.countSetsInSession(session.id)
            if (setCount == 0) {
                workoutDao.softDeleteSession(session.id, now)
                discarded++
            } else {
                val endedAt = workoutDao.lastSetInstantInSession(session.id) ?: session.startedAt
                workoutDao.updateSession(
                    session.copy(
                        endedAt = endedAt,
                        totalVolumeKg = workoutDao.computeSessionVolume(session.id),
                    )
                )
                closed++
            }
        }
        return StaleSessionCleanup(closed = closed, discarded = discarded)
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
     *
     * Trois correctifs par rapport à la version initiale :
     *  - les séries d'échauffement ne peuvent plus déclencher de record ;
     *  - `currentMaxRepsAtWeight` est réellement calculé — il était passé à `null`,
     *    ce qui rendait le PR « max de reps à un poids donné » inatteignable ;
     *  - `isPrCandidate` est renseigné, ce qui fait enfin apparaître le badge PR
     *    dans l'écran de séance.
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

        val prs = if (type == SetType.WARMUP) emptyList() else PrDetector.detect(
            newWeightKg = weightKg,
            newReps = reps,
            currentMaxWeight = workoutDao.getCurrentPr(exerciseId, PrKind.MAX_WEIGHT_ANY_REPS)?.value,
            currentOneRm = workoutDao.getCurrentPr(exerciseId, PrKind.ONE_RM_EST)?.value,
            currentMaxRepsAtWeight = workoutDao.maxRepsAtWeight(exerciseId, weightKg),
        )

        val setId = workoutDao.insertPerformedSet(
            PerformedSetEntity(
                performedExerciseId = performedExerciseId,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                type = type,
                restBeforeSec = restBeforeSec,
                isPrCandidate = prs.isNotEmpty(),
                createdAt = now,
            )
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
            // Un nouveau 1RM estimé alimente aussi l'historique de charge maximale :
            // la courbe de progression se construit toute seule, sans que
            // l'utilisateur ait à tester son max ni à saisir quoi que ce soit.
            if (pr.kind == PrKind.ONE_RM_EST) {
                workoutDao.insertMaxLoad(
                    ExerciseMaxLoadEntity(
                        exerciseId = exerciseId,
                        oneRmKg = pr.value,
                        source = MaxLoadSource.ESTIMATED,
                        sourceWeightKg = weightKg,
                        sourceReps = reps,
                        measuredAt = now,
                    )
                )
            }
        }

        workoutDao.setSessionVolume(sessionId, workoutDao.computeSessionVolume(sessionId))
        workoutDao.getSetsFor(performedExerciseId).first { it.id == setId }
    }

    /**
     * Corrige une série déjà enregistrée.
     *
     * On ne rejoue pas la détection de PR : un record déjà attribué reste acquis,
     * et re-détecter à partir d'une valeur corrigée produirait des doublons. La
     * correction d'une faute de frappe reste possible, ce qui était impossible
     * jusqu'ici — `updatePerformedSet` existait dans le DAO sans aucun appelant.
     */
    suspend fun updateSet(
        setId: Long,
        weightKg: Float,
        reps: Int,
        rpe: Float?,
        type: SetType,
    ) {
        db.withTransaction {
            val existing = workoutDao.getSet(setId) ?: return@withTransaction
            workoutDao.updatePerformedSet(
                existing.copy(weightKg = weightKg, reps = reps, rpe = rpe, type = type)
            )
            refreshVolumeFor(existing.performedExerciseId)
        }
    }

    /** Supprime une série et renumérote les suivantes. */
    suspend fun deleteSet(setId: Long) {
        db.withTransaction {
            val existing = workoutDao.getSet(setId) ?: return@withTransaction
            workoutDao.deletePerformedSet(setId)
            workoutDao.renumberSets(existing.performedExerciseId)
            refreshVolumeFor(existing.performedExerciseId)
        }
    }

    /** Retire un exercice de la séance. Ses séries partent avec (CASCADE). */
    suspend fun removeExerciseFromSession(performedExerciseId: Long) {
        db.withTransaction {
            val sessionId = sessionIdForPerformedExercise(performedExerciseId)
            workoutDao.deletePerformedExercise(performedExerciseId)
            if (sessionId != null) {
                reindexAndNormalize(sessionId)
                workoutDao.setSessionVolume(sessionId, workoutDao.computeSessionVolume(sessionId))
            }
        }
    }

    /**
     * Déplace un exercice dans la séance. [delta] vaut -1 pour monter, +1 pour descendre.
     *
     * Réordonner n'était possible qu'à la préparation : une fois en salle, un
     * ordre décidé au départ devenait figé, alors que c'est précisément là qu'on
     * s'adapte (machine occupée, fatigue).
     */
    suspend fun moveExerciseInSession(performedExerciseId: Long, delta: Int) {
        db.withTransaction {
            val sessionId = sessionIdForPerformedExercise(performedExerciseId) ?: return@withTransaction
            val ordered = workoutDao.getPerformedExercises(sessionId).toMutableList()
            val from = ordered.indexOfFirst { it.id == performedExerciseId }
            if (from < 0) return@withTransaction
            val to = (from + delta).coerceIn(0, ordered.lastIndex)
            if (to == from) return@withTransaction
            ordered.add(to, ordered.removeAt(from))
            ordered.forEachIndexed { index, pe -> workoutDao.setPerformedExerciseOrder(pe.id, index) }
            reindexAndNormalize(sessionId)
        }
    }

    /**
     * Recompacte les `orderIndex` et dissout les supersets devenus incohérents.
     *
     * Un superset n'a de sens que sur des exercices consécutifs : déplacer un
     * membre hors du groupe, ou en retirer un jusqu'à n'en laisser qu'un seul,
     * doit dissoudre le groupe plutôt que d'afficher « Superset A » sur un
     * exercice isolé — et surtout plutôt que de supprimer le repos entre deux
     * exercices qui ne s'enchaînent plus.
     */
    private suspend fun reindexAndNormalize(sessionId: Long) {
        val ordered = workoutDao.getPerformedExercises(sessionId)
        ordered.forEachIndexed { index, pe ->
            if (pe.orderIndex != index) workoutDao.setPerformedExerciseOrder(pe.id, index)
        }

        // Un groupe n'est valide que sur une plage contiguë. On repère les
        // ruptures et on ne conserve que les tronçons d'au moins deux exercices.
        val keep = mutableSetOf<Long>()
        var runStart = 0
        while (runStart < ordered.size) {
            val group = ordered[runStart].supersetGroup
            var runEnd = runStart
            while (runEnd + 1 < ordered.size && ordered[runEnd + 1].supersetGroup == group) runEnd++
            if (group != null && runEnd > runStart) {
                (runStart..runEnd).forEach { keep += ordered[it].id }
            }
            runStart = runEnd + 1
        }
        ordered.forEach { pe ->
            if (pe.supersetGroup != null && pe.id !in keep) {
                workoutDao.setPerformedExerciseSuperset(pe.id, null)
            }
        }
    }

    /**
     * Recalcule le volume de la séance à laquelle appartient cet exercice.
     * À appeler après toute modification de série : le volume était auparavant
     * calculé uniquement à la clôture, laissant une séance en cours à 0 kg.
     */
    private suspend fun refreshVolumeFor(performedExerciseId: Long) {
        val sessionId = sessionIdForPerformedExercise(performedExerciseId) ?: return
        workoutDao.setSessionVolume(sessionId, workoutDao.computeSessionVolume(sessionId))
    }

    private suspend fun sessionIdForPerformedExercise(performedExerciseId: Long): Long? =
        workoutDao.getPerformedExercise(performedExerciseId)?.sessionId

    /**
     * Auto-fill : dernière série effectuée pour cet exercice, quel que soit la séance.
     */
    suspend fun lastSetForExercise(exerciseId: Long): PerformedSetEntity? =
        workoutDao.getLastSetsForExercise(exerciseId, limit = 1).firstOrNull()

    suspend fun setsForPerformedExercise(peId: Long): List<PerformedSetEntity> =
        workoutDao.getSetsFor(peId)

    suspend fun performedExercisesForSession(sessionId: Long): List<PerformedExerciseEntity> =
        workoutDao.getPerformedExercises(sessionId)

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

    fun observeRotationGroups(): Flow<List<TemplateRotationGroupEntity>> =
        workoutDao.observeRotationGroups()

    fun observeAllRotationMembers(): Flow<List<TemplateRotationMemberEntity>> =
        workoutDao.observeAllRotationMembers()

    /**
     * Crée ou met à jour un groupe de rotation et ses membres ordonnés.
     *
     * Les membres sont réécrits intégralement : c'est plus simple et plus sûr
     * que de calculer un diff, et le volume est dérisoire (quelques lignes).
     */
    suspend fun saveRotation(
        groupId: Long?,
        name: String,
        dayOfWeek: Int,
        templateIdsInOrder: List<Long>,
    ): Long = db.withTransaction {
        val id = if (groupId == null) {
            workoutDao.insertRotationGroup(TemplateRotationGroupEntity(name = name, dayOfWeek = dayOfWeek))
        } else {
            workoutDao.updateRotationGroup(
                TemplateRotationGroupEntity(id = groupId, name = name, dayOfWeek = dayOfWeek)
            )
            groupId
        }
        workoutDao.clearRotationMembers(id)
        if (templateIdsInOrder.isNotEmpty()) {
            workoutDao.setRotationMembers(
                templateIdsInOrder.mapIndexed { index, templateId ->
                    TemplateRotationMemberEntity(
                        rotationGroupId = id,
                        templateId = templateId,
                        orderInRotation = index + 1,
                    )
                }
            )
        }
        id
    }

    suspend fun getRotationMembers(groupId: Long): List<Long> =
        workoutDao.getRotationMembers(groupId).map { it.templateId }

    suspend fun deleteRotation(groupId: Long) = workoutDao.deleteRotationGroup(groupId)

    /**
     * Template suggéré pour aujourd'hui : celui du groupe de rotation calé sur
     * le jour courant, positionné après la dernière séance faite dans ce groupe.
     *
     * `nextTemplateInRotation` existait dans le DAO et le repository sans qu'aucun
     * écran ne l'appelle : la rotation annoncée n'était jamais résolue.
     */
    suspend fun todaysRotationSuggestion(): WorkoutTemplateEntity? {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isoDay = today.dayOfWeek.isoDayNumber
        val group = workoutDao.observeRotationGroups().first().firstOrNull { it.dayOfWeek == isoDay }
            ?: return null
        return nextTemplateInRotation(group.id)
    }

    // ─────── Douleurs ───────

    fun observeRecentPain(limit: Int = 50): Flow<List<PainLogEntity>> =
        workoutDao.observeRecentPain(limit)

    /** Zones récurrentes sur les [days] derniers jours. */
    fun observePainHotspots(days: Int = 90): Flow<List<PainHotspotRow>> =
        workoutDao.observePainHotspots(today().minus(DatePeriod(days = days)))

    /**
     * Enregistre une douleur.
     *
     * [contextExerciseId] est renseigné quand le signalement part d'un exercice
     * en séance : c'est ce qui permet ensuite de rapprocher une zone
     * douloureuse des mouvements qui la sollicitent.
     */
    suspend fun logPain(
        area: PainArea,
        intensity: Int,
        contextExerciseId: Long? = null,
        notes: String? = null,
        date: LocalDate = today(),
    ): Long = workoutDao.insertPainLog(
        PainLogEntity(
            date = date,
            area = area,
            intensity = intensity.coerceIn(0, 10),
            contextExerciseId = contextExerciseId,
            notes = notes?.takeIf { it.isNotBlank() },
            createdAt = Clock.System.now(),
        )
    )

    suspend fun deletePain(id: Long) = workoutDao.deletePainLog(id)

    /** Exercices le plus souvent associés à une zone, sur les [days] derniers jours. */
    suspend fun painContextFor(area: PainArea, days: Int = 90): List<PainContextRow> =
        workoutDao.painContextExercises(area, today().minus(DatePeriod(days = days)))

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

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
