package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.gymtracker.ui.navigation.Screen
import com.example.gymtracker.ui.screens.history.HistoryScreen
import com.example.gymtracker.ui.screens.home.HomeScreen
import com.example.gymtracker.ui.screens.programs.ProgramsScreen
import com.example.gymtracker.ui.screens.settings.SettingsScreen
import com.example.gymtracker.ui.screens.stats.StatsScreen
import com.example.gymtracker.ui.screens.workout.ActiveWorkoutScreen
import com.example.gymtracker.ui.theme.GymTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GymTrackerTheme {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                val isWorkoutRoute = currentRoute?.startsWith(Screen.Workout.route) == true

                Scaffold(
                    bottomBar = {
                        if (!isWorkoutRoute) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(Screen.Home.titleRu) },
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navController.navigate(Screen.Home.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                                    label = { Text(Screen.Programs.titleRu) },
                                    selected = currentRoute == Screen.Programs.route,
                                    onClick = { navController.navigate(Screen.Programs.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text(Screen.History.titleRu) },
                                    selected = currentRoute == Screen.History.route,
                                    onClick = { navController.navigate(Screen.History.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    label = { Text(Screen.Stats.titleRu) },
                                    selected = currentRoute == Screen.Stats.route,
                                    onClick = { navController.navigate(Screen.Stats.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(Screen.Settings.titleRu) },
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = { navController.navigate(Screen.Settings.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onStartWorkout = { navController.navigate(Screen.Workout.route) },
                                onOpenPrograms = { navController.navigate(Screen.Programs.route) }
                            )
                        }
                        composable(Screen.Programs.route) {
                            ProgramsScreen(
                                onStartWorkoutDay = { dayId ->
                                    navController.navigate("${Screen.Workout.route}?dayId=$dayId")
                                }
                            )
                        }
                        composable(
                            route = "${Screen.Workout.route}?dayId={dayId}",
                            arguments = listOf(
                                navArgument("dayId") {
                                    type = NavType.LongType
                                    defaultValue = 0L
                                }
                            )
                        ) { backStackEntry ->
                            val dayId = backStackEntry.arguments?.getLong("dayId")
                            ActiveWorkoutScreen(
                                onWorkoutFinished = { navController.popBackStack() },
                                dayId = if (dayId != null && dayId > 0) dayId else null
                            )
                        }
                        composable(Screen.Workout.route) {
                            ActiveWorkoutScreen(
                                onWorkoutFinished = { navController.popBackStack() },
                                dayId = null
                            )
                        }
                        composable(Screen.History.route) {
                            HistoryScreen()
                        }
                        composable(Screen.Stats.route) {
                            StatsScreen()
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
