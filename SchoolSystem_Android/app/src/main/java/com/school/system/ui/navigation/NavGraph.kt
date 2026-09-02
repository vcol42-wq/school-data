package com.school.system.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.school.system.ui.screens.DashboardScreen
import com.school.system.ui.screens.GradeRegisterScreen
import com.school.system.ui.screens.SettingsScreen
import com.school.system.ui.screens.QrScannerScreen
import com.school.system.ui.screens.ScheduleScreen
import com.school.system.ui.screens.SmartBellScreen

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.school.system.ui.screens.SplashScreen
import com.school.system.ui.screens.TeacherPortalScreen
import com.school.system.ui.screens.OnboardingScreen
import com.school.system.ui.screens.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SchoolSystemNavHost(navController: NavHostController) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val config by settingsViewModel.config.collectAsState()
    
    if (config == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onAnimationFinished = {
                    val nextRoute = if (config?.isActivated == true) "dashboard" else "onboarding"
                    navController.navigate(nextRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onActivationComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onNavigateToQrScanner = {
                    navController.navigate("qr_scanner")
                },
                navController = navController
            )
        }

        composable("portal") {
            TeacherPortalScreen(
                config = config ?: com.school.system.data.model.SchoolConfig(),
                onStartSession = { grade, section, subject, isVerified ->
                    navController.navigate("grades/$grade/$section/$subject")
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToGrades = { grade, section, subject ->
                    navController.navigate("grades/$grade/$section/$subject")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToSchedule = {
                    navController.navigate("schedule")
                },
                onNavigateToSmartBell = {
                    navController.navigate("smart_bell")
                },
                onNavigateToHomeworkHub = {
                    navController.navigate("homework_hub")
                }
            )
        }

        composable("homework_hub") {
            com.school.system.ui.screens.HomeworkHubScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToQrScanner = { navController.navigate("qr_scanner") },
                navController = navController
            )
        }

        composable("schedule") {
            val dashboardViewModel: com.school.system.ui.screens.DashboardViewModel = hiltViewModel()
            ScheduleScreen(
                syncManager = dashboardViewModel.syncManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable("qr_scanner") {
            QrScannerScreen(
                onCodeScanned = { code ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_code", code)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("smart_bell") {
            SmartBellScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "grades/{grade}/{section}/{subject}",
            arguments = listOf(
                navArgument("grade") { type = NavType.StringType },
                navArgument("section") { type = NavType.StringType },
                navArgument("subject") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val grade = backStackEntry.arguments?.getString("grade") ?: ""
            val section = backStackEntry.arguments?.getString("section") ?: ""
            val subject = backStackEntry.arguments?.getString("subject") ?: ""
            
            GradeRegisterScreen(
                grade = grade,
                section = section,
                subject = subject,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
