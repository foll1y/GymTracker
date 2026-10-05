package com.example.gymtracker.ui.screens.workout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    Text("Запись тренировки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveWorkout(onWorkoutFinished) },
                        enabled = state.exercises.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Завершить", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            if (state.exercises.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Упражнения ещё не добавлены.\nНажмите кнопку ниже, чтобы начать.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            itemsIndexed(state.exercises) { exIndex, exerciseItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = exerciseItem.exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = exerciseItem.exercise.muscleGroup.titleRu,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Заголовки таблицы подходов
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Сет", Modifier.weight(0.6f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Прошлый", Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Вес (кг)", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Повт.", Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Готово", Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("", Modifier.weight(0.5f))
                        }

                        Spacer(Modifier.height(4.dp))

                        exerciseItem.sets.forEachIndexed { setIndex, setEntry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${setIndex + 1}", Modifier.weight(0.6f), fontWeight = FontWeight.Bold)

                                val prevText = if (setEntry.previousWeight != null && setEntry.previousReps != null) {
                                    "${setEntry.previousWeight}×${setEntry.previousReps}"
                                } else "—"
                                Text(prevText, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                LargeNumberInput(
                                    value = setEntry.weight,
                                    onValueChange = { viewModel.updateSetWeight(exIndex, setIndex, it) },
                                    placeholder = "0",
                                    isDecimal = true,
                                    modifier = Modifier.weight(1.5f).height(56.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                LargeNumberInput(
                                    value = setEntry.reps,
                                    onValueChange = { viewModel.updateSetReps(exIndex, setIndex, it) },
                                    placeholder = "0",
                                    isDecimal = false,
                                    modifier = Modifier.weight(1.3f).height(56.dp)
                                )

                                Checkbox(
                                    checked = setEntry.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        viewModel.completeSet(exIndex, setIndex, isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(0.9f)
                                )

                                IconButton(
                                    onClick = { viewModel.removeSet(exIndex, setIndex) },
                                    modifier = Modifier.weight(0.5f).size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Удалить подход",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.addSet(exIndex) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("+ Подход", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { showExerciseDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить упражнение", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showExerciseDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Выбор упражнения", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Поиск (название или группа мышц)") },
                        placeholder = { Text("Например: спина, жим...") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { selectedGroupFilter = null },
                                shape = RoundedCornerShape(14.dp),
                                label = { Text("Все") }
                            )
                        }
                        items(MuscleGroup.values()) { group ->
                            FilterChip(
                                selected = selectedGroupFilter == group,
                                onClick = {
                                    selectedGroupFilter = if (selectedGroupFilter == group) null else group
                                },
                                shape = RoundedCornerShape(14.dp),
                                label = { Text(group.titleRu) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val cleanQuery = searchQuery.trim().lowercase()
                    val filtered = allExercises.filter { ex ->
                        val matchesGroup = selectedGroupFilter == null || ex.muscleGroup == selectedGroupFilter
                        val matchesSearch = cleanQuery.isEmpty() ||
                                ex.name.lowercase().contains(cleanQuery) ||
                                ex.muscleGroup.titleRu.lowercase().contains(cleanQuery)
                        matchesGroup && matchesSearch
                    }

                    LazyColumn(Modifier.height(240.dp)) {
                        items(filtered.size) { index ->
                            val ex = filtered[index]
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(ex.muscleGroup.titleRu, color = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingContent = {
                                    FilledIconButton(
                                        onClick = {
                                            viewModel.addExercise(ex)
                                            showExerciseDialog = false
                                            searchQuery = ""
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Выбрать")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Создать своё упражнение:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        placeholder = { Text("Название упражнения") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                if (newExerciseName.isNotBlank()) {
                    Button(
                        onClick = {
                            viewModel.addCustomExercise(newExerciseName, selectedGroupFilter ?: MuscleGroup.CHEST)
                            newExerciseName = ""
                            showExerciseDialog = false
                            searchQuery = ""
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Создать и добавить")
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
