package com.example.gymtracker.domain

import kotlin.math.roundToInt

object Formulas {
    fun calculate1RM(weight: Float, reps: Int): Float {
        if (reps <= 0 || weight <= 0f) return 0f
        if (reps == 1) return weight
        return (weight * (1f + reps / 30f) * 10f).roundToInt() / 10f
    }

    fun calculateStreak(workoutDatesEpochDays: List<Long>): Int {
        if (workoutDatesEpochDays.isEmpty()) return 0
        val sortedDays = workoutDatesEpochDays.distinct().sortedDescending()
        var streak = 0
        var currentExpected = sortedDays.first()

        for (day in sortedDays) {
            if (day == currentExpected || day == currentExpected - 1) {
                streak++
                currentExpected = day
            } else {
                break
            }
        }
        return streak
    }
}
