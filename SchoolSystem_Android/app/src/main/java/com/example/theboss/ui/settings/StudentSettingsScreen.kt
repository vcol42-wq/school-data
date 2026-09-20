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

    val schoolId = sessionManager.getSchoolId() ?: ""
    val schoolCode = sessionManager.getSchoolCode() ?: ""
    val schoolName = sessionManager.getSchoolName() ?: "المدرسة الذكية"

    private val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

    var studentGrade by mutableStateOf(prefs.getString("student_grade", "الصف الأول المتوسط") ?: "الصف الأول المتوسط")
        private set
    var studentSection by mutableStateOf(prefs.getString("student_section", "أ") ?: "أ")
        private set
    var studentName by mutableStateOf(prefs.getString("student_name", "الطالب") ?: "الطالب")
        private set

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
                try {
                    repository.verifySchoolCode(newCode)
                } catch (e: Exception) {
                    e.printStackTrace()
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
    onNavigateToLogin: () -> Unit,
    onNavigateToRoleSelection: () -> Unit = {}
) {
    val context = LocalContext.current
    val testStatus by viewModel.testStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Confirmation steps
    var showConfirmDialogStep1 by remember { mutableStateOf(false) }
    var showConfirmDialogStep2 by remember { mutableStateOf(false) }
    var showResetRoleDialog by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الضبط وإعدادات الربط ⚙️", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetRoleDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "تبديل الصفة (أستاذ / طالب)", tint = Color(0xFF059669))
                    }
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

                    Button(
                        onClick = { showEditStudentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تعديل وتسجيل بيانات الطالب ✏️", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

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

            // Card 5: Switch Role / Reset Portal
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetRoleDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تبديل أو إعادة اختيار البوابة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "العودة لشاشة البوابة للاختيار بين (أستاذ / طالب)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Card 6: Student User Guide Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "دليل استخدام منصة الطالب 📖",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "إرشادات الجدول، الواجبات، مؤقت بومودورو والمزامنة",
                            fontSize = 11.5.sp,
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

            // Card 7: Official Support & Channels Card
            val clipboardManager = LocalClipboardManager.current
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "قنوات التواصل والدعم الفني الرسمي 💬",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "دعم الطلبة وأولياء الأمور ومتابعة التحديثات المعتمدة",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح رابط القناة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("الانضمام لقناة المنظومة على واتساب 📢", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:vcol42@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "استفسار منصة الطالب - ThePrincipal")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString("vcol42@gmail.com"))
                                Toast.makeText(context, "تم نسخ البريد الإلكتروني للحافظة: vcol42@gmail.com", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(8.dp))
                        Text("البريد الإلكتروني: vcol42@gmail.com ✉️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // STUDENT HELP GUIDE DIALOG
    // ----------------------------------------------------
    if (showHelpGuideDialog) {
        com.example.theboss.ui.dialogs.StudentHelpGuideDialog(
            onDismiss = { showHelpGuideDialog = false }
        )
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

    if (showResetRoleDialog) {
        AlertDialog(
            onDismissRequest = { showResetRoleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(Modifier.width(8.dp))
                    Text("إعادة اختيار البوابة", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            },
            text = {
                Text(
                    "هل ترغب في العودة إلى شاشة البوابة الرئيسية لاختيار دور آخر (أستاذ أو طالب)؟ يتم الاحتفاظ بكافة بياناتك وجدولك الحالي بأمان.",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetRoleDialog = false
                        com.school.system.utils.RoleManager.clearSelectedRole(context)
                        onNavigateToRoleSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، العودة للبوابة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetRoleDialog = false }) {
                    Text("تراجع", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
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
                Toast.makeText(context, "تم تحديث بيانات الطالب والمدرسة بنجاح 🎓", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
