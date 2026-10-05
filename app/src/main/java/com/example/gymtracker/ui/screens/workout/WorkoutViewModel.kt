package com.example.gymtracker.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.health.HealthConnectManager
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EditableSet(
    val id: Long = 0,
    val weight: String = "",
    val reps: String = "",
    val isCompleted: Boolean = false,
    val previousWeight: Float? = null,
    val previousReps: Int? = null
)

data class EditableExercise(
    val exercise: ExerciseEntity,
    val sets: List<EditableSet> = emptyList()
)

data class ActiveWorkoutUiState(
    val startTimeEpochMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val exercises: List<EditableExercise> = emptyList()
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val lastSets = workoutDao.getLastSetsForExercise(exercise.id)
            val initialSet = if (lastSets.isNotEmpty()) {
                val last = lastSets.first()
                EditableSet(
                    weight = if (last.weightKg > 0) last.weightKg.toString() else "",
                    reps = if (last.reps > 0) last.reps.toString() else "",
                    previousWeight = last.weightKg,
                    previousReps = last.reps
                )
            } else {
                EditableSet(weight = "", reps = "")
            }

            val currentList = _uiState.value.exercises.toMutableList()
            currentList.add(EditableExercise(exercise = exercise, sets = listOf(initialSet)))
            _uiState.update { it.copy(exercises = currentList) }
        }
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
                    if (sIdx == setIndex) s.copy(weight = newWeight) else s
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
                    if (sIdx == setIndex) s.copy(reps = newReps) else s
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
                        previousWeight = lastSet.previousWeight,
                        previousReps = lastSet.previousReps
                    )
                } else {
                    EditableSet(weight = "", reps = "")
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

            // Запрос данных с часов (OHealth через Health Connect)
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
                    SetEntryEntity(
                        workoutExerciseId = weId,
                        weightKg = s.weight.toFloatOrNull() ?: 0f,
                        reps = s.reps.toIntOrNull() ?: 0,
                        orderIndex = setIndex,
                        isCompleted = s.isCompleted
                    )
                }
                workoutDao.insertSets(setsToInsert)
            }
            withContext(Dispatchers.Main) { onSuccess() }
        }
    }
}
