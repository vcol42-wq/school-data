package com.example.theboss.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import com.school.system.utils.RoleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestScreen(
    viewModel: JoinRequestViewModel = hiltViewModel(),
    onNavigateToPending: () -> Unit,
    onNavigateToQrScanner: () -> Unit = {},
    onNavigateToRoleSelection: () -> Unit = {},
    navController: androidx.navigation.NavController? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var schoolCode by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }

    val scannedCode = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("scanned_code", null)
        ?.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(scannedCode?.value) {
        scannedCode?.value?.let { code ->
            try {
                val gson = com.google.gson.Gson()
                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                val data: Map<String, String> = gson.fromJson(code, mapType)
                
                val url = data["url"] ?: ""
                val apiKey = data["apiKey"] ?: ""
                val sCode = data["schoolId"] ?: ""
                
                schoolCode = sCode
                
                // Save connection info to SharedPreferences
                val prefs = context.getSharedPreferences("the_boss_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("supabase_url", url)
                    .putString("supabase_key", apiKey)
                    .putString("school_id", sCode)
                    .apply()
                
                android.widget.Toast.makeText(context, "تم قراءة باركود المدرسة بنجاح", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "الكود الممسوح غير صالح", android.widget.Toast.LENGTH_SHORT).show()
            }
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    LaunchedEffect(viewModel.isSuccess.value) {
        if (viewModel.isSuccess.value) {
            onNavigateToPending()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طلب انضمام للطالب") },
                actions = {
                    TextButton(
                        onClick = {
                            RoleManager.clearSelectedRole(context)
                            onNavigateToRoleSelection()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تبديل الصفة ⇄", fontWeight = FontWeight.Bold, fontSize = 12.0.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الثلاثي") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = schoolCode,
                onValueChange = { schoolCode = it },
                label = { Text("رمز المدرسة الموحد") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onNavigateToQrScanner,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("مسح باركود المدرسة (QR)")
            }
            OutlinedTextField(
                value = stage,
                onValueChange = { stage = it },
                label = { Text("المرحلة الدراسية") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = grade,
                onValueChange = { grade = it },
                label = { Text("الصف") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = section,
                onValueChange = { section = it },
                label = { Text("الشعبة") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val finalEmail = email.ifEmpty { "student_${System.currentTimeMillis()}@school.edu" }
                    val finalSchoolCode = schoolCode.trim()
                    viewModel.sendRequest(name, finalEmail, finalSchoolCode, stage.ifEmpty { "الصف الأول" }, grade.ifEmpty { "الأول" }, section.ifEmpty { "أ" })
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = name.isNotBlank() && !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("انضمام مباشر للمدرسة 🚀", fontWeight = FontWeight.Black)
                }
            }

            OutlinedButton(
                onClick = {
                    RoleManager.clearSelectedRole(context)
                    onNavigateToRoleSelection()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("العودة لاختيار الصفة (أستاذ / طالب) ⇄", fontWeight = FontWeight.Bold)
            }

            viewModel.error.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
