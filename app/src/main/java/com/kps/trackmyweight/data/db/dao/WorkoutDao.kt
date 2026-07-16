package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.MuscleGroupVolumeWeeklyEntity
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRotationMembers(members: List<TemplateRotationMemberEntity>)

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

    @Query("SELECT * FROM workout_session WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getSession(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_session WHERE date = :date AND deletedAt IS NULL")
    suspend fun getSessionsOnDate(date: LocalDate): List<WorkoutSessionEntity>

    @Insert
    suspend fun insertPerformedExercise(pe: PerformedExerciseEntity): Long

    @Query("SELECT * FROM performed_exercise WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getPerformedExercises(sessionId: Long): List<PerformedExerciseEntity>

    @Insert
    suspend fun insertPerformedSet(set: PerformedSetEntity): Long

    @Update
    suspend fun updatePerformedSet(set: PerformedSetEntity)

    @Query("SELECT * FROM performed_set WHERE performedExerciseId = :peId ORDER BY setNumber")
    suspend fun getSetsFor(peId: Long): List<PerformedSetEntity>

    /**
     * Renvoie la dernière séance loguée pour un exercice donné (pour auto-fill).
     */
    @Query("""
        SELECT ps.* FROM performed_set ps
        INNER JOIN performed_exercise pe ON pe.id = ps.performedExerciseId
        INNER JOIN workout_session ws ON ws.id = pe.sessionId
        WHERE pe.exerciseId = :exerciseId AND ws.deletedAt IS NULL
        ORDER BY ws.date DESC, ps.setNumber ASC
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

    @Query("SELECT * FROM cardio_session ORDER BY date DESC LIMIT :limit")
    fun observeRecentCardio(limit: Int = 30): Flow<List<CardioSessionEntity>>

    @Query("SELECT * FROM cardio_session WHERE date >= :from AND date <= :to ORDER BY date")
    suspend fun getCardioInRange(from: LocalDate, to: LocalDate): List<CardioSessionEntity>

    // ── Pain ──────────────────────────────────────────────
    @Insert
    suspend fun insertPainLog(log: PainLogEntity): Long

    @Query("SELECT * FROM pain_log ORDER BY date DESC LIMIT :limit")
    fun observeRecentPain(limit: Int = 30): Flow<List<PainLogEntity>>

    // ── Delete cascade helpers ────────────────────────────
    @Transaction
    @Query("UPDATE workout_session SET deletedAt = :now WHERE id = :id")
    suspend fun softDeleteSession(id: Long, now: kotlinx.datetime.Instant)
}
