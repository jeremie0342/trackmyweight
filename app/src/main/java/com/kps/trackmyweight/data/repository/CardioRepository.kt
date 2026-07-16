package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.WorkoutDao
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

@Singleton
class CardioRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
) {
    fun observeRecent(limit: Int = 30): Flow<List<CardioSessionEntity>> = workoutDao.observeRecentCardio(limit)

    /**
     * Enregistre une séance cardio, calcule automatiquement les calories via MET.
     */
    suspend fun log(
        type: CardioType,
        durationSec: Int,
        bodyWeightKg: Float,
        distanceM: Float? = null,
        avgRpe: Float? = null,
        notes: String? = null,
    ): Long {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val kcal = MetCalories.estimate(type, durationSec, bodyWeightKg, avgRpe)
        val avgSpeed = distanceM?.takeIf { it > 0f && durationSec > 0 }
            ?.let { d -> (d / 1000f) / (durationSec / 3600f) }
        return workoutDao.insertCardio(
            CardioSessionEntity(
                date = date,
                startedAt = now,
                endedAt = now,
                type = type,
                durationSec = durationSec,
                distanceM = distanceM,
                avgSpeedKmh = avgSpeed,
                avgRpe = avgRpe,
                caloriesEstimated = kcal.toFloat(),
                source = CardioSource.MANUAL,
                notes = notes,
                createdAt = now,
            )
        )
    }
}
