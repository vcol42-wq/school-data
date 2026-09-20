package com.school.system.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.system.data.AuthRepository
import com.school.system.data.SyncManager
import com.school.system.data.dao.ClassPackageDao
import com.school.system.data.dao.ConfigDao
import com.school.system.data.dao.StudentDao
import com.school.system.data.local.SecureKeyStorage
import com.school.system.data.model.ClassPackage
import com.school.system.data.model.SchoolConfig
import com.school.system.data.models.JoinRequest
import com.school.system.data.repository.SchoolRepository
import com.school.system.ui.components.HelpGuideDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val configDao: ConfigDao,
    val packageDao: ClassPackageDao,
    val studentDao: StudentDao,
    val syncManager: SyncManager,
    val authRepository: AuthRepository,
    val schoolRepository: SchoolRepository,
    val secureKeyStorage: SecureKeyStorage,
    val syncRepository: com.school.system.data.SyncRepository
) : ViewModel() {
    val config = configDao.getConfig()
        .map { it ?: SchoolConfig(isActivated = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _connectionStatus = kotlinx.coroutines.flow.MutableStateFlow("")
    val connectionStatus = _connectionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            fixCloudUrlIfInvalid()
            checkSupabaseConnection()
        }
    }

    private suspend fun fixCloudUrlIfInvalid() {
        val current = configDao.getConfig().first() ?: return
        val url = current.cloudUrl
        if (url.contains("your-supabase-project") || url.contains("placeholder") || url.contains("example.com")) {
            configDao.saveConfig(current.copy(
                cloudUrl = com.school.system.data.SyncRepository.DEFAULT_SUPABASE_URL,
                cloudKey = com.school.system.data.SyncRepository.DEFAULT_ANON_KEY
            ))
        }
    }

    suspend fun checkSupabaseConnection() {
        try {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            if (current.schoolId.isEmpty() || !current.isVerified) {
                _connectionStatus.value = "الوضع المحلي (أوفلاين) 👤"
                return
            }

            val url = if (current.cloudUrl.isNotEmpty() && !current.cloudUrl.contains("your-supabase-project"))
                current.cloudUrl else com.school.system.data.SyncRepository.DEFAULT_SUPABASE_URL
            
            val key = if (current.cloudKey.isNotEmpty()) current.cloudKey else com.school.system.data.SyncRepository.DEFAULT_ANON_KEY
            val schoolId = current.schoolId
            
            _connectionStatus.value = "جاري فحص الاتصال بالسحابة..."
            
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http")) formattedUrl = "https://$formattedUrl"
            val testUrl = if (formattedUrl.endsWith("/")) "${formattedUrl}rest/v1/schools?id=eq.$schoolId" else "$formattedUrl/rest/v1/schools?id=eq.$schoolId"
            
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val request = okhttp3.Request.Builder()
                .url(testUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("x-school-id", schoolId)
                .build()
            
            val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    client.newCall(request).execute()
                } catch (e: Exception) {
                    null
                }
            }
            
            if (response == null) {
                _connectionStatus.value = "السحابة: تعذر الوصول إلى الخادم (تأكد من الإنترنت)"
                return
            }

            _connectionStatus.value = when (response.code) {
                200 -> "السحابة: متصل ومصادق ✓ (المزامنة السحابية نشطة)"
                401, 403 -> "السحابة: متصل ولكن يتطلب تفعيل الصلاحيات (401)"
                404 -> "السحابة: تعذر العثور على بيانات المدرسة"
                else -> "السحابة: تعذر إكمال الفحص (رمز ${response.code})"
            }
        } catch (e: Exception) {
            _connectionStatus.value = "السحابة: تعذر إكمال الفحص، حاول مرة أخرى"
        }
    }

    fun verify(code: String) {
        viewModelScope.launch {
            _connectionStatus.value = "جاري التحقق والاتصال بالمدرسة السحابية..."
            val ok = syncManager.connectAndPairQr(code)
            if (ok) {
                _connectionStatus.value = "تم ربط المدرسة وتنزيل الشعب والطلاب بنجاح ✓"
                checkSupabaseConnection()
            } else {
                // Fallback to legacy school verification if simple code
                val result = schoolRepository.verifySchoolCode(code)
                if (result.isSuccess) {
                    val current = configDao.getConfig().first() ?: SchoolConfig()
                    val schoolId = schoolRepository.getSchoolId() ?: "SCH-KAB2-9359"
                    val schoolName = schoolRepository.getSchoolName() ?: "مدرسة سحابية"
                    
                    configDao.saveConfig(
                        current.copy(
                            schoolId = schoolId,
                            schoolName = schoolName,
                            pairingCode = code,
                            cloudUrl = com.school.system.data.SyncRepository.DEFAULT_SUPABASE_URL,
                            cloudKey = com.school.system.data.SyncRepository.DEFAULT_ANON_KEY,
                            isVerified = true,
                            isActivated = true
                        )
                    )

                    val syncSuccess = syncManager.fetchDataFromPrincipal()
                    if (syncSuccess) {
                        _connectionStatus.value = "تم ربط المدرسة وتنزيل الشعب والطلاب بنجاح ✓"
                    } else {
                        _connectionStatus.value = "تم ربط المدرسة بنجاح ✓ (جاهز للمزامنة)"
                    }
                    checkSupabaseConnection()
                } else {
                    _connectionStatus.value = "فشل الربط: ${result.exceptionOrNull()?.message ?: "تعذر التحقق من الكود"}"
                }
            }
        }
    }

    fun syncFromPrincipal() {
        viewModelScope.launch { syncManager.fetchDataFromPrincipal() }
    }

    fun unpairSchool() {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                schoolId = "",
                schoolName = "",
                pairingCode = "",
                isVerified = false,
                isActivated = false,
                syncSealToken = null
            ))
            _connectionStatus.value = "الوضع المحلي (أوفلاين) 👤"
        }
    }

    fun updateGeminiApiKey(key: String, isAiEnabled: Boolean) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                geminiApiKey = key.trim(),
                isAiActivated = isAiEnabled
            ))
        }
    }

    fun updateTeacherProfile(name: String, email: String) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                managerName = name.trim(),
                userEmail = email.trim()
            ))
        }
    }

    fun saveTeacherProfileAndSubject(name: String, email: String, subject: String, gender: String) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                managerName = name.trim(),
                userEmail = email.trim()
            ))
            if (subject.isNotBlank()) {
                val existing = packageDao.getAllPackagesList()
                val cleanSub = subject.trim()
                if (existing.none { it.subject.trim().equals(cleanSub, ignoreCase = true) }) {
                    packageDao.insertPackage(
                        ClassPackage(
                            grade = "الأول المتوسط",
                            section = "أ",
                            subject = cleanSub,
                            iconName = ""
                        )
                    )
                }
            }
        }
    }

    fun getSavedSupervisorCode(): String? = secureKeyStorage.getSupervisorCode()

    fun setSupervisorMode(enabled: Boolean, code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            if (enabled) {
                val cleanCode = code.trim()
                if (cleanCode.isBlank()) {
                    onResult(false, "يرجى إدخال رمز مدير المدرسة أو مسؤول الإشراف")
                    return@launch
                }
                secureKeyStorage.saveSupervisorCode(cleanCode)
                configDao.saveConfig(current.copy(
                    role = "supervisor",
                    syncSealToken = if (current.syncSealToken.isNullOrEmpty()) "__supervisor__" else current.syncSealToken
                ))
                // سحب كافة شعب وصفوف المدرسة فورياً
                if (current.schoolId.isNotEmpty() && current.schoolId != "school_01") {
                    try {
                        syncRepository.downloadRoster(
                            schoolId = current.schoolId,
                            teacherId = "__supervisor__",
                            providedUrl = current.cloudUrl,
                            providedKey = current.cloudKey
                        )
                    } catch (_: Exception) {}
                }
                onResult(true, "تم تفعيل وضع الإدارة والإشراف وسحب كافة شعب المدرسة بنجاح 🛡️")
            } else {
                configDao.saveConfig(current.copy(role = "teacher"))
                onResult(true, "تم العودة إلى وضع الأستاذ التدريسي 👨‍🏫")
            }
        }
    }

    fun syncAllSchoolClassesForSupervisor(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: return@launch onResult(false, "لم يتم العثور على إعدادات المدرسة")
            val success = syncRepository.downloadRoster(
                schoolId = current.schoolId,
                teacherId = "__supervisor__",
                providedUrl = current.cloudUrl,
                providedKey = current.cloudKey
            )
            if (success) {
                onResult(true, "تم سحب وتحديث كافة شعب وصفوف المدرسة بنجاح 🏫✓")
            } else {
                onResult(false, "تعذر سحب الشعب. يرجى التحقق من اتصال الإنترنت أو اتصال السحابة.")
            }
        }
    }

    fun activate(name: String, email: String, url: String, schoolId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            var formattedUrl = url.trim()
            if (formattedUrl.isNotEmpty() && !formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            if (formattedUrl.isEmpty()) {
                formattedUrl = com.school.system.data.SyncRepository.DEFAULT_SUPABASE_URL
            }
            val current = configDao.getConfig().first() ?: SchoolConfig()
            val isOnline = schoolId.isNotEmpty()
            configDao.saveConfig(current.copy(
                schoolName = if (isOnline) "مدرسة مرتبطة" else "سجل مستقل (أوفلاين)", 
                managerName = name,
                userEmail = email,
                cloudUrl = formattedUrl,
                cloudKey = com.school.system.data.SyncRepository.DEFAULT_ANON_KEY,
                schoolId = schoolId,
                isActivated = true,
                isVerified = isOnline
            ))
            delay(500)
            onComplete()
        }
    }

    fun activateStandaloneWithSubject(
        name: String,
        email: String,
        subject: String,
        grade: String,
        section: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                schoolName = "سجل مستقل (أوفلاين)",
                managerName = name.ifBlank { "أستاذ المادة" },
                userEmail = email.ifBlank { "teacher@local.edu" },
                cloudUrl = com.school.system.data.SyncRepository.DEFAULT_SUPABASE_URL,
                cloudKey = com.school.system.data.SyncRepository.DEFAULT_ANON_KEY,
                schoolId = "",
                isActivated = true,
                isVerified = false
            ))
            if (subject.isNotBlank()) {
                packageDao.insertPackage(
                    ClassPackage(
                        grade = grade.ifBlank { "الأول" },
                        section = section.ifBlank { "أ" },
                        subject = subject.trim(),
                        iconName = ""
                    )
                )
            }
            delay(500)
            onComplete()
        }
    }

    fun submitJoin(request: JoinRequest, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = schoolRepository.submitJoinRequest(request)
            if (result.isSuccess) {
                onResult(true, "تم إرسال طلب الانضمام بنجاح ✓")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "فشل إرسال الطلب")
            }
        }
    }

    fun exportBackup(context: Context, onReady: (java.io.File) -> Unit) {
        viewModelScope.launch {
            try {
                val packages = packageDao.getAllPackagesList()
                val students = studentDao.getAllStudentsList()
                val currentConfig = configDao.getConfig().first()

                val backupJson = org.json.JSONObject().apply {
                    put("appName", "SmartTeacherRegister")
                    put("version", "2.5")
                    put("exportDate", java.time.LocalDateTime.now().toString())
                    put("teacherName", currentConfig?.managerName ?: "")
                    put("teacherEmail", currentConfig?.userEmail ?: "")

                    val pkgsArray = org.json.JSONArray()
                    packages.forEach { p ->
                        pkgsArray.put(org.json.JSONObject().apply {
                            put("grade", p.grade)
                            put("section", p.section)
                            put("subject", p.subject)
                        })
                    }
                    put("packages", pkgsArray)

                    val studentsArray = org.json.JSONArray()
                    students.forEach { s ->
                        studentsArray.put(org.json.JSONObject().apply {
                            put("fullName", s.fullName)
                            put("grade", s.grade)
                            put("section", s.section)
                            put("subject", s.subject)
                            put("annualAverage", s.marks.annualAverage)
                            put("finalGrade", s.marks.finalGrade)
                        })
                    }
                    put("students", studentsArray)
                }

                val backupFile = java.io.File(context.cacheDir, "سجل_المدرس_نسخة_احتياطية_${System.currentTimeMillis()}.json")
                backupFile.writeText(backupJson.toString(2), Charsets.UTF_8)
                onReady(backupFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController? = null
) {
    val config by viewModel.config.collectAsState()

    val scannedCode = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("scanned_code")
        ?.observeAsState()

    LaunchedEffect(scannedCode?.value) {
        scannedCode?.value?.let { code ->
            viewModel.verify(code)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }

    val quickSubjects = listOf(
        "اللغة العربية", "الرياضيات", "التربية الإسلامية", "اللغة الإنكليزية",
        "العلوم", "الفيزياء", "الكيمياء", "الأحياء", "الاجتماعيات",
        "الحاسوب", "التربية الأخلاقية", "التربية الفنية", "النشاط البدني", "النشيد والموسيقى", "الفرنسية"
    )

    val schoolGenders = listOf("بنين", "بنات", "مختلط")

    var teacherName by remember { mutableStateOf(prefs.getString("teacher_name", "") ?: "") }
    var teacherEmail by remember { mutableStateOf("") }
    var teacherSubject by remember { mutableStateOf(prefs.getString("teacher_subject", "اللغة العربية") ?: "اللغة العربية") }
    var schoolGender by remember { mutableStateOf(prefs.getString("school_gender", "بنين") ?: "بنين") }

    var supervisorCodeInput by remember { mutableStateOf(viewModel.getSavedSupervisorCode() ?: "") }
    var isSupervisorMode by remember { mutableStateOf(false) }

    var geminiApiKeyInput by remember { mutableStateOf("") }
    var isAiEnabled by remember { mutableStateOf(true) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showUnpairConfirmDialog by remember { mutableStateOf(false) }
    var showResetRoleDialog by remember { mutableStateOf(false) }
    var showConnectWarningDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val isPaired = config?.isVerified == true && !config?.schoolId.isNullOrEmpty()

    LaunchedEffect(config) {
        config?.let { 
            if (it.managerName.isNotBlank()) teacherName = it.managerName
            teacherEmail = it.userEmail
            geminiApiKeyInput = it.geminiApiKey
            isAiEnabled = it.isAiActivated
            isSupervisorMode = (it.role == "supervisor")
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("إعدادات المنظومة ⚙️", fontWeight = FontWeight.Black, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetRoleDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "تبديل الصفة (أستاذ / طالب)", tint = Color(0xFF2563EB))
                    }
                    IconButton(onClick = { showHelpGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "دليل الاستخدام والتعليمات", tint = Color(0xFF2563EB))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CARD 1: اقتران وسحابة المدرسة عبر الباركود فقط
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isPaired) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isPaired) Icons.Default.CloudDone else Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (isPaired) Color(0xFF059669) else Color(0xFF2563EB),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPaired) "الربط السحابي مع المدرسة 🟢" else "ربط المدرسة عبر الباركود 📷",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isPaired) "الحالة: مقترن ونشط بالسحابة المدرسية" else "الحالة: غير مقترن (الوضع المحلي المستقل)",
                                    fontSize = 11.5.sp,
                                    color = if (isPaired) Color(0xFF059669) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isPaired) {
                        // Paired State: Show school name & Unpair / Re-scan buttons
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "المدرسة: ${config?.schoolName?.ifBlank { "مدرستي" } ?: "مدرستي"}", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Black, 
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "المزامنة نشطة: يتم حفظ ومزامنة الدرجات وجداول الحصص تلقائياً مع الإدارة.", 
                                    fontSize = 11.sp, 
                                    color = Color(0xFF15803D), 
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showUnpairConfirmDialog = true },
                                modifier = Modifier.weight(1f).height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("إلغاء الربط السحابي ✕", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                            }

                            OutlinedButton(
                                onClick = { showConnectWarningDialog = true },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مسح باركود جديد 🔄", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Unpaired State: Single Primary Button to Activate Pairing via Barcode
                        Button(
                            onClick = { showConnectWarningDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "تفعيل الربط السحابي ومسح الباركود 📷",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp
                            )
                        }

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 لربط التطبيق مع مدرستك: اضغط على زر تفعيل الربط أعلاه ووجّه الكاميرا نحو باركود QR المعروض على شاشة حاسبة الإدارة ليتم استيراد صفوفك وطلابك وحصصك مباشرة.",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // CARD 3: بيانات الأستاذ والمادة التدريسية
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("بيانات الأستاذ والمادة التدريسية 📝", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                    }

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { 
                            teacherName = it 
                            prefs.edit().putString("teacher_name", it.trim()).apply()
                        },
                        label = { Text("اسم الأستاذ الكامل") },
                        placeholder = { Text("اكتب اسمك الثلاثي أو الكامل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    var settingSubExpanded by remember { mutableStateOf(false) }
                    val filteredSettingSubjects = remember(teacherSubject) {
                        if (teacherSubject.isBlank()) quickSubjects
                        else quickSubjects.filter { it.contains(teacherSubject.trim(), ignoreCase = true) }
                    }

                    ExposedDropdownMenuBox(
                        expanded = settingSubExpanded,
                        onExpandedChange = { settingSubExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = teacherSubject,
                            onValueChange = { 
                                teacherSubject = it
                                settingSubExpanded = true
                            },
                            label = { Text("المادة أو الاختصاص التدريسي 📚") },
                            placeholder = { Text("اكتب اسم المادة أو اختر للسرعة...") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = settingSubExpanded)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (filteredSettingSubjects.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = settingSubExpanded,
                                onDismissRequest = { settingSubExpanded = false }
                            ) {
                                filteredSettingSubjects.forEach { s ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = s, 
                                                fontWeight = if (s == teacherSubject) FontWeight.Black else FontWeight.Medium,
                                                color = if (s == teacherSubject) Color(0xFF2563EB) else Color(0xFF1E293B)
                                            ) 
                                        },
                                        onClick = {
                                            teacherSubject = s
                                            settingSubExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // خيار جنس المدرسة
                    var settingGenderExpanded by remember { mutableStateOf(false) }
                    val filteredSettingGenders = remember(schoolGender) {
                        if (schoolGender.isBlank()) schoolGenders
                        else schoolGenders.filter { it.contains(schoolGender.trim(), ignoreCase = true) }
                    }

                    ExposedDropdownMenuBox(
                        expanded = settingGenderExpanded,
                        onExpandedChange = { settingGenderExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = schoolGender,
                            onValueChange = { 
                                schoolGender = it
                                settingGenderExpanded = true
                            },
                            label = { Text("جنس المدرسة 🏛️") },
                            placeholder = { Text("اختر أو اكتب: بنين، بنات، مختلط...") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = settingGenderExpanded)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (filteredSettingGenders.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = settingGenderExpanded,
                                onDismissRequest = { settingGenderExpanded = false }
                            ) {
                                filteredSettingGenders.forEach { g ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = g, 
                                                fontWeight = if (g == schoolGender) FontWeight.Black else FontWeight.Medium,
                                                color = if (g == schoolGender) Color(0xFF2563EB) else Color(0xFF1E293B)
                                            ) 
                                        },
                                        onClick = {
                                            schoolGender = g
                                            settingGenderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = teacherEmail,
                        onValueChange = { teacherEmail = it },
                        label = { Text("البريد الإلكتروني للنسخ والربط السحابي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Save Teacher Profile & Subject Button (Explicit Green Button)
                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("teacher_name", teacherName.trim())
                                .putString("teacher_subject", teacherSubject.trim())
                                .putString("school_gender", schoolGender)
                                .apply()
                            viewModel.saveTeacherProfileAndSubject(teacherName, teacherEmail, teacherSubject, schoolGender)
                            Toast.makeText(context, "تم حفظ بيانات الأستاذ والمادة وجنس المدرسة بنجاح ✓", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "حفظ بيانات الأستاذ والمادة ✓", 
                                fontWeight = FontWeight.Black, 
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // CARD 4: وضع المشرف التربوي (Supervisor Mode)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSupervisorMode) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = if (isSupervisorMode) Color(0xFFD97706) else Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("وضع الإدارة والإشراف 🛡️", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text(
                                    text = if (isSupervisorMode) "الوضع: إشرافي وإداري شامل (صلاحيات كاملة)" else "الوضع: أستاذ مادة (صلاحيات اعتيادية)",
                                    fontSize = 11.sp,
                                    color = if (isSupervisorMode) Color(0xFFD97706) else Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Switch(
                            checked = isSupervisorMode,
                            onCheckedChange = { enable ->
                                viewModel.setSupervisorMode(enable, supervisorCodeInput) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    Surface(
                        color = if (isSupervisorMode) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp, 
                            if (isSupervisorMode) Color(0xFFFDE68A) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSupervisorMode) 
                                "🛡️ وضع الإدارة والإشراف مفعّل: تملك صلاحية إدارية وإشرافية كاملة لتفقد وتدقيق وتعديل كافة السجلات والصفوف وتجاوز القفل السحابي." 
                            else 
                                "💡 عند إدخال رمز مدير المدرسة أو مسؤول الإشراف وتفعيل هذا الوضع، ستتاح لك صلاحيات الإدارة والإشراف للاطلاع على كافة الشعب والمواد الخاصة بالمدرسة.",
                            color = if (isSupervisorMode) Color(0xFF92400E) else Color(0xFF475569),
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = supervisorCodeInput,
                        onValueChange = { supervisorCodeInput = it },
                        label = { Text("رمز مدير المدرسة أو مسؤول الإشراف") },
                        placeholder = { Text("أدخل رمز مدير المدرسة أو مسؤول الإشراف...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.setSupervisorMode(!isSupervisorMode, supervisorCodeInput) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSupervisorMode) Color(0xFFDC2626) else Color(0xFFD97706)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSupervisorMode) Icons.Default.Close else Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSupervisorMode) "إلغاء وضع الإدارة والإشراف والعودة لوضع الأستاذ ✕" else "تفعيل وضع الإدارة والإشراف 🛡️",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    if (isSupervisorMode) {
                        OutlinedButton(
                            onClick = {
                                viewModel.syncAllSchoolClassesForSupervisor { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD97706)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF92400E))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("سحب وتحديث كافة شعب وصفوف المدرسة 🏫🔄", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // CARD 4: النسخ السحابي المستقل عبر Google Drive / البريد الإلكتروني
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("النسخ السحابي عبر Google Drive / البريد ☁️", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Text("تصدير وحفظ نسخة احتياطية من سجلاتك بأمان", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 في الوضع المستقل (أوفلاين)، يمكنك تصدير نسخة احتياطية من كافة سجلاتك وحفظها بضغطة زر على Google Drive أو إرسالها إلى إيميلك الشخصي.",
                            color = Color(0xFF065F46),
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.exportBackup(context) { file ->
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية - سجل المدرس الذكي - ${teacherName}")
                                    if (teacherEmail.isNotBlank()) {
                                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(teacherEmail))
                                    }
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "حفظ في Google Drive أو إرسال بالبريد:"))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "نسخ وحفظ في Google Drive / البريد 📤", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // The Principal Desktop PC App Card
            val clipboardManager = LocalClipboardManager.current
            val desktopUrl = "https://drive.google.com/file/d/1MyvouuykZmwDx7Lm5hj2qNVIIDbZlqFj/view?usp=sharing"

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "منظومة الكمبيوتر المركزية (PC) 💻",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "The Principal Desktop v6.0 Super Edition",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "تفعيل دائم 💎",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "المنظومة الاحترافية لإدارة المدرسة على أجهزة الحاسوب (Windows): استيراد وتصدير إكسل الوزاري، طباعة الشيت الإلكتروني وسجلات الدرجات، والتوليد الآلي الذكي للجدول المدرسي، مع ربط سحابي فوري مع هواتف الكادر.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(desktopUrl)).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تنزيل للحاسوب", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(desktopUrl))
                                Toast.makeText(context, "تم نسخ رابط تنزيل الحاسوب إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            modifier = Modifier.weight(0.9f).height(42.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("نسخ الرابط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                val msg = "السلام عليكم، أرغب في تفعيل ترخيص منظومة The Principal للحاسوب لمرة واحدة مدى الحياة لمدرستنا."
                                val encoded = java.net.URLEncoder.encode(msg, "UTF-8")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://api.whatsapp.com/send?text=$encoded")).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "يرجى تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("طلب كود التفعيل الدائم (زين كاش / كي كارد) 🔑", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Official Support & Communication Channels Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                                text = "الدعم المباشر ومتابعة تحديثات المنظومة المعتمدة",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J")).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:vcol42@gmail.com")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "استفسار بخصوص منظومة ThePrincipal")
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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

            // Switch / Re-select Portal Role Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetRoleDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تبديل أو إعادة اختيار البوابة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF1E293B)
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

            // Footer Version Info
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "المنظومة المدرسية الموحدة - الإصدار 4.4",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نظام متوافق مع لوائح وضوابط وزارة التربية العراقية 🇮🇶",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Connect Cloud Warning Dialog
        if (showConnectWarningDialog) {
            AlertDialog(
                onDismissRequest = { showConnectWarningDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(8.dp))
                        Text("تنبيه تفعيل الربط السحابي ☁️", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                },
                text = {
                    Text(
                        "تنبيه: سيؤدي تفعيل الربط السحابي ومسح باركود المدرسة إلى مزامنة واستيراد الشعب والطلاب والدرجات المخصصة لك فوراً مع سحابة المدرسة. هل ترغب في المتابعة وتشغيل الكاميرا لمسح الرمز؟",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConnectWarningDialog = false
                            onNavigateToQrScanner()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("نعم، تشغيل الكاميرا ومسح الباركود", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConnectWarningDialog = false }) {
                        Text("إلغاء", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Unpair Confirmation Dialog
        if (showUnpairConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showUnpairConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                        Spacer(Modifier.width(8.dp))
                        Text("تأكيد إلغاء الربط السحابي ⚠️", fontWeight = FontWeight.Black, color = Color(0xFFDC2626), fontSize = 15.sp)
                    }
                },
                text = {
                    Text(
                        "تحذير: سيؤدي إلغاء الربط إلى إيقاف المزامنة اللحظية مع سحابة المدرسة والتحول إلى الوضع المحلي المستقل (أوفلاين) مع الاحتفاظ بكافة سجلاتك ودرجاتك الحالية على هاتفك. هل أنت متأكد من المتابعة؟",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.unpairSchool()
                            showUnpairConfirmDialog = false
                            Toast.makeText(context, "تم إلغاء الربط السحابي والتحويل للوضع المحلي (أوفلاين) 👤", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نعم، إلغاء الربط والعودة للأوفلاين", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnpairConfirmDialog = false }) { Text("تراجع", color = Color(0xFF64748B), fontWeight = FontWeight.Bold) }
                }
            )
        }

        // Help Guide Dialog
        if (showHelpGuideDialog) {
            HelpGuideDialog(onDismiss = { showHelpGuideDialog = false })
        }

        // Switch / Reset Role Confirmation Dialog
        if (showResetRoleDialog) {
            AlertDialog(
                onDismissRequest = { showResetRoleDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF4F46E5))
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة اختيار البوابة", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                },
                text = {
                    Text(
                        "هل ترغب في العودة إلى شاشة البوابة الرئيسية لاختيار دور آخر (أستاذ • طالب • مدير)؟ يتم الاحتفاظ بكافة بياناتك وسجلاتك الحالية بأمان.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetRoleDialog = false
                            com.school.system.utils.RoleManager.clearSelectedRole(context)
                            navController?.navigate("role_selection") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
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
    }
}
