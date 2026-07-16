package com.kps.trackmyweight.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kps.trackmyweight.ui.home.HomeScreen
import com.kps.trackmyweight.ui.measurements.MeasurementsScreen
import com.kps.trackmyweight.ui.photos.PhotosScreen
import com.kps.trackmyweight.ui.weight.WeightScreen

enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Aujourd'hui", Icons.Outlined.Home),
    WEIGHT("weight", "Poids", Icons.Outlined.MonitorWeight),
    MEASUREMENTS("measurements", "Mensurations", Icons.Outlined.Straighten),
    PHOTOS("photos", "Photos", Icons.Outlined.CameraAlt),
    // Placeholder — sera activé en Phase 3
    // GYM("gym", "Salle", Icons.Outlined.FitnessCenter),
}

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

@Composable
fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        TopLevel.entries.forEach { dest ->
            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == dest.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != dest.route) {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    padding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevel.HOME.route,
    ) {
        composable(TopLevel.HOME.route) { HomeScreen() }
        composable(TopLevel.WEIGHT.route) { WeightScreen() }
        composable(TopLevel.MEASUREMENTS.route) { MeasurementsScreen() }
        composable(TopLevel.PHOTOS.route) { PhotosScreen() }
    }
}
