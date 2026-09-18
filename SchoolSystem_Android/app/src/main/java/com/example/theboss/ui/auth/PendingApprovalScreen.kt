package com.example.theboss.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.local.SessionManager
import com.example.theboss.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _status = mutableStateOf("pending")
    val status: State<String> = _status

    fun startChecking(deviceId: String) {
        val schoolId = sessionManager.getSchoolId() ?: ""
        viewModelScope.launch {
            while (_status.value == "pending") {
                try {
                    val result = repository.checkStatus(deviceId, schoolId)
                    if (result.isSuccess) {
                        _status.value = result.getOrNull() ?: "pending"
                    }
                } catch (e: Exception) {
                    // Ignore and retry
                }
                delay(5000)
            }
        }
    }
}

@SuppressLint("HardwareIds")
@Composable
fun PendingApprovalScreen(
    email: String,
    viewModel: PendingViewModel = hiltViewModel(),
    onApproved: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = remember { 
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) 
    }
    val activeEmail = remember {
        context.getSharedPreferences("the_boss_prefs", android.content.Context.MODE_PRIVATE)
            .getString("student_email", email) ?: email
    }

    LaunchedEffect(Unit) {
        viewModel.startChecking(deviceId)
    }

    LaunchedEffect(viewModel.status.value) {
        if (viewModel.status.value == "approved") {
            onApproved()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("في انتظار موافقة الإدارة...")
            Text("حالة الطلب: ${viewModel.status.value}")
        }
    }
}
