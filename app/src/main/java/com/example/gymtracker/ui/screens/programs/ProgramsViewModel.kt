package com.example.gymtracker.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.UserPreferencesManager
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.ProgramDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.ProgramWithDays
import com.example.gymtracker.data.online.OnlineExerciseCatalog
import com.example.gymtracker.domain.ExerciseMatchSource
import com.example.gymtracker.domain.LlmPlanParser
import com.example.gymtracker.domain.ParsedExerciseItem
import com.example.gymtracker.domain.ParsedProgramPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LlmImportTargetMode(val titleRu: String) {
    NEW_PROGRAM("В новую программу"),
    ADD_DAYS_TO_PROGRAM("Дни в текущую программу"),
    ADD_TO_CURRENT_DAY("В текущий день")
}

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val onlineCatalog: OnlineExerciseCatalog,
    userPreferences: UserPreferencesManager
) : ViewModel() {

    val programs: StateFlow<List<ProgramWithDays>> = programDao.getAllProgramsWithDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()
    val isKg: StateFlow<Boolean> = userPreferences.isKg

    fun getAvailableEquipment(): List<String> = onlineCatalog.getAvailableEquipment()

    fun searchCatalog(
        query: String,
        filterGroup: MuscleGroup?,
        filterEquipment: String? = null
    ): List<ExerciseEntity> {
        return onlineCatalog.search(query, filterGroup, filterEquipment)
    }

    suspend fun importCatalogExercise(exercise: ExerciseEntity): ExerciseEntity {
        val id = exerciseDao.insertExercise(exercise.copy(id = 0, isCustom = false))
        return exercise.copy(id = id)
    }

    fun parseLlmPlan(rawText: String, localExercises: List<ExerciseEntity>): ParsedProgramPlan {
        val catalog = onlineCatalog.loadCatalog()
        return LlmPlanParser.parse(rawText, localExercises, catalog)
    }

    fun saveImportedPlan(
        plan: ParsedProgramPlan,
        mode: LlmImportTargetMode,
        setAsActive: Boolean,
        targetProgramId: Long?,
        targetDayId: Long?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val existingByName = exerciseDao.getAllExercises().first()
                .associateBy { it.name.trim().lowercase() }
                .toMutableMap()

            suspend fun resolveExerciseId(item: ParsedExerciseItem): Long {
                return when (item.matchSource) {
                    ExerciseMatchSource.LOCAL_DB -> {
                        val matched = item.matchedExercise
                        if (matched != null && matched.id > 0) {
                            matched.id
                        } else {
                            val key = item.displayName.trim().lowercase()
                            existingByName[key]?.id ?: run {
                                val newEx = ExerciseEntity(
                                    name = item.displayName.trim().ifBlank { "Упражнение" },
                                    muscleGroup = item.displayMuscleGroup,
                                    isCustom = true
                                )
                                val newId = exerciseDao.insertExercise(newEx)
                                existingByName[key] = newEx.copy(id = newId)
                                newId
                            }
                        }
                    }
                    ExerciseMatchSource.ONLINE_CATALOG -> {
                        val catalogEx = item.matchedExercise
                        if (catalogEx != null) {
                            val key = catalogEx.name.trim().lowercase()
                            existingByName[key]?.id ?: run {
                                val toInsert = catalogEx.copy(id = 0, isCustom = false)
                                val newId = exerciseDao.insertExercise(toInsert)
                                existingByName[key] = toInsert.copy(id = newId)
                                newId
                            }
                        } else {
                            val key = item.displayName.trim().lowercase()
                            existingByName[key]?.id ?: run {
                                val newEx = ExerciseEntity(
                                    name = item.displayName.trim().ifBlank { "Упражнение" },
                                    muscleGroup = item.displayMuscleGroup,
                                    isCustom = true
                                )
                                val newId = exerciseDao.insertExercise(newEx)
                                existingByName[key] = newEx.copy(id = newId)
                                newId
                            }
                        }
                    }
                    ExerciseMatchSource.NEW_CUSTOM -> {
                        val finalName = item.customName.trim().ifBlank { item.rawName.trim() }.ifBlank { "Упражнение" }
                        val key = finalName.lowercase()
                        existingByName[key]?.id ?: run {
                            val newEx = ExerciseEntity(
                                name = finalName,
                                muscleGroup = item.customMuscleGroup,
                                isCustom = true
                            )
                            val newId = exerciseDao.insertExercise(newEx)
                            existingByName[key] = newEx.copy(id = newId)
                            newId
                        }
                    }
                }
            }

            when (mode) {
                LlmImportTargetMode.NEW_PROGRAM -> {
                    val pId = programDao.insertProgram(
                        ProgramEntity(
                            title = plan.title.trim().ifBlank { "Программа из LLM" },
                            description = plan.description.trim(),
                            isActive = false
                        )
                    )
                    if (setAsActive) {
                        programDao.setActiveProgram(pId)
                    }
                    if (plan.days.isEmpty()) {
                        programDao.insertProgramDay(
                            ProgramDayEntity(programId = pId, name = "День 1", orderIndex = 0)
                        )
                    } else {
                        plan.days.forEachIndexed { dIdx, dPlan ->
                            val dId = programDao.insertProgramDay(
                                ProgramDayEntity(
                                    programId = pId,
                                    name = dPlan.name.trim().ifBlank { "День ${dIdx + 1}" },
                                    orderIndex = dIdx
                                )
                            )
                            dPlan.exercises.forEachIndexed { exIdx, exItem ->
                                val exId = resolveExerciseId(exItem)
                                programDao.insertProgramDayExercise(
                                    ProgramDayExerciseEntity(
                                        dayId = dId,
                                        exerciseId = exId,
                                        orderIndex = exIdx,
                                        targetSets = exItem.targetSets.coerceIn(1, 20),
                                        targetReps = exItem.targetReps.trim().ifBlank { "8-12" },
                                        targetWeightKg = exItem.targetWeightKg,
                                        supersetLabel = exItem.supersetLabel
                                    )
                                )
                            }
                        }
                    }
                }

                LlmImportTargetMode.ADD_DAYS_TO_PROGRAM -> {
                    val pId = targetProgramId ?: return@launch
                    val existingProgram = programDao.getProgramWithDaysById(pId)
                    val startDayIdx = existingProgram?.days?.size ?: 0
                    plan.days.forEachIndexed { offset, dPlan ->
                        val dId = programDao.insertProgramDay(
                            ProgramDayEntity(
                                programId = pId,
                                name = dPlan.name.trim().ifBlank { "День ${startDayIdx + offset + 1}" },
                                orderIndex = startDayIdx + offset
                            )
                        )
                        dPlan.exercises.forEachIndexed { exIdx, exItem ->
                            val exId = resolveExerciseId(exItem)
                            programDao.insertProgramDayExercise(
                                ProgramDayExerciseEntity(
                                    dayId = dId,
                                    exerciseId = exId,
                                    orderIndex = exIdx,
                                    targetSets = exItem.targetSets.coerceIn(1, 20),
                                    targetReps = exItem.targetReps.trim().ifBlank { "8-12" },
                                    targetWeightKg = exItem.targetWeightKg,
                                    supersetLabel = exItem.supersetLabel
                                )
                            )
                        }
                    }
                }

                LlmImportTargetMode.ADD_TO_CURRENT_DAY -> {
                    val dId = targetDayId ?: return@launch
                    val existingDay = programDao.getDayWithExercisesById(dId)
                    var nextExIdx = existingDay?.exercises?.size ?: 0
                    val allImportedExercises = plan.days.flatMap { it.exercises }
                    for (exItem in allImportedExercises) {
                        val exId = resolveExerciseId(exItem)
                        programDao.insertProgramDayExercise(
                            ProgramDayExerciseEntity(
                                dayId = dId,
                                exerciseId = exId,
                                orderIndex = nextExIdx++,
                                targetSets = exItem.targetSets.coerceIn(1, 20),
                                targetReps = exItem.targetReps.trim().ifBlank { "8-12" },
                                targetWeightKg = exItem.targetWeightKg,
                                supersetLabel = exItem.supersetLabel
                            )
                        )
                    }
                }
            }

            onSaved()
        }
    }

    fun createProgram(title: String, description: String = "") {
        viewModelScope.launch {
            val pId = programDao.insertProgram(
                ProgramEntity(title = title.trim(), description = description.trim(), isActive = false)
            )
            programDao.insertProgramDay(ProgramDayEntity(programId = pId, name = "День 1", orderIndex = 0))
        }
    }

    fun updateProgram(program: ProgramEntity, newTitle: String, newDescription: String) {
        viewModelScope.launch {
            programDao.updateProgram(
                program.copy(title = newTitle.trim(), description = newDescription.trim())
            )
        }
    }

    fun deleteProgram(program: ProgramEntity) {
        viewModelScope.launch {
            programDao.deleteProgram(program)
        }
    }

    fun setActiveProgram(programId: Long) {
        viewModelScope.launch {
            programDao.setActiveProgram(programId)
        }
    }

    fun addDayToProgram(programId: Long, name: String) {
        viewModelScope.launch {
            val program = programDao.getProgramWithDaysById(programId)
            val nextIndex = program?.days?.size ?: 0
            programDao.insertProgramDay(
                ProgramDayEntity(programId = programId, name = name.trim(), orderIndex = nextIndex)
            )
        }
    }

    fun renameProgramDay(day: ProgramDayEntity, newName: String) {
        viewModelScope.launch {
            programDao.updateProgramDay(day.copy(name = newName.trim()))
        }
    }

    fun deleteProgramDay(day: ProgramDayEntity) {
        viewModelScope.launch {
            programDao.deleteProgramDay(day)
        }
    }

    fun addExerciseToDay(
        dayId: Long,
        exerciseId: Long,
        targetSets: Int = 3,
        targetReps: String = "8-12",
        targetWeight: Float? = null
    ) {
        viewModelScope.launch {
            val dayWithExercises = programDao.getDayWithExercisesById(dayId)
            val nextIndex = dayWithExercises?.exercises?.size ?: 0
            programDao.insertProgramDayExercise(
                ProgramDayExerciseEntity(
                    dayId = dayId,
                    exerciseId = exerciseId,
                    orderIndex = nextIndex,
                    targetSets = targetSets,
                    targetReps = targetReps,
                    targetWeightKg = targetWeight
                )
            )
        }
    }

    fun updateExerciseInDay(item: ProgramDayExerciseEntity) {
        viewModelScope.launch {
            programDao.updateProgramDayExercise(item)
        }
    }

    fun removeExerciseFromDay(item: ProgramDayExerciseEntity) {
        viewModelScope.launch {
            programDao.deleteProgramDayExercise(item)
        }
    }

    fun reorderExercisesInDay(dayId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val day = programDao.getDayWithExercisesById(dayId) ?: return@launch
            val list = day.exercises.map { it.planExercise }.sortedBy { it.orderIndex }.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                val moved = list.removeAt(fromIndex)
                list.add(toIndex, moved)
                val updated = list.mapIndexed { idx, it -> it.copy(orderIndex = idx) }
                programDao.updateProgramDayExercises(updated)
            }
        }
    }

    fun setSupersetForDayExercises(dayId: Long, planExerciseId1: Long, planExerciseId2: Long) {
        viewModelScope.launch {
            val day = programDao.getDayWithExercisesById(dayId) ?: return@launch
            val existingLabels = day.exercises.mapNotNull { it.planExercise.supersetLabel }.distinct()
            val nextLabel = ('A'..'Z').firstOrNull { it.toString() !in existingLabels }?.toString() ?: "A"

            day.exercises.forEach { pe ->
                if (pe.planExercise.id == planExerciseId1 || pe.planExercise.id == planExerciseId2) {
                    programDao.updateProgramDayExercise(pe.planExercise.copy(supersetLabel = nextLabel))
                }
            }
        }
    }

    fun removeSupersetForDayExercise(planExercise: ProgramDayExerciseEntity) {
        viewModelScope.launch {
            val day = programDao.getDayWithExercisesById(planExercise.dayId) ?: return@launch
            val label = planExercise.supersetLabel ?: return@launch
            day.exercises.filter { it.planExercise.supersetLabel == label }.forEach {
                programDao.updateProgramDayExercise(it.planExercise.copy(supersetLabel = null))
            }
        }
    }

    suspend fun createCustomExercise(name: String, group: MuscleGroup): Long {
        return exerciseDao.insertExercise(
            ExerciseEntity(name = name.trim(), muscleGroup = group, isCustom = true)
        )
    }
}
