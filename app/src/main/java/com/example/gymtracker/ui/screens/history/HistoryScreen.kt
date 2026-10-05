package com.example.gymtracker.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gymtracker.data.local.dao.WorkoutDao
import com.example.gymtracker.data.model.WorkoutWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    workoutDao: WorkoutDao
) : ViewModel() {
    val workouts = workoutDao.getAllWorkouts()
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val list by viewModel.workouts.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { w ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        dateFormat.format(Date(w.workout.dateEpochMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Длительность: ${w.workout.durationMinutes} мин", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))

                    w.exercises.forEach { ex ->
                        Text("• ${ex.exercise.name}: ${ex.sets.size} подходов", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
