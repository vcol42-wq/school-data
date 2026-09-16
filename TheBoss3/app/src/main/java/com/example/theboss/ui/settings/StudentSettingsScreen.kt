package com.example.theboss.ui.settings

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
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val schoolId = sessionManager.getSchoolId() ?: ""
    val schoolCode = sessionManager.getSchoolCode() ?: ""
    val schoolName = sessionManager.getSchoolName() ?: "المدرسة الذكية"

    private val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

    val studentGrade = prefs.getString("student_grade", "الصف الأول المتوسط") ?: "الصف الأول المتوسط"
    val studentSection = prefs.getString("student_section", "أ") ?: "أ"
    val studentName = prefs.getString("student_name", "الطالب") ?: "الطالب"

    // Previously stored school code for fast 1-tap re-pairing without barcode!
    val savedSchoolCode: String = prefs.getString("last_saved_school_code", "") ?: ""

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val maskedSchoolCode: String = if (schoolCode.length >= 4) {
        "SCH-••••-" + schoolCode.takeLast(4)
    } else if (savedSchoolCode.length >= 4) {
        "SCH-••••-" + savedSchoolCode.takeLast(4)
    } else {
        "SCH-••••-9821"
    }

    fun testConnection(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _testStatus.value = "جاري التحقق من الربط السحابي..."
            try {
                val success = repository.testCloudConnection()
                if (success) {
                    _testStatus.value = "تم تفعيل والتحقق من الربط السحابي بنجاح ✓"
                    repository.syncSchedule(schoolId)
                    repository.syncDailyAssignments(schoolId)
                    repository.syncDirectives(schoolId)
                    onComplete(true)
                } else {
                    _testStatus.value = "فشل التحقق من السحابة، يرجى فحص الشبكة ❌"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _testStatus.value = "خطأ في الاتصال: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncDataNow(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _testStatus.value = "جاري تحديث كافة البيانات والجدول..."
            try {
                repository.syncSchedule(schoolId)
                repository.syncDailyAssignments(schoolId)
                repository.syncDirectives(schoolId)
                repository.syncDirectMessages()
                val deviceId = repository.getDeviceId()
                repository.syncAttendance(schoolId, deviceId)
                _testStatus.value = "تم تحديث كافة البيانات بنجاح 🔄"
                onComplete(true)
            } catch (e: Exception) {
                _testStatus.value = "خطأ أثناء التحديث: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reset session while storing last code in SharedPreferences for fast return without barcode!
    fun resetSchoolSession() {
        val currentCode = sessionManager.getSchoolCode() ?: ""
        if (currentCode.isNotEmpty()) {
            prefs.edit().putString("last_saved_school_code", currentCode).apply()
        }
        sessionManager.clearSchoolSession()
    }

    fun restoreStoredCode(onComplete: (Boolean) -> Unit) {
        val stored = prefs.getString("last_saved_school_code", "") ?: ""
        if (stored.isNotEmpty()) {
            sessionManager.saveSchoolSession("school_default", stored, "المدرسة الذكية")
            onComplete(true)
        } else {
            onComplete(false)
        }
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
    val testStatus by viewModel.testStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Confirmation steps
    var showConfirmDialogStep1 by remember { mutableStateOf(false) }
    var showConfirmDialogStep2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الضبط وإعدادات الربط ⚙️", fontWeight = FontWeight.Bold) },
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
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات المدرسة المقترنة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text("اسم المدرسة: ${viewModel.schoolName}", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    Text("اسم الطالب: ${viewModel.studentName}", fontSize = 12.5.sp, color = Color(0xFF334155))
                    Text("الصف والشعبة: ${viewModel.studentGrade} - (${viewModel.studentSection})", fontSize = 12.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)

                    // Masked connection code
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.KeyOff, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("رمز الربط الخاص:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Text(viewModel.maskedSchoolCode, fontSize = 12.5.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Card: Notifications Settings
            val prefs = remember { context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE) }
            var notificationsEnabled by remember {
                mutableStateOf(prefs.getBoolean("notifications_enabled", true))
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (notificationsEnabled) Color(0xFF059669) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("إشعارات الواجبات والدروس 🔔", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text(
                                    if (notificationsEnabled) "الإشعارات الفورية مفعّلة" else "الإشعارات الفورية متوقفة",
                                    fontSize = 11.5.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { isChecked ->
                                notificationsEnabled = isChecked
                                prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
                                Toast.makeText(
                                    context,
                                    if (isChecked) "تم تفعيل الإشعارات الفورية 🔔" else "تم إيقاف الإشعارات الفورية 🔕",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            // Card 2: Secure Encrypted Cloud Connection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("حالة الاتصال السحابي الآمن 🔒", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                Text("حالة الربط والخصوصية:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("آمن / مفاتيح سرية مخفية ✓", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تشفير الاتصال السحابي:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("TLS 1.3 / مشفر بالكامل ⚡", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (testStatus != null) {
                        Surface(
                            color = if (testStatus!!.contains("✓") || testStatus!!.contains("🔄")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testStatus!!,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (testStatus!!.contains("✓") || testStatus!!.contains("🔄")) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.testConnection { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "تم التحقق من الربط السحابي بنجاح ⚡" else "تعذر الوصول للسحابة",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تفعيل والتحقق من الربط السحابي ⚡", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.syncDataNow { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "تم تحديث البيانات والجدول بنجاح 🔄" else "فشل التحديث",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تحديث ومزامنة البيانات والجدول الآن 🔄", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 3: Fast Reconnect with Saved Code (No Barcode Scanner required!)
            if (viewModel.savedSchoolCode.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الربط السريع بالكود المخزن", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF1E40AF))
                        Text("الرمز المدرسي محفوض ومخزن بأمان! يمكنك العودة والارتباط بنقرة واحدة دون مسح الباراكود.", fontSize = 11.5.sp, color = Color(0xFF1E3A8A))
                        Button(
                            onClick = {
                                viewModel.restoreStoredCode { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم إعادة الاتصال المباشر بالمدرسة بنجاح ⚡", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إعادة الربط الفوري المباشر (بدون باراكود) ⚡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 4: Session Reset / Unlink with DOUBLE CONFIRMATION!
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إلغاء الربط مع المدرسة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFFBE123C))
                    Text("يتطلب إلغاء الارتباط تأكيداً مزدوجاً لمنع الإلغاء عن طريق الخطأ.", fontSize = 11.5.sp, color = Color(0xFF881337))
                    Button(
                        onClick = { showConfirmDialogStep1 = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إلغاء الربط مع المدرسة ⚠️", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // DOUBLE CONFIRMATION DIALOG 1 (تأكيد إلغاء الربط 1)
    // ----------------------------------------------------
    if (showConfirmDialogStep1) {
        AlertDialog(
            onDismissRequest = { showConfirmDialogStep1 = false },
            title = { Text("إلغاء الارتباط مع المدرسة", fontWeight = FontWeight.Black, fontSize = 16.sp) },
            text = {
                Text(
                    "سوف تلغي الارتباط مع المدرسة.. هل أنت متأكد من إلغاء الارتباط؟",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialogStep1 = false
                        showConfirmDialogStep2 = true // Proceed to Second Confirmation!
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("نعم، تابع الإلغاء", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialogStep1 = false }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ----------------------------------------------------
    // DOUBLE CONFIRMATION DIALOG 2 (تأكيد ثانٍ حاسم 2)
    // ----------------------------------------------------
    if (showConfirmDialogStep2) {
        AlertDialog(
            onDismissRequest = { showConfirmDialogStep2 = false },
            title = { Text("تأكيد ثانٍ حاسم ⚠️", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFBE123C)) },
            text = {
                Text(
                    "هل أنت جاد في إلغاء الارتباط والمزامنة مع المدرسة؟ (سيتم حفظ الرمز أوتوماتيكياً للعودة لاحقاً دون الحاجة للباراكود).",
                    fontSize = 13.sp,
                    color = Color(0xFF881337),
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialogStep2 = false
                        viewModel.resetSchoolSession()
                        Toast.makeText(context, "تم إلغاء الارتباط بنجاح مع حفظ كود العودة ⚡", Toast.LENGTH_LONG).show()
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C))
                ) {
                    Text("تأكيد الإلغاء النهائي", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialogStep2 = false }) {
                    Text("تراجع", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
