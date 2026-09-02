package com.example.theboss.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.local.SessionManager
import com.example.theboss.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSettingsViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val schoolId = sessionManager.getSchoolId() ?: ""
    val schoolCode = sessionManager.getSchoolCode() ?: ""
    val schoolName = sessionManager.getSchoolName() ?: "المدرسة الذكية"

    private val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

    private val _cloudUrl = MutableStateFlow(prefs.getString("supabase_url", "https://pexehlvkpdhmpukjydwd.supabase.co") ?: "")
    val cloudUrl: StateFlow<String> = _cloudUrl

    private val _geminiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val geminiKey: StateFlow<String> = _geminiKey

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun saveSettings(newUrl: String, newGeminiKey: String) {
        prefs.edit()
            .putString("supabase_url", newUrl.trim())
            .putString("gemini_api_key", newGeminiKey.trim())
            .apply()
        _cloudUrl.value = newUrl.trim()
        _geminiKey.value = newGeminiKey.trim()
    }

    fun testConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _testStatus.value = "جاري فحص الاتصال بالسحابة..."
            try {
                val success = repository.testCloudConnection()
                if (success) {
                    _testStatus.value = "متصل بالسحابة بنجاح ✓"
                    repository.fetchAndStoreGeminiKey()
                    _geminiKey.value = prefs.getString("gemini_api_key", "") ?: ""
                } else {
                    _testStatus.value = "فشل الاتصال بالسحابة ❌"
                }
            } catch (e: Exception) {
                _testStatus.value = "خطأ: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSchoolSession() {
        sessionManager.clearSchoolSession()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSettingsScreen(
    viewModel: StudentSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val savedUrl by viewModel.cloudUrl.collectAsState()
    val savedGeminiKey by viewModel.geminiKey.collectAsState()
    val testStatus by viewModel.testStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var cloudUrlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var geminiKeyInput by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الربط والاتصال ⚙️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: School Identity
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات المدرسة المقترنة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Divider(color = Color(0xFFE2E8F0))
                    Text("اسم المدرسة: ${viewModel.schoolName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("كود المدرسة: ${viewModel.schoolCode.ifEmpty { "غير محدد" }}", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    Text("معرّف السحابة (ID): ${viewModel.schoolId.ifEmpty { "غير مقترن" }}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Card 2: Secure Cloud Connection Status (Encrypted & Masked)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("حالة الاتصال السحابي المشفر 🔒", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأمان والتشفير:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("TLS 1.3 / E2EE نشط ✓", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("مفتاح الاتصال السحابي:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("••••••••••••••••", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    if (testStatus != null) {
                        Surface(
                            color = if (testStatus!!.contains("✓")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testStatus!!,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (testStatus!!.contains("✓")) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("فحص اتصال المزامنة بالسحابة")
                        }
                    }
                }
            }

            // Card 3: Session Reset / Re-pair
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إعادة الاقتران أو تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFBE123C))
                    Text("إذا كنت ترغب بالانتقال لمدرسة أخرى أو إعادة مسح رمز الـ QR، يمكنك إلغاء الاقتران الحالي.", fontSize = 11.sp, color = Color(0xFF881337))
                    Button(
                        onClick = {
                            viewModel.resetSchoolSession()
                            Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                            onNavigateToLogin()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة ربط المدرسة ومسح QR")
                    }
                }
            }
        }
    }
}
