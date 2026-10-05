package com.example.gymtracker.ui.screens.workout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.ui.components.LargeNumberInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onWorkoutFinished: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var showExerciseDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var newExerciseName by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Тренировка", style = MaterialTheme.typography.titleMedium)
                        val mins = state.durationSeconds / 60
                        val secs = state.durationSeconds % 60
                        Text(
                            String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    Button(onClick = { viewModel.saveWorkout(onWorkoutFinished) }) {
                        Text("Завершить")
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isTimerActive,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Отдых: ${state.restTimerRemainingSeconds} с",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            itemsIndexed(state.exercises) { exIndex, exerciseItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = exerciseItem.exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = exerciseItem.exercise.muscleGroup.titleRu,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Сет", Modifier.weight(0.7f), style = MaterialTheme.typography.labelMedium)
                            Text("Прошлый", Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium)
                            Text("Вес (кг)", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                            Text("Повт.", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                            Text("Готово", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        }

                        exerciseItem.sets.forEachIndexed { setIndex, setEntry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${setIndex + 1}", Modifier.weight(0.7f), fontWeight = FontWeight.SemiBold)

                                val prevText = if (setEntry.previousWeight != null && setEntry.previousReps != null) {
                                    "${setEntry.previousWeight}×${setEntry.previousReps}"
                                } else "—"
                                Text(prevText, Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall)

                                LargeNumberInput(
                                    value = setEntry.weight,
                                    onValueChange = { setEntry.weight = it },
                                    placeholder = "0",
                                    modifier = Modifier.weight(1.5f).height(56.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                LargeNumberInput(
                                    value = setEntry.reps,
                                    onValueChange = { setEntry.reps = it },
                                    placeholder = "0",
                                    modifier = Modifier.weight(1.5f).height(56.dp)
                                )

                                Checkbox(
                                    checked = setEntry.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        viewModel.completeSet(exIndex, setIndex, isChecked)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { viewModel.addSet(exIndex) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("+ Подход")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showExerciseDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить упражнение", fontSize = 16.sp)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showExerciseDialog = false },
            title = { Text("Выбор упражнения") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Поиск упражнения") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    val filtered = allExercises.filter {
                        (selectedGroupFilter == null || it.muscleGroup == selectedGroupFilter) &&
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(Modifier.height(250.dp)) {
                        items(filtered.size) { index ->
                            val ex = filtered[index]
                            ListItem(
                                headlineContent = { Text(ex.name) },
                                supportingContent = { Text(ex.muscleGroup.titleRu) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingContent = {
                                    IconButton(onClick = {
                                        viewModel.addExercise(ex)
                                        showExerciseDialog = false
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Не нашли нужное?", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        placeholder = { Text("Своё упражнение") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                if (newExerciseName.isNotBlank()) {
                    Button(onClick = {
                        viewModel.addCustomExercise(newExerciseName, MuscleGroup.CHEST)
                        newExerciseName = ""
                        showExerciseDialog = false
                    }) {
                        Text("Создать")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExerciseDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
