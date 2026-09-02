package com.example.theboss

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.theboss.ui.auth.JoinRequestScreen
import com.example.theboss.ui.auth.OnboardingScreen
import com.example.theboss.ui.auth.QrScannerScreen
import com.example.theboss.ui.auth.PendingApprovalScreen
import com.example.theboss.ui.dashboard.MainDashboardScreen
import com.example.theboss.ui.subject.SubjectDetailScreen
import com.example.theboss.ui.theme.TheBossTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request audio recording and notifications permissions
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Exact alarm requires special handling, but we request general permissions here
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())

        setContent {
            TheBossTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val startDestination = remember {
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
        val onboardingShown = prefs.getBoolean("onboarding_shown", false)
        val independentMode = prefs.getBoolean("independent_mode", false)
        val isSchoolConfigured = !prefs.getString("school_id", null).isNullOrEmpty()

        if (!onboardingShown) {
            "onboarding"
        } else if (independentMode || isSchoolConfigured) {
            "dashboard"
        } else {
            "join_request"
        }
    }
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToJoin = { navController.navigate("join_request") { popUpTo("onboarding") { inclusive = true } } },
                onNavigateToDashboard = { navController.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } } }
            )
        }
        composable("join_request") {
            JoinRequestScreen(
                onNavigateToPending = { navController.navigate("dashboard") { popUpTo("join_request") { inclusive = true } } },
                onNavigateToQrScanner = { navController.navigate("qr_scanner") },
                navController = navController
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
        composable("pending") {
            PendingApprovalScreen(
                email = "test@example.com", // Should be passed from JoinRequest
                onApproved = { navController.navigate("dashboard") { popUpTo("pending") { inclusive = true } } }
            )
        }
        composable("dashboard") {
            MainDashboardScreen(
                onSubjectClick = { id -> navController.navigate("subject_detail/$id") },
                onScheduleClick = { navController.navigate("student_schedule") },
                onExamsClick = { /* Navigate inside dashboard or tab */ },
                onTutoringClick = { navController.navigate("direct_tutoring") },
                onParentAttendanceClick = { navController.navigate("parent_attendance") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("student_schedule") {
            com.example.theboss.ui.dashboard.StudentTimetableScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            com.example.theboss.ui.settings.StudentSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate("join_request") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("parent_attendance") {
            com.example.theboss.ui.attendance.ParentAttendanceTrackerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("direct_tutoring") {
            com.example.theboss.ui.tutoring.DirectTutoringScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("subject_detail/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectDetailScreen(
                subjectId = subjectId,
                subjectName = "تفاصيل المادة" // Name resolved inside Screen
            )
        }
    }
}
