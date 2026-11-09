package com.school.system.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
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
import com.school.system.data.SyncRepository
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
                _connectionStatus.value = "غير متصل"
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
                _connectionStatus.value = "غير متصل"
                return
            }

            _connectionStatus.value = "متصل"
        } catch (e: Exception) {
            _connectionStatus.value = "غير متصل"
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
                    val schoolId = schoolRepository.getSchoolId()
                    if (schoolId.isNullOrBlank()) {
                        _connectionStatus.value = "فشل الربط: لم يتم العثور على معرف مدرسة صالح"
                        return@launch
                    }
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

    fun unpairSchool(context: Context? = null) {
        viewModelScope.launch {
            if (context != null) {
                syncManager.purgeEnrolledSchoolData(context)
            } else {
                val current = configDao.getConfig().first() ?: SchoolConfig()
                configDao.saveConfig(current.copy(
                    schoolId = "",
                    schoolName = "",
                    pairingCode = "",
                    isVerified = false,
                    isActivated = false,
                    syncSealToken = null
                ))
            }
            _connectionStatus.value = "غير متصل"
        }
    }

    private fun legacyUnpair() {
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
            _connectionStatus.value = "غير متصل"
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
            val newManager = if (current.role == "supervisor") name.trim() else current.managerName
            configDao.saveConfig(current.copy(
                managerName = newManager,
                userEmail = email.trim()
            ))
        }
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(isCloudSyncEnabled = enabled))
        }
    }

    fun setTeacherRole() {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            if (current.role != "teacher") {
                configDao.saveConfig(current.copy(role = "teacher"))
            }
        }
    }

    fun saveTeacherProfileAndSubject(name: String, email: String, subject: String, gender: String) {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            val newManager = if (current.role == "supervisor") name.trim() else current.managerName
            configDao.saveConfig(current.copy(
                managerName = newManager,
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

    fun saveSchoolIdentity(schoolName: String, directorateName: String, managerName: String = "") {
        viewModelScope.launch {
            val current = configDao.getConfig().first() ?: SchoolConfig()
            val cleanSchool = schoolName.trim()
            val cleanDir = directorateName.trim()
            val cleanMgr = managerName.trim()
            configDao.saveConfig(current.copy(
                schoolName = cleanSchool,
                directorateName = cleanDir,
                managerName = if (cleanMgr.isNotBlank()) cleanMgr else current.managerName
            ))
            if (cleanSchool.isNotBlank()) {
                schoolRepository.updateSchoolLocally(current.schoolId, current.pairingCode, cleanSchool)
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

                // Strictly verify manager/supervisor code before activating and pulling roster
                val localPairingCode = current.pairingCode.trim()
                var isCodeValid = localPairingCode.isNotEmpty() && (cleanCode == localPairingCode || cleanCode == "SUP-$localPairingCode")

                if (!isCodeValid && current.schoolId.isNotEmpty() && current.schoolId != "school_01") {
                    try {
                        val rawUrl = current.cloudUrl.trim().ifEmpty { SyncRepository.DEFAULT_SUPABASE_URL }
                        val apiKey = current.cloudKey.trim().ifEmpty { SyncRepository.DEFAULT_ANON_KEY }
                        val api = syncRepository.getApi(rawUrl)
                        val schoolRes = api.getSchools(apiKey, "Bearer $apiKey", current.schoolId, "eq.${current.schoolId}")
                        val schoolDto = schoolRes.body()?.firstOrNull()

                        val cloudPairing = schoolDto?.pairing_code?.trim().orEmpty()
                        val cloudSupCode = (schoolDto?.config?.get("supervisor_code") as? String)?.trim().orEmpty()
                        val cloudManagerCode = (schoolDto?.config?.get("manager_code") as? String)?.trim().orEmpty()

                        if ((cloudPairing.isNotEmpty() && cleanCode == cloudPairing) ||
                            (cloudSupCode.isNotEmpty() && (cleanCode == cloudSupCode || cleanCode == cloudSupCode.removePrefix("SUP-"))) ||
                            (cloudManagerCode.isNotEmpty() && cleanCode == cloudManagerCode)) {
                            isCodeValid = true
                        }
                    } catch (e: Exception) {
                        // Ignore cloud check failure
                    }
                }

                if (!isCodeValid) {
                    onResult(false, "رمز مدير المدرسة أو مسؤول الإشراف غير صحيح! تعذر تفعيل وضع الإدارة وسحب الأسماء ❌")
                    return@launch
                }

                secureKeyStorage.saveSupervisorCode(cleanCode)
                configDao.saveConfig(current.copy(
                    role = "supervisor",
                    syncSealToken = if (current.syncSealToken.isNullOrEmpty()) "__supervisor__" else current.syncSealToken
                ))
                // سحب كافة شعب وصفوف المدرسة فورياً بعد التحقق التام من صحة الرمز
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
                onResult(true, "تم التحقق من الرمز وتفعيل وضع الإدارة والإشراف بنجاح 🛡️")
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

    fun exportBackup(
        context: Context,
        onReady: (java.io.File) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val packages = packageDao.getAllPackagesList()
                val students = studentDao.getAllStudentsList()
                val currentConfig = configDao.getConfig().first()

                val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
                val resolvedTeacher = prefs.getString("teacher_name", "")?.takeIf { it.isNotBlank() }
                    ?: context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).getString("teacher_name", "")?.takeIf { it.isNotBlank() }
                    ?: (if (currentConfig?.role == "supervisor") currentConfig?.managerName else "")
                    ?: ""

                val backupJson = org.json.JSONObject().apply {
                    put("appName", "SmartTeacherRegister")
                    put("version", "2.5")
                    put("exportDate", java.time.LocalDateTime.now().toString())
                    put("teacherName", resolvedTeacher)
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
                onError(e.localizedMessage ?: "تعذر إنشاء النسخة الاحتياطية")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    isAppLockEnabled: Boolean = false,
    onAppLockEnabledChange: (Boolean) -> Unit = {},
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
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val settingsFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = currentTheme.textPrimaryColor,
        unfocusedTextColor = currentTheme.textPrimaryColor,
        focusedContainerColor = currentTheme.surfaceColor,
        unfocusedContainerColor = currentTheme.surfaceColor,
        focusedLabelColor = currentTheme.primaryColor,
        unfocusedLabelColor = currentTheme.textSecondaryColor
    )

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
    var schoolNameInput by remember { mutableStateOf(prefs.getString("school_name", "") ?: "") }
    var directorateInput by remember { mutableStateOf(prefs.getString("directorate_name", "المديرية العامة لتربية ديالى") ?: "المديرية العامة لتربية ديالى") }

    var supervisorCodeInput by remember { mutableStateOf(viewModel.getSavedSupervisorCode() ?: "") }
    var isSupervisorMode by remember { mutableStateOf(false) }

    var geminiApiKeyInput by remember { mutableStateOf("") }
    var isAiEnabled by remember { mutableStateOf(true) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showUnpairConfirmDialog by remember { mutableStateOf(false) }
    var showConnectWarningDialog by remember { mutableStateOf(false) }
    var showManualCodeDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var cloudBackupEmail by remember {
        mutableStateOf(prefs.getString("cloud_backup_email", "") ?: "")
    }
    var directCodeInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val scope = rememberCoroutineScope()
    val isPaired = config?.isVerified == true && !config?.schoolId.isNullOrEmpty()
    var currentSubPage by remember { mutableIntStateOf(0) }

    if (currentSubPage == 1) {
        BackHandler { currentSubPage = 0 }
    }

    LaunchedEffect(config) {
        config?.let { 
            val activeRole = com.school.system.utils.RoleManager.getSelectedRole(context)
            val isSuper = (it.role == "supervisor" && activeRole == com.school.system.utils.AppRole.PRINCIPAL)
            isSupervisorMode = isSuper
            val savedTeacher = prefs.getString("teacher_name", "")?.takeIf { s -> s.isNotBlank() }
                ?: context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).getString("teacher_name", "")?.takeIf { s -> s.isNotBlank() }
            if (!savedTeacher.isNullOrBlank()) {
                teacherName = savedTeacher
            } else if (isSuper && it.managerName.isNotBlank()) {
                teacherName = it.managerName
            }
            if (it.schoolName.isNotBlank() && it.schoolName != "مدرسة سحابية" && it.schoolName != "سجل مستقل (أوفلاين)") {
                schoolNameInput = it.schoolName
            }
            if (it.directorateName.isNotBlank()) {
                directorateInput = it.directorateName
            }
            teacherEmail = it.userEmail
            geminiApiKeyInput = it.geminiApiKey
            isAiEnabled = it.isAiActivated
        }
    }

    Scaffold(
        containerColor = currentTheme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (currentSubPage == 0) "الإعدادات ⚙️" else "المزيد ⚙️", fontWeight = FontWeight.Black, fontSize = 17.sp, color = currentTheme.textPrimaryColor)
                        Text(if (currentSubPage == 0) "الحساب والربط" else "النسخ الاحتياطي", fontSize = 11.sp, color = currentTheme.textSecondaryColor, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (currentSubPage == 0) onBack() else currentSubPage = 0 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = currentTheme.textPrimaryColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "دليل الاستخدام والتعليمات", tint = Color(0xFF2563EB))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = currentTheme.surfaceColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentSubPage == 0) {
                // بطاقة بيانات الأستاذ وحفظ المعلومات
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFBAE6FD)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB).copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "اسم الأستاذ 👤",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = currentTheme.textPrimaryColor
                                )
                            }
                        }

                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { 
                                teacherName = it 
                                prefs.edit().putString("teacher_name", it.trim()).apply()
                            },
                            label = { Text("اسم الأستاذ الكامل") },
                            placeholder = { Text("اكتب اسمك الكامل") },
                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = settingsFieldColors
                        )

                        Button(
                            onClick = {
                                prefs.edit()
                                    .putString("teacher_name", teacherName.trim())
                                    .apply()
                                viewModel.saveTeacherProfileAndSubject(teacherName, teacherEmail, teacherSubject, schoolGender)
                                Toast.makeText(context, "تم حفظ المعلومات بنجاح ✓", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "حفظ المعلومات 💾", 
                                    fontWeight = FontWeight.Black, 
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                }
            }

            if (currentSubPage == 1 && false) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFBBF7D0)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPaired) Color(0xFF059669).copy(alpha = 0.12f) else Color(0xFF2563EB).copy(alpha = 0.12f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPaired) Icons.Default.CloudDone else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isPaired) Color(0xFF059669) else Color(0xFF2563EB),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = teacherName.ifBlank { "أستاذ المادة" },
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = currentTheme.textPrimaryColor
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "مادة: $teacherSubject • $schoolGender",
                            fontSize = 12.sp,
                            color = currentTheme.textSecondaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPaired) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isPaired) Color(0xFF86EFAC) else Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = if (isPaired) "متصل" else "غير متصل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaired) Color(0xFF166534) else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            }

            if (currentSubPage == 0) {
                // CARD 1: الربط السحابي ومزامنة المدرسة (باركود + رمز المدرسة)
            Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFBAE6FD)),
                    shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isPaired) Color(0xFF059669).copy(alpha = 0.12f) else Color(0xFF2563EB).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPaired) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (isPaired) Color(0xFF059669) else Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الربط مع سحابة المدرسة ☁️",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.5.sp,
                                color = currentTheme.textPrimaryColor
                            )
                            Text(
                                text = if (isPaired) "متصل" else "غير متصل",
                                fontSize = 11.5.sp,
                                color = if (isPaired) Color(0xFF059669) else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isPaired) {
                        Surface(
                            color = if (currentTheme.isDark) currentTheme.tableAltCellBg else Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "المدرسة المقترنة: ${config?.schoolName?.ifBlank { "مدرستي" } ?: "مدرستي"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (currentTheme.isDark) currentTheme.textPrimaryColor else Color(0xFF166534)
                                )
                            }
                        }

                        // مفتاح تفعيل أو إيقاف التحديث والمزامنة الآلية
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (config?.isCloudSyncEnabled != false) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (config?.isCloudSyncEnabled != false) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        if (config?.isCloudSyncEnabled != false) Icons.Default.Sync else Icons.Default.SyncDisabled,
                                        contentDescription = null,
                                        tint = if (config?.isCloudSyncEnabled != false) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (config?.isCloudSyncEnabled != false) "التحديث والمزامنة الآلية: مفعّل 🟢" else "التحديث والمزامنة الآلية: متوقف 🔴",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.5.sp,
                                            color = if (config?.isCloudSyncEnabled != false) Color(0xFF166534) else Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = if (config?.isCloudSyncEnabled != false) "يتم سحب ورفع الدرجات والجدول والأسماء تلقائياً" else "المزامنة اليدوية فقط عبر الأزرار",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked = config?.isCloudSyncEnabled != false,
                                    onCheckedChange = { isEnabled ->
                                        viewModel.setCloudSyncEnabled(isEnabled)
                                        Toast.makeText(context, if (isEnabled) "تم تفعيل التحديث والمزامنة الآلية 🟢" else "تم إيقاف التحديث الآلي مؤقتاً 🔴", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF16A34A),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFDC2626)
                                    )
                                )
                            }
                        }

                        // أزرار التحكم في حالة الربط
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.syncFromPrincipal()
                                        Toast.makeText(context, "جاري مزامنة وسحب البيانات السحابية 🔄", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("مزامنة الآن 🔄", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showManualCodeDialog = true },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("تغيير الرمز 🔑", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showConnectWarningDialog = true },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("مسح باركود جديد 📷", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showUnpairConfirmDialog = true },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("إلغاء الربط ✕", fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    } else {
                        // Unpaired State: 2 Prominent Options (Barcode & School Code)
                        Text(
                            text = "اربط مدرستك بالباركود أو الرمز:",
                            fontSize = 11.5.sp,
                            color = currentTheme.textSecondaryColor,
                            lineHeight = 17.sp
                        )

                        Button(
                            onClick = { showConnectWarningDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "تفعيل الربط السحابي ومسح الباركود 📷",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showManualCodeDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E3A8A)),
                            border = BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2563EB))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "أو الربط بواسطة كود المدرسة (الرمز) 🔢",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    if (connectionStatus.isNotBlank()) {
                        Surface(
                            color = if (currentTheme.isDark) currentTheme.surfaceColor else if (connectionStatus == "متصل") Color(0xFFF0FDF4) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else if (connectionStatus == "متصل") Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = connectionStatus,
                                color = currentTheme.textPrimaryColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

                if (currentSubPage == 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDBEAFE),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "قفل التطبيق 🔒",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = currentTheme.textPrimaryColor
                                    )
                                    Text(
                                        text = if (isAppLockEnabled) "يُطلب التحقق عند كل فتح" else "بالبصمة أو رمز الهاتف",
                                        fontSize = 10.5.sp,
                                        color = currentTheme.textSecondaryColor
                                    )
                                }
                                Switch(
                                    checked = isAppLockEnabled,
                                    onCheckedChange = onAppLockEnabledChange
                                )
                        }
                    }
                }

                // بطاقة "المزيد" للدخول إلى باقي الإعدادات
                Surface(
                    onClick = { currentSubPage = 1 },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F3FF),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المزيد من الإعدادات ⚙️",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = currentTheme.textPrimaryColor
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "النسخ الاحتياطي",
                                fontSize = 11.5.sp,
                                color = currentTheme.textSecondaryColor
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "دخول",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (currentSubPage == 1) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFA7F3D0)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCloudBackupDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentTheme.isDark) currentTheme.tableAltCellBg else Color(0xFFD1FAE5),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("النسخ السحابي", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF064E3B))
                            Text("حفظ نسخة احتياطية من السجلات", fontSize = 12.sp, color = Color(0xFF047857))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF059669))
                    }
                }
            }

            if (currentSubPage == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, currentTheme.tableBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (currentTheme.isDark) 0.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                tint = currentTheme.primaryColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "روابط المنظومة",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = currentTheme.textPrimaryColor
                                )
                                Text(
                                    "تحميل تطبيق سطح المكتب ومتابعة القناة الرسمية",
                                    fontSize = 11.sp,
                                    color = currentTheme.textSecondaryColor
                                )
                            }
                        }

                        Button(
                            onClick = {
                                try {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://apps.microsoft.com/detail/9P0SWQHDT4H5")
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح رابط تحميل تطبيق سطح المكتب", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("تحميل تطبيق سطح المكتب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J")
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح رابط قناة المنظومة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryColor)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("متابعة قناة المنظومة على واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (currentSubPage == 1 && false) {
                // CARD 1.5: هوية المدرسة الرسمية وترويسة الطباعة
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF2563EB).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("بيانات المدرسة للطباعة 🏫", fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = currentTheme.textPrimaryColor)
                            Text("تظهر في السجلات المطبوعة", fontSize = 11.sp, color = currentTheme.textSecondaryColor)
                        }
                    }

                    OutlinedTextField(
                        value = schoolNameInput,
                        onValueChange = { 
                            schoolNameInput = it 
                            prefs.edit().putString("school_name", it.trim()).apply()
                        },
                        label = { Text("اسم المدرسة الرسمي 🏫") },
                        placeholder = { Text("اكتب اسم مدرستك الرسمي") },
                        leadingIcon = { Icon(Icons.Default.School, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = settingsFieldColors
                    )

                    OutlinedTextField(
                        value = directorateInput,
                        onValueChange = { 
                            directorateInput = it 
                            prefs.edit().putString("directorate_name", it.trim()).apply()
                        },
                        label = { Text("المديرية العامة للتربية 🏛️") },
                        placeholder = { Text("مثال: المديرية العامة لتربية ديالى") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = settingsFieldColors
                    )

                    Button(
                        onClick = {
                            val cleanSchool = schoolNameInput.trim()
                            val cleanDir = directorateInput.trim()
                            if (cleanSchool.isNotBlank()) {
                                viewModel.saveSchoolIdentity(cleanSchool, cleanDir)
                                prefs.edit()
                                    .putString("school_name", cleanSchool)
                                    .putString("directorate_name", cleanDir)
                                    .apply()
                                try {
                                    context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("school_name", cleanSchool)
                                        .putString("directorate_name", cleanDir)
                                        .apply()
                                } catch (_: Exception) {}
                                Toast.makeText(context, "تم حفظ وتثبيت اسم المدرسة في الطباعة: $cleanSchool ✓", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "يرجى كتابة اسم المدرسة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("حفظ وتثبيت اسم المدرسة في الطباعة 💾", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }

            // CARD 2: بيانات الأستاذ والمادة التدريسية
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFBAE6FD)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
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
                        Column {
                            Text("بيانات الأستاذ والمادة التدريسية 📝", fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = currentTheme.textPrimaryColor)
                            Text("تعديل الاسم والمادة الأساسية للتطبيق", fontSize = 11.sp, color = currentTheme.textSecondaryColor)
                        }
                    }

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { 
                            teacherName = it 
                            prefs.edit().putString("teacher_name", it.trim()).apply()
                        },
                        label = { Text("اسم الأستاذ الكامل") },
                        placeholder = { Text("اكتب اسمك الكامل") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = settingsFieldColors
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
                            leadingIcon = { Icon(Icons.Default.MenuBook, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = settingSubExpanded)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = settingsFieldColors
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
                                                color = if (s == teacherSubject) Color(0xFF2563EB) else currentTheme.textPrimaryColor
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
                            placeholder = { Text("اختر: بنين، بنات، مختلط...") },
                            leadingIcon = { Icon(Icons.Default.School, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = settingGenderExpanded)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = settingsFieldColors
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
                                                color = if (g == schoolGender) Color(0xFF2563EB) else currentTheme.textPrimaryColor
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
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = settingsFieldColors
                    )

                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("teacher_name", teacherName.trim())
                                .putString("teacher_subject", teacherSubject.trim())
                                .putString("school_gender", schoolGender)
                                .apply()
                            viewModel.saveTeacherProfileAndSubject(teacherName, teacherEmail, teacherSubject, schoolGender)
                            Toast.makeText(context, "تم حفظ بيانات الأستاذ والمادة بنجاح ✓", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "حفظ التعديلات ✓", 
                                fontWeight = FontWeight.Black, 
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // CARD 3: المشرف والمدير (فقط في وضع الإشراف)
            if (isSupervisorMode) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("بوابة الإدارة المدرسية المباشرة 👑", fontWeight = FontWeight.Black, fontSize = 14.sp, color = currentTheme.textPrimaryColor)
                                Text(
                                    text = "الوضع الحالي: إشراف وإدارة كاملة للمدرسة",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🛡️ بصفتك مديراً/مشرفاً: يمكنك سحب كافة بيانات المدرسة والصفوف ومتابعة السجلات فورياً.",
                                color = if (currentTheme.isDark) currentTheme.textPrimaryColor else Color(0xFF92400E),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

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
                            border = BorderStroke(1.2.dp, Color(0xFFD97706)),
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
                color = if (currentTheme.isDark) currentTheme.surfaceColor else Color(0xFFECFDF5),
                border = BorderStroke(1.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFA7F3D0)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Text("النسخ السحابي عبر Google Drive / البريد ☁️", fontWeight = FontWeight.Black, fontSize = 14.sp, color = currentTheme.textPrimaryColor)
                            Text("تصدير وحفظ نسخة احتياطية من سجلاتك بأمان", fontSize = 11.sp, color = currentTheme.textSecondaryColor)
                        }
                    }

                    Surface(
                        color = if (currentTheme.isDark) currentTheme.tableAltCellBg else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 في الوضع المستقل (أوفلاين)، يمكنك تصدير نسخة احتياطية من كافة سجلاتك وحفظها بضغطة زر على Google Drive أو إرسالها إلى إيميلك الشخصي.",
                            color = currentTheme.textPrimaryColor,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.exportBackup(context = context, onReady = { file ->
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
                            })
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

            // CARD 5: قنوات التواصل والدعم الفني الرسمي
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                color = currentTheme.textPrimaryColor
                            )
                            Text(
                                text = "الدعم المباشر ومتابعة تحديثات المنظومة المعتمدة",
                                fontSize = 11.sp,
                                color = currentTheme.textSecondaryColor
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
                        modifier = Modifier.fillMaxWidth().height(46.dp)
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
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(8.dp))
                        Text("البريد الإلكتروني: vcol42@gmail.com ✉️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

                // زر الرجوع للصفحة الأولى
                OutlinedButton(
                    onClick = { currentSubPage = 0 },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("رجوع للإعدادات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

            }

            // Footer Version Info
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "المنظومة المدرسية الموحدة - الإصدار 6.3",
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

        if (showCloudBackupDialog) {
            AlertDialog(
                onDismissRequest = { showCloudBackupDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(Modifier.width(8.dp))
                        Text("النسخ السحابي", fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    OutlinedTextField(
                        value = cloudBackupEmail,
                        onValueChange = { cloudBackupEmail = it },
                        label = { Text("البريد الإلكتروني") },
                        placeholder = { Text("name@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val email = cloudBackupEmail.trim()
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                Toast.makeText(context, "أدخل بريداً إلكترونياً صحيحاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            prefs.edit().putString("cloud_backup_email", email).apply()
                            showCloudBackupDialog = false
                            viewModel.exportBackup(
                                context = context,
                                onReady = { file ->
                                    try {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية - سجل المدرس الذكي - $teacherName")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            android.content.Intent.createChooser(sendIntent, "حفظ النسخة الاحتياطية")
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            e.localizedMessage ?: "تعذر فتح تطبيق الحفظ أو البريد",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCloudBackupDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
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

        // Manual School Code Entry Dialog 🔑
        if (showManualCodeDialog) {
            AlertDialog(
                onDismissRequest = { showManualCodeDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "الربط بواسطة كود المدرسة 🔑",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "أدخل رمز التحقق والاقتران المعتمد لمدرستك (المعروض في منظومة الإدارة):",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                        OutlinedTextField(
                            value = directCodeInput,
                            onValueChange = { directCodeInput = it },
                            label = { Text("كود أو رمز المدرسة") },
                            placeholder = { Text("أدخل الرمز هنا...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val codeToVerify = directCodeInput.trim()
                            if (codeToVerify.isNotBlank()) {
                                showManualCodeDialog = false
                                viewModel.verify(codeToVerify)
                                Toast.makeText(context, "جاري فحص كود المدرسة والربط بالسحابة... ⏳", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = directCodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تأكيد والربط 🔑", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showManualCodeDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء", color = Color(0xFF64748B))
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
    }
}
