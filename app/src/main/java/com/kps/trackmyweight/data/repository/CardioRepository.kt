package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import com.kps.trackmyweight.data.db.entity.CardioBlockEntity
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.enums.CardioSource
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.domain.calc.MetCalories
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Description d'un bloc cardio en entrée (pour l'UI).
 */
/** Totaux agrégés d'une séance multi-blocs. */
private data class CardioTotals(
    val durationSec: Int,
    val distanceM: Float?,
    val avgSpeedKmh: Float?,
    val avgRpe: Float?,
    val kcal: Float,
)

data class CardioBlockDraft(
    val type: CardioType,
    val durationSec: Int,
    val distanceM: Float? = null,
    val avgRpe: Float? = null,
    val notes: String? = null,
)

@Singleton
class CardioRepository @Inject constructor(
    private val db: TrackMyWeightDatabase,
    private val workoutDao: WorkoutDao,
) {
    fun observeRecent(limit: Int = 30): Flow<List<CardioSessionEntity>> = workoutDao.observeRecentCardio(limit)

    suspend fun blocksFor(sessionId: Long): List<CardioBlockEntity> = workoutDao.getBlocksFor(sessionId)

    suspend fun get(sessionId: Long): CardioSessionEntity? = workoutDao.getCardioSession(sessionId)

    /**
     * Corrige une séance cardio déjà enregistrée.
     *
     * Les blocs sont réécrits et les totaux entièrement recalculés : modifier la
     * durée sans recalculer les calories laisserait une séance incohérente,
     * pire qu'une saisie erronée puisqu'elle aurait l'air corrigée.
     *
     * Une séance rattachée à une muscu comme échauffement garde son lien : seul
     * son contenu change.
     */
    suspend fun update(
        sessionId: Long,
        blocks: List<CardioBlockDraft>,
        bodyWeightKg: Float,
    ) {
        require(blocks.isNotEmpty()) { "At least one block required" }
        val existing = workoutDao.getCardioSession(sessionId) ?: return
        val totals = aggregate(blocks, bodyWeightKg)

        db.withTransaction {
            workoutDao.updateCardio(
                existing.copy(
                    type = blocks.first().type,
                    durationSec = totals.durationSec,
                    distanceM = totals.distanceM,
                    avgSpeedKmh = totals.avgSpeedKmh,
                    avgRpe = totals.avgRpe,
                    caloriesEstimated = totals.kcal,
                )
            )
            workoutDao.clearCardioBlocks(sessionId)
            blocks.forEachIndexed { index, block ->
                workoutDao.insertCardioBlock(
                    CardioBlockEntity(
                        sessionId = sessionId,
                        orderIndex = index,
                        type = block.type,
                        durationSec = block.durationSec,
                        distanceM = block.distanceM,
                        avgRpe = block.avgRpe,
                        caloriesEstimated = MetCalories.estimate(
                            block.type, block.durationSec, bodyWeightKg, block.avgRpe,
                        ).toFloat(),
                        notes = block.notes,
                    )
                )
            }
        }
    }

    suspend fun delete(sessionId: Long) = workoutDao.deleteCardio(sessionId)

    /** Totaux d'une séance multi-blocs. Extrait pour être partagé entre création et correction. */
    private fun aggregate(blocks: List<CardioBlockDraft>, bodyWeightKg: Float): CardioTotals {
        val durationSec = blocks.sumOf { it.durationSec }
        val kcal = blocks.sumOf {
            MetCalories.estimate(it.type, it.durationSec, bodyWeightKg, it.avgRpe)
        }.toFloat()
        val distanceM = blocks.mapNotNull { it.distanceM }.takeIf { it.isNotEmpty() }?.sum()
        val avgRpe = blocks.mapNotNull { it.avgRpe }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val avgSpeedKmh = distanceM?.takeIf { it > 0f && durationSec > 0 }
            ?.let { d -> (d / 1000f) / (durationSec / 3600f) }
        return CardioTotals(durationSec, distanceM, avgSpeedKmh, avgRpe, kcal)
    }

    /**
     * Enregistre une séance cardio à bloc unique (compat). Calcule les kcal via MET.
     */
    suspend fun log(
        type: CardioType,
        durationSec: Int,
        bodyWeightKg: Float,
        distanceM: Float? = null,
        avgRpe: Float? = null,
        notes: String? = null,
    ): Long = logMultiBlock(
        blocks = listOf(CardioBlockDraft(type, durationSec, distanceM, avgRpe, notes)),
        bodyWeightKg = bodyWeightKg,
    )

    /**
     * Enregistre une séance cardio multi-blocs.
     * Le champ `type` de CardioSessionEntity prend le type du premier bloc (compat).
     * `durationSec` et `caloriesEstimated` sont les totaux agrégés.
     */
    suspend fun logMultiBlock(
        blocks: List<CardioBlockDraft>,
        bodyWeightKg: Float,
    ): Long {
        require(blocks.isNotEmpty()) { "At least one block required" }
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val totalDuration = blocks.sumOf { it.durationSec }
        val totalKcal = blocks.sumOf {
            MetCalories.estimate(it.type, it.durationSec, bodyWeightKg, it.avgRpe)
        }
        val totalDistance = blocks.mapNotNull { it.distanceM }.takeIf { it.isNotEmpty() }?.sum()
        val avgRpe = blocks.mapNotNull { it.avgRpe }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val avgSpeed = totalDistance?.takeIf { it > 0f && totalDuration > 0 }
            ?.let { d -> (d / 1000f) / (totalDuration / 3600f) }

        return db.withTransaction {
            val sessionId = workoutDao.insertCardio(
                CardioSessionEntity(
                    date = date,
                    startedAt = now,
                    endedAt = now,
                    type = blocks.first().type,
                    durationSec = totalDuration,
                    distanceM = totalDistance,
                    avgSpeedKmh = avgSpeed,
                    avgRpe = avgRpe,
                    caloriesEstimated = totalKcal.toFloat(),
                    source = CardioSource.MANUAL,
                    notes = null,
                    createdAt = now,
                )
            )
            blocks.forEachIndexed { i, b ->
                val kcal = MetCalories.estimate(b.type, b.durationSec, bodyWeightKg, b.avgRpe)
                workoutDao.insertCardioBlock(
                    CardioBlockEntity(
                        sessionId = sessionId,
                        orderIndex = i,
                        type = b.type,
                        durationSec = b.durationSec,
                        distanceM = b.distanceM,
                        avgRpe = b.avgRpe,
                        caloriesEstimated = kcal.toFloat(),
                        notes = b.notes,
                    )
                )
            }
            sessionId
        }
    }
}
