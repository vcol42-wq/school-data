package com.school.system

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.school.system.ui.navigation.SchoolSystemNavHost

@Composable
fun SchoolSystemApp() {
    val navController = rememberNavController()
    SchoolSystemNavHost(navController = navController)
}
