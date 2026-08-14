package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.ExerciseMaxLoadEntity
import com.kps.trackmyweight.data.db.entity.ExerciseSetCountRow
import com.kps.trackmyweight.data.db.entity.MonthlyTonnageRow
import com.kps.trackmyweight.data.db.entity.MuscleGroupVolumeWeeklyEntity
import com.kps.trackmyweight.data.db.entity.PainContextRow
import com.kps.trackmyweight.data.db.entity.PainHotspotRow
import com.kps.trackmyweight.data.db.entity.PainLogEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.ProgramDayEntity
import com.kps.trackmyweight.data.db.entity.ProgramEntity
import com.kps.trackmyweight.data.db.entity.TemplateExerciseEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationMemberEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.data.db.enums.PrKind
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface WorkoutDao {

    // ── Templates ─────────────────────────────────────────
    @Upsert
    suspend fun upsertTemplate(t: WorkoutTemplateEntity): Long

    @Query("SELECT * FROM workout_template WHERE isArchived = 0 ORDER BY name")
    fun observeTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_template WHERE id = :id LIMIT 1")
    suspend fun getTemplate(id: Long): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(items: List<TemplateExerciseEntity>)

    @Query("SELECT * FROM template_exercise WHERE templateId = :templateId ORDER BY orderIndex")
    suspend fun getTemplateExercises(templateId: Long): List<TemplateExerciseEntity>

    @Query("DELETE FROM template_exercise WHERE templateId = :templateId")
    suspend fun clearTemplateExercises(templateId: Long)

    // ── Rotations ─────────────────────────────────────────
    @Insert
    suspend fun insertRotationGroup(g: TemplateRotationGroupEntity): Long

    @Query("SELECT * FROM template_rotation_group ORDER BY dayOfWeek")
    fun observeRotationGroups(): Flow<List<TemplateRotationGroupEntity>>

    @Update
    suspend fun updateRotationGroup(g: TemplateRotationGroupEntity)

    @Query("DELETE FROM template_rotation_group WHERE id = :id")
    suspend fun deleteRotationGroup(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRotationMembers(members: List<TemplateRotationMemberEntity>)

    @Query("SELECT * FROM template_rotation_member WHERE rotationGroupId = :groupId ORDER BY orderInRotation")
    suspend fun getRotationMembers(groupId: Long): List<TemplateRotationMemberEntity>

    @Query("SELECT * FROM template_rotation_member ORDER BY rotationGroupId, orderInRotation")
    fun observeAllRotationMembers(): Flow<List<TemplateRotationMemberEntity>>

    @Query("DELETE FROM template_rotation_member WHERE rotationGroupId = :groupId")
    suspend fun clearRotationMembers(groupId: Long)

    /**
     * Renvoie l'id du template à faire aujourd'hui pour un groupe de rotation,
     * en s'appuyant sur la dernière séance liée à un de ses membres.
     */
    @Query("""
        WITH members AS (
            SELECT templateId, orderInRotation
            FROM template_rotation_member
            WHERE rotationGroupId = :groupId
            ORDER BY orderInRotation
        ),
        last_done AS (
            SELECT ws.templateId AS lastTemplateId
            FROM workout_session ws
            WHERE ws.deletedAt IS NULL
            AND ws.templateId IN (SELECT templateId FROM members)
            ORDER BY ws.date DESC
            LIMIT 1
        )
        SELECT templateId FROM members
        WHERE orderInRotation = (
            COALESCE(
                (SELECT (m.orderInRotation) % (SELECT COUNT(*) FROM members) + 1
                 FROM members m
                 JOIN last_done ON m.templateId = last_done.lastTemplateId
                 LIMIT 1),
                (SELECT MIN(orderInRotation) FROM members)
            )
        )
        LIMIT 1
    """)
    suspend fun nextTemplateInRotation(groupId: Long): Long?

    // ── Programmes ────────────────────────────────────────
    @Insert
    suspend fun insertProgram(p: ProgramEntity): Long

    @Update
    suspend fun updateProgram(p: ProgramEntity)

    @Query("SELECT * FROM program WHERE isActive = 1 LIMIT 1")
    fun observeActiveProgram(): Flow<ProgramEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProgramDays(days: List<ProgramDayEntity>)

    @Query("SELECT * FROM program_day WHERE programId = :programId ORDER BY dayOfWeek")
    suspend fun getProgramDays(programId: Long): List<ProgramDayEntity>

    // ── Sessions ──────────────────────────────────────────
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_session WHERE deletedAt IS NULL ORDER BY date DESC, startedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 50): Flow<List<WorkoutSessionEntity>>

    /**
     * Historique : uniquement les séances effectivement terminées.
     *
     * Une séance sans `endedAt` est encore en cours et n'a pas sa place dans
     * l'historique — son volume n'est pas consolidé et l'utilisateur peut encore
     * la reprendre. Voir [observeActiveSession].
     */
    @Query("""
        SELECT * FROM workout_session
        WHERE deletedAt IS NULL AND endedAt IS NOT NULL
        ORDER BY date DESC, startedAt DESC
        LIMIT :limit
    """)
    fun observeFinishedSessions(limit: Int = 50): Flow<List<WorkoutSessionEntity>>

    /**
     * La séance en cours, s'il y en a une.
     *
     * Le cycle de vie se lit entièrement sur deux colonnes, sans champ de statut :
     *  - en cours  : `endedAt IS NULL AND deletedAt IS NULL`
     *  - terminée  : `endedAt IS NOT NULL AND deletedAt IS NULL`
     *  - abandonnée: `deletedAt IS NOT NULL`
     */
    @Query("""
        SELECT * FROM workout_session
        WHERE deletedAt IS NULL AND endedAt IS NULL
        ORDER BY startedAt DESC
        LIMIT 1
    """)
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("""
        SELECT * FROM workout_session
        WHERE deletedAt IS NULL AND endedAt IS NULL
        ORDER BY startedAt DESC
    """)
    suspend fun getOpenSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_session WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("UPDATE workout_session SET totalVolumeKg = :volume WHERE id = :id")
    suspend fun setSessionVolume(id: Long, volume: Float)

    @Query("UPDATE workout_session SET warmupCardioSessionId = :cardioId WHERE id = :sessionId")
    suspend fun setWarmupCardio(sessionId: Long, cardioId: Long?)

    @Query("SELECT * FROM workout_session WHERE date = :date AND deletedAt IS NULL")
    suspend fun getSessionsOnDate(date: LocalDate): List<WorkoutSessionEntity>

    @Insert
    suspend fun insertPerformedExercise(pe: PerformedExerciseEntity): Long

    @Query("SELECT * FROM performed_exercise WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getPerformedExercises(sessionId: Long): List<PerformedExerciseEntity>

    @Query("SELECT * FROM performed_exercise WHERE id = :id LIMIT 1")
    suspend fun getPerformedExercise(id: Long): PerformedExerciseEntity?

    @Query("DELETE FROM performed_exercise WHERE id = :id")
    suspend fun deletePerformedExercise(id: Long)

    @Query("UPDATE performed_exercise SET orderIndex = :order WHERE id = :id")
    suspend fun setPerformedExerciseOrder(id: Long, order: Int)

    @Query("UPDATE performed_exercise SET supersetGroup = :group WHERE id = :id")
    suspend fun setPerformedExerciseSuperset(id: Long, group: Int?)

    @Insert
    suspend fun insertPerformedSet(set: PerformedSetEntity): Long

    @Update
    suspend fun updatePerformedSet(set: PerformedSetEntity)

    @Query("DELETE FROM performed_set WHERE id = :id")
    suspend fun deletePerformedSet(id: Long)

    @Query("SELECT * FROM performed_set WHERE id = :id LIMIT 1")
    suspend fun getSet(id: Long): PerformedSetEntity?

    @Query("SELECT * FROM performed_set WHERE performedExerciseId = :peId ORDER BY setNumber")
    suspend fun getSetsFor(peId: Long): List<PerformedSetEntity>

    /** Renumérote les séries d'un exercice après une suppression, pour éviter les trous. */
    @Query("""
        UPDATE performed_set
        SET setNumber = (
            SELECT COUNT(*) FROM performed_set inner_set
            WHERE inner_set.performedExerciseId = performed_set.performedExerciseId
              AND inner_set.id <= performed_set.id
        )
        WHERE performedExerciseId = :peId
    """)
    suspend fun renumberSets(peId: Long)

    /** Nombre de séries loguées dans une séance, tous exercices confondus. */
    @Query("""
        SELECT COUNT(*) FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        WHERE pe.sessionId = :sessionId
    """)
    suspend fun countSetsInSession(sessionId: Long): Int

    /** Instant de la dernière série loguée dans une séance, ou null si aucune. */
    @Query("""
        SELECT MAX(ps.createdAt) FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        WHERE pe.sessionId = :sessionId
    """)
    suspend fun lastSetInstantInSession(sessionId: Long): kotlinx.datetime.Instant?

    /**
     * Meilleur nombre de reps déjà réalisé à ce poids exact pour cet exercice.
     * Alimente [com.kps.trackmyweight.domain.calc.PrDetector] : sans cette valeur,
     * le PR « max de reps à un poids donné » ne peut jamais se déclencher.
     */
    @Query("""
        SELECT MAX(ps.reps) FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        INNER JOIN workout_session ws ON ws.id = pe.sessionId
        WHERE pe.exerciseId = :exerciseId
          AND ws.deletedAt IS NULL
          AND ps.type <> 'WARMUP'
          AND ps.weightKg = :weightKg
          AND ps.id <> :excludeSetId
    """)
    suspend fun maxRepsAtWeight(exerciseId: Long, weightKg: Float, excludeSetId: Long = -1L): Int?

    /** Volume total (poids × reps) des séries comptabilisées d'une séance. */
    @Query("""
        SELECT COALESCE(SUM(ps.weightKg * ps.reps), 0) FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        WHERE pe.sessionId = :sessionId
          AND ps.type IN ('WORKING', 'BACKOFF', 'FAILURE', 'AMRAP', 'DROP')
    """)
    suspend fun computeSessionVolume(sessionId: Long): Float

    /**
     * Dernières séries de travail loguées pour un exercice, de la plus récente
     * à la plus ancienne. Alimente le pré-remplissage « dernière séance ».
     *
     * L'ordre était `ws.date DESC, ps.setNumber ASC`, ce qui posait deux
     * problèmes : à l'intérieur d'une séance il renvoyait la PREMIÈRE série au
     * lieu de la dernière, et deux séances le même jour se départageaient de
     * façon indéterminée. `ps.createdAt DESC` est sans ambiguïté.
     *
     * L'échauffement est exclu : pré-remplir avec la barre à vide n'aide pas.
     */
    @Query("""
        SELECT ps.* FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        INNER JOIN workout_session ws ON ws.id = pe.sessionId
        WHERE pe.exerciseId = :exerciseId
          AND ws.deletedAt IS NULL
          AND ps.type <> 'WARMUP'
        ORDER BY ps.createdAt DESC
        LIMIT :limit
    """)
    suspend fun getLastSetsForExercise(exerciseId: Long, limit: Int = 10): List<PerformedSetEntity>

    // ── Personal records ──────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPr(pr: PersonalRecordEntity): Long

    @Query("SELECT * FROM personal_record WHERE exerciseId = :exerciseId AND kind = :kind ORDER BY achievedAt DESC LIMIT 1")
    suspend fun getCurrentPr(exerciseId: Long, kind: PrKind): PersonalRecordEntity?

    @Query("SELECT * FROM personal_record ORDER BY achievedAt DESC LIMIT :limit")
    fun observeRecentPrs(limit: Int = 20): Flow<List<PersonalRecordEntity>>

    // ── Charge maximale ───────────────────────────────────
    @Insert
    suspend fun insertMaxLoad(entry: ExerciseMaxLoadEntity): Long

    @Query("DELETE FROM exercise_max_load WHERE id = :id")
    suspend fun deleteMaxLoad(id: Long)

    /** Historique complet d'un exercice, du plus ancien au plus récent (pour la courbe). */
    @Query("SELECT * FROM exercise_max_load WHERE exerciseId = :exerciseId ORDER BY measuredAt ASC")
    fun observeMaxLoadHistory(exerciseId: Long): Flow<List<ExerciseMaxLoadEntity>>

    /** Dernière valeur connue, quelle que soit sa provenance. */
    @Query("SELECT * FROM exercise_max_load WHERE exerciseId = :exerciseId ORDER BY measuredAt DESC LIMIT 1")
    suspend fun getLatestMaxLoad(exerciseId: Long): ExerciseMaxLoadEntity?

    /**
     * Meilleure valeur mesurée avant une date donnée.
     * Sert à calculer la progression sur une période.
     */
    @Query("""
        SELECT MAX(oneRmKg) FROM exercise_max_load
        WHERE exerciseId = :exerciseId AND measuredAt <= :before
    """)
    suspend fun bestMaxLoadBefore(exerciseId: Long, before: kotlinx.datetime.Instant): Float?

    /** Dernière valeur de chaque exercice ayant au moins une mesure. */
    @Query("""
        SELECT m.* FROM exercise_max_load m
        INNER JOIN (
            SELECT exerciseId, MAX(measuredAt) AS latest
            FROM exercise_max_load
            GROUP BY exerciseId
        ) last ON last.exerciseId = m.exerciseId AND last.latest = m.measuredAt
        GROUP BY m.exerciseId
    """)
    fun observeLatestMaxLoads(): Flow<List<ExerciseMaxLoadEntity>>

    // ── Tonnage ───────────────────────────────────────────
    /**
     * Volume soulevé par mois, séances terminées uniquement.
     *
     * `workout_session.date` est stocké en TEXT ISO (`YYYY-MM-DD`), d'où le
     * `substr` pour regrouper par mois sans convertisseur de date côté SQL.
     */
    @Query("""
        SELECT substr(ws.date, 1, 7) AS month,
               COALESCE(SUM(ps.weightKg * ps.reps), 0) AS volumeKg,
               COUNT(ps.id) AS setCount,
               COUNT(DISTINCT ws.id) AS sessionCount
        FROM workout_session ws
        INNER JOIN performed_exercise pe ON pe.sessionId = ws.id
        INNER JOIN performed_set ps ON ps.performedExerciseId = pe.id
        WHERE ws.deletedAt IS NULL
          AND ws.endedAt IS NOT NULL
          AND ps.type IN ('WORKING', 'BACKOFF', 'FAILURE', 'AMRAP', 'DROP')
        GROUP BY month
        ORDER BY month DESC
        LIMIT :limit
    """)
    fun observeMonthlyTonnage(limit: Int = 12): Flow<List<MonthlyTonnageRow>>

    /** Même agrégation, restreinte à un exercice. */
    @Query("""
        SELECT substr(ws.date, 1, 7) AS month,
               COALESCE(SUM(ps.weightKg * ps.reps), 0) AS volumeKg,
               COUNT(ps.id) AS setCount,
               COUNT(DISTINCT ws.id) AS sessionCount
        FROM workout_session ws
        INNER JOIN performed_exercise pe ON pe.sessionId = ws.id
        INNER JOIN performed_set ps ON ps.performedExerciseId = pe.id
        WHERE ws.deletedAt IS NULL
          AND ws.endedAt IS NOT NULL
          AND pe.exerciseId = :exerciseId
          AND ps.type IN ('WORKING', 'BACKOFF', 'FAILURE', 'AMRAP', 'DROP')
        GROUP BY month
        ORDER BY month DESC
        LIMIT :limit
    """)
    fun observeMonthlyTonnageForExercise(exerciseId: Long, limit: Int = 12): Flow<List<MonthlyTonnageRow>>

    /**
     * Séries travaillées par exercice sur une plage de dates.
     *
     * L'échauffement est exclu : il ne compte pas dans le volume de travail
     * confronté aux repères MEV/MAV/MRV.
     */
    @Query("""
        SELECT pe.exerciseId AS exerciseId,
               COUNT(ps.id) AS totalSets,
               SUM(ps.reps) AS totalReps,
               COALESCE(SUM(ps.weightKg * ps.reps), 0) AS totalVolumeKg
        FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        INNER JOIN workout_session ws ON ws.id = pe.sessionId
        WHERE ws.deletedAt IS NULL
          AND ws.date >= :from AND ws.date <= :to
          AND ps.type <> 'WARMUP'
        GROUP BY pe.exerciseId
    """)
    suspend fun setsPerExerciseBetween(from: LocalDate, to: LocalDate): List<ExerciseSetCountRow>

    // ── Volume weekly ─────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMuscleGroupVolumeAll(items: List<MuscleGroupVolumeWeeklyEntity>)

    @Query("SELECT * FROM muscle_group_volume_weekly WHERE isoWeek = :isoWeek")
    suspend fun getWeeklyVolume(isoWeek: String): List<MuscleGroupVolumeWeeklyEntity>

    @Query("SELECT * FROM muscle_group_volume_weekly WHERE isoWeek >= :fromWeek ORDER BY isoWeek DESC")
    fun observeWeeklyVolumeSince(fromWeek: String): Flow<List<MuscleGroupVolumeWeeklyEntity>>

    // ── Cardio ────────────────────────────────────────────
    @Insert
    suspend fun insertCardio(session: CardioSessionEntity): Long

    @Update
    suspend fun updateCardio(session: CardioSessionEntity)

    @Query("SELECT * FROM cardio_session ORDER BY date DESC LIMIT :limit")
    fun observeRecentCardio(limit: Int = 30): Flow<List<CardioSessionEntity>>

    @Query("SELECT * FROM cardio_session WHERE id = :id LIMIT 1")
    suspend fun getCardioSession(id: Long): CardioSessionEntity?

    @Query("SELECT * FROM cardio_session WHERE date >= :from AND date <= :to ORDER BY date")
    suspend fun getCardioInRange(from: LocalDate, to: LocalDate): List<CardioSessionEntity>

    // ── Cardio blocks ─────────────────────────────────────
    @Insert
    suspend fun insertCardioBlock(block: com.kps.trackmyweight.data.db.entity.CardioBlockEntity): Long

    @Query("SELECT * FROM cardio_block WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getBlocksFor(sessionId: Long): List<com.kps.trackmyweight.data.db.entity.CardioBlockEntity>

    // ── Pain ──────────────────────────────────────────────
    @Insert
    suspend fun insertPainLog(log: PainLogEntity): Long

    @Query("SELECT * FROM pain_log ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecentPain(limit: Int = 30): Flow<List<PainLogEntity>>

    @Query("DELETE FROM pain_log WHERE id = :id")
    suspend fun deletePainLog(id: Long)

    /**
     * Zones les plus signalées sur une période, avec leur intensité moyenne.
     *
     * C'est la vue utile : une douleur isolée n'apprend rien, une zone qui
     * revient dix fois en un mois est un signal.
     */
    @Query("""
        SELECT area,
               COUNT(*) AS occurrences,
               AVG(intensity) AS averageIntensity,
               MAX(intensity) AS peakIntensity,
               MAX(date) AS lastDate
        FROM pain_log
        WHERE date >= :since
        GROUP BY area
        ORDER BY occurrences DESC, averageIntensity DESC
    """)
    fun observePainHotspots(since: LocalDate): Flow<List<PainHotspotRow>>

    /**
     * Exercices le plus souvent associés à une zone donnée.
     * Ne prouve rien à lui seul, mais oriente vers ce qu'il faut regarder.
     */
    @Query("""
        SELECT e.name AS exerciseName, COUNT(*) AS occurrences
        FROM pain_log pl
        INNER JOIN exercise e ON e.id = pl.contextExerciseId
        WHERE pl.area = :area AND pl.date >= :since
        GROUP BY pl.contextExerciseId
        ORDER BY occurrences DESC
        LIMIT :limit
    """)
    suspend fun painContextExercises(area: PainArea, since: LocalDate, limit: Int = 3): List<PainContextRow>

    // ── Delete cascade helpers ────────────────────────────
    @Transaction
    @Query("UPDATE workout_session SET deletedAt = :now WHERE id = :id")
    suspend fun softDeleteSession(id: Long, now: kotlinx.datetime.Instant)
}
