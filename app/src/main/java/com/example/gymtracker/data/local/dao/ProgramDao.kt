package com.example.gymtracker.data.local.dao

import androidx.room.*
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.ProgramDayWithExercises
import com.example.gymtracker.data.model.ProgramWithDays
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Transaction
    @Query("SELECT * FROM programs ORDER BY id ASC")
    fun getAllProgramsWithDays(): Flow<List<ProgramWithDays>>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getProgramWithDaysById(id: Long): ProgramWithDays?

    @Transaction
    @Query("SELECT * FROM program_days WHERE id = :dayId")
    suspend fun getDayWithExercisesById(dayId: Long): ProgramDayWithExercises?

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1 LIMIT 1")
    fun getActiveProgram(): Flow<ProgramWithDays?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity): Long

    @Update
    suspend fun updateProgram(program: ProgramEntity)

    @Delete
    suspend fun deleteProgram(program: ProgramEntity)

    @Query("UPDATE programs SET isActive = CASE WHEN id = :programId THEN 1 ELSE 0 END")
    suspend fun setActiveProgram(programId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramDay(day: ProgramDayEntity): Long

    @Delete
    suspend fun deleteProgramDay(day: ProgramDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramDayExercise(item: ProgramDayExerciseEntity): Long

    @Update
    suspend fun updateProgramDayExercise(item: ProgramDayExerciseEntity)

    @Update
    suspend fun updateProgramDayExercises(items: List<ProgramDayExerciseEntity>)

    @Delete
    suspend fun deleteProgramDayExercise(item: ProgramDayExerciseEntity)

    @Query("DELETE FROM program_day_exercises WHERE dayId = :dayId")
    suspend fun clearExercisesForDay(dayId: Long)
}
