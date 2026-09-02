package com.principal.system.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.principal.system.data.repository.PrincipalRepository
import com.principal.system.ui.screens.*

@Composable
fun PrincipalNavGraph(
    navController: NavHostController,
    repository: PrincipalRepository
) {
    val startDestination = if (repository.isConfigured()) "dashboard" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            PrincipalOnboardingScreen(
                onPairedSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            ExecutiveDashboardScreen(
                onNavigateToTeachers = { navController.navigate("teacher_radar") },
                onNavigateToAttendance = { navController.navigate("attendance_radar") },
                onNavigateToBroadcast = { navController.navigate("broadcast_screen") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            PrincipalSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("teacher_radar") {
            TeacherActivityRadarScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("attendance_radar") {
            AttendanceRadarScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("broadcast_screen") {
            BroadcastAnnouncementsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
