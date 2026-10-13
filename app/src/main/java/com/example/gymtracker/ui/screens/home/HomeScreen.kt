package com.example.gymtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.local.dao.ProgramDao
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.preferences.UserPreferencesManager
import com.example.gymtracker.domain.Formulas
import com.example.gymtracker.ui.theme.ExpressivePrimary
import com.example.gymtracker.ui.theme.ExpressiveSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class RecommendedDayInfo(
    val programTitle: String,
    val dayId: Long,
    val dayName: String,
    val exercisesSummary: String,
    val exercisesCount: Int
)

data class HomeUiState(
    val hasActiveDraft: Boolean = false,
    val isKg: Boolean = true,
    val weeklyWorkoutsCount: Int = 0,
    val weeklyTonnageKg: Float = 0f,
    val currentStreak: Int = 0,
    val recommendedDay: RecommendedDayInfo? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    workoutDao: WorkoutDao,
    programDao: ProgramDao,
    private val userPreferences: UserPreferencesManager
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        workoutDao.getAllWorkouts(),
        programDao.getActiveProgram(),
        userPreferences.hasActiveDraft,
        userPreferences.isKg
    ) { workouts, activeProgram, hasDraft, isKg ->
        val now = System.currentTimeMillis()
        val weekMillis = TimeUnit.DAYS.toMillis(7)
        val weeklyWorkouts = workouts.filter { now - it.workout.dateEpochMillis <= weekMillis }

        var weeklyTonnage = 0f
        weeklyWorkouts.forEach { wd ->
            wd.exercises.forEach { ex ->
                weeklyTonnage += Formulas.calculateWorkingTonnage(ex.sets)
            }
        }

        val workoutDays = workouts.map { TimeUnit.MILLISECONDS.toDays(it.workout.dateEpochMillis) }
        val streak = Formulas.calculateStreak(workoutDays)

        val sortedDays = activeProgram?.days?.sortedBy { it.day.orderIndex } ?: emptyList()
        val recommended = if (sortedDays.isNotEmpty()) {
            // Определяем следующий день по ротации на основе последней тренировки
            val latestWorkout = workouts.firstOrNull()
            val nextDay = if (latestWorkout != null && sortedDays.size > 1) {
                val latestExerciseIds = latestWorkout.exercises.map { it.exercise.id }.toSet()
                val lastMatchedDayIndex = sortedDays.indexOfFirst { d ->
                    val dayExIds = d.exercises.map { it.exercise.id }.toSet()
                    dayExIds.isNotEmpty() && dayExIds.intersect(latestExerciseIds).size >= (dayExIds.size + 1) / 2
                }
                if (lastMatchedDayIndex >= 0) {
                    sortedDays[(lastMatchedDayIndex + 1) % sortedDays.size]
                } else {
                    sortedDays.first()
                }
            } else {
                sortedDays.first()
            }

            val sortedEx = nextDay.exercises.sortedBy { it.planExercise.orderIndex }
            val previewNames = sortedEx.take(4).joinToString(", ") { it.exercise.name }
            RecommendedDayInfo(
                programTitle = activeProgram!!.program.title,
                dayId = nextDay.day.id,
                dayName = nextDay.day.name,
                exercisesSummary = if (previewNames.isNotBlank()) previewNames else "Нажмите, чтобы начать тренировку",
                exercisesCount = sortedEx.size
            )
        } else null

        HomeUiState(
            hasActiveDraft = hasDraft,
            isKg = isKg,
            weeklyWorkoutsCount = weeklyWorkouts.size,
            weeklyTonnageKg = weeklyTonnage,
            currentStreak = streak,
            recommendedDay = recommended
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun discardDraft() {
        userPreferences.clearWorkoutDraft()
    }
}

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onStartWorkoutDay: (Long) -> Unit,
    onOpenPrograms: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(84.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(ExpressivePrimary, ExpressiveSecondary)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Gym Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Твой мощный тренировочный дневник",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Сводка за последние 7 дней
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMiniStatCard(
                label = "За 7 дней",
                value = "${state.weeklyWorkoutsCount} тр.",
                modifier = Modifier.weight(1f)
            )
            HomeMiniStatCard(
                label = "Серия",
                value = "🔥 ${state.currentStreak}",
                modifier = Modifier.weight(1f),
                isHighlighted = state.currentStreak > 0
            )
            HomeMiniStatCard(
                label = "Тоннаж (7 дн.)",
                value = if (state.weeklyTonnageKg > 0f) Formulas.formatWeight(state.weeklyTonnageKg, state.isKg) else "—",
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(Modifier.height(18.dp))

        // Баннер незавершённой тренировки (если есть сохранённый черновик)
        if (state.hasActiveDraft) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Незавершённая тренировка",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "У вас есть сохранённый черновик текущей тренировки. Хотите продолжить?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onStartWorkout,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Продолжить", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.discardDraft() },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Удалить")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Карточка следующей тренировки по активной программе
        val rec = state.recommendedDay
        if (rec != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "По плану • ${rec.programTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${rec.exercisesCount} упр.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = rec.dayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = rec.exercisesSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { onStartWorkoutDay(rec.dayId) },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Начать по плану", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = onStartWorkout,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (rec == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (rec == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Свободная тренировка", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenPrograms,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Планы и программы", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HomeMiniStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
