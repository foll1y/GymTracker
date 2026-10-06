package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.local.dao.RawHistoryEntry
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.SetType
import com.example.gymtracker.domain.Formulas
import com.example.gymtracker.ui.theme.ExpressivePrimary
import com.example.gymtracker.ui.theme.ExpressiveSecondary
import com.example.gymtracker.ui.theme.ExpressiveSuccess
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class ProgressPeriod(val title: String, val days: Long) {
    ONE_MONTH("1 мес", 30),
    THREE_MONTHS("3 мес", 90),
    SIX_MONTHS("6 мес", 180),
    ALL("Всё", Long.MAX_VALUE)
}

data class DayProgressPoint(
    val dateEpoch: Long,
    val maxWeightKg: Float,
    val maxReps: Int,
    val oneRepMaxKg: Float,
    val totalSets: Int,
    val entries: List<RawHistoryEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressBottomSheet(
    exercise: ExerciseEntity?,
    historyEntries: List<RawHistoryEntry>,
    onDismiss: () -> Unit
) {
    if (exercise == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPeriod by remember { mutableStateOf(ProgressPeriod.THREE_MONTHS) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("ru")) }
    val shortDateFormat = remember { SimpleDateFormat("dd.MM", Locale("ru")) }

    // Группировка подходов по дате тренировки
    val dayPoints = remember(historyEntries, selectedPeriod) {
        val now = System.currentTimeMillis()
        val minEpoch = if (selectedPeriod.days == Long.MAX_VALUE) 0L else now - TimeUnit.DAYS.toMillis(selectedPeriod.days)
        val filtered = historyEntries.filter { it.date >= minEpoch }

        // Группируем по календарным суткам
        val cal = Calendar.getInstance()
        val grouped = filtered.groupBy { entry ->
            cal.timeInMillis = entry.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        grouped.map { (dayEpoch, entries) ->
            // При расчёте 1ПМ и рекорда исключаем разминочные сеты
            val workingEntries = entries.filter { it.setType != SetType.WARMUP }
            val candidateEntries = if (workingEntries.isNotEmpty()) workingEntries else entries

            var bestWeight = 0f
            var bestReps = 0
            var best1RM = 0f

            candidateEntries.forEach { e ->
                val oneRm = Formulas.calculate1RM(e.weightKg, e.reps)
                if (oneRm > best1RM || (oneRm == best1RM && e.weightKg > bestWeight)) {
                    best1RM = oneRm
                    bestWeight = e.weightKg
                    bestReps = e.reps
                }
            }

            DayProgressPoint(
                dateEpoch = dayEpoch,
                maxWeightKg = bestWeight,
                maxReps = bestReps,
                oneRepMaxKg = best1RM,
                totalSets = entries.size,
                entries = entries
            )
        }.sortedBy { it.dateEpoch }
    }

    // Абсолютные рекорды за всё время
    val allTimeBestWeight = remember(historyEntries) {
        historyEntries.filter { it.setType != SetType.WARMUP }.maxOfOrNull { it.weightKg } ?: 0f
    }
    val allTimeBest1RM = remember(historyEntries) {
        historyEntries.filter { it.setType != SetType.WARMUP }
            .map { Formulas.calculate1RM(it.weightKg, it.reps) }
            .maxOrNull() ?: 0f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = exercise.muscleGroup.titleRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = exercise.exerciseType.titleRu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            }

            // Фильтры периода
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ProgressPeriod.values()) { p ->
                        FilterChip(
                            selected = p == selectedPeriod,
                            onClick = {
                                selectedPeriod = p
                                selectedPointIndex = null
                            },
                            shape = RoundedCornerShape(14.dp),
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

            // Карточки рекордов
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "🏆 Рекордный 1ПМ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (allTimeBest1RM > 0f) "${allTimeBest1RM} кг" else "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "⚡ Лучший рабочий вес",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (allTimeBestWeight > 0f) "${allTimeBestWeight} кг" else "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // График прогресса
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "График прогрессии (1ПМ)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(ExpressivePrimary, CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Text("1ПМ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(ExpressiveSecondary, CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Вес", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (dayPoints.size < 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (dayPoints.isEmpty()) {
                                        "В выбранном периоде нет данных"
                                    } else {
                                        "Для построения графика необходимо минимум 2 тренировки с этим упражнением"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            // Canvas график
                            val max1RMValue = (dayPoints.maxOf { it.oneRepMaxKg } * 1.15f).coerceAtLeast(10f)
                            val min1RMValue = 0f

                            val pointCount = dayPoints.size
                            val selectedPoint = selectedPointIndex?.let { dayPoints.getOrNull(it) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(dayPoints) {
                                            detectTapGestures { offset ->
                                                val colWidth = size.width / (pointCount - 1).coerceAtLeast(1)
                                                val clickedIdx = ((offset.x + colWidth / 2) / colWidth)
                                                    .toInt()
                                                    .coerceIn(0, pointCount - 1)
                                                selectedPointIndex = clickedIdx
                                            }
                                        }
                                ) {
                                    val w = size.width
                                    val h = size.height - 30f

                                    // Сетка горизонтальная
                                    val gridSteps = 4
                                    val gridColor = Color.White.copy(alpha = 0.08f)
                                    for (i in 0..gridSteps) {
                                        val y = h - (h * i / gridSteps)
                                        drawLine(
                                            color = gridColor,
                                            start = Offset(0f, y),
                                            end = Offset(w, y),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    }

                                    // Координаты точек
                                    val oneRmOffsets = mutableListOf<Offset>()
                                    val weightOffsets = mutableListOf<Offset>()

                                    dayPoints.forEachIndexed { idx, pt ->
                                        val x = if (pointCount > 1) idx * (w / (pointCount - 1)) else w / 2
                                        val y1RM = h - ((pt.oneRepMaxKg - min1RMValue) / (max1RMValue - min1RMValue) * h)
                                        val yWeight = h - ((pt.maxWeightKg - min1RMValue) / (max1RMValue - min1RMValue) * h)
                                        oneRmOffsets.add(Offset(x, y1RM.coerceIn(0f, h)))
                                        weightOffsets.add(Offset(x, yWeight.coerceIn(0f, h)))
                                    }

                                    // Линия 1: Рабочий вес (Secondary)
                                    val weightPath = Path().apply {
                                        weightOffsets.forEachIndexed { i, offset ->
                                            if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                                        }
                                    }
                                    drawPath(
                                        path = weightPath,
                                        color = ExpressiveSecondary.copy(alpha = 0.7f),
                                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Линия 2: 1ПМ (Primary)
                                    val oneRmPath = Path().apply {
                                        oneRmOffsets.forEachIndexed { i, offset ->
                                            if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                                        }
                                    }
                                    drawPath(
                                        path = oneRmPath,
                                        color = ExpressivePrimary,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Точки
                                    oneRmOffsets.forEachIndexed { i, offset ->
                                        val isSelected = selectedPointIndex == i
                                        drawCircle(
                                            color = if (isSelected) Color.White else ExpressivePrimary,
                                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                            center = offset
                                        )
                                        if (isSelected) {
                                            drawCircle(
                                                color = ExpressivePrimary,
                                                radius = 9.dp.toPx(),
                                                center = offset,
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                                }

                                // Плашка выбранной точки
                                if (selectedPoint != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                shortDateFormat.format(Date(selectedPoint.dateEpoch)),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "1ПМ: ${selectedPoint.oneRepMaxKg} кг",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Рабочий: ${selectedPoint.maxWeightKg} кг × ${selectedPoint.maxReps}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Подписи дат внизу
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    shortDateFormat.format(Date(dayPoints.first().dateEpoch)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (dayPoints.size > 2) {
                                    Text(
                                        shortDateFormat.format(Date(dayPoints[dayPoints.size / 2].dateEpoch)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    shortDateFormat.format(Date(dayPoints.last().dateEpoch)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // История всех тренировок с этим упражнением
            item {
                Text(
                    text = "История подходов (${dayPoints.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(dayPoints.reversed()) { pt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateFormat.format(Date(pt.dateEpoch)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (pt.oneRepMaxKg > 0f) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "1ПМ: ${pt.oneRepMaxKg} кг",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        pt.entries.forEachIndexed { setIdx, s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                            text = if (s.setType == SetType.NORMAL) "${setIdx + 1}" else s.setType.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (s.setType) {
                                                SetType.NORMAL -> MaterialTheme.colorScheme.onSurface
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
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                val oneRmVal = Formulas.calculate1RM(s.weightKg, s.reps)
                                if (oneRmVal > 0f) {
                                    Text(
                                        text = "1ПМ: $oneRmVal кг",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
