package com.example.gymtracker.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var isKg by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Единицы измерения")
            Row {
                FilterChip(selected = isKg, onClick = { isKg = true }, label = { Text("кг") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = !isKg, onClick = { isKg = false }, label = { Text("фунты") })
            }
        }
    }
}
