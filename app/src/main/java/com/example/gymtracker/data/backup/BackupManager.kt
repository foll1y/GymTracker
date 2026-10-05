package com.example.gymtracker.data.backup

import android.content.Context
import android.net.Uri
import com.example.gymtracker.data.local.AppDatabase
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val database: AppDatabase
) {
    suspend fun exportBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val exercises = database.exerciseDao().getAllExercises().first()
            val exercisesJson = JSONArray()
            exercises.forEach { ex ->
                val obj = JSONObject()
                obj.put("id", ex.id)
                obj.put("name", ex.name)
                obj.put("muscleGroup", ex.muscleGroup.name)
                obj.put("isCustom", ex.isCustom)
                exercisesJson.put(obj)
            }
            root.put("exercises", exercisesJson)

            val workoutsWithDetails = database.workoutDao().getAllWorkouts().first()
            val workoutsJson = JSONArray()
            workoutsWithDetails.forEach { wd ->
                val wObj = JSONObject()
                val w = wd.workout
                wObj.put("dateEpochMillis", w.dateEpochMillis)
                wObj.put("durationMinutes", w.durationMinutes)
                wObj.put("note", w.note)
                wObj.put("startTimeEpochMillis", w.startTimeEpochMillis)
                wObj.put("endTimeEpochMillis", w.endTimeEpochMillis)
                if (w.avgHeartRate != null) wObj.put("avgHeartRate", w.avgHeartRate)
                if (w.maxHeartRate != null) wObj.put("maxHeartRate", w.maxHeartRate)
                if (w.activeCalories != null) wObj.put("activeCalories", w.activeCalories)

                val exercisesListJson = JSONArray()
                wd.exercises.forEach { exDetails ->
                    val exItem = JSONObject()
                    exItem.put("exerciseName", exDetails.exercise.name)
                    exItem.put("muscleGroup", exDetails.exercise.muscleGroup.name)
                    exItem.put("orderIndex", exDetails.workoutExercise.orderIndex)

                    val setsJson = JSONArray()
                    exDetails.sets.forEach { s ->
                        val sObj = JSONObject()
                        sObj.put("weightKg", s.weightKg.toDouble())
                        sObj.put("reps", s.reps)
                        sObj.put("orderIndex", s.orderIndex)
                        sObj.put("isCompleted", s.isCompleted)
                        setsJson.put(sObj)
                    }
                    exItem.put("sets", setsJson)
                    exercisesListJson.put(exItem)
                }
                wObj.put("exercises", exercisesListJson)
                workoutsJson.put(wObj)
            }
            root.put("workouts", workoutsJson)

            context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
                os.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Не удалось открыть файл для записи"))

            Result.success(workoutsWithDetails.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { isStream: InputStream ->
                isStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("Не удалось прочитать файл резервной копии"))

            val root = JSONObject(jsonString)
            val exercisesJson = root.optJSONArray("exercises") ?: JSONArray()
            val existingExercises = database.exerciseDao().getAllExercises().first().associateBy { it.name.lowercase().trim() }.toMutableMap()

            for (i in 0 until exercisesJson.length()) {
                val exObj = exercisesJson.getJSONObject(i)
                val name = exObj.getString("name").trim()
                val muscleGroupName = exObj.optString("muscleGroup", "CHEST")
                val isCustom = exObj.optBoolean("isCustom", false)
                val group = try { enumValueOf<MuscleGroup>(muscleGroupName) } catch (e: Exception) { MuscleGroup.CHEST }

                if (!existingExercises.containsKey(name.lowercase())) {
                    val id = database.exerciseDao().insertExercise(ExerciseEntity(name = name, muscleGroup = group, isCustom = isCustom))
                    existingExercises[name.lowercase()] = ExerciseEntity(id = id, name = name, muscleGroup = group, isCustom = isCustom)
                }
            }

            val workoutsJson = root.optJSONArray("workouts") ?: JSONArray()
            var importedCount = 0

            for (i in 0 until workoutsJson.length()) {
                val wObj = workoutsJson.getJSONObject(i)
                val date = wObj.getLong("dateEpochMillis")
                val duration = wObj.optInt("durationMinutes", 0)
                val note = wObj.optString("note", "")
                val startTime = wObj.optLong("startTimeEpochMillis", 0L)
                val endTime = wObj.optLong("endTimeEpochMillis", 0L)
                val avgHr = if (wObj.has("avgHeartRate")) wObj.getInt("avgHeartRate") else null
                val maxHr = if (wObj.has("maxHeartRate")) wObj.getInt("maxHeartRate") else null
                val calories = if (wObj.has("activeCalories")) wObj.getInt("activeCalories") else null

                val workoutId = database.workoutDao().insertWorkout(
                    WorkoutEntity(
                        dateEpochMillis = date,
                        durationMinutes = duration,
                        note = note,
                        startTimeEpochMillis = startTime,
                        endTimeEpochMillis = endTime,
                        avgHeartRate = avgHr,
                        maxHeartRate = maxHr,
                        activeCalories = calories
                    )
                )

                val exercisesListJson = wObj.optJSONArray("exercises") ?: JSONArray()
                for (j in 0 until exercisesListJson.length()) {
                    val exItem = exercisesListJson.getJSONObject(j)
                    val exName = exItem.getString("exerciseName").trim()
                    val orderIndex = exItem.optInt("orderIndex", j)

                    var exerciseEntity = existingExercises[exName.lowercase()]
                    if (exerciseEntity == null) {
                        val mGroup = try { enumValueOf<MuscleGroup>(exItem.optString("muscleGroup", "CHEST")) } catch (e: Exception) { MuscleGroup.CHEST }
                        val id = database.exerciseDao().insertExercise(ExerciseEntity(name = exName, muscleGroup = mGroup, isCustom = true))
                        exerciseEntity = ExerciseEntity(id = id, name = exName, muscleGroup = mGroup, isCustom = true)
                        existingExercises[exName.lowercase()] = exerciseEntity
                    }

                    val weId = database.workoutDao().insertWorkoutExercises(
                        listOf(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseEntity.id, orderIndex = orderIndex))
                    ).first()

                    val setsJson = exItem.optJSONArray("sets") ?: JSONArray()
                    val setsList = mutableListOf<SetEntryEntity>()
                    for (k in 0 until setsJson.length()) {
                        val sObj = setsJson.getJSONObject(k)
                        setsList.add(
                            SetEntryEntity(
                                workoutExerciseId = weId,
                                weightKg = sObj.optDouble("weightKg", 0.0).toFloat(),
                                reps = sObj.optInt("reps", 0),
                                orderIndex = sObj.optInt("orderIndex", k),
                                isCompleted = sObj.optBoolean("isCompleted", true)
                            )
                        )
                    }
                    if (setsList.isNotEmpty()) {
                        database.workoutDao().insertSets(setsList)
                    }
                }
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
