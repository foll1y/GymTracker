package com.example.gymtracker.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.ProgramDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.ProgramWithDays
import com.example.gymtracker.data.online.OnlineExerciseCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val onlineCatalog: OnlineExerciseCatalog
) : ViewModel() {

    val programs: StateFlow<List<ProgramWithDays>> = programDao.getAllProgramsWithDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun searchCatalog(query: String, filterGroup: MuscleGroup?): List<ExerciseEntity> {
        return onlineCatalog.search(query, filterGroup)
    }

    suspend fun importCatalogExercise(exercise: ExerciseEntity): ExerciseEntity {
        val id = exerciseDao.insertExercise(exercise.copy(id = 0, isCustom = false))
        return exercise.copy(id = id)
    }

    fun createProgram(title: String, description: String = "") {
        viewModelScope.launch {
            val pId = programDao.insertProgram(
                ProgramEntity(title = title.trim(), description = description.trim(), isActive = false)
            )
            programDao.insertProgramDay(ProgramDayEntity(programId = pId, name = "День 1", orderIndex = 0))
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
