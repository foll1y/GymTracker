package com.example.gymtracker.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtracker.data.local.entity.*

data class WorkoutExerciseWithDetails(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutExerciseId"
    )
    val sets: List<SetEntryEntity>
)

data class WorkoutWithDetails(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<WorkoutExerciseWithDetails>
)

data class ProgramDayExerciseWithExercise(
    @Embedded val planExercise: ProgramDayExerciseEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)

data class ProgramDayWithExercises(
    @Embedded val day: ProgramDayEntity,
    @Relation(
        entity = ProgramDayExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val exercises: List<ProgramDayExerciseWithExercise>
)

data class ProgramWithDays(
    @Embedded val program: ProgramEntity,
    @Relation(
        entity = ProgramDayEntity::class,
        parentColumn = "id",
        entityColumn = "programId"
    )
    val days: List<ProgramDayWithExercises>
)
