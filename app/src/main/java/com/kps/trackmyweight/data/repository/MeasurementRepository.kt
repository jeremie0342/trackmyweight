package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.BodyDao
import com.kps.trackmyweight.data.db.entity.BodyCompositionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.BodyMeasurementSessionEntity
import com.kps.trackmyweight.data.db.enums.BodyFatMethod
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.domain.calc.BodyFatCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository @Inject constructor(
    private val bodyDao: BodyDao,
) {
    fun observeAll(): Flow<List<BodyMeasurementSessionEntity>> = bodyDao.observeMeasurements()
    fun observeLast(): Flow<BodyMeasurementSessionEntity?> = bodyDao.observeLastMeasurement()
    fun observeLastComposition() = bodyDao.observeLastBodyComposition()
    fun observeCompositionHistory() = bodyDao.observeBodyCompositionHistory()

    suspend fun getOnDate(date: LocalDate): BodyMeasurementSessionEntity? = bodyDao.getMeasurementOnDate(date)

    /**
     * Enregistre une session et met à jour la composition corporelle si
     * suffisamment de mesures + poids + profil sont disponibles.
     */
    suspend fun save(
        date: LocalDate,
        neckCm: Float? = null,
        shoulderCm: Float? = null,
        chestCm: Float? = null,
        waistCm: Float? = null,
        hipCm: Float? = null,
        armLeftCm: Float? = null,
        armRightCm: Float? = null,
        forearmLeftCm: Float? = null,
        forearmRightCm: Float? = null,
        thighLeftCm: Float? = null,
        thighRightCm: Float? = null,
        calfLeftCm: Float? = null,
        calfRightCm: Float? = null,
        wristCm: Float? = null,
        notes: String? = null,
        // pour composition
        sex: Sex? = null,
        heightCm: Float? = null,
        weightKg: Float? = null,
    ): Long {
        val now = Clock.System.now()
        val existing = bodyDao.getMeasurementOnDate(date)
        val sessionId = bodyDao.upsertMeasurement(
            BodyMeasurementSessionEntity(
                id = existing?.id ?: 0,
                date = date,
                neckCm = neckCm,
                shoulderCm = shoulderCm,
                chestCm = chestCm,
                waistCm = waistCm,
                hipCm = hipCm,
                armLeftCm = armLeftCm,
                armRightCm = armRightCm,
                forearmLeftCm = forearmLeftCm,
                forearmRightCm = forearmRightCm,
                thighLeftCm = thighLeftCm,
                thighRightCm = thighRightCm,
                calfLeftCm = calfLeftCm,
                calfRightCm = calfRightCm,
                wristCm = wristCm,
                notes = notes,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )

        if (sex != null && heightCm != null && weightKg != null) {
            val comp = BodyFatCalculator.compose(sex, heightCm, weightKg, neckCm, waistCm, hipCm)
            if (comp != null) {
                bodyDao.insertBodyComposition(
                    BodyCompositionSnapshotEntity(
                        date = date,
                        bodyFatPct = comp.bodyFatPct,
                        method = BodyFatMethod.NAVY,
                        leanMassKg = comp.leanMassKg,
                        fatMassKg = comp.fatMassKg,
                        sourceMeasurementSessionId = sessionId,
                        computedAt = now,
                    )
                )
            }
        }
        return sessionId
    }
}
