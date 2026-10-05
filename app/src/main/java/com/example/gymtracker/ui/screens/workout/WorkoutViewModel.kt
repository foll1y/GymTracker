package com.example.gymtracker.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EditableSet(
    val id: Long = 0,
    var weight: String = "",
    var reps: String = "",
    var isCompleted: Boolean = false,
    val previousWeight: Float? = null,
    val previousReps: Int? = null
)

data class EditableExercise(
    val exercise: ExerciseEntity,
    val sets: MutableList<EditableSet> = mutableListOf()
)

data class ActiveWorkoutUiState(
    val workoutDate: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val note: String = "",
    val exercises: List<EditableExercise> = emptyListOf(),
    val isTimerActive: Boolean = false,
    val restTimerRemainingSeconds: Int = 0,
    val isExerciseSearchOpen: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    private var timerJob: Job? = null
    private var restJob: Job? = null

    init {
        startWorkoutDurationTimer()
    }

    private fun startWorkoutDurationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val lastSets = workoutDao.getLastSetsForExercise(exercise.id)
            val initialSet = if (lastSets.isNotEmpty()) {
                val last = lastSets.first()
                EditableSet(
                    weight = last.weightKg.toString(),
                    reps = last.reps.toString(),
                    previousWeight = last.weightKg,
                    previousReps = last.reps
                )
            } else {
                EditableSet(weight = "", reps = "")
            }

            val currentList = _uiState.value.exercises.toMutableList()
            currentList.add(EditableExercise(exercise = exercise, sets = mutableListOf(initialSet)))
            _uiState.update { it.copy(exercises = currentList, isExerciseSearchOpen = false) }
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

    fun addSet(exerciseIndex: Int) {
        val currentList = _uiState.value.exercises.toMutableList()
        val target = currentList[exerciseIndex]
        val lastSet = target.sets.lastOrNull()

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

        target.sets.add(newSet)
        _uiState.update { it.copy(exercises = currentList) }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val currentList = _uiState.value.exercises.toMutableList()
        currentList[exerciseIndex].sets.removeAt(setIndex)
        _uiState.update { it.copy(exercises = currentList) }
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int, isChecked: Boolean, restTimeSeconds: Int = 90) {
        val currentList = _uiState.value.exercises.toMutableList()
        currentList[exerciseIndex].sets[setIndex].isCompleted = isChecked
        _uiState.update { it.copy(exercises = currentList) }

        if (isChecked) {
            startRestTimer(restTimeSeconds)
        }
    }

    private fun startRestTimer(seconds: Int) {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            _uiState.update { it.copy(isTimerActive = true, restTimerRemainingSeconds = seconds) }
            for (remaining in seconds downTo 1) {
                delay(1000)
                _uiState.update { it.copy(restTimerRemainingSeconds = remaining - 1) }
            }
            _uiState.update { it.copy(isTimerActive = false) }
        }
    }

    fun saveWorkout(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            if (state.exercises.isEmpty()) return@launch

            val workoutId = workoutDao.insertWorkout(
                WorkoutEntity(
                    dateEpochMillis = state.workoutDate,
                    durationMinutes = (state.durationSeconds / 60).toInt(),
                    note = state.note
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
