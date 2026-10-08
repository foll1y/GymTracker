package com.example.gymtracker.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.health.HealthConnectManager
import com.example.gymtracker.data.local.dao.RawHistoryEntry
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.local.entity.WorkoutEntity
import com.example.gymtracker.data.model.SetType
import com.example.gymtracker.data.model.WorkoutWithDetails
import com.example.gymtracker.ui.components.ExerciseProgressBottomSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {
    val workouts: Flow<List<WorkoutWithDetails>> = workoutDao.getAllWorkouts()

    fun getExerciseHistory(exerciseId: Long): Flow<List<RawHistoryEntry>> = workoutDao.getExerciseHistory(exerciseId)

    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            workoutDao.deleteWorkout(workout)
        }
    }

    fun syncWorkoutWithWatch(workoutDetails: WorkoutWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            val w = workoutDetails.workout
            val start = if (w.startTimeEpochMillis > 0) w.startTimeEpochMillis else (w.dateEpochMillis - 3600000L)
            val end = if (w.endTimeEpochMillis > 0) w.endTimeEpochMillis else w.dateEpochMillis
            val healthData = healthConnectManager.fetchWorkoutHealthData(start, end)
            if (healthData.avgHeartRate != null || healthData.activeCalories != null) {
                workoutDao.updateWorkoutHealthStats(
                    workoutId = w.id,
                    avgHr = healthData.avgHeartRate ?: w.avgHeartRate,
                    maxHr = healthData.maxHeartRate ?: w.maxHeartRate,
                    calories = healthData.activeCalories ?: w.activeCalories
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val list by viewModel.workouts.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

    var selectedExerciseForProgress by remember { mutableStateOf<ExerciseEntity?>(null) }
    val progressHistory by remember(selectedExerciseForProgress) {
        if (selectedExerciseForProgress != null) {
            viewModel.getExerciseHistory(selectedExerciseForProgress!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "История тренировок",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (list.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "История пуста.\nЗавершите тренировку, чтобы она появилась здесь.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(list, key = { it.workout.id }) { w ->
                    WorkoutHistoryCard(
                        workoutDetails = w,
                        dateFormat = dateFormat,
                        onDelete = { viewModel.deleteWorkout(w.workout) },
                        onSyncWatch = { viewModel.syncWorkoutWithWatch(w) },
                        onExerciseClick = { selectedExerciseForProgress = it }
                    )
                }
            }
        }
    }

    if (selectedExerciseForProgress != null) {
        ExerciseProgressBottomSheet(
            exercise = selectedExerciseForProgress,
            historyEntries = progressHistory,
            onDismiss = { selectedExerciseForProgress = null }
        )
    }
}

@Composable
fun WorkoutHistoryCard(
    workoutDetails: WorkoutWithDetails,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit,
    onSyncWatch: () -> Unit,
    onExerciseClick: (ExerciseEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Расчёт суммарного тоннажа (исключая разминочные сеты)
    val totalTonnage = workoutDetails.exercises.sumOf { ex ->
        ex.sets.filter { it.setType != SetType.WARMUP }.sumOf { (it.weightKg * it.reps).toDouble() }
    }.toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(workoutDetails.workout.dateEpochMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${workoutDetails.exercises.size} упр. • $totalSets подх.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSyncWatch) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить с часов",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            // Блок данных с часов (Пульс и Калории)
            val w = workoutDetails.workout
            if (w.avgHeartRate != null || w.activeCalories != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (w.avgHeartRate != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "❤️ ${w.avgHeartRate} уд/мин" + if (w.maxHeartRate != null) " (пик ${w.maxHeartRate})" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (w.activeCalories != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "🔥 ${w.activeCalories} ккал",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (!expanded) {
                Spacer(Modifier.height(10.dp))
                workoutDetails.exercises.forEach { ex ->
                    Text(
                        text = "• ${ex.exercise.name}: ${ex.sets.size} подх.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Нажмите, чтобы развернуть подходы и график",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))

                    workoutDetails.exercises.forEachIndexed { exIndex, ex ->
                        if (exIndex > 0) Spacer(Modifier.height(14.dp))

                        // Заголовок упражнения (кликабелен для открытия графика прогресса)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onExerciseClick(ex.exercise) },
                            color = Color.Transparent
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = ex.exercise.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = "График прогресса",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (ex.workoutExercise.supersetLabel != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "🔗 Суперсет ${ex.workoutExercise.supersetLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = ex.exercise.muscleGroup.titleRu,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        ex.sets.forEachIndexed { setIndex, s ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Бейдж типа сета
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (s.setType) {
                                                SetType.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest
                                                SetType.WARMUP -> Color(0xFF5A4D1A)
                                                SetType.DROP -> Color(0xFF4A148C)
                                                SetType.FAILURE -> Color(0xFF7F0000)
                                            }
                                        ) {
                                            Text(
                                                text = if (s.setType == SetType.NORMAL) "${setIndex + 1}" else s.setType.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = when (s.setType) {
                                                    SetType.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    SetType.WARMUP -> Color(0xFFFFD54F)
                                                    SetType.DROP -> Color(0xFFE1BEE7)
                                                    SetType.FAILURE -> Color(0xFFFF8A80)
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = "${s.weightKg} кг × ${s.reps} повт.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }


                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
