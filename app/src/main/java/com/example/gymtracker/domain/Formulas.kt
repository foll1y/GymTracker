package com.example.gymtracker.domain

import com.example.gymtracker.data.local.entity.SetEntryEntity
import com.example.gymtracker.data.model.SetType
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object Formulas {
    private const val KG_TO_LBS = 2.20462f

    fun calculate1RM(weight: Float, reps: Int): Float {
        if (reps <= 0 || weight <= 0f) return 0f
        if (reps == 1) return weight
        return (weight * (1f + reps / 30f) * 10f).roundToInt() / 10f
    }

    /**
     * Считает актуальную серию тренировок.
     * Учитывает стандартные дни отдыха между тренировками (до 3 дней перерыва включительно,
     * например при графике Пн-Ср-Пт), и проверяет, что последняя тренировка была не более 4 дней назад.
     */
    fun calculateStreak(
        workoutDatesEpochDays: List<Long>,
        todayEpochDay: Long = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
    ): Int {
        if (workoutDatesEpochDays.isEmpty()) return 0
        val sortedDays = workoutDatesEpochDays.distinct().sortedDescending()
        val latestDay = sortedDays.first()

        // Если последняя тренировка была больше 4 дней назад, текущая серия прервалась
        if (todayEpochDay - latestDay > 4L) return 0

        var streak = 1
        var previousWorkoutDay = latestDay

        for (i in 1 until sortedDays.size) {
            val day = sortedDays[i]
            val gapDays = previousWorkoutDay - day
            if (gapDays in 1L..4L) {
                streak++
                previousWorkoutDay = day
            } else if (gapDays > 4L) {
                break
            }
        }
        return streak
    }

    /**
     * Считает рабочий тоннаж (в кг) без учёта разминочных подходов.
     */
    fun calculateWorkingTonnage(sets: Iterable<SetEntryEntity>): Float {
        var sum = 0f
        for (s in sets) {
            if (s.setType != SetType.WARMUP && s.weightKg > 0f && s.reps > 0 && s.isCompleted) {
                sum += s.weightKg * s.reps
            }
        }
        return (sum * 10f).roundToInt() / 10f
    }

    fun convertWeight(weightKg: Float, isKg: Boolean): Float {
        if (weightKg <= 0f) return 0f
        val raw = if (isKg) weightKg else weightKg * KG_TO_LBS
        return (raw * 10f).roundToInt() / 10f
    }

    fun toKg(inputWeight: Float, isKg: Boolean): Float {
        if (inputWeight <= 0f) return 0f
        val raw = if (isKg) inputWeight else inputWeight / KG_TO_LBS
        return (raw * 10f).roundToInt() / 10f
    }

    fun unitLabel(isKg: Boolean): String = if (isKg) "кг" else "lbs"

    fun formatNumber(value: Float): String {
        val rounded = (value * 10f).roundToInt() / 10f
        return if (rounded % 1f == 0f) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    fun formatWeight(weightKg: Float, isKg: Boolean = true): String {
        val converted = convertWeight(weightKg, isKg)
        return "${formatNumber(converted)} ${unitLabel(isKg)}"
    }
}
