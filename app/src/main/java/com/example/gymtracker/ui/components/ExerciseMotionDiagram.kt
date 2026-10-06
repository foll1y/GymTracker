package com.example.gymtracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.ui.theme.ExpressivePrimary

enum class MotionCategory {
    PRESS_CHEST,
    PULL_BACK,
    SQUAT_LEGS,
    DEADLIFT,
    PRESS_SHOULDERS,
    BICEPS,
    TRICEPS,
    ABS_CORE,
    GENERIC
}

fun getMotionCategory(exercise: ExerciseEntity): MotionCategory {
    val name = exercise.name.lowercase()
    return when {
        "жим" in name && (exercise.muscleGroup == MuscleGroup.CHEST || "лёжа" in name || "наклон" in name) -> MotionCategory.PRESS_CHEST
        "отжимания" in name && "брусьях" !in name && exercise.muscleGroup == MuscleGroup.CHEST -> MotionCategory.PRESS_CHEST
        "подтягиван" in name || "верхнего блока" in name || "горизонтального" in name || ("тяга" in name && exercise.muscleGroup == MuscleGroup.BACK) -> MotionCategory.PULL_BACK
        "приседан" in name || "ногами" in name || "выпад" in name || "сплит" in name || "гакк" in name -> MotionCategory.SQUAT_LEGS
        "стан" in name || "румынск" in name || "мост" in name || "гиперэкстенз" in name -> MotionCategory.DEADLIFT
        exercise.muscleGroup == MuscleGroup.SHOULDERS || "армейск" in name || "махи" in name -> MotionCategory.PRESS_SHOULDERS
        "бицепс" in name || "молотков" in name || "скотт" in name -> MotionCategory.BICEPS
        "трицепс" in name || "французск" in name || "брусьях" in name || ("разгибан" in name && exercise.muscleGroup == MuscleGroup.ARMS) -> MotionCategory.TRICEPS
        exercise.muscleGroup == MuscleGroup.ABS || "скручиван" in name || "планка" in name || "пресс" in name -> MotionCategory.ABS_CORE
        else -> when (exercise.muscleGroup) {
            MuscleGroup.CHEST -> MotionCategory.PRESS_CHEST
            MuscleGroup.BACK -> MotionCategory.PULL_BACK
            MuscleGroup.LEGS -> MotionCategory.SQUAT_LEGS
            MuscleGroup.SHOULDERS -> MotionCategory.PRESS_SHOULDERS
            MuscleGroup.ARMS -> MotionCategory.BICEPS
            MuscleGroup.ABS -> MotionCategory.ABS_CORE
        }
    }
}

@Composable
fun ExerciseMotionDiagram(
    exercise: ExerciseEntity,
    modifier: Modifier = Modifier
) {
    val category = remember(exercise) { getMotionCategory(exercise) }

    val transition = rememberInfiniteTransition(label = "exercise_motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val strokeBody = 6.dp.toPx()
            val strokeLimb = 4.dp.toPx()
            val bodyColor = Color(0xFFD6DAE5)
            val accentColor = ExpressivePrimary
            val gearColor = Color(0xFF6B7280)

            when (category) {
                MotionCategory.PRESS_CHEST -> {
                    // Bench Press: скамья горизонтальная
                    drawLine(gearColor, Offset(cx - 70.dp.toPx(), cy + 20.dp.toPx()), Offset(cx + 70.dp.toPx(), cy + 20.dp.toPx()), 6.dp.toPx(), StrokeCap.Round)
                    drawLine(gearColor, Offset(cx - 50.dp.toPx(), cy + 20.dp.toPx()), Offset(cx - 50.dp.toPx(), cy + 60.dp.toPx()), 4.dp.toPx())
                    drawLine(gearColor, Offset(cx + 50.dp.toPx(), cy + 20.dp.toPx()), Offset(cx + 50.dp.toPx(), cy + 60.dp.toPx()), 4.dp.toPx())

                    val headX = cx - 45.dp.toPx()
                    val headY = cy + 10.dp.toPx()
                    drawCircle(bodyColor, 12.dp.toPx(), Offset(headX, headY))
                    val hipX = cx + 25.dp.toPx()
                    val hipY = cy + 14.dp.toPx()
                    drawLine(bodyColor, Offset(headX + 12.dp.toPx(), headY), Offset(hipX, hipY), strokeBody, StrokeCap.Round)

                    val chestX = cx - 15.dp.toPx()
                    val chestY = cy + 10.dp.toPx()
                    drawCircle(accentColor.copy(alpha = 0.3f + 0.7f * phase), (14 + 6 * phase).dp.toPx(), Offset(chestX, chestY))
                    drawCircle(accentColor, 7.dp.toPx(), Offset(chestX, chestY))

                    val barY = (cy - 30.dp.toPx()) + (35.dp.toPx() * (1f - phase))
                    val elbowX = cx - 10.dp.toPx()
                    val elbowY = cy + (15.dp.toPx() * (1f - phase))

                    drawLine(bodyColor, Offset(chestX, chestY), Offset(elbowX, elbowY), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(elbowX, elbowY), Offset(cx - 10.dp.toPx(), barY), strokeLimb, StrokeCap.Round)

                    drawLine(gearColor, Offset(cx - 75.dp.toPx(), barY), Offset(cx + 55.dp.toPx(), barY), 5.dp.toPx(), StrokeCap.Round)
                    drawCircle(accentColor, 9.dp.toPx(), Offset(cx - 75.dp.toPx(), barY))
                    drawCircle(accentColor, 9.dp.toPx(), Offset(cx + 55.dp.toPx(), barY))
                }

                MotionCategory.PULL_BACK -> {
                    drawLine(gearColor, Offset(cx - 70.dp.toPx(), cy - 60.dp.toPx()), Offset(cx + 70.dp.toPx(), cy - 60.dp.toPx()), 5.dp.toPx(), StrokeCap.Round)

                    val bodyOffset = -30.dp.toPx() * phase
                    val headY = cy - 25.dp.toPx() + bodyOffset
                    val torsoTopY = headY + 12.dp.toPx()
                    val torsoBottomY = torsoTopY + 45.dp.toPx()

                    drawCircle(bodyColor, 12.dp.toPx(), Offset(cx, headY))
                    drawLine(bodyColor, Offset(cx, torsoTopY), Offset(cx, torsoBottomY), strokeBody, StrokeCap.Round)

                    val latAlpha = 0.3f + 0.7f * phase
                    drawCircle(accentColor.copy(alpha = latAlpha), 12.dp.toPx(), Offset(cx - 16.dp.toPx(), torsoTopY + 18.dp.toPx()))
                    drawCircle(accentColor.copy(alpha = latAlpha), 12.dp.toPx(), Offset(cx + 16.dp.toPx(), torsoTopY + 18.dp.toPx()))

                    val elbowX = 35.dp.toPx() + 10.dp.toPx() * (1f - phase)
                    val elbowY = cy - 40.dp.toPx() + bodyOffset + 25.dp.toPx() * phase

                    drawLine(bodyColor, Offset(cx - 12.dp.toPx(), torsoTopY + 6.dp.toPx()), Offset(cx - elbowX, elbowY), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(cx - elbowX, elbowY), Offset(cx - 45.dp.toPx(), cy - 60.dp.toPx()), strokeLimb, StrokeCap.Round)

                    drawLine(bodyColor, Offset(cx + 12.dp.toPx(), torsoTopY + 6.dp.toPx()), Offset(cx + elbowX, elbowY), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(cx + elbowX, elbowY), Offset(cx + 45.dp.toPx(), cy - 60.dp.toPx()), strokeLimb, StrokeCap.Round)
                }

                MotionCategory.SQUAT_LEGS -> {
                    drawLine(gearColor, Offset(cx - 60.dp.toPx(), cy + 60.dp.toPx()), Offset(cx + 60.dp.toPx(), cy + 60.dp.toPx()), 3.dp.toPx())

                    val squatY = 32.dp.toPx() * phase
                    val headY = cy - 50.dp.toPx() + squatY
                    val hipY = cy + squatY
                    val kneeX = cx + 18.dp.toPx() * phase
                    val kneeY = cy + 30.dp.toPx() + 12.dp.toPx() * phase

                    drawCircle(bodyColor, 11.dp.toPx(), Offset(cx, headY))
                    drawLine(bodyColor, Offset(cx, headY + 11.dp.toPx()), Offset(cx - 5.dp.toPx() * phase, hipY), strokeBody, StrokeCap.Round)

                    val barY = headY + 14.dp.toPx()
                    drawLine(gearColor, Offset(cx - 65.dp.toPx(), barY), Offset(cx + 65.dp.toPx(), barY), 5.dp.toPx(), StrokeCap.Round)
                    drawCircle(accentColor, 8.dp.toPx(), Offset(cx - 65.dp.toPx(), barY))
                    drawCircle(accentColor, 8.dp.toPx(), Offset(cx + 65.dp.toPx(), barY))

                    drawLine(bodyColor, Offset(cx - 5.dp.toPx() * phase, hipY), Offset(kneeX, kneeY), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(kneeX, kneeY), Offset(cx, cy + 60.dp.toPx()), strokeLimb, StrokeCap.Round)

                    val quadAlpha = 0.3f + 0.7f * phase
                    drawCircle(accentColor.copy(alpha = quadAlpha), 14.dp.toPx(), Offset((cx + kneeX) / 2f, (hipY + kneeY) / 2f))
                }

                MotionCategory.PRESS_SHOULDERS -> {
                    val headY = cy - 20.dp.toPx()
                    val hipY = cy + 25.dp.toPx()
                    drawCircle(bodyColor, 12.dp.toPx(), Offset(cx, headY))
                    drawLine(bodyColor, Offset(cx, headY + 12.dp.toPx()), Offset(cx, hipY), strokeBody, StrokeCap.Round)

                    val deltY = headY + 16.dp.toPx()
                    drawCircle(accentColor, 10.dp.toPx(), Offset(cx - 16.dp.toPx(), deltY))
                    drawCircle(accentColor, 10.dp.toPx(), Offset(cx + 16.dp.toPx(), deltY))

                    val barY = (cy - 10.dp.toPx()) - (45.dp.toPx() * phase)
                    drawLine(gearColor, Offset(cx - 60.dp.toPx(), barY), Offset(cx + 60.dp.toPx(), barY), 5.dp.toPx(), StrokeCap.Round)
                    drawCircle(accentColor, 8.dp.toPx(), Offset(cx - 60.dp.toPx(), barY))
                    drawCircle(accentColor, 8.dp.toPx(), Offset(cx + 60.dp.toPx(), barY))

                    drawLine(bodyColor, Offset(cx - 16.dp.toPx(), deltY), Offset(cx - 30.dp.toPx(), barY + 10.dp.toPx() * (1f - phase)), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(cx - 30.dp.toPx(), barY + 10.dp.toPx() * (1f - phase)), Offset(cx - 25.dp.toPx(), barY), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(cx + 16.dp.toPx(), deltY), Offset(cx + 30.dp.toPx(), barY + 10.dp.toPx() * (1f - phase)), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(cx + 30.dp.toPx(), barY + 10.dp.toPx() * (1f - phase)), Offset(cx + 25.dp.toPx(), barY), strokeLimb, StrokeCap.Round)
                }

                MotionCategory.BICEPS -> {
                    val headY = cy - 40.dp.toPx()
                    val hipY = cy + 15.dp.toPx()
                    drawCircle(bodyColor, 12.dp.toPx(), Offset(cx, headY))
                    drawLine(bodyColor, Offset(cx, headY + 12.dp.toPx()), Offset(cx, hipY), strokeBody, StrokeCap.Round)

                    val shoulderY = headY + 18.dp.toPx()
                    val elbowY = shoulderY + 25.dp.toPx()
                    val elbowX = cx + 12.dp.toPx()

                    drawLine(bodyColor, Offset(cx, shoulderY), Offset(elbowX, elbowY), strokeLimb, StrokeCap.Round)

                    val handAngle = (1f - phase) * 2.2f - 0.6f
                    val handX = elbowX + kotlin.math.cos(handAngle) * 30.dp.toPx()
                    val handY = elbowY + kotlin.math.sin(handAngle) * 30.dp.toPx()
                    drawLine(bodyColor, Offset(elbowX, elbowY), Offset(handX, handY), strokeLimb, StrokeCap.Round)

                    drawCircle(gearColor, 8.dp.toPx(), Offset(handX, handY))
                    drawCircle(accentColor.copy(alpha = 0.3f + 0.7f * phase), (9 + 5 * phase).dp.toPx(), Offset(elbowX - 4.dp.toPx(), elbowY - 12.dp.toPx()))
                }

                MotionCategory.TRICEPS -> {
                    val headY = cy - 40.dp.toPx()
                    val hipY = cy + 15.dp.toPx()
                    drawCircle(bodyColor, 12.dp.toPx(), Offset(cx, headY))
                    drawLine(bodyColor, Offset(cx, headY + 12.dp.toPx()), Offset(cx, hipY), strokeBody, StrokeCap.Round)

                    val shoulderY = headY + 18.dp.toPx()
                    val elbowY = shoulderY + 22.dp.toPx()
                    val elbowX = cx + 14.dp.toPx()
                    drawLine(bodyColor, Offset(cx, shoulderY), Offset(elbowX, elbowY), strokeLimb, StrokeCap.Round)

                    val handY = elbowY + (12.dp.toPx() + 24.dp.toPx() * phase)
                    drawLine(bodyColor, Offset(elbowX, elbowY), Offset(elbowX, handY), strokeLimb, StrokeCap.Round)
                    drawLine(gearColor, Offset(elbowX - 10.dp.toPx(), handY), Offset(elbowX + 10.dp.toPx(), handY), 4.dp.toPx(), StrokeCap.Round)

                    drawCircle(accentColor.copy(alpha = 0.3f + 0.7f * phase), 10.dp.toPx(), Offset(elbowX - 6.dp.toPx(), elbowY - 10.dp.toPx()))
                }

                MotionCategory.ABS_CORE -> {
                    val headX = cx - 35.dp.toPx() + 15.dp.toPx() * phase
                    val headY = cy + 15.dp.toPx() - 25.dp.toPx() * phase
                    val hipX = cx + 15.dp.toPx()
                    val hipY = cy + 30.dp.toPx()

                    drawCircle(bodyColor, 11.dp.toPx(), Offset(headX, headY))
                    drawLine(bodyColor, Offset(headX + 8.dp.toPx(), headY + 6.dp.toPx()), Offset(hipX, hipY), strokeBody, StrokeCap.Round)

                    drawLine(bodyColor, Offset(hipX, hipY), Offset(hipX + 25.dp.toPx(), cy + 5.dp.toPx()), strokeLimb, StrokeCap.Round)
                    drawLine(bodyColor, Offset(hipX + 25.dp.toPx(), cy + 5.dp.toPx()), Offset(hipX + 35.dp.toPx(), cy + 30.dp.toPx()), strokeLimb, StrokeCap.Round)

                    val absX = (headX + hipX) / 2f
                    val absY = (headY + hipY) / 2f
                    drawCircle(accentColor.copy(alpha = 0.3f + 0.7f * phase), 14.dp.toPx(), Offset(absX, absY))
                }

                else -> {
                    drawCircle(bodyColor, 18.dp.toPx(), Offset(cx, cy))
                    drawCircle(accentColor.copy(alpha = 0.3f + 0.7f * phase), (22 + 8 * phase).dp.toPx(), Offset(cx, cy))
                    drawLine(gearColor, Offset(cx - 50.dp.toPx(), cy), Offset(cx + 50.dp.toPx(), cy), 6.dp.toPx(), StrokeCap.Round)
                    drawCircle(accentColor, 14.dp.toPx(), Offset(cx - 50.dp.toPx(), cy))
                    drawCircle(accentColor, 14.dp.toPx(), Offset(cx + 50.dp.toPx(), cy))
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
        ) {
            val phaseLabel = if (phase < 0.5f) "1. Исходное положение" else "2. Пик сокращения"
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (phase < 0.5f) MaterialTheme.colorScheme.onSurfaceVariant else ExpressivePrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
