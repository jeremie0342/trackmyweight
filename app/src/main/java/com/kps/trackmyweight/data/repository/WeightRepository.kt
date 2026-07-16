package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.BodyDao
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.db.enums.WeightSource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val bodyDao: BodyDao,
) {
    fun observeRecent(limit: Int = 180): Flow<List<WeightEntryEntity>> =
        bodyDao.observeRecentWeights(limit)

    fun observeLast(): Flow<WeightEntryEntity?> = bodyDao.observeLastWeight()

    suspend fun getOnDate(date: LocalDate): WeightEntryEntity? = bodyDao.getWeightOnDate(date)

    /**
     * Enregistre (ou remplace) la pesée du jour. Retourne l'id de la ligne.
     */
    suspend fun log(
        date: LocalDate,
        weightKg: Float,
        source: WeightSource = WeightSource.MANUAL,
        note: String? = null,
    ): Long {
        val now = Clock.System.now()
        return bodyDao.upsertWeight(
            WeightEntryEntity(
                date = date,
                weightKg = weightKg,
                source = source,
                recordedAt = now,
                note = note,
                createdAt = now,
            )
        )
    }

    suspend fun softDelete(id: Long) {
        bodyDao.softDeleteWeight(id, Clock.System.now())
    }
}
