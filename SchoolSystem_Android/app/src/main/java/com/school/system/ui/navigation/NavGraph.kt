package com.school.system.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import com.school.system.ui.screens.RoleSelectionScreen
import com.school.system.ui.screens.PrincipalOnboardingScreen

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.school.system.ui.screens.SplashScreen
import com.school.system.ui.screens.TeacherPortalScreen
import com.school.system.ui.screens.OnboardingScreen
import com.school.system.ui.screens.SettingsViewModel
import com.school.system.utils.AppRole
import com.school.system.utils.RoleManager
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SchoolSystemNavHost(navController: NavHostController) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val config by settingsViewModel.config.collectAsState()
    val context = LocalContext.current
    val studentSessionManager = remember { com.example.theboss.data.local.SessionManager(context) }
    
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
                    val role = RoleManager.getSelectedRole(context)
                    val nextRoute = when (role) {
                        AppRole.TEACHER -> if (config?.isActivated == true) "dashboard" else "onboarding"
                        AppRole.STUDENT -> if (studentSessionManager.isSchoolConfigured()) "student_schedule" else "student_onboarding"
                        AppRole.PRINCIPAL -> if (config?.isActivated == true && config?.role == "supervisor") "dashboard" else "principal_onboarding"
                        null -> "role_selection"
                    }
                    navController.navigate(nextRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("role_selection") {
            RoleSelectionScreen(
                onSelectTeacher = {
                    val dest = if (config?.isActivated == true) "dashboard" else "onboarding"
                    navController.navigate(dest) {
                        popUpTo("role_selection") { inclusive = true }
                    }
                },
                onSelectStudent = {
                    val dest = if (studentSessionManager.isSchoolConfigured()) "student_schedule" else "student_onboarding"
                    navController.navigate(dest) {
                        popUpTo("role_selection") { inclusive = true }
                    }
                },
                onSelectPrincipal = {
                    val dest = if (config?.isActivated == true && config?.role == "supervisor") "dashboard" else "principal_onboarding"
                    navController.navigate(dest) {
                        popUpTo("role_selection") { inclusive = true }
                    }
                }
            )
        }

        // ==================== PRINCIPAL / SUPERVISOR ONBOARDING ====================
        composable("principal_onboarding") {
            PrincipalOnboardingScreen(
                onActivationComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("principal_onboarding") { inclusive = true }
                    }
                },
                onNavigateToQrScanner = {
                    navController.navigate("qr_scanner")
                },
                onNavigateToRoleSelection = {
                    navController.navigate("role_selection") {
                        popUpTo("principal_onboarding") { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        // ==================== TEACHER / ADMIN ROUTES ====================
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
                onBack = { navController.popBackStack() },
                onNavigateToGrades = { grade, section, subject ->
                    navController.navigate("grades/$grade/$section/$subject")
                }
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

        // ==================== STUDENT / GUARDIAN ROUTES ====================
        composable("student_onboarding") {
            com.example.theboss.ui.auth.OnboardingScreen(
                onNavigateToJoin = {
                    navController.navigate("student_join_request") {
                        popUpTo("student_onboarding") { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate("student_schedule") {
                        popUpTo("student_onboarding") { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = {
                    RoleManager.clearSelectedRole(context)
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("student_join_request") {
            com.example.theboss.ui.auth.JoinRequestScreen(
                onNavigateToPending = {
                    navController.navigate("student_schedule") {
                        popUpTo("student_join_request") { inclusive = true }
                    }
                },
                onNavigateToQrScanner = {
                    navController.navigate("student_qr_scanner")
                },
                onNavigateToRoleSelection = {
                    RoleManager.clearSelectedRole(context)
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        composable("student_qr_scanner") {
            com.example.theboss.ui.auth.QrScannerScreen(
                onCodeScanned = { code ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_code", code)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("student_schedule") {
            com.example.theboss.ui.dashboard.StudentTimetableScreen(
                onNavigateToDashboard = { navController.navigate("student_dashboard") },
                onNavigateToSettings = { navController.navigate("student_settings") },
                onBack = if (navController.previousBackStackEntry != null) { { navController.popBackStack() } } else null
            )
        }

        composable("student_dashboard") {
            com.example.theboss.ui.dashboard.MainDashboardScreen(
                onSubjectClick = { id -> navController.navigate("subject_detail/$id") },
                onScheduleClick = { navController.navigate("student_schedule") },
                onExamsClick = { },
                onTutoringClick = { navController.navigate("direct_tutoring") },
                onParentAttendanceClick = { navController.navigate("student_attendance") },
                onSettingsClick = { navController.navigate("student_settings") }
            )
        }

        composable("student_attendance") {
            com.example.theboss.ui.attendance.ParentAttendanceTrackerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("student_settings") {
            com.example.theboss.ui.settings.StudentSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate("student_join_request") {
                        popUpTo("student_schedule") { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = {
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("direct_tutoring") {
            com.example.theboss.ui.tutoring.DirectTutoringScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("subject_detail/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            com.example.theboss.ui.subject.SubjectDetailScreen(
                subjectId = subjectId,
                subjectName = "تفاصيل المادة"
            )
        }
    }
}
