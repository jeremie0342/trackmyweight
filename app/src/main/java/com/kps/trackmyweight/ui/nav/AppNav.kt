package com.kps.trackmyweight.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
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
import com.kps.trackmyweight.ui.nutrition.NutritionScreen
import com.kps.trackmyweight.ui.photos.PhotosScreen
import com.kps.trackmyweight.ui.weight.WeightScreen
import com.kps.trackmyweight.ui.workout.WorkoutOverviewScreen
import com.kps.trackmyweight.ui.workout.session.SessionActiveScreen
import com.kps.trackmyweight.ui.workout.template.TemplateEditorScreen

enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Aujourd'hui", Icons.Outlined.Home),
    WORKOUT("workout", "Séance", Icons.Outlined.FitnessCenter),
    NUTRITION("nutrition", "Nutrition", Icons.Outlined.Restaurant),
    WEIGHT("weight", "Poids", Icons.Outlined.MonitorWeight),
    MEASUREMENTS("measurements", "Corps", Icons.Outlined.Straighten),
    PHOTOS("photos", "Photos", Icons.Outlined.CameraAlt),
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
        composable(TopLevel.WORKOUT.route) {
            WorkoutOverviewScreen(
                onStartSession = { sessionId ->
                    navController.navigate("session/$sessionId")
                },
                onEditTemplate = { id -> navController.navigate("template/${id ?: 0L}") },
            )
        }
        composable(
            route = "template/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) {
            TemplateEditorScreen(
                templateId = it.arguments?.getLong("id")?.takeIf { v -> v > 0L },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(TopLevel.NUTRITION.route) { NutritionScreen() }
        composable(TopLevel.WEIGHT.route) { WeightScreen() }
        composable(TopLevel.MEASUREMENTS.route) { MeasurementsScreen() }
        composable(TopLevel.PHOTOS.route) { PhotosScreen() }
        composable(
            route = "session/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: 0L
            SessionActiveScreen(sessionId = id, onFinished = { navController.popBackStack() })
        }
    }
}
