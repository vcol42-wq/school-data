package com.example.theboss.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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

    var schoolId by mutableStateOf(sessionManager.getSchoolId() ?: "")
        private set
    var schoolCode by mutableStateOf(sessionManager.getSchoolCode() ?: "")
        private set
    var schoolName by mutableStateOf(sessionManager.getSchoolName() ?: "المدرسة الذكية")
        private set

    private val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

    var studentGrade by mutableStateOf(prefs.getString("student_grade", "الصف الأول المتوسط") ?: "الصف الأول المتوسط")
        private set
    var studentSection by mutableStateOf(prefs.getString("student_section", "أ") ?: "أ")
        private set
    var studentName by mutableStateOf(prefs.getString("student_name", "الطالب") ?: "الطالب")
        private set

    fun pairWithQrData(
        schoolId: String,
        schoolCode: String,
        schoolName: String,
        url: String,
        apiKey: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = repository.pairSchoolDirectly(
                    schoolId = schoolId,
                    schoolCode = schoolCode,
                    schoolName = schoolName,
                    url = url.ifBlank { null },
                    apiKey = apiKey.ifBlank { null }
                )
                if (res.isSuccess) {
                    this@StudentSettingsViewModel.schoolId = schoolId
                    this@StudentSettingsViewModel.schoolCode = schoolCode
                    this@StudentSettingsViewModel.schoolName = schoolName
                    _testStatus.value = "تم الاقتران بمدرسة ($schoolName) وتحديث الجداول بنجاح ✓"
                } else {
                    _testStatus.value = "فشل في حفظ بيانات الربط السحابي ❌"
                }
            } catch (e: Exception) {
                _testStatus.value = "خطأ في الربط: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStudentInfo(name: String, grade: String, section: String) {
        studentName = name.trim()
        studentGrade = grade.trim()
        studentSection = section.trim()

        prefs.edit()
            .putString("student_name", studentName)
            .putString("student_grade", studentGrade)
            .putString("student_section", studentSection)
            .apply()

        try {
            context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE).edit()
                .putString("selected_grade", studentGrade)
                .putString("selected_section", studentSection)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        syncDataNow()
    }

    fun updateStudentAndSchool(name: String, grade: String, section: String, newCode: String) {
        updateStudentInfo(name, grade, section)
        if (newCode.isNotBlank()) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val res = repository.verifySchoolCode(newCode)
                    if (res.isSuccess) {
                        this@StudentSettingsViewModel.schoolId = sessionManager.getSchoolId() ?: ""
                        this@StudentSettingsViewModel.schoolCode = sessionManager.getSchoolCode() ?: newCode
                        this@StudentSettingsViewModel.schoolName = sessionManager.getSchoolName() ?: "المدرسة الذكية"
                        _testStatus.value = "تم التحقق والاقتران بمدرسة (${this@StudentSettingsViewModel.schoolName}) بنجاح ✓"
                    } else {
                        _testStatus.value = res.exceptionOrNull()?.message ?: "كود المدرسة غير صحيح ❌"
                    }
                } catch (e: Exception) {
                    _testStatus.value = "خطأ في التحقق: ${e.localizedMessage}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    // Previously stored school code for fast 1-tap re-pairing without barcode!
    val savedSchoolCode: String = prefs.getString("last_saved_school_code", "") ?: ""

    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val maskedSchoolCode: String
        get() = if (schoolCode.length >= 4) {
            "SCH-••••-" + schoolCode.takeLast(4)
        } else if (savedSchoolCode.length >= 4) {
            "SCH-••••-" + savedSchoolCode.takeLast(4)
        } else {
            "غير مقترن"
        }

    fun testConnection(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val currentSchoolId = sessionManager.getSchoolId()
            if (currentSchoolId.isNullOrBlank() || !sessionManager.isSchoolConfigured()) {
                _testStatus.value = "التطبيق غير مقترن بمدرسة سحابية حالياً ❌"
                onComplete(false)
                return@launch
            }
            _isLoading.value = true
            _testStatus.value = "جاري التحقق من الربط السحابي..."
            try {
                val success = repository.testCloudConnection()
                if (success) {
                    _testStatus.value = "تم تفعيل والتحقق من الربط السحابي بنجاح ✓"
                    repository.syncSchedule(currentSchoolId)
                    repository.syncDailyAssignments(currentSchoolId)
                    repository.syncDirectives(currentSchoolId)
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
            val currentSchoolId = sessionManager.getSchoolId()
            if (currentSchoolId.isNullOrBlank() || !sessionManager.isSchoolConfigured()) {
                onComplete(false)
                return@launch
            }
            _isLoading.value = true
            _testStatus.value = "جاري تحديث كافة البيانات والجدول..."
            try {
                val recordNumber = repository.getStudentRecordNumber()
                repository.syncSchedule(currentSchoolId)
                repository.syncDailyAssignments(currentSchoolId)
                repository.syncDirectives(currentSchoolId)
                repository.syncStudentGrades(currentSchoolId, recordNumber)
                repository.syncAttendance(currentSchoolId, recordNumber)
                _testStatus.value = "تم تحديث كافة البيانات والدرجات بنجاح 🔄"
                onComplete(true)
            } catch (e: Exception) {
                _testStatus.value = "خطأ أثناء التحديث: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSchoolSession() {
        sessionManager.clearSchoolSession()
        prefs.edit()
            .remove("last_saved_school_code")
            .remove("school_id")
            .remove("school_code")
            .remove("school_name")
            .remove("synced_schedule")
            .remove("independent_mode")
            .apply()
        this.schoolId = ""
        this.schoolCode = ""
        this.schoolName = "غير مقترن"
        _testStatus.value = "تم إلغاء الارتباط بالمدرسة بنجاح"
    }

    fun switchToIndependent() {
        sessionManager.saveSchoolSession(
            schoolId = "independent_student",
            schoolCode = "INDEPENDENT",
            schoolName = "طالب مستقل"
        )
        prefs.edit()
            .putBoolean("independent_mode", true)
            .putString("student_name", if (studentName != "الطالب" && studentName.isNotBlank()) studentName else "طالب مستقل")
            .apply()
        this.schoolId = "independent_student"
        this.schoolCode = "INDEPENDENT"
        this.schoolName = "طالب مستقل"
        _testStatus.value = "أنت الآن في وضع الطالب المستقل (بدون ربط مدرسي) 👤"
    }

    fun isIndependent(): Boolean = schoolId == "independent_student" || sessionManager.isIndependentStudent()

    fun restoreStoredCode(onComplete: (Boolean) -> Unit) {
        val stored = prefs.getString("last_saved_school_code", "") ?: ""
        if (stored.isNotEmpty()) {
            viewModelScope.launch {
                val res = repository.verifySchoolCode(stored)
                if (res.isSuccess) {
                    schoolId = sessionManager.getSchoolId() ?: ""
                    schoolCode = sessionManager.getSchoolCode() ?: stored
                    schoolName = sessionManager.getSchoolName() ?: "المدرسة الذكية"
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
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
    onNavigateToLogin: () -> Unit,
    onNavigateToQrScanner: () -> Unit = {},
    navController: androidx.navigation.NavController? = null
) {
    val context = LocalContext.current
    val testStatus by viewModel.testStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showUnpairConfirmDialog by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf(false) }

    // Listen to barcode scanned from QrScannerScreen
    val scannedCode = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("scanned_code", null)
        ?.collectAsState()

    LaunchedEffect(scannedCode?.value) {
        scannedCode?.value?.let { code ->
            try {
                val clean = code.trim()
                if (clean.startsWith("{") && clean.endsWith("}")) {
                    val gson = com.google.gson.Gson()
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    val data: Map<String, Any> = gson.fromJson(clean, mapType)
                    
                    val url = data["url"]?.toString()?.trim()?.removeSurrounding("\"") ?: ""
                    val apiKey = data["apiKey"]?.toString()?.trim()?.removeSurrounding("\"") ?: ""
                    val sCode = (data["schoolId"] ?: data["id"])?.toString()?.trim()?.removeSurrounding("\"") ?: ""
                    val pCode = (data["studentPairingCode"] ?: data["student_pairing_code"] ?: data["pairingCode"])
                        ?.toString()?.trim()?.removeSurrounding("\"") ?: ""
                    val sName = (data["schoolName"] ?: data["name"])?.toString()?.trim()?.removeSurrounding("\"") ?: ""
                    
                    val targetCode = if (pCode.isNotBlank()) pCode else sCode
                    val targetName = if (sName.isNotBlank()) sName else "المدرسة الذكية"
                    viewModel.pairWithQrData(
                        schoolId = sCode,
                        schoolCode = targetCode,
                        schoolName = targetName,
                        url = url,
                        apiKey = apiKey
                    )
                    val nameMsg = if (sName.isNotBlank()) " ($sName)" else ""
                    Toast.makeText(context, "تم مسح باركود المدرسة وتحديث الربط بنجاح$nameMsg ✓", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateStudentAndSchool(viewModel.studentName, viewModel.studentGrade, viewModel.studentSection, clean)
                    Toast.makeText(context, "تم مسح كود المدرسة بنجاح: $clean ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                viewModel.updateStudentAndSchool(viewModel.studentName, viewModel.studentGrade, viewModel.studentSection, code.trim())
                Toast.makeText(context, "تم مسح الكود: ${code.trim()}", Toast.LENGTH_SHORT).show()
            }
            navController?.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات بوابة الطالب ⚙️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpGuideDialog = true }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "دليل الاستخدام", tint = Color(0xFF059669))
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Student & School Profile
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات الطالب والمدرسة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text("اسم الطالب: ${viewModel.studentName}", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    Text("المدرسة: ${viewModel.schoolName}", fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                    Text("الصف والشعبة: ${viewModel.studentGrade} - (${viewModel.studentSection})", fontSize = 12.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)

                    Button(
                        onClick = { showEditStudentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تعديل بيانات الطالب والشعبة ✏️", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 2: Barcode Scanner & Cloud Sync (Super Clean & Direct)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("قارئ الباركود والمزامنة السحابية 📷", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF166534))
                    }

                    Text(
                        "امسح باركود المدرسة المعروض في شاشة المدير لربط التطبيق أو تحديث الجداول فورياً.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF15803D)
                    )

                    // Prominent Barcode Scanner Button
                    Button(
                        onClick = onNavigateToQrScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("مسح باركود المدرسة (QR Scanner) 📸", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    // Sync & Update Now Button
                    OutlinedButton(
                        onClick = {
                            viewModel.syncDataNow { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "تم تحديث البيانات والجدول بنجاح 🔄" else "تعذر التحديث، تحقق من الاتصال",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                        border = BorderStroke(1.2.dp, Color(0xFF0284C7)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0284C7), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تحديث ومزامنة البيانات والجدول الآن 🔄", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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
                                modifier = Modifier.padding(10.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = if (testStatus!!.contains("✓") || testStatus!!.contains("🔄")) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // Card 3: Notifications
            val prefs = remember { context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE) }
            var notificationsEnabled by remember {
                mutableStateOf(prefs.getBoolean("notifications_enabled", true))
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("إشعارات الحصص والواجبات 🔔", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text(
                                if (notificationsEnabled) "الإشعارات الفورية مفعّلة" else "الإشعارات الفورية متوقفة",
                                fontSize = 11.sp,
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
                                if (isChecked) "تم تفعيل الإشعارات الفورية 🔔" else "تم إيقاف الإشعارات 🔕",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // Card 4: Student User Guide
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHelpGuideDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "دليل استخدام منصة الطالب 📖",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "إرشادات الجدول، الواجبات، ومتابعة الدروس",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Card: Independent Student Mode
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isIndependent()) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.2.dp, if (viewModel.isIndependent()) Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (viewModel.isIndependent()) Color(0xFF16A34A) else Color(0xFF0F766E),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "وضع الطالب المستقل 👤",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    if (viewModel.isIndependent()) "الحالة: مفعّل كطالب مستقل (بدون ربط مدرسي)" else "استخدام التطبيق بدون ربط بأي مدرسة لمتابعة الدروس شخصياً",
                                    fontSize = 11.sp,
                                    color = if (viewModel.isIndependent()) Color(0xFF15803D) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    if (!viewModel.isIndependent()) {
                        Button(
                            onClick = {
                                viewModel.switchToIndependent()
                                Toast.makeText(context, "تم التحويل إلى وضع الطالب المستقل 👤", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("التحويل إلى طالب مستقل 👤", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "✓ وضع الطالب المستقل نشط حالياً. يمكنك مسح باركود المدرسة في أي وقت للارتباط.",
                                modifier = Modifier.padding(8.dp),
                                fontSize = 11.sp,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                com.school.system.utils.RoleManager.resetRoleBeforeLock(context)
                                navController?.navigate("role_selection") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E)),
                            border = BorderStroke(1.2.dp, Color(0xFF0D9488)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("الرجوع إلى اختيار البوابات ↩️ (طالب / أستاذ / مدير)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Card 5: Unlink School
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إلغاء الارتباط مع المدرسة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFBE123C))
                        Text("للربط مع مدرسة أخرى أو إعادة المسح", fontSize = 11.sp, color = Color(0xFF881337))
                    }
                    Button(
                        onClick = { showUnpairConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء الربط ⚠️", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Student Help Guide Dialog
    if (showHelpGuideDialog) {
        com.example.theboss.ui.dialogs.StudentHelpGuideDialog(
            onDismiss = { showHelpGuideDialog = false }
        )
    }

    // Single Clean Unpair Confirmation Dialog
    if (showUnpairConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirmDialog = false },
            title = { Text("إلغاء الارتباط مع المدرسة ⚠️", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFBE123C)) },
            text = {
                Text(
                    "هل أنت متأكد من إلغاء الارتباط بالمدرسة الحالية؟ ستتمكن من مسح باركود مدرسة جديدة أو إعادة الانضمام في أي وقت.",
                    fontSize = 12.5.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpairConfirmDialog = false
                        viewModel.resetSchoolSession()
                        Toast.makeText(context, "تم إلغاء الارتباط بنجاح", Toast.LENGTH_SHORT).show()
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، إلغاء الارتباط", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairConfirmDialog = false }) {
                    Text("تراجع", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                }
            }
        )
    }

    if (showEditStudentDialog) {
        com.example.theboss.ui.dashboard.StudentInfoEditDialog(
            initialName = viewModel.studentName,
            initialGrade = viewModel.studentGrade,
            initialSection = viewModel.studentSection,
            initialSchoolCode = viewModel.schoolCode,
            onDismiss = { showEditStudentDialog = false },
            onSave = { newName, newGrade, newSection, newCode ->
                viewModel.updateStudentAndSchool(newName, newGrade, newSection, newCode)
                showEditStudentDialog = false
                Toast.makeText(context, "تم حفظ بيانات الطالب بنجاح 🎓", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
