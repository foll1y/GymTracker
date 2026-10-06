package com.example.gymtracker.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.health.HealthConnectManager
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.ProgramDao
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.SetType
import com.example.gymtracker.data.online.OnlineExerciseCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class EditableSet(
    val id: Long = 0,
    val weight: String = "",
    val reps: String = "",
    val isCompleted: Boolean = false,
    val setType: SetType = SetType.NORMAL,
    val previousWeight: Float? = null,
    val previousReps: Int? = null,
    val historicalMaxWeight: Float? = null
)

data class EditableExercise(
    val exercise: ExerciseEntity,
    val sets: List<EditableSet> = emptyList(),
    val allTimeMaxWeight: Float = 0f,
    val isWeighted: Boolean = false,
    val supersetLabel: String? = null
)

data class ActiveWorkoutUiState(
    val startTimeEpochMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val exercises: List<EditableExercise> = emptyList(),
    val programDayTitle: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val programDao: ProgramDao,
    private val healthConnectManager: HealthConnectManager,
    private val onlineCatalog: OnlineExerciseCatalog
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private val isSavingInProgress = AtomicBoolean(false)

    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun searchCatalog(query: String, filterGroup: MuscleGroup?): List<ExerciseEntity> {
        return onlineCatalog.search(query, filterGroup)
    }

    fun addOnlineExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val id = exerciseDao.insertExercise(exercise.copy(id = 0, isCustom = false))
            addExercise(exercise.copy(id = id))
        }
    }

    fun initWorkout(dayId: Long?) {
        if (dayId != null && dayId > 0 && _uiState.value.exercises.isEmpty()) {
            loadWorkoutFromProgramDay(dayId)
        }
    }

    private fun loadWorkoutFromProgramDay(dayId: Long) {
        viewModelScope.launch {
            val dayWithExercises = programDao.getDayWithExercisesById(dayId) ?: return@launch
            val editableExercisesList = mutableListOf<EditableExercise>()

            dayWithExercises.exercises.forEach { planEx ->
                val ex = planEx.exercise
                val lastSets = workoutDao.getLastSetsForExercise(ex.id)
                val history = workoutDao.getExerciseHistory(ex.id).first()
                val maxHistoricalWeight = history.maxOfOrNull { it.weightKg } ?: 0f

                val targetCount = planEx.planExercise.targetSets.coerceIn(1, 10)
                val defaultWeightStr = if (planEx.planExercise.targetWeightKg != null && planEx.planExercise.targetWeightKg > 0f) {
                    planEx.planExercise.targetWeightKg.toString()
                } else if (lastSets.isNotEmpty() && lastSets.first().weightKg > 0f) {
                    lastSets.first().weightKg.toString()
                } else ""

                val defaultRepsStr = if (planEx.planExercise.targetReps.isNotBlank()) {
                    val firstNum = Regex("\\d+").find(planEx.planExercise.targetReps)?.value ?: "10"
                    firstNum
                } else if (lastSets.isNotEmpty() && lastSets.first().reps > 0) {
                    lastSets.first().reps.toString()
                } else "10"

                val isWeightedDefault = ex.exerciseType == ExerciseType.WEIGHTED_BODYWEIGHT && defaultWeightStr.isNotBlank()

                val setsList = (0 until targetCount).map { sIdx ->
                    val last = lastSets.getOrNull(sIdx) ?: lastSets.firstOrNull()
                    EditableSet(
                        weight = defaultWeightStr,
                        reps = defaultRepsStr,
                        isCompleted = false,
                        previousWeight = last?.weightKg,
                        previousReps = last?.reps,
                        historicalMaxWeight = maxHistoricalWeight
                    )
                }

                editableExercisesList.add(
                    EditableExercise(
                        exercise = ex,
                        sets = setsList,
                        allTimeMaxWeight = maxHistoricalWeight,
                        isWeighted = isWeightedDefault,
                        supersetLabel = planEx.planExercise.supersetLabel
                    )
                )
            }

            _uiState.update {
                it.copy(
                    exercises = editableExercisesList,
                    programDayTitle = dayWithExercises.day.name
                )
            }
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val lastSets = workoutDao.getLastSetsForExercise(exercise.id)
            val history = workoutDao.getExerciseHistory(exercise.id).first()
            val maxHistoricalWeight = history.maxOfOrNull { it.weightKg } ?: 0f

            val initialSet = if (lastSets.isNotEmpty()) {
                val last = lastSets.first()
                EditableSet(
                    weight = if (last.weightKg > 0) last.weightKg.toString() else "",
                    reps = if (last.reps > 0) last.reps.toString() else "",
                    isCompleted = false,
                    previousWeight = last.weightKg,
                    previousReps = last.reps,
                    historicalMaxWeight = maxHistoricalWeight
                )
            } else {
                EditableSet(
                    weight = "",
                    reps = "",
                    historicalMaxWeight = maxHistoricalWeight
                )
            }

            val currentList = _uiState.value.exercises.toMutableList()
            currentList.add(
                EditableExercise(
                    exercise = exercise,
                    sets = listOf(initialSet),
                    allTimeMaxWeight = maxHistoricalWeight,
                    isWeighted = exercise.exerciseType == ExerciseType.WEIGHTED_BODYWEIGHT && (initialSet.previousWeight ?: 0f) > 0f
                )
            )
            _uiState.update { it.copy(exercises = currentList) }
        }
    }

    fun updateSetType(exerciseIndex: Int, setIndex: Int, setType: SetType) {
        val updated = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) s.copy(setType = setType) else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updated) }
    }

    fun setSuperset(exerciseIndex1: Int, exerciseIndex2: Int) {
        val exercises = _uiState.value.exercises
        if (exerciseIndex1 !in exercises.indices || exerciseIndex2 !in exercises.indices || exerciseIndex1 == exerciseIndex2) return
        val existingLabels = exercises.mapNotNull { it.supersetLabel }.distinct()
        val nextLabel = ('A'..'Z').firstOrNull { it.toString() !in existingLabels }?.toString() ?: "A"
        val updated = exercises.mapIndexed { idx, ex ->
            if (idx == exerciseIndex1 || idx == exerciseIndex2) {
                ex.copy(supersetLabel = nextLabel)
            } else ex
        }
        _uiState.update { it.copy(exercises = updated) }
    }

    fun removeSuperset(exerciseIndex: Int) {
        val exercises = _uiState.value.exercises
        if (exerciseIndex !in exercises.indices) return
        val targetLabel = exercises[exerciseIndex].supersetLabel ?: return
        val updated = exercises.map { ex ->
            if (ex.supersetLabel == targetLabel) {
                ex.copy(supersetLabel = null)
            } else ex
        }
        _uiState.update { it.copy(exercises = updated) }
    }

    fun toggleWeighted(exerciseIndex: Int) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                ex.copy(isWeighted = !ex.isWeighted)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun addCustomExercise(name: String, group: MuscleGroup) {
        viewModelScope.launch {
            val id = exerciseDao.insertExercise(
                ExerciseEntity(name = name.trim(), muscleGroup = group, isCustom = true)
            )
            addExercise(ExerciseEntity(id = id, name = name.trim(), muscleGroup = group, isCustom = true))
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, newWeight: String) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) {
                        val w = newWeight.toFloatOrNull() ?: 0f
                        val r = s.reps.toIntOrNull() ?: 0
                        val autoDone = s.isCompleted || (w > 0f && r > 0)
                        s.copy(weight = newWeight, isCompleted = autoDone)
                    } else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, newReps: String) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) {
                        val w = s.weight.toFloatOrNull() ?: 0f
                        val r = newReps.toIntOrNull() ?: 0
                        val autoDone = s.isCompleted || (w > 0f && r > 0) || (r > 0 && ex.exercise.exerciseType == ExerciseType.BODYWEIGHT_ONLY)
                        s.copy(reps = newReps, isCompleted = autoDone)
                    } else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun addSet(exerciseIndex: Int) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val lastSet = ex.sets.lastOrNull()
                val newSet = if (lastSet != null) {
                    EditableSet(
                        weight = lastSet.weight,
                        reps = lastSet.reps,
                        isCompleted = false,
                        previousWeight = lastSet.previousWeight,
                        previousReps = lastSet.previousReps,
                        historicalMaxWeight = ex.allTimeMaxWeight
                    )
                } else {
                    EditableSet(
                        weight = "",
                        reps = "",
                        historicalMaxWeight = ex.allTimeMaxWeight
                    )
                }
                ex.copy(sets = ex.sets + newSet)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.toMutableList()
                if (setIndex in updatedSets.indices) {
                    updatedSets.removeAt(setIndex)
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int, isChecked: Boolean) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) s.copy(isCompleted = isChecked) else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun saveWorkout(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            if (state.exercises.isEmpty()) return@launch

            val endTimeEpoch = System.currentTimeMillis()
            val startTimeEpoch = state.startTimeEpochMillis
            val durationMin = ((endTimeEpoch - startTimeEpoch) / 60000L).toInt().coerceAtLeast(1)

            val healthData = healthConnectManager.fetchWorkoutHealthData(startTimeEpoch, endTimeEpoch)

            val workoutId = workoutDao.insertWorkout(
                WorkoutEntity(
                    dateEpochMillis = endTimeEpoch,
                    durationMinutes = durationMin,
                    note = state.note,
                    startTimeEpochMillis = startTimeEpoch,
                    endTimeEpochMillis = endTimeEpoch,
                    avgHeartRate = healthData.avgHeartRate,
                    maxHeartRate = healthData.maxHeartRate,
                    activeCalories = healthData.activeCalories
                )
            )

            state.exercises.forEachIndexed { exIndex, exItem ->
                val weId = workoutDao.insertWorkoutExercises(
                    listOf(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exItem.exercise.id, orderIndex = exIndex))
                ).first()

                val setsToInsert = exItem.sets.mapIndexed { setIndex, s ->
                    val w = s.weight.toFloatOrNull() ?: 0f
                    val r = s.reps.toIntOrNull() ?: 0
                    val isDone = s.isCompleted || (w > 0f && r > 0) || (r > 0)
                    SetEntryEntity(
                        workoutExerciseId = weId,
                        weightKg = w,
                        reps = r,
                        orderIndex = setIndex,
                        isCompleted = isDone
                    )
                }
                workoutDao.insertSets(setsToInsert)
            }
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }
}
