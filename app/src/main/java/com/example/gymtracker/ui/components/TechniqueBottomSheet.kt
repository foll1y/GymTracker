package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.local.entity.ExerciseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueBottomSheet(
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit
) {
    if (exercise == null) return

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

            Spacer(Modifier.height(16.dp))

            // Векторная двухкадровая схема анимации
            ExerciseMotionDiagram(exercise = exercise)

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
