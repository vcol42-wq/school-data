package com.school.system.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.school.system.data.model.SchoolConfig

@Composable
fun TeacherPortalScreen(
    config: SchoolConfig,
    onStartSession: (grade: String, section: String, subject: String, isVerified: Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Teacher Portal - Temporary")
    }
}
