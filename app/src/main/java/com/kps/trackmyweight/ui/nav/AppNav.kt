package com.kps.trackmyweight.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kps.trackmyweight.ui.corps.CorpsHubScreen
import com.kps.trackmyweight.ui.goal.GoalScreen
import com.kps.trackmyweight.ui.gyms.GymEditScreen
import com.kps.trackmyweight.ui.gyms.GymsScreen
import com.kps.trackmyweight.ui.habits.HabitsScreen
import com.kps.trackmyweight.ui.home.HomeScreen
import com.kps.trackmyweight.ui.measurements.MeasurementsScreen
import com.kps.trackmyweight.ui.nutrition.NutritionScreen
import com.kps.trackmyweight.ui.photos.CameraCaptureScreen
import com.kps.trackmyweight.ui.photos.PhotosScreen
import com.kps.trackmyweight.ui.reports.ReportsScreen
import com.kps.trackmyweight.ui.settings.SettingsScreen
import com.kps.trackmyweight.ui.weight.WeightScreen
import com.kps.trackmyweight.ui.workout.WorkoutOverviewScreen
import com.kps.trackmyweight.ui.workout.cardio.CardioLogScreen
import com.kps.trackmyweight.ui.workout.session.SessionActiveScreen
import com.kps.trackmyweight.ui.workout.template.TemplateEditorScreen
import kotlinx.coroutines.launch

enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Aujourd'hui", Icons.Outlined.Home),
    WORKOUT("workout", "Séance", Icons.Outlined.FitnessCenter),
    NUTRITION("nutrition", "Nutrition", Icons.Outlined.Restaurant),
    CORPS("corps", "Corps", Icons.Outlined.Person),
    SETTINGS("settings", "Plus", Icons.Outlined.Settings),
}

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "root",
        enterTransition = { slideInHorizontally(tween(220)) { it / 6 } },
        exitTransition = { slideOutHorizontally(tween(180)) { -it / 8 } },
        popEnterTransition = { slideInHorizontally(tween(220)) { -it / 6 } },
        popExitTransition = { slideOutHorizontally(tween(180)) { it / 8 } },
    ) {
        composable("root") { RootPagerScreen(navController) }

        composable("reports") { ReportsScreen(onBack = { navController.popBackStack() }) }
        composable("cardio") {
            CardioLogScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "template/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) {
            TemplateEditorScreen(
                templateId = it.arguments?.getLong("id")?.takeIf { v -> v > 0L },
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "session/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) { backStack ->
            val id = backStack.arguments?.getLong("id") ?: 0L
            SessionActiveScreen(
                sessionId = id,
                onFinished = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable("weight") { WeightScreen(onBack = { navController.popBackStack() }) }
        composable("measurements") { MeasurementsScreen(onBack = { navController.popBackStack() }) }
        composable("photos") {
            PhotosScreen(
                onOpenCamera = { navController.navigate("camera_capture") },
                onBack = { navController.popBackStack() },
            )
        }
        composable("camera_capture") {
            CameraCaptureScreen(onDone = { navController.popBackStack() })
        }
        composable("gyms") {
            GymsScreen(
                onBack = { navController.popBackStack() },
                onEditGym = { id -> navController.navigate("gym_edit/$id") },
            )
        }
        composable(
            route = "gym_edit/{id}",
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType }),
        ) {
            GymEditScreen(onBack = { navController.popBackStack() })
        }
        composable("goal") { GoalScreen(onBack = { navController.popBackStack() }) }
        composable("habits") { HabitsScreen(onBack = { navController.popBackStack() }) }
    }
}

/**
 * Écran racine : les 5 destinations top-level sont exposées via un HorizontalPager
 * (swipe gauche/droite comme WhatsApp) synchronisé avec la BottomBar.
 * Les sous-destinations (Reports, Weight, Camera, etc.) restent des routes NavHost séparées.
 */
@Composable
private fun RootPagerScreen(navController: NavHostController) {
    val pagerState = rememberPagerState(pageCount = { TopLevel.entries.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevel.entries.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (TopLevel.entries[page]) {
                    TopLevel.HOME -> HomeScreen(onOpenReports = { navController.navigate("reports") })
                    TopLevel.WORKOUT -> WorkoutOverviewScreen(
                        onStartSession = { sessionId -> navController.navigate("session/$sessionId") },
                        onEditTemplate = { id -> navController.navigate("template/${id ?: 0L}") },
                        onOpenCardio = { navController.navigate("cardio") },
                    )
                    TopLevel.NUTRITION -> NutritionScreen()
                    TopLevel.CORPS -> CorpsHubScreen(
                        onOpenWeight = { navController.navigate("weight") },
                        onOpenMeasurements = { navController.navigate("measurements") },
                        onOpenPhotos = { navController.navigate("photos") },
                    )
                    TopLevel.SETTINGS -> SettingsScreen(
                        onOpenGyms = { navController.navigate("gyms") },
                        onOpenGoal = { navController.navigate("goal") },
                        onOpenHabits = { navController.navigate("habits") },
                    )
                }
            }
        }
    }
}
