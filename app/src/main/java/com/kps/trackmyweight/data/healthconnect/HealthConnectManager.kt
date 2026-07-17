package com.kps.trackmyweight.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper Health Connect : détection dispo, permissions, lecture des 3 signaux principaux
 * (poids, pas, sommeil). L'écriture n'est pas nécessaire : Health Connect n'est utilisé qu'en source.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** Permissions que l'app demande pour la lecture. */
    val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    suspend fun grantedPermissions(): Set<String> {
        val c = client ?: return emptySet()
        return c.permissionController.getGrantedPermissions()
    }

    suspend fun hasAllPermissions(): Boolean =
        grantedPermissions().containsAll(readPermissions)

    // ─────── Reads ───────

    data class WeightSample(val timestamp: Instant, val kg: Float)
    data class StepsAggregate(val fromInstant: Instant, val toInstant: Instant, val count: Int)
    data class SleepSample(val start: Instant, val end: Instant)

    suspend fun readWeights(from: Instant, to: Instant): List<WeightSample> {
        val c = client ?: return emptyList()
        return runCatching {
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from.toJavaInstant(), to.toJavaInstant()),
                )
            )
            response.records.map { r ->
                WeightSample(
                    timestamp = Instant.fromEpochMilliseconds(r.time.toEpochMilli()),
                    kg = r.weight.inKilograms.toFloat(),
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun readSteps(from: Instant, to: Instant): List<StepsAggregate> {
        val c = client ?: return emptyList()
        return runCatching {
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from.toJavaInstant(), to.toJavaInstant()),
                )
            )
            response.records.map { r ->
                StepsAggregate(
                    fromInstant = Instant.fromEpochMilliseconds(r.startTime.toEpochMilli()),
                    toInstant = Instant.fromEpochMilliseconds(r.endTime.toEpochMilli()),
                    count = r.count.toInt(),
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun readSleep(from: Instant, to: Instant): List<SleepSample> {
        val c = client ?: return emptyList()
        return runCatching {
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from.toJavaInstant(), to.toJavaInstant()),
                )
            )
            response.records.map { r ->
                SleepSample(
                    start = Instant.fromEpochMilliseconds(r.startTime.toEpochMilli()),
                    end = Instant.fromEpochMilliseconds(r.endTime.toEpochMilli()),
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val LOOKBACK_DAYS = 30L
    }
}
