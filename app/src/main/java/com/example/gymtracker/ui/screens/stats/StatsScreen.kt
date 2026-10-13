package com.example.gymtracker.ui.screens.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.domain.Formulas
import com.example.gymtracker.ui.components.ExerciseProgressBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.statsState.collectAsState()
    val period by viewModel.period.collectAsState()
    val isKg by viewModel.isKg.collectAsState()
    var expandedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var selectedExerciseForProgress by remember { mutableStateOf<ExerciseEntity?>(null) }
    val progressHistory by remember(selectedExerciseForProgress) {
        if (selectedExerciseForProgress != null) {
            viewModel.getExerciseHistory(selectedExerciseForProgress!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Статистика и рекорды",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StatsPeriod.values()) { p ->
                    FilterChip(
                        selected = p == period,
                        onClick = { viewModel.selectPeriod(p) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        label = { Text(p.title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Тренировок", "${state.totalWorkouts}", null, Modifier.weight(1f))
                MetricCard("Серия", "🔥 ${state.currentStreak} тр.", null, Modifier.weight(1f), isAccent = true)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Макс. 1ПМ (1 повт.)",
                    value = if (state.maxOneRepMaxKg > 0f) Formulas.formatWeight(state.maxOneRepMaxKg, isKg) else "—",
                    subtitle = if (state.maxOneRepMaxExercise.isNotEmpty()) state.maxOneRepMaxExercise else null,
                    modifier = Modifier.weight(1f),
                    isAccent = true
                )
                MetricCard(
                    title = "Макс. подход",
                    value = if (state.maxWorkingWeightKg > 0f) Formulas.formatWeight(state.maxWorkingWeightKg, isKg) else "—",
                    subtitle = null,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Рабочий тоннаж",
                    value = if (state.totalWorkingTonnageKg > 0f) Formulas.formatWeight(state.totalWorkingTonnageKg, isKg) else "—",
                    subtitle = "${state.totalSets} подходов",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Калории",
                    value = if (state.totalCaloriesBurned > 0) "${state.totalCaloriesBurned} ккал" else "—",
                    subtitle = if (state.averageDurationMinutes > 0) "Ср. время: ${state.averageDurationMinutes} мин" else null,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.avgHeartRate > 0) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Ср. пульс", "❤️ ${state.avgHeartRate} уд/мин", null, Modifier.weight(1f))
                    MetricCard("Ср. время", "${state.averageDurationMinutes} мин", null, Modifier.weight(1f))
                }
            }
        }

        // Прогресс по группам мышц с раскрытием по клику
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "Группы мышц и максимум (1ПМ)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Нажмите на группу мышц, чтобы увидеть рекорды и тоннаж по упражнениям",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    state.muscleStats.forEach { stat ->
                        val isExpanded = expandedGroup == stat.group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    expandedGroup = if (isExpanded) null else stat.group
                                }
                                .padding(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = stat.group.titleRu,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subText = buildString {
                                        append("${stat.totalSets} подх.")
                                        if (stat.maxOneRepMaxKg > 0f) {
                                            append(" • 1ПМ: ${Formulas.formatWeight(stat.maxOneRepMaxKg, isKg)}")
                                        }
                                        if (stat.workingTonnageKg > 0f) {
                                            append(" • Тоннаж: ${Formulas.formatWeight(stat.workingTonnageKg, isKg)}")
                                        }
                                    }
                                    Text(
                                        text = subText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (stat.share > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text(
                                            text = "${(stat.share * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stat.share > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { stat.share },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeCap = StrokeCap.Round
                            )

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (stat.exercises.isEmpty()) {
                                        Text(
                                            text = "За выбранный период подходов в этой группе нет",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        stat.exercises.forEach { ex ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            selectedExerciseForProgress = viewModel.getExercise(ex.exerciseId)
                                                                ?: ExerciseEntity(id = ex.exerciseId, name = ex.exerciseName, muscleGroup = stat.group)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            text = ex.exerciseName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        val detailStr = if (ex.maxWeightKg > 0f) {
                                                            "Рабочий: ${Formulas.formatWeight(ex.maxWeightKg, isKg)} × ${ex.repsAtMaxWeight} повт. • ${ex.totalSets} подх."
                                                        } else {
                                                            "${ex.totalSets} подх."
                                                        }
                                                        Text(
                                                            text = detailStr,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (ex.oneRepMaxKg > 0f) {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer
                                                        ) {
                                                            Text(
                                                                text = "1ПМ: ${Formulas.formatWeight(ex.oneRepMaxKg, isKg)}",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                }
            }
        }
    }

    if (selectedExerciseForProgress != null) {
        ExerciseProgressBottomSheet(
            exercise = selectedExerciseForProgress,
            historyEntries = progressHistory,
            isKg = isKg,
            onDismiss = { selectedExerciseForProgress = null }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isAccent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAccent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
