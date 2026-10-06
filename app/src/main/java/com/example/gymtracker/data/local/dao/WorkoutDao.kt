package com.example.gymtracker.data.local.dao

import androidx.room.*
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.SetType
import com.example.gymtracker.data.model.WorkoutWithDetails
import kotlinx.coroutines.flow.Flow

data class RawHistoryEntry(
    val date: Long,
    val weightKg: Float,
    val reps: Int,
    val setType: SetType = SetType.NORMAL
)

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts ORDER BY dateEpochMillis DESC")
    fun getAllWorkouts(): Flow<List<WorkoutWithDetails>>

    @Query("SELECT * FROM workouts ORDER BY dateEpochMillis DESC LIMIT 1")
    suspend fun getLatestWorkout(): WorkoutEntity?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): WorkoutWithDetails?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>): List<Long>

    @Insert
    suspend fun insertSets(sets: List<SetEntryEntity>)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET avgHeartRate = :avgHr, maxHeartRate = :maxHr, activeCalories = :calories WHERE id = :workoutId")
    suspend fun updateWorkoutHealthStats(workoutId: Long, avgHr: Int?, maxHr: Int?, calories: Int?)

    @Transaction
    @Query("""
        SELECT s.* FROM set_entries s
        JOIN workout_exercises we ON s.workoutExerciseId = we.id
        JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId
        ORDER BY w.dateEpochMillis DESC, s.orderIndex ASC
        LIMIT 10
    """)
    suspend fun getLastSetsForExercise(exerciseId: Long): List<SetEntryEntity>

    @Transaction
    @Query("""
        SELECT w.dateEpochMillis as date, s.weightKg, s.reps, s.setType 
        FROM set_entries s
        JOIN workout_exercises we ON s.workoutExerciseId = we.id
        JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND (s.isCompleted = 1 OR s.reps > 0 OR s.weightKg > 0)
        ORDER BY w.dateEpochMillis ASC
    """)
    fun getExerciseHistory(exerciseId: Long): Flow<List<RawHistoryEntry>>
}
