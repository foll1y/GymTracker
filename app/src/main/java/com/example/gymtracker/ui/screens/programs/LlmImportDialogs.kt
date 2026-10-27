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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmImportFlowDialog(
    initialMode: LlmImportTargetMode,
    hasCurrentProgram: Boolean,
    currentProgramTitle: String?,
    hasCurrentDay: Boolean,
    currentDayName: String?,
    allExercises: List<ExerciseEntity>,
    isKg: Boolean,
    unitLabel: String,
    onParseText: (String) -> ParsedProgramPlan,
    onSearchCatalog: (String, MuscleGroup?) -> List<ExerciseEntity>,
    onConfirmSave: (ParsedProgramPlan, LlmImportTargetMode, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    var selectedMode by remember { mutableStateOf(initialMode) }
    var rawTextInput by remember { mutableStateOf("") }
    var promptCopiedBanner by remember { mutableStateOf(false) }
    var parseErrorText by remember { mutableStateOf<String?>(null) }

    var parsedPlan by remember { mutableStateOf<ParsedProgramPlan?>(null) }
    var setAsActiveProgram by remember { mutableStateOf(true) }

    // Состояния для модалок редактирования внутри экрана предпросмотра
    var replacingTarget by remember { mutableStateOf<Pair<String, ParsedExerciseItem>?>(null) } // dayId to exerciseItem
    var editingParamsTarget by remember { mutableStateOf<Pair<String, ParsedExerciseItem>?>(null) } // dayId to exerciseItem
    var renamingDayTarget by remember { mutableStateOf<ParsedDayPlan?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            if (parsedPlan == null) {
                // ==========================================
                // ШАГ 1: ВСТАВКА ОТВЕТА LLM И КОПИРОВАНИЕ ПРОМПТА
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Импорт плана из LLM",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ChatGPT, Claude, Gemini, DeepSeek или любой текст",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Выбор режима сохранения
                    Text(
                        text = "Куда импортировать:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedMode == LlmImportTargetMode.NEW_PROGRAM,
                                onClick = { selectedMode = LlmImportTargetMode.NEW_PROGRAM },
                                shape = RoundedCornerShape(14.dp),
                                label = { Text("Новая программа") }
                            )
                        }
                        if (hasCurrentProgram) {
                            item {
                                FilterChip(
                                    selected = selectedMode == LlmImportTargetMode.ADD_DAYS_TO_PROGRAM,
                                    onClick = { selectedMode = LlmImportTargetMode.ADD_DAYS_TO_PROGRAM },
                                    shape = RoundedCornerShape(14.dp),
                                    label = {
                                        Text(
                                            text = if (!currentProgramTitle.isNullOrBlank())
                                                "Дни в «$currentProgramTitle»"
                                            else "Дни в текущую программу"
                                        )
                                    }
                                )
                            }
                        }
                        if (hasCurrentDay) {
                            item {
                                FilterChip(
                                    selected = selectedMode == LlmImportTargetMode.ADD_TO_CURRENT_DAY,
                                    onClick = { selectedMode = LlmImportTargetMode.ADD_TO_CURRENT_DAY },
                                    shape = RoundedCornerShape(14.dp),
                                    label = {
                                        Text(
                                            text = if (!currentDayName.isNullOrBlank())
                                                "Упр. в «$currentDayName»"
                                            else "В текущий день"
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Карточка с готовым промптом для LLM
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = "💡 Совет: скопируйте готовый промпт для нейросети, чтобы она выдала план в идеальном формате, или просто вставьте уже готовый ответ из чата.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(LlmPlanParser.LLM_PROMPT_TEMPLATE))
                                        promptCopiedBanner = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Скопировать промпт", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            rawTextInput = clip
                                            parseErrorText = null
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Вставить ответ", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (promptCopiedBanner) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "✅ Промпт скопирован в буфер обмена! Отправьте его в чат с LLM и вставьте полученный ответ ниже.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rawTextInput,
                        onValueChange = {
                            rawTextInput = it
                            parseErrorText = null
                        },
                        label = { Text("Текст ответа от LLM") },
                        placeholder = {
                            Text(
                                "Например:\nДень 1 — Грудь и Трицепс\n1. Жим штанги лёжа — 4x8-10 (70 кг)\n2. Разводка гантелей — 3x12-15\n..."
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    if (parseErrorText != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = parseErrorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                if (rawTextInput.isBlank()) {
                                    parseErrorText = "Вставьте текст плана тренировок из ответа нейросети"
                                } else {
                                    val result = onParseText(rawTextInput)
                                    val totalEx = result.days.sumOf { it.exercises.size }
                                    if (totalEx == 0) {
                                        parseErrorText = "Не удалось найти упражнения в тексте. Проверьте, что в строках указаны упражнения и подходы (например: 1. Жим лёжа — 4x10)."
                                    } else {
                                        parsedPlan = result
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1.4f).height(50.dp)
                        ) {
                            Text("Распознать и проверить →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ==========================================
                // ШАГ 2: ЭКРАН ПРЕДПРОСМОТРА И РЕДАКТИРОВАНИЯ
                // ==========================================
                val currentPlan = parsedPlan!!
                val totalExercisesCount = currentPlan.days.sumOf { it.exercises.size }
                val matchedCount = currentPlan.days.sumOf { d ->
                    d.exercises.count { it.matchSource != ExerciseMatchSource.NEW_CUSTOM }
                }
                val newCustomCount = totalExercisesCount - matchedCount

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Предпросмотр плана",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Распознано: ${currentPlan.days.size} дн. • $totalExercisesCount упр. (в базе: $matchedCount, новых: $newCustomCount)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Если создаётся новая программа — показываем поля названия, описания и чекбокс «Сделать активной»
                        if (selectedMode == LlmImportTargetMode.NEW_PROGRAM) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = currentPlan.title,
                                            onValueChange = { parsedPlan = currentPlan.copy(title = it) },
                                            label = { Text("Название программы") },
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = currentPlan.description,
                                            onValueChange = { parsedPlan = currentPlan.copy(description = it) },
                                            label = { Text("Описание (опционально)") },
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { setAsActiveProgram = !setAsActiveProgram }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Checkbox(
                                                checked = setAsActiveProgram,
                                                onCheckedChange = { setAsActiveProgram = it }
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Сделать эту программу активной",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Карточки распознанных дней
                        itemsIndexed(currentPlan.days, key = { _, d -> d.id }) { dayIndex, dayPlan ->
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { renamingDayTarget = dayPlan }
                                        ) {
                                            Text(
                                                text = if (selectedMode == LlmImportTargetMode.ADD_TO_CURRENT_DAY)
                                                    "В день «${currentDayName ?: dayPlan.name}» (${dayPlan.exercises.size} упр.)"
                                                else dayPlan.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (selectedMode != LlmImportTargetMode.ADD_TO_CURRENT_DAY) {
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Переименовать день",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (currentPlan.days.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    parsedPlan = currentPlan.copy(
                                                        days = currentPlan.days.filterNot { it.id == dayPlan.id }
                                                    )
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Удалить день",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        dayPlan.exercises.forEachIndexed { exIdx, exItem ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${exIdx + 1}. ${exItem.displayName}",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            if (!exItem.displayName.equals(exItem.rawName, ignoreCase = true)) {
                                                                Text(
                                                                    text = "В тексте LLM: «${exItem.rawName}»",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            Spacer(Modifier.height(4.dp))
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                // Бейдж источника (В базе / В каталоге / Новое)
                                                                val badgeContainer = when (exItem.matchSource) {
                                                                    ExerciseMatchSource.LOCAL_DB -> MaterialTheme.colorScheme.primaryContainer
                                                                    ExerciseMatchSource.ONLINE_CATALOG -> MaterialTheme.colorScheme.secondaryContainer
                                                                    ExerciseMatchSource.NEW_CUSTOM -> MaterialTheme.colorScheme.surfaceContainer
                                                                }
                                                                val badgeContent = when (exItem.matchSource) {
                                                                    ExerciseMatchSource.LOCAL_DB -> MaterialTheme.colorScheme.onPrimaryContainer
                                                                    ExerciseMatchSource.ONLINE_CATALOG -> MaterialTheme.colorScheme.onSecondaryContainer
                                                                    ExerciseMatchSource.NEW_CUSTOM -> MaterialTheme.colorScheme.primary
                                                                }
                                                                Surface(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = badgeContainer
                                                                ) {
                                                                    Text(
                                                                        text = when (exItem.matchSource) {
                                                                            ExerciseMatchSource.LOCAL_DB -> "✓ В вашей базе"
                                                                            ExerciseMatchSource.ONLINE_CATALOG -> "🌐 Из каталога"
                                                                            ExerciseMatchSource.NEW_CUSTOM -> "✨ Новое упражнение"
                                                                        },
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = badgeContent,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }

                                                                Surface(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = MaterialTheme.colorScheme.surfaceContainer
                                                                ) {
                                                                    Text(
                                                                        text = exItem.displayMuscleGroup.titleRu,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }

                                                                if (exItem.supersetLabel != null) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                                    ) {
                                                                        Text(
                                                                            text = "🔗 Суперсет ${exItem.supersetLabel}",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                val updatedDays = currentPlan.days.map { d ->
                                                                    if (d.id == dayPlan.id) {
                                                                        d.copy(exercises = d.exercises.filterNot { it.id == exItem.id })
                                                                    } else d
                                                                }.filter { it.exercises.isNotEmpty() || currentPlan.days.size == 1 }
                                                                parsedPlan = currentPlan.copy(days = updatedDays)
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Удалить упражнение",
                                                                tint = MaterialTheme.colorScheme.outline,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(Modifier.height(8.dp))

                                                    // Кнопки редактирования: План (подходы/повторения/вес) и Заменить упражнение
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    editingParamsTarget = dayPlan.id to exItem
                                                                }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                val wSuffix = if (exItem.targetWeightKg != null && exItem.targetWeightKg > 0f) {
                                                                    " • ${Formulas.formatWeight(exItem.targetWeightKg, isKg)}"
                                                                } else ""
                                                                Text(
                                                                    text = "${exItem.targetSets} подх. × ${exItem.targetReps} повт.$wSuffix",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Изменить подходы",
                                                                    modifier = Modifier.size(14.dp),
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }

                                                        OutlinedButton(
                                                            onClick = { replacingTarget = dayPlan.id to exItem },
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(34.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.SwapHoriz,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Заменить", style = MaterialTheme.typography.labelSmall)
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

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { parsedPlan = null },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("← К тексту")
                        }
                        Button(
                            onClick = {
                                if (totalExercisesCount > 0) {
                                    onConfirmSave(currentPlan, selectedMode, setAsActiveProgram)
                                }
                            },
                            enabled = totalExercisesCount > 0,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1.4f).height(50.dp)
                        ) {
                            Text("Сохранить ($totalExercisesCount упр.)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Модалка переименования дня в предпросмотре
    if (renamingDayTarget != null && parsedPlan != null) {
        val targetDay = renamingDayTarget!!
        var dayNameInput by remember(targetDay) { mutableStateOf(targetDay.name) }
        AlertDialog(
            onDismissRequest = { renamingDayTarget = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Название тренировочного дня", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = dayNameInput,
                    onValueChange = { dayNameInput = it },
                    label = { Text("Название дня") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dayNameInput.isNotBlank()) {
                            parsedPlan = parsedPlan!!.copy(
                                days = parsedPlan!!.days.map { d ->
                                    if (d.id == targetDay.id) d.copy(name = dayNameInput.trim()) else d
                                }
                            )
                        }
                        renamingDayTarget = null
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingDayTarget = null }) { Text("Отмена") }
            }
        )
    }

    // Модалка редактирования подходов / повторений / веса / суперсета в предпросмотре
    if (editingParamsTarget != null && parsedPlan != null) {
        val (dayId, exItem) = editingParamsTarget!!
        var setsStr by remember(exItem) { mutableStateOf(exItem.targetSets.toString()) }
        var repsStr by remember(exItem) { mutableStateOf(exItem.targetReps) }
        var weightStr by remember(exItem) {
            val w = exItem.targetWeightKg
            mutableStateOf(if (w != null && w > 0f) Formulas.formatNumber(Formulas.convertWeight(w, isKg)) else "")
        }
        var supersetStr by remember(exItem) { mutableStateOf(exItem.supersetLabel ?: "") }

        AlertDialog(
            onDismissRequest = { editingParamsTarget = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Параметры упражнения", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(exItem.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = setsStr,
                        onValueChange = { setsStr = it },
                        label = { Text("Количество подходов") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repsStr,
                        onValueChange = { repsStr = it },
                        label = { Text("Повторения (например: 8-12 или 10)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it.replace(',', '.') },
                        label = { Text("Целевой вес ($unitLabel, опционально)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Метка суперсета:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("" to "Нет", "A" to "A", "B" to "B", "C" to "C").forEach { (code, label) ->
                            FilterChip(
                                selected = supersetStr == code,
                                onClick = { supersetStr = code },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = setsStr.toIntOrNull()?.coerceIn(1, 20) ?: exItem.targetSets
                        val r = repsStr.trim().ifBlank { "8-12" }
                        val rawW = weightStr.toFloatOrNull()
                        val wKg = if (rawW != null && rawW > 0f) Formulas.toKg(rawW, isKg) else null
                        val sup = supersetStr.trim().takeIf { it.isNotEmpty() }

                        parsedPlan = parsedPlan!!.copy(
                            days = parsedPlan!!.days.map { d ->
                                if (d.id == dayId) {
                                    d.copy(
                                        exercises = d.exercises.map { item ->
                                            if (item.id == exItem.id) {
                                                item.copy(
                                                    targetSets = s,
                                                    targetReps = r,
                                                    targetWeightKg = wKg,
                                                    supersetLabel = sup
                                                )
                                            } else item
                                        }
                                    )
                                } else d
                            }
                        )
                        editingParamsTarget = null
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingParamsTarget = null }) { Text("Отмена") }
            }
        )
    }

    // Модалка замены упражнения (выбор из локальной БД, каталога 970+ или настройка как своего нового упражнения)
    if (replacingTarget != null && parsedPlan != null) {
        val (dayId, exItem) = replacingTarget!!
        var searchQuery by remember(exItem) { mutableStateOf(exItem.rawName) }
        var selectedGroupFilter by remember { mutableStateOf<MuscleGroup?>(null) }
        var customNameInput by remember(exItem) { mutableStateOf(exItem.customName) }
        var customGroupInput by remember(exItem) { mutableStateOf(exItem.displayMuscleGroup) }

        val q = searchQuery.trim().lowercase()
        val localMatches = remember(q, selectedGroupFilter, allExercises) {
            allExercises.filter { ex ->
                val gOk = selectedGroupFilter == null || ex.muscleGroup == selectedGroupFilter
                val qOk = q.isEmpty() || ex.name.lowercase().contains(q) || ex.nameEn.lowercase().contains(q)
                gOk && qOk
            }.take(20)
        }
        val localNamesSet = remember(allExercises) { allExercises.map { it.name.lowercase().trim() }.toSet() }
        val catalogMatches = remember(searchQuery, selectedGroupFilter, allExercises) {
            onSearchCatalog(searchQuery, selectedGroupFilter)
                .filter { it.name.lowercase().trim() !in localNamesSet }
                .take(25)
        }

        AlertDialog(
            onDismissRequest = { replacingTarget = null },
            shape = RoundedCornerShape(26.dp),
            title = { Text("Выбор упражнения", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "Распознано из LLM: «${exItem.rawName}»",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Поиск по базе (970+ упражнений)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { selectedGroupFilter = null },
                                label = { Text("Все") }
                            )
                        }
                        items(MuscleGroup.values()) { g ->
                            FilterChip(
                                selected = selectedGroupFilter == g,
                                onClick = { selectedGroupFilter = if (selectedGroupFilter == g) null else g },
                                label = { Text(g.titleRu) }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    LazyColumn(Modifier.height(210.dp)) {
                        if (localMatches.isNotEmpty()) {
                            item {
                                Text(
                                    text = "В вашей базе:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(localMatches) { ex ->
                                ListItem(
                                    headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            parsedPlan = parsedPlan!!.copy(
                                                days = parsedPlan!!.days.map { d ->
                                                    if (d.id == dayId) {
                                                        d.copy(
                                                            exercises = d.exercises.map { item ->
                                                                if (item.id == exItem.id) {
                                                                    item.copy(
                                                                        matchedExercise = ex,
                                                                        matchSource = ExerciseMatchSource.LOCAL_DB,
                                                                        customMuscleGroup = ex.muscleGroup
                                                                    )
                                                                } else item
                                                            }
                                                        )
                                                    } else d
                                                }
                                            )
                                            replacingTarget = null
                                        }
                                )
                            }
                        }

                        if (catalogMatches.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🌐 В каталоге (с техникой и иллюстрацией):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(catalogMatches) { ex ->
                                ListItem(
                                    headlineContent = { Text(ex.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("${ex.muscleGroup.titleRu} • ${ex.exerciseType.titleRu}") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            parsedPlan = parsedPlan!!.copy(
                                                days = parsedPlan!!.days.map { d ->
                                                    if (d.id == dayId) {
                                                        d.copy(
                                                            exercises = d.exercises.map { item ->
                                                                if (item.id == exItem.id) {
                                                                    item.copy(
                                                                        matchedExercise = ex,
                                                                        matchSource = ExerciseMatchSource.ONLINE_CATALOG,
                                                                        customMuscleGroup = ex.muscleGroup
                                                                    )
                                                                } else item
                                                            }
                                                        )
                                                    } else d
                                                }
                                            )
                                            replacingTarget = null
                                        }
                                )
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Или сохранить как новое своё упражнение:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Своё название") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MuscleGroup.values()) { g ->
                            FilterChip(
                                selected = customGroupInput == g,
                                onClick = { customGroupInput = g },
                                label = { Text(g.titleRu, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customNameInput.isNotBlank()) {
                            parsedPlan = parsedPlan!!.copy(
                                days = parsedPlan!!.days.map { d ->
                                    if (d.id == dayId) {
                                        d.copy(
                                            exercises = d.exercises.map { item ->
                                                if (item.id == exItem.id) {
                                                    item.copy(
                                                        matchedExercise = null,
                                                        matchSource = ExerciseMatchSource.NEW_CUSTOM,
                                                        customName = customNameInput.trim(),
                                                        customMuscleGroup = customGroupInput
                                                    )
                                                } else item
                                            }
                                        )
                                    } else d
                                }
                            )
                            replacingTarget = null
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Сохранить как новое")
                }
            },
            dismissButton = {
                TextButton(onClick = { replacingTarget = null }) { Text("Закрыть") }
            }
        )
    }
}
