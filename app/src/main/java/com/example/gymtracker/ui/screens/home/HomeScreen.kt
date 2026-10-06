package com.example.gymtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.ui.theme.ExpressivePrimary
import com.example.gymtracker.ui.theme.ExpressiveSecondary

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onOpenPrograms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
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
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Gym Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Твой мощный тренировочный дневник",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(44.dp))

        Button(
            onClick = onStartWorkout,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text("Свободная тренировка", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = onOpenPrograms,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Планы и программы", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
