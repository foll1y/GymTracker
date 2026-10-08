path = 'app/src/main/java/com/example/gymtracker/ui/screens/programs/ProgramsScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Add state
old_st = '    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }'
new_st = '''    var selectedExerciseForTechnique by remember { mutableStateOf<ExerciseEntity?>(null) }
    var planExerciseForSupersetPairing by remember { mutableStateOf<Long?>(null) }'''
text = text.replace(old_st, new_st)

# Replace itemsIndexed(currentDay.exercises) with sortedExercises
old_items = '''                    } else {
                        itemsIndexed(currentDay.exercises) { _, planEx ->'''

new_items = '''                    } else {
                        val sortedExercises = currentDay.exercises.sortedBy { it.planExercise.orderIndex }
                        itemsIndexed(sortedExercises, key = { _, item -> item.planExercise.id }) { exIndex, planEx ->'''
text = text.replace(old_items, new_items)

# Add partner and superset badge in card
old_c_hdr = '''                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { selectedExerciseForTechnique = planEx.exercise }) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Техника выполнения",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { viewModel.removeExerciseFromDay(planEx.planExercise) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Удалить",
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }'''

new_c_hdr = '''                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Суперсет кнопка или бейдж
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

                                            // Перемещение (стрелки + рукоятка)
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
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Перетащить",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp).padding(horizontal = 2.dp)
                                            )

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
                                        }'''
text = text.replace(old_c_hdr, new_c_hdr)

# Add partner name display in card
old_p_name = '''                                                Surface(
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
                                        }'''

new_p_name = '''                                                Surface(
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
                                                val partner = sortedExercises.find { it.planExercise.id != planEx.planExercise.id && it.planExercise.supersetLabel == planEx.planExercise.supersetLabel }
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
                                        }'''
text = text.replace(old_p_name, new_p_name)

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
}'''
text = text.replace(old_end, new_end)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)

print('Updated ProgramsScreen.kt successfully!')
