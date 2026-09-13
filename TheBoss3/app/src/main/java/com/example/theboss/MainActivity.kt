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
import com.example.theboss.data.worker.StudentSyncWorker
import com.example.theboss.ui.auth.JoinRequestScreen
import com.example.theboss.ui.auth.OnboardingScreen
import com.example.theboss.ui.auth.QrScannerScreen
import com.example.theboss.ui.auth.PendingApprovalScreen
import com.example.theboss.ui.dashboard.MainDashboardScreen
import com.example.theboss.ui.dashboard.StudentTimetableScreen
import com.example.theboss.ui.settings.StudentSettingsScreen
import com.example.theboss.ui.subject.SubjectDetailScreen
import com.example.theboss.ui.theme.TheBossTheme
import com.example.theboss.ui.tutoring.DirectTutoringScreen
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

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        // Schedule continuous background periodic sync for teacher updates and lesson preparations
        StudentSyncWorker.schedule(applicationContext)
        com.example.theboss.widget.StudentScheduleWidgetProvider.sendRefreshBroadcast(applicationContext)

        setContent {
            TheBossTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.theboss.widget.StudentScheduleWidgetProvider.sendRefreshBroadcast(applicationContext)
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    val startDestination = "student_schedule"
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToJoin = { navController.navigate("join_request") { popUpTo("onboarding") { inclusive = true } } },
                onNavigateToDashboard = { navController.navigate("student_schedule") { popUpTo("onboarding") { inclusive = true } } }
            )
        }
        composable("join_request") {
            JoinRequestScreen(
                onNavigateToPending = { navController.navigate("student_schedule") { popUpTo("join_request") { inclusive = true } } },
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
                email = "test@example.com",
                onApproved = { navController.navigate("student_schedule") { popUpTo("pending") { inclusive = true } } }
            )
        }
        composable("dashboard") {
            MainDashboardScreen(
                onSubjectClick = { id -> navController.navigate("subject_detail/$id") },
                onScheduleClick = { navController.navigate("student_schedule") },
                onExamsClick = { },
                onTutoringClick = { navController.navigate("direct_tutoring") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("student_schedule") {
            StudentTimetableScreen(
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToSettings = { navController.navigate("settings") },
                onBack = if (navController.previousBackStackEntry != null) { { navController.popBackStack() } } else null
            )
        }
        composable("settings") {
            StudentSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate("join_request") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("direct_tutoring") {
            DirectTutoringScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("subject_detail/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectDetailScreen(
                subjectId = subjectId,
                subjectName = "تفاصيل المادة"
            )
        }
    }
}
