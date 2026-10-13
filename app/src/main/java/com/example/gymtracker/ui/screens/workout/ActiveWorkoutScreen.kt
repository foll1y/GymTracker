package com.example.gymtracker.ui.screens.workout

import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.SetType
import com.example.gymtracker.domain.Formulas
import com.example.gymtracker.ui.components.ExerciseProgressBottomSheet
import com.example.gymtracker.ui.components.LargeNumberInput
import com.example.gymtracker.ui.components.TechniqueBottomSheet
import com.example.gymtracker.ui.theme.ExpressiveSuccess
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onWorkoutFinished: () -> Unit,
    dayId: Long? = null,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isKg by viewModel.isKg.collectAsState()
    val defaultRestSec by viewModel.defaultRestSeconds.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val unitLabel = Formulas.unitLabel(isKg)

    LaunchedEffect(dayId) {
        viewModel.initWorkout(dayId)
    }

    // Секундомер общей длительности тренировки
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(state.startTimeEpochMillis) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - state.startTimeEpochMillis) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    // Звуковой сигнал при завершении таймера отдыха
    LaunchedEffect(state.restTimerFinishedEvent) {
        if (state.restTimerFinishedEvent > 0L) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            } catch (e: Exception) {
                // Игнорируем, если аудио недоступно
            }
        }
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var showNoteField by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedEquipmentFilter by remember { mutableStateOf<String?>(null) }
    var newExerciseName by remember { mutableStateOf("") }

    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }
    var selectedExerciseForProgress by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseForSupersetPairing by remember { mutableStateOf<Int?>(null) }

    val progressHistory by remember(selectedExerciseForProgress) {
        if (selectedExerciseForProgress != null) {
            viewModel.getExerciseHistory(selectedExerciseForProgress!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    BackHandler(enabled = state.exercises.isNotEmpty() && !state.isSaving) {
        showExitDialog = true
    }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val elapsedMin = elapsedSeconds / 60
    val elapsedSec = elapsedSeconds % 60
    val formattedElapsed = String.format("%02d:%02d", elapsedMin, elapsedSec)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.exercises.isNotEmpty()) {
                                showExitDialog = true
                            } else {
                                onWorkoutFinished()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.programDayTitle ?: "Запись тренировки",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "⏱ $formattedElapsed" + if (state.programDayTitle != null) " • По плану" else " • Свободная",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNoteField = !showNoteField || state.note.isNotBlank() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Заметка к тренировке",
                            tint = if (state.note.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (state.isRestTimerRunning) {
                                viewModel.stopRestTimer()
                            } else {
                                viewModel.startRestTimer(defaultRestSec)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Таймер отдыха",
                            tint = if (state.isRestTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.saveWorkout(onWorkoutFinished) },
                        enabled = state.exercises.isNotEmpty() && !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Сохранение...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Text("Завершить", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.isRestTimerRunning) {
                RestTimerBottomBar(
                    remainingSeconds = state.restTimerRemainingSec,
                    totalSeconds = state.restTimerTotalSec,
                    onAdjustSeconds = { viewModel.adjustRestTimer(it) },
                    onSkip = { viewModel.stopRestTimer() }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                AnimatedVisibility(visible = showNoteField || state.note.isNotBlank()) {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { viewModel.updateNote(it) },
                        label = { Text("Заметка к тренировке (самочувствие, вес тела...)") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }

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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { selectedExerciseForProgress = ex }
                                    )
                                    IconButton(
                                        onClick = { selectedExerciseForTechnique = ex },
                                        modifier = Modifier.size(28.dp).padding(start = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Техника выполнения",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedExerciseForProgress = ex },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShowChart,
                                            contentDescription = "График прогресса",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (exerciseItem.supersetLabel != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🔗 " + exerciseItem.supersetLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Убрать суперсет",
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable { viewModel.removeSuperset(exIndex) },
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { exerciseForSupersetPairing = exIndex },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Объединить в суперсет",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
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

                                    if (exerciseItem.allTimeMaxWeight > 0f && showWeightField) {
                                        Text(
                                            text = "• PR: ${Formulas.formatWeight(exerciseItem.allTimeMaxWeight, isKg)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

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

                                if (exerciseItem.supersetLabel != null) {
                                    val partner = state.exercises.find { it != exerciseItem && it.supersetLabel == exerciseItem.supersetLabel }
                                    if (partner != null) {
                                        Text(
                                            text = "В связке с: " + partner.exercise.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Кнопки перемещения и удаления упражнения из тренировки
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (exIndex > 0) {
                                    IconButton(
                                        onClick = { viewModel.moveExercise(exIndex, exIndex - 1) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Вверх",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (exIndex < state.exercises.size - 1) {
                                    IconButton(
                                        onClick = { viewModel.moveExercise(exIndex, exIndex + 1) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Вниз",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeExercise(exIndex) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Убрать упражнение",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
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
                            Text("Сет", Modifier.weight(0.7f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Прошлый", Modifier.weight(1.3f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (showWeightField) {
                                Text("Вес ($unitLabel)", Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Повт.", Modifier.weight(if (showWeightField) 1.2f else 2.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Готово", Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("", Modifier.weight(0.5f))
                        }

                        Spacer(Modifier.height(4.dp))

                        exerciseItem.sets.forEachIndexed { setIndex, setEntry ->
                            val currentInputW = setEntry.weight.toFloatOrNull()
                            val currentWeightKg = currentInputW?.let { Formulas.toKg(it, isKg) }
                            val currentR = setEntry.reps.toIntOrNull()
                            val prevWKg = setEntry.previousWeight
                            val prevR = setEntry.previousReps
                            val histMaxKg = setEntry.historicalMaxWeight ?: 0f

                            val isPR = showWeightField &&
                                setEntry.setType != SetType.WARMUP &&
                                currentWeightKg != null &&
                                currentWeightKg > 0f &&
                                histMaxKg > 0f &&
                                currentWeightKg > histMaxKg + 0.05f

                            val deltaText: String? = when {
                                showWeightField && currentWeightKg != null && prevWKg != null && kotlin.math.abs(currentWeightKg - prevWKg) >= 0.1f -> {
                                    val diffDisplay = Formulas.convertWeight(kotlin.math.abs(currentWeightKg - prevWKg), isKg)
                                    val sign = if (currentWeightKg > prevWKg) "+" else "-"
                                    "$sign${Formulas.formatNumber(diffDisplay)} $unitLabel"
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
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .padding(end = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Surface(
                                        onClick = { menuExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (setEntry.setType) {
                                            SetType.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest
                                            SetType.WARMUP -> Color(0xFF5A4D1A)
                                            SetType.DROP -> Color(0xFF4A148C)
                                            SetType.FAILURE -> Color(0xFF7F0000)
                                        }
                                    ) {
                                        val label = if (setEntry.setType == SetType.NORMAL) "${setIndex + 1}" else setEntry.setType.badge
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = when (setEntry.setType) {
                                                SetType.NORMAL -> MaterialTheme.colorScheme.onSurface
                                                SetType.WARMUP -> Color(0xFFFFD54F)
                                                SetType.DROP -> Color(0xFFE1BEE7)
                                                SetType.FAILURE -> Color(0xFFFF8A80)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Обычный подход (1, 2...)") },
                                            onClick = {
                                                viewModel.updateSetType(exIndex, setIndex, SetType.NORMAL)
                                                menuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("W — Разминочный (Warmup)") },
                                            onClick = {
                                                viewModel.updateSetType(exIndex, setIndex, SetType.WARMUP)
                                                menuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("D — Дропсет (Drop-set)") },
                                            onClick = {
                                                viewModel.updateSetType(exIndex, setIndex, SetType.DROP)
                                                menuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("F — До отказа (Failure)") },
                                            onClick = {
                                                viewModel.updateSetType(exIndex, setIndex, SetType.FAILURE)
                                                menuExpanded = false
                                            }
                                        )
                                    }
                                }

                                Column(Modifier.weight(1.3f)) {
                                    val prevText = if (showWeightField && prevWKg != null && prevWKg > 0f && prevR != null) {
                                        "${Formulas.formatNumber(Formulas.convertWeight(prevWKg, isKg))}×$prevR"
                                    } else if (prevR != null && prevR > 0) {
                                        "$prevR повт."
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
                                    val placeholderW = if (prevWKg != null && prevWKg > 0f) {
                                        Formulas.formatNumber(Formulas.convertWeight(prevWKg, isKg))
                                    } else "0"
                                    LargeNumberInput(
                                        value = setEntry.weight,
                                        onValueChange = { viewModel.updateSetWeight(exIndex, setIndex, it) },
                                        placeholder = placeholderW,
                                        isDecimal = true,
                                        modifier = Modifier.weight(1.5f).height(56.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }

                                val placeholderR = if (prevR != null && prevR > 0) prevR.toString() else "0"
                                LargeNumberInput(
                                    value = setEntry.reps,
                                    onValueChange = { viewModel.updateSetReps(exIndex, setIndex, it) },
                                    placeholder = placeholderR,
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

    // Диалог выхода из активной тренировки
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Выйти из тренировки?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Черновик тренировки сохранён автоматически — вы сможете продолжить её с главного экрана в любой момент или полностью отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onWorkoutFinished()
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Свернуть (сохранить черновик)")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            viewModel.discardWorkout(onWorkoutFinished)
                        }
                    ) {
                        Text("Удалить черновик", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Остаться")
                    }
                }
            }
        )
    }

    if (showExerciseDialog) {
        val equipmentOptions = remember { viewModel.getAvailableEquipment() }

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
                        label = { Text("Поиск (рус / англ / снаряд)") },
                        placeholder = { Text("Например: жим, bench, гантели...") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { selectedGroupFilter = null },
                                shape = RoundedCornerShape(14.dp),
                                label = { Text("Все мышцы") }
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

                    if (equipmentOptions.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedEquipmentFilter == null,
                                    onClick = { selectedEquipmentFilter = null },
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text("Любой инвентарь", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            items(equipmentOptions) { eq ->
                                FilterChip(
                                    selected = selectedEquipmentFilter == eq,
                                    onClick = {
                                        selectedEquipmentFilter = if (selectedEquipmentFilter == eq) null else eq
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text(eq, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val cleanQuery = searchQuery.trim().lowercase()
                    val filtered = allExercises.filter { ex ->
                        val matchesGroup = selectedGroupFilter == null || ex.muscleGroup == selectedGroupFilter
                        val matchesEquipment = selectedEquipmentFilter == null ||
                            ex.equipment.equals(selectedEquipmentFilter, ignoreCase = true)
                        val matchesSearch = cleanQuery.isEmpty() ||
                            ex.name.lowercase().contains(cleanQuery) ||
                            ex.nameEn.lowercase().contains(cleanQuery) ||
                            ex.muscleGroup.titleRu.lowercase().contains(cleanQuery) ||
                            ex.equipment.lowercase().contains(cleanQuery)
                        matchesGroup && matchesEquipment && matchesSearch
                    }

                    val existingNames = remember(allExercises) { allExercises.map { it.name.lowercase().trim() }.toSet() }
                    val onlineMatches = remember(searchQuery, selectedGroupFilter, selectedEquipmentFilter, allExercises) {
                        if (searchQuery.isNotBlank() || selectedGroupFilter != null || selectedEquipmentFilter != null) {
                            viewModel.searchCatalog(searchQuery, selectedGroupFilter, selectedEquipmentFilter)
                                .filter { it.name.lowercase().trim() !in existingNames }
                                .take(25)
                        } else emptyList()
                    }

                    LazyColumn(Modifier.height(250.dp)) {
                        items(filtered.size) { index ->
                            val ex = filtered[index]
                            val eqSuffix = if (ex.equipment.isNotBlank()) " • ${ex.equipment}" else ""
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = {
                                    Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}$eqSuffix", color = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.addExercise(ex)
                                    showExerciseDialog = false
                                    searchQuery = ""
                                },
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
                                val eqSuffix = if (ex.equipment.isNotBlank()) " • ${ex.equipment}" else ""
                                ListItem(
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = {
                                        Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}$eqSuffix", color = MaterialTheme.colorScheme.secondary)
                                    },
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

    if (selectedExerciseForProgress != null) {
        ExerciseProgressBottomSheet(
            exercise = selectedExerciseForProgress,
            historyEntries = progressHistory,
            isKg = isKg,
            onDismiss = { selectedExerciseForProgress = null }
        )
    }

    if (exerciseForSupersetPairing != null) {
        val currentEx = state.exercises.getOrNull(exerciseForSupersetPairing!!)
        AlertDialog(
            onDismissRequest = { exerciseForSupersetPairing = null },
            title = { Text("Объединить в суперсет") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Выберите упражнение для связки с «" + (currentEx?.exercise?.name ?: "") + "»:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    val availableOthers = state.exercises.mapIndexed { idx, it -> Pair(idx, it) }
                        .filter { it.first != exerciseForSupersetPairing }

                    if (availableOthers.isEmpty()) {
                        Text(
                            "В тренировке нет других упражнений для объединения.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        availableOthers.forEach { (idx, other) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setSuperset(exerciseForSupersetPairing!!, idx)
                                        exerciseForSupersetPairing = null
                                    },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(other.exercise.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text(other.exercise.muscleGroup.titleRu, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (other.supersetLabel != null) {
                                        Text("(" + other.supersetLabel + ")", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exerciseForSupersetPairing = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun RestTimerBottomBar(
    remainingSeconds: Int,
    totalSeconds: Int,
    onAdjustSeconds: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val timeText = String.format("%02d:%02d", mins, secs)
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Отдых: $timeText",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Таймер между подходами",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onAdjustSeconds(-15) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("-15с", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAdjustSeconds(30) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+30с", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Сброс", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
