package com.example.gymtracker.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.health.HealthConnectManager
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.ProgramDao
import com.example.gymtracker.data.local.dao.RawHistoryEntry
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.SetType
import com.example.gymtracker.data.online.OnlineExerciseCatalog
import com.example.gymtracker.data.preferences.UserPreferencesManager
import com.example.gymtracker.domain.Formulas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    val dayId: Long? = null,
    val startTimeEpochMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val exercises: List<EditableExercise> = emptyList(),
    val programDayTitle: String? = null,
    val isSaving: Boolean = false,
    val isRestTimerRunning: Boolean = false,
    val restTimerRemainingSec: Int = 0,
    val restTimerTotalSec: Int = 90,
    val restTimerFinishedEvent: Long = 0L
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val programDao: ProgramDao,
    private val healthConnectManager: HealthConnectManager,
    private val onlineCatalog: OnlineExerciseCatalog,
    private val userPreferences: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private val isSavingInProgress = AtomicBoolean(false)
    private var restTimerJob: Job? = null

    val isKg: StateFlow<Boolean> = userPreferences.isKg
    val defaultRestSeconds: StateFlow<Int> = userPreferences.defaultRestSeconds
    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun getExerciseHistory(exerciseId: Long): Flow<List<RawHistoryEntry>> =
        workoutDao.getExerciseHistory(exerciseId)

    fun getAvailableEquipment(): List<String> = onlineCatalog.getAvailableEquipment()

    fun searchCatalog(
        query: String,
        filterGroup: MuscleGroup?,
        filterEquipment: String? = null
    ): List<ExerciseEntity> {
        return onlineCatalog.search(query, filterGroup, filterEquipment)
    }

    fun addOnlineExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val id = exerciseDao.insertExercise(exercise.copy(id = 0, isCustom = false))
            addExercise(exercise.copy(id = id))
        }
    }

    fun initWorkout(dayId: Long?) {
        if (_uiState.value.exercises.isNotEmpty()) return

        viewModelScope.launch {
            val restored = tryRestoreDraft(dayId)
            if (!restored && dayId != null && dayId > 0) {
                loadWorkoutFromProgramDay(dayId)
            }
        }
    }

    private suspend fun tryRestoreDraft(requestedDayId: Long?): Boolean {
        val rawJson = userPreferences.loadWorkoutDraft() ?: return false
        return try {
            val root = JSONObject(rawJson)
            val savedDayId = if (root.has("dayId") && !root.isNull("dayId")) root.getLong("dayId") else null
            // Если пользователь явно запустил конкретный день программы, а в черновике другой день — загружаем запрошенный день
            if (requestedDayId != null && requestedDayId > 0 && savedDayId != null && savedDayId != requestedDayId) {
                return false
            }

            val startTime = root.optLong("startTimeEpochMillis", System.currentTimeMillis())
            val note = root.optString("note", "")
            val programDayTitle = if (root.has("programDayTitle") && !root.isNull("programDayTitle")) {
                root.getString("programDayTitle")
            } else null

            val exArray = root.optJSONArray("exercises") ?: JSONArray()
            val restoredExercises = mutableListOf<EditableExercise>()

            for (i in 0 until exArray.length()) {
                val exObj = exArray.getJSONObject(i)
                val exId = exObj.getLong("exerciseId")
                val exerciseEntity = exerciseDao.getExerciseById(exId) ?: ExerciseEntity(
                    id = exId,
                    name = exObj.optString("exerciseName", "Упражнение"),
                    muscleGroup = try {
                        enumValueOf<MuscleGroup>(exObj.optString("muscleGroup", "CHEST"))
                    } catch (e: Exception) {
                        MuscleGroup.CHEST
                    },
                    exerciseType = try {
                        enumValueOf<ExerciseType>(exObj.optString("exerciseType", "WEIGHT_AND_REPS"))
                    } catch (e: Exception) {
                        ExerciseType.WEIGHT_AND_REPS
                    }
                )

                val setsArray = exObj.optJSONArray("sets") ?: JSONArray()
                val restoredSets = mutableListOf<EditableSet>()
                for (j in 0 until setsArray.length()) {
                    val sObj = setsArray.getJSONObject(j)
                    restoredSets.add(
                        EditableSet(
                            weight = sObj.optString("weight", ""),
                            reps = sObj.optString("reps", ""),
                            isCompleted = sObj.optBoolean("isCompleted", false),
                            setType = try {
                                enumValueOf<SetType>(sObj.optString("setType", "NORMAL"))
                            } catch (e: Exception) {
                                SetType.NORMAL
                            },
                            previousWeight = if (sObj.has("previousWeight")) sObj.getDouble("previousWeight").toFloat() else null,
                            previousReps = if (sObj.has("previousReps")) sObj.getInt("previousReps") else null,
                            historicalMaxWeight = if (sObj.has("historicalMaxWeight")) sObj.getDouble("historicalMaxWeight").toFloat() else null
                        )
                    )
                }

                restoredExercises.add(
                    EditableExercise(
                        exercise = exerciseEntity,
                        sets = restoredSets,
                        allTimeMaxWeight = exObj.optDouble("allTimeMaxWeight", 0.0).toFloat(),
                        isWeighted = exObj.optBoolean("isWeighted", false),
                        supersetLabel = if (exObj.has("supersetLabel") && !exObj.isNull("supersetLabel")) {
                            exObj.getString("supersetLabel")
                        } else null
                    )
                )
            }

            if (restoredExercises.isEmpty()) return false

            _uiState.update {
                it.copy(
                    dayId = savedDayId,
                    startTimeEpochMillis = startTime,
                    note = note,
                    exercises = restoredExercises,
                    programDayTitle = programDayTitle
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun persistDraft() {
        val state = _uiState.value
        if (state.exercises.isEmpty() && state.note.isBlank()) {
            userPreferences.clearWorkoutDraft()
            return
        }
        try {
            val root = JSONObject()
            if (state.dayId != null) root.put("dayId", state.dayId)
            root.put("startTimeEpochMillis", state.startTimeEpochMillis)
            root.put("note", state.note)
            if (state.programDayTitle != null) root.put("programDayTitle", state.programDayTitle)

            val exArray = JSONArray()
            state.exercises.forEach { exItem ->
                val exObj = JSONObject()
                exObj.put("exerciseId", exItem.exercise.id)
                exObj.put("exerciseName", exItem.exercise.name)
                exObj.put("muscleGroup", exItem.exercise.muscleGroup.name)
                exObj.put("exerciseType", exItem.exercise.exerciseType.name)
                exObj.put("allTimeMaxWeight", exItem.allTimeMaxWeight.toDouble())
                exObj.put("isWeighted", exItem.isWeighted)
                if (exItem.supersetLabel != null) exObj.put("supersetLabel", exItem.supersetLabel)

                val setsArray = JSONArray()
                exItem.sets.forEach { s ->
                    val sObj = JSONObject()
                    sObj.put("weight", s.weight)
                    sObj.put("reps", s.reps)
                    sObj.put("isCompleted", s.isCompleted)
                    sObj.put("setType", s.setType.name)
                    if (s.previousWeight != null) sObj.put("previousWeight", s.previousWeight.toDouble())
                    if (s.previousReps != null) sObj.put("previousReps", s.previousReps)
                    if (s.historicalMaxWeight != null) sObj.put("historicalMaxWeight", s.historicalMaxWeight.toDouble())
                    setsArray.put(sObj)
                }
                exObj.put("sets", setsArray)
                exArray.put(exObj)
            }
            root.put("exercises", exArray)
            userPreferences.saveWorkoutDraft(root.toString())
        } catch (e: Exception) {
            // Игнорируем ошибки сериализации черновика
        }
    }

    private suspend fun loadWorkoutFromProgramDay(dayId: Long) {
        val dayWithExercises = programDao.getDayWithExercisesById(dayId) ?: return
        val editableExercisesList = mutableListOf<EditableExercise>()
        val useKg = isKg.value

        val sortedPlanExercises = dayWithExercises.exercises.sortedBy { it.planExercise.orderIndex }

        sortedPlanExercises.forEach { planEx ->
            val ex = planEx.exercise
            val lastSets = workoutDao.getLastSetsForExercise(ex.id)
            val history = workoutDao.getExerciseHistory(ex.id).first()
            val maxHistoricalWeight = history
                .filter { it.setType != SetType.WARMUP }
                .maxOfOrNull { it.weightKg } ?: 0f

            val targetCount = planEx.planExercise.targetSets.coerceIn(1, 12)
            val defaultWeightKg = when {
                planEx.planExercise.targetWeightKg != null && planEx.planExercise.targetWeightKg > 0f ->
                    planEx.planExercise.targetWeightKg
                lastSets.isNotEmpty() && lastSets.first().weightKg > 0f ->
                    lastSets.first().weightKg
                else -> 0f
            }
            val defaultWeightStr = if (defaultWeightKg > 0f) {
                Formulas.formatNumber(Formulas.convertWeight(defaultWeightKg, useKg))
            } else ""

            val defaultRepsStr = if (planEx.planExercise.targetReps.isNotBlank()) {
                Regex("\\d+").find(planEx.planExercise.targetReps)?.value ?: "10"
            } else if (lastSets.isNotEmpty() && lastSets.first().reps > 0) {
                lastSets.first().reps.toString()
            } else "10"

            val isWeightedDefault = ex.exerciseType == ExerciseType.WEIGHTED_BODYWEIGHT && defaultWeightStr.isNotBlank()

            val setsList = (0 until targetCount).map { sIdx ->
                val last = lastSets.getOrNull(sIdx) ?: lastSets.lastOrNull()
                val setWeightStr = if (planEx.planExercise.targetWeightKg == null && last != null && last.weightKg > 0f) {
                    Formulas.formatNumber(Formulas.convertWeight(last.weightKg, useKg))
                } else {
                    defaultWeightStr
                }
                EditableSet(
                    weight = setWeightStr,
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
                dayId = dayId,
                startTimeEpochMillis = System.currentTimeMillis(),
                exercises = editableExercisesList,
                programDayTitle = dayWithExercises.day.name
            )
        }
        persistDraft()
    }

    fun startRestTimer(seconds: Int = userPreferences.defaultRestSeconds.value) {
        restTimerJob?.cancel()
        val total = seconds.coerceIn(5, 600)
        _uiState.update {
            it.copy(
                isRestTimerRunning = true,
                restTimerRemainingSec = total,
                restTimerTotalSec = total
            )
        }
        restTimerJob = viewModelScope.launch {
            while (_uiState.value.restTimerRemainingSec > 0) {
                delay(1000L)
                val next = _uiState.value.restTimerRemainingSec - 1
                if (next <= 0) {
                    _uiState.update {
                        it.copy(
                            isRestTimerRunning = false,
                            restTimerRemainingSec = 0,
                            restTimerFinishedEvent = System.currentTimeMillis()
                        )
                    }
                    break
                } else {
                    _uiState.update { it.copy(restTimerRemainingSec = next) }
                }
            }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val current = _uiState.value.restTimerRemainingSec
        val next = (current + deltaSeconds).coerceIn(0, 600)
        if (next == 0) {
            stopRestTimer()
        } else {
            val newTotal = maxOf(_uiState.value.restTimerTotalSec, next)
            _uiState.update {
                it.copy(
                    restTimerRemainingSec = next,
                    restTimerTotalSec = newTotal
                )
            }
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isRestTimerRunning = false,
                restTimerRemainingSec = 0
            )
        }
    }

    fun updateNote(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
        persistDraft()
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val useKg = isKg.value
            val lastSets = workoutDao.getLastSetsForExercise(exercise.id)
            val history = workoutDao.getExerciseHistory(exercise.id).first()
            val maxHistoricalWeight = history
                .filter { it.setType != SetType.WARMUP }
                .maxOfOrNull { it.weightKg } ?: 0f

            val initialSet = if (lastSets.isNotEmpty()) {
                val last = lastSets.first()
                EditableSet(
                    weight = if (last.weightKg > 0f) Formulas.formatNumber(Formulas.convertWeight(last.weightKg, useKg)) else "",
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
            persistDraft()
        }
    }

    fun removeExercise(exerciseIndex: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (exerciseIndex !in current.indices) return
        val removed = current.removeAt(exerciseIndex)
        val removedLabel = removed.supersetLabel
        val cleaned = if (removedLabel != null && current.count { it.supersetLabel == removedLabel } < 2) {
            current.map { if (it.supersetLabel == removedLabel) it.copy(supersetLabel = null) else it }
        } else {
            current
        }
        _uiState.update { it.copy(exercises = cleaned) }
        persistDraft()
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.exercises.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _uiState.update { it.copy(exercises = current) }
        persistDraft()
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
        persistDraft()
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
        persistDraft()
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
        persistDraft()
    }

    fun toggleWeighted(exerciseIndex: Int) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                ex.copy(isWeighted = !ex.isWeighted)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
        persistDraft()
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
                        s.copy(weight = newWeight)
                    } else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
        persistDraft()
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, newReps: String) {
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) {
                        s.copy(reps = newReps)
                    } else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
        persistDraft()
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
                        setType = SetType.NORMAL,
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
        persistDraft()
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
        persistDraft()
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int, isChecked: Boolean) {
        val useKg = isKg.value
        val updatedExercises = _uiState.value.exercises.mapIndexed { exIdx, ex ->
            if (exIdx == exerciseIndex) {
                val updatedSets = ex.sets.mapIndexed { sIdx, s ->
                    if (sIdx == setIndex) {
                        val filledWeight = if (isChecked && s.weight.isBlank() && s.previousWeight != null && s.previousWeight > 0f) {
                            Formulas.formatNumber(Formulas.convertWeight(s.previousWeight, useKg))
                        } else s.weight
                        val filledReps = if (isChecked && s.reps.isBlank() && s.previousReps != null && s.previousReps > 0) {
                            s.previousReps.toString()
                        } else s.reps
                        s.copy(
                            weight = filledWeight,
                            reps = filledReps,
                            isCompleted = isChecked
                        )
                    } else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        _uiState.update { it.copy(exercises = updatedExercises) }
        persistDraft()

        if (isChecked && userPreferences.autoStartRestTimer.value) {
            startRestTimer(userPreferences.defaultRestSeconds.value)
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        stopRestTimer()
        userPreferences.clearWorkoutDraft()
        _uiState.value = ActiveWorkoutUiState()
        onDiscarded()
    }

    fun saveWorkout(onSuccess: () -> Unit) {
        if (!isSavingInProgress.compareAndSet(false, true)) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _uiState.value
                if (state.exercises.isEmpty()) {
                    isSavingInProgress.set(false)
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }

                val useKg = isKg.value
                val endTimeEpoch = System.currentTimeMillis()
                val startTimeEpoch = state.startTimeEpochMillis
                val durationMin = ((endTimeEpoch - startTimeEpoch) / 60000L).toInt().coerceAtLeast(1)

                val healthData = healthConnectManager.fetchWorkoutHealthData(startTimeEpoch, endTimeEpoch)

                val anyExplicitlyCompleted = state.exercises.any { ex ->
                    ex.sets.any { it.isCompleted }
                }

                val workoutId = workoutDao.insertWorkout(
                    WorkoutEntity(
                        dateEpochMillis = endTimeEpoch,
                        durationMinutes = durationMin,
                        note = state.note.trim(),
                        startTimeEpochMillis = startTimeEpoch,
                        endTimeEpochMillis = endTimeEpoch,
                        avgHeartRate = healthData.avgHeartRate,
                        maxHeartRate = healthData.maxHeartRate,
                        activeCalories = healthData.activeCalories
                    )
                )

                var savedExerciseOrder = 0
                state.exercises.forEach { exItem ->
                    val candidateSets = exItem.sets.filter { s ->
                        val rawW = s.weight.toFloatOrNull() ?: 0f
                        val r = s.reps.toIntOrNull() ?: 0
                        if (anyExplicitlyCompleted) {
                            s.isCompleted && (rawW > 0f || r > 0)
                        } else {
                            rawW > 0f || r > 0
                        }
                    }

                    if (candidateSets.isNotEmpty()) {
                        val weId = workoutDao.insertWorkoutExercises(
                            listOf(
                                WorkoutExerciseEntity(
                                    workoutId = workoutId,
                                    exerciseId = exItem.exercise.id,
                                    orderIndex = savedExerciseOrder++,
                                    supersetLabel = exItem.supersetLabel
                                )
                            )
                        ).first()

                        val setsToInsert = candidateSets.mapIndexed { setIndex, s ->
                            val rawW = s.weight.toFloatOrNull() ?: 0f
                            val wKg = Formulas.toKg(rawW, useKg)
                            val r = s.reps.toIntOrNull() ?: 0
                            SetEntryEntity(
                                workoutExerciseId = weId,
                                weightKg = wKg,
                                reps = r,
                                orderIndex = setIndex,
                                isCompleted = true,
                                setType = s.setType
                            )
                        }
                        workoutDao.insertSets(setsToInsert)
                    }
                }

                stopRestTimer()
                userPreferences.clearWorkoutDraft()
                withContext(Dispatchers.Main) {
                    isSavingInProgress.set(false)
                    onSuccess()
                }
            } catch (e: Exception) {
                isSavingInProgress.set(false)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
