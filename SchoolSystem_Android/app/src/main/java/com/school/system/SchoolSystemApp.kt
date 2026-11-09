package com.school.system

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.school.system.ui.navigation.SchoolSystemNavHost

@Composable
fun SchoolSystemApp(
    isAppLockEnabled: Boolean,
    onAppLockEnabledChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    SchoolSystemNavHost(
        navController = navController,
        isAppLockEnabled = isAppLockEnabled,
        onAppLockEnabledChange = onAppLockEnabledChange
    )
}
