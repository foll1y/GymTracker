package com.example.gymtracker.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.WorkoutEntity
import com.example.gymtracker.data.model.WorkoutWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutDao: WorkoutDao
) : ViewModel() {
    val workouts: Flow<List<WorkoutWithDetails>> = workoutDao.getAllWorkouts()

    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutDao.deleteWorkout(workout)
        }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val list by viewModel.workouts.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

    if (list.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "История тренировок пуста.\nЗавершите тренировку, чтобы она появилась здесь.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.workout.id }) { w ->
                WorkoutHistoryCard(
                    workoutDetails = w,
                    dateFormat = dateFormat,
                    onDelete = { viewModel.deleteWorkout(w.workout) }
                )
            }
        }
    }
}

@Composable
fun WorkoutHistoryCard(
    workoutDetails: WorkoutWithDetails,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Расчет общего тоннажа тренировки
    val totalVolumeKg = remember(workoutDetails) {
        workoutDetails.exercises.sumOf { ex ->
            ex.sets.filter { it.isCompleted }.sumOf { (it.weightKg * it.reps).toDouble() }
        }.toInt()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Верхняя плашка: дата, иконка разворота и удаление
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(workoutDetails.workout.dateEpochMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Упражнений: ${workoutDetails.exercises.size} • Тоннаж: $totalVolumeKg кг",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить тренировку",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }

            // Краткий вид, когда карточка свернута
            if (!expanded) {
                Spacer(Modifier.height(8.dp))
                workoutDetails.exercises.forEach { ex ->
                    Text(
                        text = "• ${ex.exercise.name}: ${ex.sets.size} подх.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Нажмите, чтобы посмотреть все веса и повторы",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Подробный вид всех подходов, когда карточка раскрыта
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    workoutDetails.exercises.forEachIndexed { exIndex, ex ->
                        if (exIndex > 0) Spacer(Modifier.height(12.dp))

                        Text(
                            text = ex.exercise.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ex.exercise.muscleGroup.titleRu,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(Modifier.height(4.dp))

                        // Список всех подходов упражнения
                        ex.sets.forEachIndexed { setIndex, s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Подход ${setIndex + 1}:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${s.weightKg} кг × ${s.reps} повт.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(s.weightKg * s.reps).toInt()} кг",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
