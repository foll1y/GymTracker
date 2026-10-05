package com.example.gymtracker.ui.navigation

sealed class Screen(val route: String, val titleRu: String) {
    object Home : Screen("home", "Главная")
    object Workout : Screen("workout", "Тренировка")
    object History : Screen("history", "История")
    object Stats : Screen("stats", "Статистика")
    object Settings : Screen("settings", "Настройки")
}
