package com.vitasync.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vitasync.app.ui.screens.*

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    Destination("dashboard", "Today", Icons.Filled.Home),
    Destination("activity", "Activity", Icons.Filled.DirectionsRun),
    Destination("food", "Food", Icons.Filled.Restaurant),
    Destination("fasting", "Fasting", Icons.Filled.HourglassEmpty),
    Destination("coach", "Coach", Icons.Filled.AutoAwesome),
    Destination("more", "More", Icons.Filled.MoreHoriz)
)

@Composable
fun AppNavHost(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") { DashboardScreen() }
            composable("activity") { ActivityScreen() }
            composable("food") { FoodScreen() }
            composable("fasting") { FastingScreen() }
            composable("coach") { CoachScreen() }
            composable("more") { MoreScreen(onNavigate = { navController.navigate(it) }) }
            composable("water") { WaterScreen() }
            composable("sleep") { SleepScreen() }
            composable("vices") { VicesScreen() }
            composable("devices") { DevicesScreen() }
        }
    }
}
