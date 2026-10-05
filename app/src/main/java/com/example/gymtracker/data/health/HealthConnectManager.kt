package com.example.gymtracker.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class WorkoutHealthData(
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val activeCalories: Int? = null
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val c = client ?: return false
            val granted = c.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchWorkoutHealthData(startEpochMillis: Long, endEpochMillis: Long): WorkoutHealthData {
        val c = client ?: return WorkoutHealthData()
        if (!hasAllPermissions()) return WorkoutHealthData()

        val startInstant = Instant.ofEpochMilli(startEpochMillis)
        val endInstant = Instant.ofEpochMilli(endEpochMillis)

        var avgHr: Int? = null
        var maxHr: Int? = null
        var calories: Int? = null

        try {
            // 1. Поиск официальной тренировки от OHealth
            val sessionRequest = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startInstant.minusSeconds(600),
                    endInstant.plusSeconds(300)
                )
            )
            val sessionResponse = c.readRecords(sessionRequest)
            val matchingSession = sessionResponse.records.firstOrNull()

            val effectiveStart = matchingSession?.startTime ?: startInstant
            val effectiveEnd = matchingSession?.endTime ?: endInstant

            // 2. Чтение пульса за период тренировки
            val hrRequest = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(effectiveStart, effectiveEnd)
            )
            val hrResponse = c.readRecords(hrRequest)
            val allSamples = hrResponse.records.flatMap { it.samples }

            if (allSamples.isNotEmpty()) {
                val bpmList = allSamples.map { it.beatsPerMinute }
                avgHr = bpmList.average().toInt()
                maxHr = bpmList.maxOrNull()?.toInt()
            }

            // 3. Чтение сожжённых калорий
            val calRequest = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(effectiveStart, effectiveEnd)
            )
            val calResponse = c.readRecords(calRequest)
            val totalCal = calResponse.records.sumOf { it.energy.inKilocalories }
            if (totalCal > 0) {
                calories = totalCal.toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return WorkoutHealthData(avgHr, maxHr, calories)
    }
}
