package com.principal.system.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import com.principal.system.data.repository.PrincipalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrincipalSettingsViewModel @Inject constructor(
    private val repository: PrincipalRepository,
    private val sessionManager: com.principal.system.data.local.SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val schoolId = sessionManager.getSchoolId() ?: ""
    val schoolName = sessionManager.getSchoolName() ?: "إدارة المدرسة"
    val schoolCode = sessionManager.getSchoolCode() ?: ""

    private val _cloudUrl = MutableStateFlow(sessionManager.getSupabaseUrl() ?: "https://ejkocidagfgyvwtjffwv.supabase.co")
    val cloudUrl: StateFlow<String> = _cloudUrl

    private val _cloudKey = MutableStateFlow(sessionManager.getApiKey() ?: "")
    val cloudKey: StateFlow<String> = _cloudKey

    private val prefs = context.getSharedPreferences("principal_prefs", Context.MODE_PRIVATE)
    private val _geminiKey = MutableStateFlow(prefs.getString("gemini_key", "") ?: "")
    val geminiKey: StateFlow<String> = _geminiKey

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus

    fun saveConfig(newUrl: String, newKey: String, newGeminiKey: String) {
        sessionManager.saveSchoolCredentials(
            schoolId = schoolId.ifEmpty { "school_01" },
            schoolName = schoolName,
            schoolCode = schoolCode,
            supabaseUrl = newUrl.trim(),
            apiKey = newKey.trim()
        )
        prefs.edit().putString("gemini_key", newGeminiKey.trim()).apply()
        _cloudUrl.value = newUrl.trim()
        _cloudKey.value = newKey.trim()
        _geminiKey.value = newGeminiKey.trim()
    }

    fun testCloudConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testStatus.value = "جاري فحص الاتصال بسحابة المدرسة ومزامنة المؤشرات..."
            try {
                val result = repository.syncAllExecutiveMetrics()
                if (result.isSuccess) {
                    _testStatus.value = "متصل بالسحابة بنجاح ومزامنة القيود مكتملة ✓"
                } else {
                    _testStatus.value = "فشل المزامنة: ${result.exceptionOrNull()?.message ?: "خطأ غير معروف"}"
                }
            } catch (e: Exception) {
                _testStatus.value = "خطأ: ${e.localizedMessage}"
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun clearPairing() {
        sessionManager.clearSession()
        prefs.edit().clear().apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalSettingsScreen(
    viewModel: PrincipalSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val savedUrl by viewModel.cloudUrl.collectAsState()
    val savedKey by viewModel.cloudKey.collectAsState()
    val savedGeminiKey by viewModel.geminiKey.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testStatus by viewModel.testStatus.collectAsState()

    var cloudUrlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var cloudKeyInput by remember(savedKey) { mutableStateOf(savedKey) }
    var geminiKeyInput by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الربط السحابي ⚙️", fontWeight = FontWeight.Black) },
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات المدرسة الحالية", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Divider(color = Color(0xFF334155))
                    Text("اسم المدرسة: ${viewModel.schoolName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("رمز الاقتران (School / Pairing Code): ${viewModel.schoolCode.ifEmpty { "غير محدد" }}", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("معرّف السحابة (School ID): ${viewModel.schoolId.ifEmpty { "غير مقترن" }}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }

            // Card 2: Cloud URL and Keys
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات سحابة Supabase ومفتاح الذكاء الاصطناعي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = cloudUrlInput,
                        onValueChange = { cloudUrlInput = it },
                        label = { Text("رابط السحابة (Supabase URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cloudKeyInput,
                        onValueChange = { cloudKeyInput = it },
                        label = { Text("مفتاح الوصول السحابي (Supabase Anon Key)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("مفتاح الذكاء الاصطناعي (Gemini API Key)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.saveConfig(cloudUrlInput, cloudKeyInput, geminiKeyInput)
                                Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ التغييرات")
                        }

                        OutlinedButton(
                            onClick = { viewModel.testCloudConnection() },
                            modifier = Modifier.weight(1f),
                            enabled = !isTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("فحص ومزامنة")
                            }
                        }
                    }
                }
            }

            // Card 3: Disconnect / Re-pair
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إعادة مسح الـ QR أو تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFBE123C))
                    Text("إذا كنت تريد إعادة الربط مع كمبيوتر الإدارة أو مسح رمز QR جديد:", fontSize = 11.sp, color = Color(0xFF881337))
                    Button(
                        onClick = {
                            viewModel.clearPairing()
                            Toast.makeText(context, "تم إلغاء الربط", Toast.LENGTH_SHORT).show()
                            onNavigateToOnboarding()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة مسح رمز الـ QR من الكمبيوتر")
                    }
                }
            }
        }
    }
}
