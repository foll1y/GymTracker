package com.example.gymtracker.ui.screens.programs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.ProgramDayExerciseWithExercise
import com.example.gymtracker.domain.Formulas
import com.example.gymtracker.ui.components.TechniqueBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    onStartWorkoutDay: (Long) -> Unit,
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val programs by viewModel.programs.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val isKg by viewModel.isKg.collectAsState()
    val unitLabel = Formulas.unitLabel(isKg)
    val coroutineScope = rememberCoroutineScope()

    var selectedProgramIndex by remember { mutableStateOf(0) }
    var selectedDayIndex by remember { mutableStateOf(0) }

    var showCreateProgramDialog by remember { mutableStateOf(false) }
    var newProgramTitle by remember { mutableStateOf("") }
    var newProgramDesc by remember { mutableStateOf("") }

    var showEditProgramDialog by remember { mutableStateOf(false) }
    var editProgramTitle by remember { mutableStateOf("") }
    var editProgramDesc by remember { mutableStateOf("") }

    var showDeleteProgramDialog by remember { mutableStateOf(false) }
    var showDeleteDayDialog by remember { mutableStateOf(false) }

    var showAddDayDialog by remember { mutableStateOf(false) }
    var newDayName by remember { mutableStateOf("") }

    var showRenameDayDialog by remember { mutableStateOf(false) }
    var renameDayInput by remember { mutableStateOf("") }

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var selectedExerciseForAdd by remember { mutableStateOf<ExerciseEntity?>(null) }
    var targetSetsInput by remember { mutableStateOf("3") }
    var targetRepsInput by remember { mutableStateOf("8-12") }
    var targetWeightInput by remember { mutableStateOf("") }

    var editingPlanExercise by remember { mutableStateOf<ProgramDayExerciseWithExercise?>(null) }
    var editSetsInput by remember { mutableStateOf("3") }
    var editRepsInput by remember { mutableStateOf("8-12") }
    var editWeightInput by remember { mutableStateOf("") }

    var exerciseSearchQuery by remember { mutableStateOf("") }
    var filterMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var filterEquipment by remember { mutableStateOf<String?>(null) }
    var customExName by remember { mutableStateOf("") }

    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }
    var planExerciseForSupersetPairing by remember { mutableStateOf<Long?>(null) }

    val currentProgram = programs.getOrNull(selectedProgramIndex) ?: programs.firstOrNull()
    val currentDays = currentProgram?.days?.sortedBy { it.day.orderIndex } ?: emptyList()
    val currentDay = currentDays.getOrNull(selectedDayIndex) ?: currentDays.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Планы и программы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { showCreateProgramDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Создать программу", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (programs.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(programs) { index, p ->
                            FilterChip(
                                selected = index == selectedProgramIndex,
                                onClick = {
                                    selectedProgramIndex = index
                                    selectedDayIndex = 0
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                label = { Text(p.program.title, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            if (currentProgram == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "У вас пока нет созданных программ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Создайте свой первый тренировочный сплит или используйте готовые шаблоны.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateProgramDialog = true },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Создать программу")
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = currentProgram.program.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentProgram.program.description.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = currentProgram.program.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentProgram.program.isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Активная",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.setActiveProgram(currentProgram.program.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Выбрать", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            editProgramTitle = currentProgram.program.title
                                            editProgramDesc = currentProgram.program.description
                                            showEditProgramDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Редактировать программу",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { showDeleteProgramDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить программу",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Дни программы (${currentDays.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showAddDayDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Добавить день")
                                }
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(currentDays) { dIndex, d ->
                                    FilterChip(
                                        selected = dIndex == selectedDayIndex,
                                        onClick = { selectedDayIndex = dIndex },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ),
                                        label = { Text(d.day.name, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (currentDay != null) {
                    item {
                        Button(
                            onClick = { onStartWorkoutDay(currentDay.day.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Начать тренировку (${currentDay.day.name})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Упражнения дня (${currentDay.exercises.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        renameDayInput = currentDay.day.name
                                        showRenameDayDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Переименовать день", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { showDeleteDayDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить день", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (currentDay.exercises.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "В этом дне ещё нет упражнений",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { showAddExerciseDialog = true },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Добавить упражнение в план")
                                    }
                                }
                            }
                        }
                    } else {
                        val sortedExercises = currentDay.exercises.sortedBy { it.planExercise.orderIndex }
                        itemsIndexed(sortedExercises, key = { _, item -> item.planExercise.id }) { exIndex, planEx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = planEx.exercise.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = planEx.exercise.muscleGroup.titleRu,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        text = planEx.exercise.exerciseType.titleRu,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (planEx.planExercise.supersetLabel != null) {
                                                val partner = sortedExercises.find {
                                                    it.planExercise.id != planEx.planExercise.id &&
                                                        it.planExercise.supersetLabel == planEx.planExercise.supersetLabel
                                                }
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

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (planEx.planExercise.supersetLabel != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "🔗 " + planEx.planExercise.supersetLabel,
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
                                                                .clickable { viewModel.removeSupersetForDayExercise(planEx.planExercise) },
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { planExerciseForSupersetPairing = planEx.planExercise.id },
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

                                            if (exIndex > 0) {
                                                IconButton(
                                                    onClick = { viewModel.reorderExercisesInDay(currentDay.day.id, exIndex, exIndex - 1) },
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
                                            if (exIndex < sortedExercises.size - 1) {
                                                IconButton(
                                                    onClick = { viewModel.reorderExercisesInDay(currentDay.day.id, exIndex, exIndex + 1) },
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
                                                onClick = { selectedExerciseForTechnique = planEx.exercise },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Техника выполнения",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeExerciseFromDay(planEx.planExercise) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Удалить",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Кликабельная плашка плана (открывает редактирование подходов/веса)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                editingPlanExercise = planEx
                                                editSetsInput = planEx.planExercise.targetSets.toString()
                                                editRepsInput = planEx.planExercise.targetReps
                                                val wKg = planEx.planExercise.targetWeightKg
                                                editWeightInput = if (wKg != null && wKg > 0f) {
                                                    Formulas.formatNumber(Formulas.convertWeight(wKg, isKg))
                                                } else ""
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "План: ${planEx.planExercise.targetSets} подх. × ${planEx.planExercise.targetReps} повт.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Изменить план упражнения",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (planEx.planExercise.targetWeightKg != null && planEx.planExercise.targetWeightKg > 0f) {
                                                Text(
                                                    text = "Цель: ${Formulas.formatWeight(planEx.planExercise.targetWeightKg, isKg)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { showAddExerciseDialog = true },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("+ Добавить упражнение в план", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }

    // 1. Диалог создания программы
    if (showCreateProgramDialog) {
        AlertDialog(
            onDismissRequest = { showCreateProgramDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Новая программа", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newProgramTitle,
                        onValueChange = { newProgramTitle = it },
                        label = { Text("Название (например: Сплит на массу)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProgramDesc,
                        onValueChange = { newProgramDesc = it },
                        label = { Text("Описание (опционально)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProgramTitle.isNotBlank()) {
                            viewModel.createProgram(newProgramTitle, newProgramDesc)
                            newProgramTitle = ""
                            newProgramDesc = ""
                            showCreateProgramDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProgramDialog = false }) { Text("Отмена") }
            }
        )
    }

    // 1b. Диалог редактирования программы
    if (showEditProgramDialog && currentProgram != null) {
        AlertDialog(
            onDismissRequest = { showEditProgramDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Редактировать программу", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editProgramTitle,
                        onValueChange = { editProgramTitle = it },
                        label = { Text("Название программы") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editProgramDesc,
                        onValueChange = { editProgramDesc = it },
                        label = { Text("Описание") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editProgramTitle.isNotBlank()) {
                            viewModel.updateProgram(currentProgram.program, editProgramTitle, editProgramDesc)
                            showEditProgramDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProgramDialog = false }) { Text("Отмена") }
            }
        )
    }

    // 2. Диалог подтверждения удаления программы
    if (showDeleteProgramDialog && currentProgram != null) {
        AlertDialog(
            onDismissRequest = { showDeleteProgramDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Удалить программу?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Вы уверены, что хотите удалить программу «${currentProgram.program.title}» и все входящие в неё тренировочные дни?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProgram(currentProgram.program)
                        selectedProgramIndex = 0
                        selectedDayIndex = 0
                        showDeleteProgramDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProgramDialog = false }) { Text("Отмена") }
            }
        )
    }

    // 2b. Диалог подтверждения удаления дня
    if (showDeleteDayDialog && currentDay != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDayDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Удалить тренировочный день?", fontWeight = FontWeight.Bold) },
            text = {
                Text("День «${currentDay.day.name}» и все запланированные в нём упражнения будут удалены.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProgramDay(currentDay.day)
                        selectedDayIndex = 0
                        showDeleteDayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDayDialog = false }) { Text("Отмена") }
            }
        )
    }

    // 3. Диалог добавления дня в программу
    if (showAddDayDialog && currentProgram != null) {
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Добавить тренировочный день", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newDayName,
                    onValueChange = { newDayName = it },
                    label = { Text("Название дня (например: Спина и бицепс)") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDayName.isNotBlank()) {
                            viewModel.addDayToProgram(currentProgram.program.id, newDayName)
                            newDayName = ""
                            showAddDayDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDayDialog = false }) { Text("Отмена") }
            }
        )
    }

    // 3b. Диалог переименования дня
    if (showRenameDayDialog && currentDay != null) {
        AlertDialog(
            onDismissRequest = { showRenameDayDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Переименовать день", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameDayInput,
                    onValueChange = { renameDayInput = it },
                    label = { Text("Название тренировочного дня") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDayInput.isNotBlank()) {
                            viewModel.renameProgramDay(currentDay.day, renameDayInput)
                            showRenameDayDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDayDialog = false }) { Text("Отмена") }
            }
        )
    }

    // 4. Диалог редактирования параметров упражнения в плане
    if (editingPlanExercise != null) {
        val item = editingPlanExercise!!
        AlertDialog(
            onDismissRequest = { editingPlanExercise = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Изменить план упражнения", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = editSetsInput,
                        onValueChange = { editSetsInput = it },
                        label = { Text("Количество подходов") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editRepsInput,
                        onValueChange = { editRepsInput = it },
                        label = { Text("Диапазон повторений (например: 8-12)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (item.exercise.exerciseType != ExerciseType.BODYWEIGHT_ONLY) {
                        OutlinedTextField(
                            value = editWeightInput,
                            onValueChange = { editWeightInput = it.replace(',', '.') },
                            label = { Text("Целевой вес ($unitLabel, опционально)") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sets = editSetsInput.toIntOrNull()?.coerceIn(1, 20) ?: item.planExercise.targetSets
                        val reps = editRepsInput.trim().ifBlank { item.planExercise.targetReps }
                        val rawW = editWeightInput.toFloatOrNull()
                        val weightKg = if (rawW != null && rawW > 0f) Formulas.toKg(rawW, isKg) else null
                        viewModel.updateExerciseInDay(
                            item.planExercise.copy(
                                targetSets = sets,
                                targetReps = reps,
                                targetWeightKg = weightKg
                            )
                        )
                        editingPlanExercise = null
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlanExercise = null }) { Text("Отмена") }
            }
        )
    }

    // 5. Диалог выбора и настройки упражнения в план
    if (showAddExerciseDialog && currentDay != null) {
        val equipmentOptions = remember { viewModel.getAvailableEquipment() }

        AlertDialog(
            onDismissRequest = {
                showAddExerciseDialog = false
                selectedExerciseForAdd = null
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = if (selectedExerciseForAdd == null) "Выберите упражнение" else "Настройка подходов",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (selectedExerciseForAdd == null) {
                        OutlinedTextField(
                            value = exerciseSearchQuery,
                            onValueChange = { exerciseSearchQuery = it },
                            label = { Text("Поиск (рус / англ / инвентарь)") },
                            shape = RoundedCornerShape(14.dp),
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
                                    selected = filterMuscleGroup == null,
                                    onClick = { filterMuscleGroup = null },
                                    label = { Text("Все мышцы") }
                                )
                            }
                            items(MuscleGroup.values()) { g ->
                                FilterChip(
                                    selected = filterMuscleGroup == g,
                                    onClick = { filterMuscleGroup = if (filterMuscleGroup == g) null else g },
                                    label = { Text(g.titleRu) }
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
                                        selected = filterEquipment == null,
                                        onClick = { filterEquipment = null },
                                        label = { Text("Любой инвентарь", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                items(equipmentOptions) { eq ->
                                    FilterChip(
                                        selected = filterEquipment == eq,
                                        onClick = { filterEquipment = if (filterEquipment == eq) null else eq },
                                        label = { Text(eq, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val q = exerciseSearchQuery.trim().lowercase()
                        val filteredList = allExercises.filter { ex ->
                            val matchGroup = filterMuscleGroup == null || ex.muscleGroup == filterMuscleGroup
                            val matchEq = filterEquipment == null || ex.equipment.equals(filterEquipment, ignoreCase = true)
                            val matchQ = q.isEmpty() ||
                                ex.name.lowercase().contains(q) ||
                                ex.nameEn.lowercase().contains(q) ||
                                ex.muscleGroup.titleRu.lowercase().contains(q) ||
                                ex.equipment.lowercase().contains(q)
                            matchGroup && matchEq && matchQ
                        }

                        val existingNames = remember(allExercises) { allExercises.map { it.name.lowercase().trim() }.toSet() }
                        val onlineMatches = remember(exerciseSearchQuery, filterMuscleGroup, filterEquipment, allExercises) {
                            if (exerciseSearchQuery.isNotBlank() || filterMuscleGroup != null || filterEquipment != null) {
                                viewModel.searchCatalog(exerciseSearchQuery, filterMuscleGroup, filterEquipment)
                                    .filter { it.name.lowercase().trim() !in existingNames }
                                    .take(25)
                            } else emptyList()
                        }

                        LazyColumn(Modifier.height(230.dp)) {
                            items(filteredList.size) { i ->
                                val ex = filteredList[i]
                                val eqSuffix = if (ex.equipment.isNotBlank()) " • ${ex.equipment}" else ""
                                ListItem(
                                    headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}$eqSuffix") },
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedExerciseForAdd = ex
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
                                items(onlineMatches.size) { i ->
                                    val ex = onlineMatches[i]
                                    val eqSuffix = if (ex.equipment.isNotBlank()) " • ${ex.equipment}" else ""
                                    ListItem(
                                        headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                        supportingContent = {
                                            Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}$eqSuffix", color = MaterialTheme.colorScheme.secondary)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingContent = {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val savedEx = viewModel.importCatalogExercise(ex)
                                                        selectedExerciseForAdd = savedEx
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("В план", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Создать своё упражнение:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customExName,
                            onValueChange = { customExName = it },
                            placeholder = { Text("Название упражнения") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (customExName.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val id = viewModel.createCustomExercise(customExName, filterMuscleGroup ?: MuscleGroup.CHEST)
                                        selectedExerciseForAdd = ExerciseEntity(id = id, name = customExName.trim(), muscleGroup = filterMuscleGroup ?: MuscleGroup.CHEST, isCustom = true)
                                        customExName = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Создать и настроить")
                            }
                        }
                    } else {
                        val ex = selectedExerciseForAdd!!
                        Text(ex.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(ex.muscleGroup.titleRu, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = targetSetsInput,
                            onValueChange = { targetSetsInput = it },
                            label = { Text("Количество подходов") },
                            placeholder = { Text("Например: 3") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = targetRepsInput,
                            onValueChange = { targetRepsInput = it },
                            label = { Text("Диапазон повторений") },
                            placeholder = { Text("Например: 8-12 или 10") },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (ex.exerciseType != ExerciseType.BODYWEIGHT_ONLY) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = targetWeightInput,
                                onValueChange = { targetWeightInput = it.replace(',', '.') },
                                label = { Text("Целевой вес ($unitLabel, опционально)") },
                                placeholder = { Text("Например: 80") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedExerciseForAdd != null) {
                    Button(
                        onClick = {
                            val sets = targetSetsInput.toIntOrNull() ?: 3
                            val reps = if (targetRepsInput.isNotBlank()) targetRepsInput.trim() else "8-12"
                            val rawW = targetWeightInput.toFloatOrNull()
                            val weightKg = if (rawW != null && rawW > 0f) Formulas.toKg(rawW, isKg) else null
                            viewModel.addExerciseToDay(
                                dayId = currentDay.day.id,
                                exerciseId = selectedExerciseForAdd!!.id,
                                targetSets = sets,
                                targetReps = reps,
                                targetWeight = weightKg
                            )
                            selectedExerciseForAdd = null
                            showAddExerciseDialog = false
                            targetWeightInput = ""
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Добавить в план")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (selectedExerciseForAdd != null) {
                        selectedExerciseForAdd = null
                    } else {
                        showAddExerciseDialog = false
                    }
                }) {
                    Text(if (selectedExerciseForAdd != null) "Назад к списку" else "Закрыть")
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

    if (planExerciseForSupersetPairing != null && currentDay != null) {
        val currentPlanEx = currentDay.exercises.find { it.planExercise.id == planExerciseForSupersetPairing }
        val availableOthers = currentDay.exercises.filter { it.planExercise.id != planExerciseForSupersetPairing }

        AlertDialog(
            onDismissRequest = { planExerciseForSupersetPairing = null },
            title = { Text("Объединить в суперсет") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Выберите упражнение для связки в суперсет с «" + (currentPlanEx?.exercise?.name ?: "") + "»:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))

                    if (availableOthers.isEmpty()) {
                        Text(
                            "В плане этого дня нет других упражнений для объединения.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        availableOthers.forEach { other ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setSupersetForDayExercises(
                                            currentDay.day.id,
                                            planExerciseForSupersetPairing!!,
                                            other.planExercise.id
                                        )
                                        planExerciseForSupersetPairing = null
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
                                    if (other.planExercise.supersetLabel != null) {
                                        Text("(" + other.planExercise.supersetLabel + ")", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { planExerciseForSupersetPairing = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
