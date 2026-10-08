path = 'app/src/main/java/com/example/gymtracker/ui/screens/workout/ActiveWorkoutScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Add import SetType
text = text.replace('import com.example.gymtracker.data.model.MuscleGroup', 'import com.example.gymtracker.data.model.MuscleGroup\nimport com.example.gymtracker.data.model.SetType\nimport androidx.compose.foundation.clickable')

# Add state for pairing dialog
old_state = '    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }'
new_state = '''    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseForSupersetPairing by remember { mutableStateOf<Int?>(null) }'''
text = text.replace(old_state, new_state)

# In card header: add superset controls and partner name
old_hdr = '''                                    IconButton(
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
                                }'''

new_hdr = '''                                    IconButton(
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

                                    if (exerciseItem.supersetLabel != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.padding(start = 6.dp)
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
                                }'''
text = text.replace(old_hdr, new_hdr)

# Add partner name display
old_partner_pos = '''                                    } else if (isBodyweightOnly) {
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
                                }'''

new_partner_pos = '''                                    } else if (isBodyweightOnly) {
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
                                }'''
text = text.replace(old_partner_pos, new_partner_pos)

# Set isPR logic to ignore WARMUP
old_pr = 'val isPR = showWeightField && currentW != null && currentW > 0f && histMax > 0f && currentW > histMax'
new_pr = 'val isPR = showWeightField && setEntry.setType != SetType.WARMUP && currentW != null && currentW > 0f && histMax > 0f && currentW > histMax'
text = text.replace(old_pr, new_pr)

# Table headers
text = text.replace('Text("Сет", Modifier.weight(0.6f),', 'Text("Сет", Modifier.weight(0.7f),')

# Replace static set number with DropdownMenu badge
old_set_num = 'Text("${setIndex + 1}", Modifier.weight(0.6f), fontWeight = FontWeight.Bold)'

new_set_num = '''                                var menuExpanded by remember { mutableStateOf(false) }
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
                                }'''
text = text.replace(old_set_num, new_set_num)

# Add pairing dialog at the end
old_end = '''    if (selectedExerciseForTechnique != null) {
        TechniqueBottomSheet(
            exercise = selectedExerciseForTechnique,
            onDismiss = { selectedExerciseForTechnique = null }
        )
    }
}'''

new_end = '''    if (selectedExerciseForTechnique != null) {
        TechniqueBottomSheet(
            exercise = selectedExerciseForTechnique,
            onDismiss = { selectedExerciseForTechnique = null }
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
}'''
text = text.replace(old_end, new_end)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

print('Updated ActiveWorkoutScreen.kt successfully!')
