package com.example.gymtracker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.domain.Formulas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class StatsPeriod(val title: String) {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
    ALL("Всё")
}

data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.MONTH,
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalSets: Int = 0,
    val averageDurationMinutes: Int = 0,
    val currentStreak: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val avgHeartRate: Int = 0,
    val muscleDistribution: Map<MuscleGroup, Float> = emptyMap()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _period = MutableStateFlow(StatsPeriod.MONTH)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    val statsState: StateFlow<StatsUiState> = combine(workoutDao.getAllWorkouts(), _period) { workouts, period ->
        val now = System.currentTimeMillis()
        val filtered = workouts.filter { w ->
            when (period) {
                StatsPeriod.DAY -> now - w.workout.dateEpochMillis <= TimeUnit.DAYS.toMillis(1)
                StatsPeriod.WEEK -> now - w.workout.dateEpochMillis <= TimeUnit.DAYS.toMillis(7)
                StatsPeriod.MONTH -> now - w.workout.dateEpochMillis <= TimeUnit.DAYS.toMillis(30)
                StatsPeriod.YEAR -> now - w.workout.dateEpochMillis <= TimeUnit.DAYS.toMillis(365)
                StatsPeriod.ALL -> true
            }
        }

        var totalVol = 0.0
        var totalSets = 0
        val muscleSetCount = mutableMapOf<MuscleGroup, Int>()

        filtered.forEach { w ->
            w.exercises.forEach { ex ->
                val group = ex.exercise.muscleGroup
                ex.sets.forEach { s ->
                    if (s.isCompleted) {
                        totalVol += (s.weightKg * s.reps)
                        totalSets++
                        muscleSetCount[group] = (muscleSetCount[group] ?: 0) + 1
                    }
                }
            }
        }

        val totalMuscleSets = muscleSetCount.values.sum().toFloat().coerceAtLeast(1f)
        val distribution = muscleSetCount.mapValues { it.value / totalMuscleSets }

        val days = workouts.map { TimeUnit.MILLISECONDS.toDays(it.workout.dateEpochMillis) }
        val streak = Formulas.calculateStreak(days)

        val totalCal = filtered.mapNotNull { it.workout.activeCalories }.sum()
        val hrs = filtered.mapNotNull { it.workout.avgHeartRate }
        val avgHr = if (hrs.isNotEmpty()) hrs.average().toInt() else 0

        StatsUiState(
            selectedPeriod = period,
            totalWorkouts = filtered.size,
            totalVolumeKg = totalVol,
            totalSets = totalSets,
            averageDurationMinutes = if (filtered.isNotEmpty()) filtered.sumOf { it.workout.durationMinutes } / filtered.size else 0,
            currentStreak = streak,
            totalCaloriesBurned = totalCal,
            avgHeartRate = avgHr,
            muscleDistribution = distribution
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun selectPeriod(newPeriod: StatsPeriod) {
        _period.value = newPeriod
    }
}
