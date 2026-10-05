package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.gymtracker.ui.navigation.Screen
import com.example.gymtracker.ui.screens.history.HistoryScreen
import com.example.gymtracker.ui.screens.home.HomeScreen
import com.example.gymtracker.ui.screens.settings.SettingsScreen
import com.example.gymtracker.ui.screens.stats.StatsScreen
import com.example.gymtracker.ui.screens.workout.ActiveWorkoutScreen
import com.example.gymtracker.ui.theme.GymTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymTrackerTheme {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != Screen.Workout.route) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(Screen.Home.titleRu) },
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navController.navigate(Screen.Home.route) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text(Screen.History.titleRu) },
                                    selected = currentRoute == Screen.History.route,
                                    onClick = { navController.navigate(Screen.History.route) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    label = { Text(Screen.Stats.titleRu) },
                                    selected = currentRoute == Screen.Stats.route,
                                    onClick = { navController.navigate(Screen.Stats.route) }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(Screen.Settings.titleRu) },
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = { navController.navigate(Screen.Settings.route) }
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
                            HomeScreen(onStartWorkout = { navController.navigate(Screen.Workout.route) })
                        }
                        composable(Screen.Workout.route) {
                            ActiveWorkoutScreen(onWorkoutFinished = { navController.popBackStack() })
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
