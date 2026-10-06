package com.example.gymtracker.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.dao.ProgramDao
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.ProgramWithDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    val programs: StateFlow<List<ProgramWithDays>> = programDao.getAllProgramsWithDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

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

    suspend fun createCustomExercise(name: String, group: MuscleGroup): Long {
        return exerciseDao.insertExercise(
            ExerciseEntity(name = name.trim(), muscleGroup = group, isCustom = true)
        )
    }
}
