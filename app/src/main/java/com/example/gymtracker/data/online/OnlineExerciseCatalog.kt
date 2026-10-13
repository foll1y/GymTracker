package com.example.gymtracker.data.online

import android.content.Context
import com.example.gymtracker.data.local.dao.ExerciseDao
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineExerciseCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao
) {
    private var cachedCatalog: List<ExerciseEntity>? = null

    fun loadCatalog(): List<ExerciseEntity> {
        cachedCatalog?.let { return it }
        return try {
            val jsonString = context.assets.open("extended_exercises.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val list = mutableListOf<ExerciseEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val nameEn = obj.optString("nameEn", "")
                val muscleStr = obj.optString("muscleGroup", "CHEST")
                val muscle = try { enumValueOf<MuscleGroup>(muscleStr) } catch (e: Exception) { MuscleGroup.CHEST }
                val typeStr = obj.optString("exerciseType", "WEIGHT_AND_REPS")
                val exType = try { enumValueOf<ExerciseType>(typeStr) } catch (e: Exception) { ExerciseType.WEIGHT_AND_REPS }
                val equipment = obj.optString("equipment", "").trim()
                val setup = obj.optString("setupTip", "")
                val exec = obj.optString("executionTip", "")
                val mistake = obj.optString("mistakeTip", "")
                val img = obj.optString("imagePath", "")

                list.add(
                    ExerciseEntity(
                        name = name,
                        nameEn = nameEn,
                        muscleGroup = muscle,
                        isCustom = false,
                        exerciseType = exType,
                        equipment = equipment,
                        setupTip = setup,
                        executionTip = exec,
                        mistakeTip = mistake,
                        imagePath = img
                    )
                )
            }
            cachedCatalog = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAvailableEquipment(): List<String> {
        return loadCatalog()
            .map { it.equipment.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun search(
        query: String,
        filterGroup: MuscleGroup?,
        filterEquipment: String? = null
    ): List<ExerciseEntity> {
        val q = query.trim().lowercase()
        val eqFilter = filterEquipment?.trim()?.lowercase()
        val catalog = loadCatalog()
        return catalog.filter { ex ->
            val matchesGroup = filterGroup == null || ex.muscleGroup == filterGroup
            val matchesEquipment = eqFilter.isNullOrEmpty() || ex.equipment.lowercase() == eqFilter
            val matchesQuery = q.isEmpty() ||
                ex.name.lowercase().contains(q) ||
                ex.nameEn.lowercase().contains(q) ||
                ex.muscleGroup.titleRu.lowercase().contains(q) ||
                ex.equipment.lowercase().contains(q)
            matchesGroup && matchesEquipment && matchesQuery
        }
    }

    suspend fun importFullCatalog(): Int = withContext(Dispatchers.IO) {
        val catalog = loadCatalog()
        val existing = exerciseDao.getAllExercises().first().associateBy { it.name.lowercase().trim() }
        val toInsert = catalog.filter { !existing.containsKey(it.name.lowercase().trim()) }
        if (toInsert.isNotEmpty()) {
            exerciseDao.insertAll(toInsert)
        }
        toInsert.size
    }
}
