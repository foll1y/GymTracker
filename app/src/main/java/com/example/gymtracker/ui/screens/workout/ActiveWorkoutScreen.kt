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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.ui.components.LargeNumberInput
import com.example.gymtracker.ui.components.TechniqueBottomSheet
import com.example.gymtracker.ui.theme.ExpressiveSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onWorkoutFinished: () -> Unit,
    dayId: Long? = null,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val context = LocalContext.current

    LaunchedEffect(dayId) {
        viewModel.initWorkout(dayId)
    }

    var showExerciseDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var newExerciseName by remember { mutableStateOf("") }

    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }

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
                        Text(
                            text = state.programDayTitle ?: "Запись тренировки",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (state.programDayTitle != null) {
                            Text(
                                text = "Тренировка по плану",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
                .imePadding()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
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
                val ex = exerciseItem.exercise
                val isBodyweightOnly = ex.exerciseType == ExerciseType.BODYWEIGHT_ONLY
                val isWeightedBodyweight = ex.exerciseType == ExerciseType.WEIGHTED_BODYWEIGHT
                val showWeightField = !isBodyweightOnly && (!isWeightedBodyweight || exerciseItem.isWeighted)

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
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { selectedExerciseForTechnique = ex },
                                        modifier = Modifier.size(28.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Техника выполнения",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ex.muscleGroup.titleRu,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (isWeightedBodyweight) {
                                        FilterChip(
                                            selected = exerciseItem.isWeighted,
                                            onClick = { viewModel.toggleWeighted(exIndex) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                            label = {
                                                Text(
                                                    if (exerciseItem.isWeighted) "+ Доп. вес" else "Свой вес",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    } else if (isBodyweightOnly) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ) {
                                            Text(
                                                text = "Свой вес",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (exerciseItem.allTimeMaxWeight > 0f && showWeightField) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = "Рекорд: ${exerciseItem.allTimeMaxWeight} кг",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
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
                            Text("Прошлый", Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (showWeightField) {
                                Text("Вес (кг)", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Повт.", Modifier.weight(if (showWeightField) 1.2f else 2.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Готово", Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("", Modifier.weight(0.5f))
                        }

                        Spacer(Modifier.height(4.dp))

                        exerciseItem.sets.forEachIndexed { setIndex, setEntry ->
                            val currentW = setEntry.weight.toFloatOrNull()
                            val currentR = setEntry.reps.toIntOrNull()
                            val prevW = setEntry.previousWeight
                            val prevR = setEntry.previousReps
                            val histMax = setEntry.historicalMaxWeight ?: 0f

                            val isPR = showWeightField && currentW != null && currentW > 0f && histMax > 0f && currentW > histMax

                            val deltaText: String? = when {
                                showWeightField && currentW != null && prevW != null && currentW > prevW -> {
                                    val diff = ((currentW - prevW) * 10).toInt() / 10f
                                    "+$diff кг"
                                }
                                showWeightField && currentW != null && prevW != null && currentW < prevW -> {
                                    val diff = ((prevW - currentW) * 10).toInt() / 10f
                                    "-$diff кг"
                                }
                                currentR != null && prevR != null && currentR > prevR -> {
                                    "+${currentR - prevR} повт."
                                }
                                currentR != null && prevR != null && currentR < prevR -> {
                                    "-${prevR - currentR} повт."
                                }
                                else -> null
                            }
                            val isPositiveDelta = deltaText?.startsWith("+") == true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${setIndex + 1}", Modifier.weight(0.6f), fontWeight = FontWeight.Bold)

                                Column(Modifier.weight(1.3f)) {
                                    val prevText = if (showWeightField && prevW != null && prevW > 0f && prevR != null) {
                                        "${prevW}×${prevR}"
                                    } else if (prevR != null && prevR > 0) {
                                        "${prevR} повт."
                                    } else "—"
                                    Text(prevText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (isPR) {
                                        Text("🏆 Рекорд", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    } else if (deltaText != null) {
                                        Text(
                                            deltaText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPositiveDelta) ExpressiveSuccess else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                if (showWeightField) {
                                    LargeNumberInput(
                                        value = setEntry.weight,
                                        onValueChange = { viewModel.updateSetWeight(exIndex, setIndex, it) },
                                        placeholder = "0",
                                        isDecimal = true,
                                        modifier = Modifier.weight(1.5f).height(56.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }

                                LargeNumberInput(
                                    value = setEntry.reps,
                                    onValueChange = { viewModel.updateSetReps(exIndex, setIndex, it) },
                                    placeholder = "0",
                                    isDecimal = false,
                                    modifier = Modifier.weight(if (showWeightField) 1.2f else 2.5f).height(56.dp)
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

                    val existingNames = remember(allExercises) { allExercises.map { it.name.lowercase().trim() }.toSet() }
                    val onlineMatches = remember(searchQuery, selectedGroupFilter, allExercises) {
                        if (searchQuery.isNotBlank() || selectedGroupFilter != null) {
                            viewModel.searchCatalog(searchQuery, selectedGroupFilter)
                                .filter { it.name.lowercase().trim() !in existingNames }
                                .take(20)
                        } else emptyList()
                    }

                    LazyColumn(Modifier.height(260.dp)) {
                        items(filtered.size) { index ->
                            val ex = filtered[index]
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}", color = MaterialTheme.colorScheme.primary) },
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

                        if (onlineMatches.isNotEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "🌐 Найдено в полной энциклопедии (${onlineMatches.size}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(onlineMatches.size) { index ->
                                val ex = onlineMatches[index]
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}", color = MaterialTheme.colorScheme.secondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingContent = {
                                        Button(
                                            onClick = {
                                                viewModel.addOnlineExercise(ex)
                                                showExerciseDialog = false
                                                searchQuery = ""
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("В базу", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                )
                            }
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

    if (selectedExerciseForTechnique != null) {
        TechniqueBottomSheet(
            exercise = selectedExerciseForTechnique,
            onDismiss = { selectedExerciseForTechnique = null }
        )
    }
}
