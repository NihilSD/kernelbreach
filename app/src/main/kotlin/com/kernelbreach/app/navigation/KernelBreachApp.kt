package com.kernelbreach.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kernelbreach.app.feature.checkpoint.CheckpointScreen
import com.kernelbreach.app.feature.dev.UnreviewedScreen
import com.kernelbreach.app.feature.home.HomeScreen
import com.kernelbreach.app.feature.lab.LabScreen
import com.kernelbreach.app.feature.lesson.LessonScreen
import com.kernelbreach.app.feature.library.LibraryScreen
import com.kernelbreach.app.feature.map.MapScreen
import com.kernelbreach.app.feature.me.MeScreen
import com.kernelbreach.app.feature.module.ModuleScreen
import com.kernelbreach.app.feature.onboarding.OnboardingScreen
import com.kernelbreach.app.feature.refresh.RefreshScreen

@Composable
fun KernelBreachApp(
    onboardingDone: Boolean,
    settingsLoaded: Boolean,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.REFRESH, Routes.LIBRARY, Routes.ME)

    // Wait until settings are known so we start on the right screen (no flicker
    // between onboarding and home).
    if (!settingsLoaded) return
    val start = if (onboardingDone) Routes.HOME else Routes.ONBOARDING

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                KbBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { tab -> navController.navigateTopTab(tab.route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenMap = { navController.navigate(Routes.MAP) },
                    onContinueLesson = { id -> navController.navigate(Routes.lesson(id)) },
                    onOpenModule = { code -> navController.navigate(Routes.module(code)) },
                    onStartRefresh = { navController.navigateTopTab(Routes.REFRESH) },
                )
            }

            composable(Routes.MAP) {
                MapScreen(onOpenModule = { code -> navController.navigate(Routes.module(code)) })
            }

            composable(
                route = Routes.MODULE,
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
            ) { entry ->
                val code = entry.arguments?.getString("code").orEmpty()
                ModuleScreen(
                    moduleCode = code,
                    onOpenLesson = { id -> navController.navigate(Routes.lesson(id)) },
                    onOpenLab = { id -> navController.navigate(Routes.lab(id)) },
                    onOpenCheckpoint = { navController.navigate(Routes.checkpoint(code)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.LESSON,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val id = decodeId(entry.arguments?.getString("lessonId").orEmpty())
                LessonScreen(
                    lessonId = id,
                    onFinished = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.LAB,
                arguments = listOf(navArgument("labId") { type = NavType.StringType }),
            ) { entry ->
                val id = decodeId(entry.arguments?.getString("labId").orEmpty())
                LabScreen(
                    labId = id,
                    onFinished = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.CHECKPOINT,
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
            ) { entry ->
                val code = entry.arguments?.getString("code").orEmpty()
                CheckpointScreen(
                    moduleCode = code,
                    onDone = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }

            composable(Routes.REFRESH) { RefreshScreen() }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.ME) {
                MeScreen(onOpenDev = { navController.navigate(Routes.DEV_UNREVIEWED) })
            }
            composable(Routes.DEV_UNREVIEWED) {
                UnreviewedScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateTopTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
