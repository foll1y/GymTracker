package com.example.gymtracker.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.RawHistoryEntry
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.SetType
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

data class ExerciseStat(
    val exerciseId: Long,
    val exerciseName: String,
    val maxWeightKg: Float,
    val repsAtMaxWeight: Int,
    val oneRepMaxKg: Float,
    val totalSets: Int
)

data class MuscleGroupStat(
    val group: MuscleGroup,
    val maxOneRepMaxKg: Float,
    val maxWeightKg: Float,
    val bestExerciseName: String,
    val totalSets: Int,
    val share: Float,
    val exercises: List<ExerciseStat> = emptyList()
)

data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.MONTH,
    val totalWorkouts: Int = 0,
    val maxWorkingWeightKg: Float = 0f,
    val maxOneRepMaxKg: Float = 0f,
    val maxOneRepMaxExercise: String = "",
    val totalSets: Int = 0,
    val averageDurationMinutes: Int = 0,
    val currentStreak: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val avgHeartRate: Int = 0,
    val muscleStats: List<MuscleGroupStat> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    private val _period = MutableStateFlow(StatsPeriod.MONTH)
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    fun getExerciseHistory(exerciseId: Long): Flow<List<RawHistoryEntry>> = workoutDao.getExerciseHistory(exerciseId)

    suspend fun getExercise(id: Long): ExerciseEntity? = exerciseDao.getExerciseById(id)

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

        var totalSets = 0
        var overallMaxWeight = 0f
        var overallMax1RM = 0f
        var overallBestExercise = ""

        val muscleSetsCount = mutableMapOf<MuscleGroup, Int>()

        class ExerciseAgg(
            val id: Long,
            val name: String,
            val group: MuscleGroup,
            var maxWeight: Float = 0f,
            var repsAtMax: Int = 0,
            var max1RM: Float = 0f,
            var setsCount: Int = 0
        )

        val exerciseMap = mutableMapOf<Long, ExerciseAgg>()

        filtered.forEach { w ->
            w.exercises.forEach { ex ->
                val group = ex.exercise.muscleGroup
                val exAgg = exerciseMap.getOrPut(ex.exercise.id) {
                    ExerciseAgg(ex.exercise.id, ex.exercise.name, group)
                }

                ex.sets.forEach { s ->
                    val isDone = s.isCompleted || s.reps > 0 || s.weightKg > 0f
                    if (isDone && (s.reps > 0 || s.weightKg > 0f)) {
                        totalSets++
                        muscleSetsCount[group] = (muscleSetsCount[group] ?: 0) + 1
                        exAgg.setsCount++

                        // Исключаем разминочные сеты из рекордов и 1ПМ
                        if (s.setType != SetType.WARMUP) {
                            val oneRm = if (s.weightKg > 0f && s.reps > 0) {
                                Formulas.calculate1RM(s.weightKg, s.reps)
                            } else if (s.weightKg > 0f) {
                                s.weightKg
                            } else 0f

                            if (oneRm > exAgg.max1RM || (oneRm == exAgg.max1RM && s.weightKg > exAgg.maxWeight)) {
                                exAgg.max1RM = oneRm
                                exAgg.maxWeight = s.weightKg
                                exAgg.repsAtMax = s.reps
                            }

                            if (s.weightKg > overallMaxWeight) {
                                overallMaxWeight = s.weightKg
                            }
                            if (oneRm > overallMax1RM) {
                                overallMax1RM = oneRm
                                overallBestExercise = ex.exercise.name
                            }
                        }
                    }
                }
            }
        }

        val totalMuscleSets = muscleSetsCount.values.sum().toFloat().coerceAtLeast(1f)
        val muscleStatsList = MuscleGroup.values().map { group ->
            val groupExercises = exerciseMap.values
                .filter { it.group == group && it.setsCount > 0 }
                .map {
                    ExerciseStat(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        maxWeightKg = it.maxWeight,
                        repsAtMaxWeight = it.repsAtMax,
                        oneRepMaxKg = it.max1RM,
                        totalSets = it.setsCount
                    )
                }
                .sortedByDescending { it.oneRepMaxKg }

            val sets = muscleSetsCount[group] ?: 0
            val max1RM = groupExercises.firstOrNull()?.oneRepMaxKg ?: 0f
            val maxWeight = groupExercises.maxOfOrNull { it.maxWeightKg } ?: 0f
            val bestExName = groupExercises.firstOrNull()?.exerciseName ?: ""
            val share = sets / totalMuscleSets

            MuscleGroupStat(
                group = group,
                maxOneRepMaxKg = max1RM,
                maxWeightKg = maxWeight,
                bestExerciseName = bestExName,
                totalSets = sets,
                share = share,
                exercises = groupExercises
            )
        }.sortedWith(compareByDescending<MuscleGroupStat> { it.totalSets }.thenBy { it.group.ordinal })

        val days = workouts.map { TimeUnit.MILLISECONDS.toDays(it.workout.dateEpochMillis) }
        val streak = Formulas.calculateStreak(days)

        val totalCal = filtered.mapNotNull { it.workout.activeCalories }.sum()
        val hrs = filtered.mapNotNull { it.workout.avgHeartRate }
        val avgHr = if (hrs.isNotEmpty()) hrs.average().toInt() else 0

        StatsUiState(
            selectedPeriod = period,
            totalWorkouts = filtered.size,
            maxWorkingWeightKg = overallMaxWeight,
            maxOneRepMaxKg = overallMax1RM,
            maxOneRepMaxExercise = overallBestExercise,
            totalSets = totalSets,
            averageDurationMinutes = if (filtered.isNotEmpty()) filtered.sumOf { it.workout.durationMinutes } / filtered.size else 0,
            currentStreak = streak,
            totalCaloriesBurned = totalCal,
            avgHeartRate = avgHr,
            muscleStats = muscleStatsList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun selectPeriod(newPeriod: StatsPeriod) {
        _period.value = newPeriod
    }
}
