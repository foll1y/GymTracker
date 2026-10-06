package com.example.gymtracker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.ui.theme.ExpressivePrimary

enum class DisplayMode(val title: String) {
    SIDE_BY_SIDE("Оба кадра"),
    ANIMATION("Анимация (1 ↔ 2)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueBottomSheet(
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit
) {
    if (exercise == null) return

    var displayMode by remember { mutableStateOf(DisplayMode.SIDE_BY_SIDE) }

    val baseUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"
    val frame0Url = if (exercise.imagePath.isNotBlank()) "$baseUrl/${exercise.imagePath}/0.jpg" else null
    val frame1Url = if (exercise.imagePath.isNotBlank()) "$baseUrl/${exercise.imagePath}/1.jpg" else null

    val transition = rememberInfiniteTransition(label = "exercise_photo_loop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "photo_phase"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
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
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = exercise.muscleGroup.titleRu,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = exercise.exerciseType.titleRu,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Переключатель режима отображения визуала: «Оба кадра» / «Анимация»
            if (frame0Url != null && frame1Url != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    DisplayMode.values().forEach { mode ->
                        FilterChip(
                            selected = displayMode == mode,
                            onClick = { displayMode = mode },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            label = { Text(mode.title, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Блок визуальных примеров из free-exercise-db
            if (frame0Url != null && frame1Url != null) {
                if (displayMode == DisplayMode.SIDE_BY_SIDE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExercisePhotoCard(
                            imageUrl = frame0Url,
                            label = "1. Исходное положение",
                            modifier = Modifier.weight(1f)
                        )
                        ExercisePhotoCard(
                            imageUrl = frame1Url,
                            label = "2. Пик сокращения",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Анимация (плавное циклическое переключение кадров 0 ↔ 1)
                    val activeUrl = if (phase < 0.5f) frame0Url else frame1Url
                    val activeLabel = if (phase < 0.5f) "1. Исходное положение" else "2. Пик сокращения"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = activeUrl, label = "crossfade_exercise") { url ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = activeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (phase < 0.5f) MaterialTheme.colorScheme.onSurfaceVariant else ExpressivePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Если упражнение кастомное или нет фото — показываем схематичную диаграмму
                ExerciseMotionDiagram(exercise = exercise)
            }

            Spacer(Modifier.height(18.dp))

            // Чек-лист техники
            Text(
                text = "Руководство по технике",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            val setup = if (exercise.setupTip.isNotBlank()) exercise.setupTip else "Займите устойчивое исходное положение, зафиксируйте корпус."
            val exec = if (exercise.executionTip.isNotBlank()) exercise.executionTip else "Двигайтесь подконтрольно. Выдох на максимальном усилии, плавный вдох при опускании."
            val mistake = if (exercise.mistakeTip.isNotBlank()) exercise.mistakeTip else "Избегайте резких рывков и инерции телом."

            TipItem(
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "1. Исходное положение",
                text = setup
            )

            Spacer(Modifier.height(8.dp))

            TipItem(
                icon = Icons.Default.CheckCircle,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "2. Движение и дыхание",
                text = exec
            )

            Spacer(Modifier.height(8.dp))

            TipItem(
                icon = Icons.Default.ErrorOutline,
                iconTint = MaterialTheme.colorScheme.error,
                title = "3. Чего избегать",
                text = mistake
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Понятно", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ExercisePhotoCard(
    imageUrl: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(175.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
