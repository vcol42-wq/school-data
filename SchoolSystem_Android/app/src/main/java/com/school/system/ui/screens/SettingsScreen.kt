package com.school.system.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.system.data.SyncManager
import com.school.system.data.dao.ConfigDao
import com.school.system.data.model.SchoolConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val configDao: ConfigDao,
    val syncManager: SyncManager
) : ViewModel() {
    val config = configDao.getConfig()
        .map { it ?: SchoolConfig(isActivated = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun connect(url: String) {
        viewModelScope.launch { syncManager.connectToCloud(url) }
    }

    fun verify(code: String) {
        viewModelScope.launch {
            val success = syncManager.connectAndPairQr(code)
            if (!success) {
                syncManager.connectToCloud(code)
            }
        }
    }

    fun syncFromPrincipal() {
        viewModelScope.launch { syncManager.fetchDataFromPrincipal() }
    }

    fun activate(name: String, email: String, url: String, schoolId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            var formattedUrl = url.trim()
            if (formattedUrl.isNotEmpty() && !formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "http://$formattedUrl"
            }
            val current = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                schoolName = "مدرسة مرتبطة", 
                managerName = name,
                userEmail = email,
                cloudUrl = formattedUrl,
                schoolId = schoolId,
                isActivated = true,
                isVerified = true
            ))
            delay(1500)
            onComplete()
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

    SettingsContent(
        onBack = onBack,
        onNavigateToQrScanner = onNavigateToQrScanner,
        config = config,
        viewModel = viewModel,
        onConnect = { viewModel.connect(it) },
        onVerify = { viewModel.verify(it) },
        onSync = { viewModel.syncFromPrincipal() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onBack: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    config: SchoolConfig?,
    viewModel: SettingsViewModel,
    onConnect: (String) -> Unit,
    onVerify: (String) -> Unit,
    onSync: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("الأول الابتدائي") }
    var section by remember { mutableStateOf("أ") }
    var subject by remember { mutableStateOf("اللغة العربية") }
    var pairingCode by remember { mutableStateOf("") }
    var teacherEmail by remember { mutableStateOf("") }
    var geminiApiKey by remember { mutableStateOf("") }
    var isCloudSyncEnabled by remember { mutableStateOf(true) }

    var statusMessage by remember { mutableStateOf("") }
    var isWarning by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(config) {
        config?.let { 
            url = it.cloudUrl 
            if (!it.managerName.isNullOrEmpty()) {
                teacherName = it.managerName
            }
            pairingCode = it.pairingCode
            teacherEmail = it.userEmail
            geminiApiKey = it.geminiApiKey
            isCloudSyncEnabled = it.isCloudSyncEnabled
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الربط والمزامنة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Card 1: Connection setup
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الربط مع السحاب", fontWeight = FontWeight.Bold)
                    
                    if (config?.cloudUrl?.isNotEmpty() == true) {
                        Text("متصل بالمدرسة: ${config?.schoolName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("الرابط النشط: ${config?.cloudUrl}", fontSize = 11.sp)
                        
                        OutlinedButton(
                            onClick = onNavigateToQrScanner,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("تغيير المدرسة (مسح QR)")
                        }
                    } else {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("رابط سحابة المدرسة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onConnect(url) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("اتصال يدوي")
                            }
                            OutlinedButton(
                                onClick = onNavigateToQrScanner,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("مسح QR Code")
                            }
                        }
                    }
                }
            }

            // Card 2: Pairing Request
            if (config?.cloudUrl?.isNotEmpty() == true) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("طلب اقتران المدرس وتحديد الشعبة", fontWeight = FontWeight.Bold)

                        if (config?.isVerified == true) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("الربط نشط ومعتمد سحابياً ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text("الاسم المعتمد: ${config?.managerName}", fontSize = 12.sp)
                            Text("رمز الختم السحابي: ${config?.syncSealToken}", fontSize = 10.sp)

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = onSync,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("تحديث وجلب الأسماء من السحاب 🔄")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        config?.let { cfg ->
                                            viewModel.configDao.saveConfig(
                                                cfg.copy(isVerified = false, syncSealToken = "")
                                            )
                                        }
                                        statusMessage = "تم إلغاء اقتران المعلم بنجاح."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("إلغاء الاقتران وتغيير الشعبة 🗑️")
                            }
                        } else if (!config?.syncSealToken.isNullOrEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("بانتظار مصادقة وتدقيق المدير...", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            }
                            Text("الاسم المرفوع: ${config?.managerName}", fontSize = 12.sp)

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        statusMessage = "جاري التحقق من مصادقة المدير..."
                                        val success = viewModel.syncManager.fetchDataFromPrincipal()
                                        if (success) {
                                            statusMessage = "تم تفعيل الربط وتنزيل قوائم الأسماء بنجاح!"
                                        } else {
                                            statusMessage = "طلبك لا يزال معلقاً بانتظار مصادقة المدير."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تحديث حالة الموافقة 🔄")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        config?.let { cfg ->
                                            viewModel.configDao.saveConfig(
                                                cfg.copy(isVerified = false, syncSealToken = "")
                                            )
                                        }
                                        statusMessage = "تم إلغاء طلب الاقتران المعلق."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("إلغاء الطلب والتغيير 🗑️")
                            }
                        } else {
                            OutlinedTextField(
                                value = teacherName,
                                onValueChange = { teacherName = it },
                                label = { Text("اسم المدرس بالكامل") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = grade,
                                onValueChange = { grade = it },
                                label = { Text("الصف الدراسي (مثال: الأول الابتدائي)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = section,
                                onValueChange = { section = it },
                                label = { Text("الشعبة (مثال: أ)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                label = { Text("المادة الدراسية (مثال: اللغة العربية)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pairingCode,
                                onValueChange = { pairingCode = it },
                                label = { Text("رمز الاقتران الموحد للمدرسة") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        statusMessage = "جاري إرسال طلب الاقتران..."
                                        isWarning = false
                                        val result = viewModel.syncManager.requestPairing(teacherName, grade, section, subject, pairingCode)
                                        statusMessage = result.message
                                        isWarning = result.warning
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إرسال طلب اقتران مع المدير 📤")
                            }
                        }
                    }
                }
            }

            // Card 2.5: Account & Cloud Sync & AI setup
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حساب المدرس والذكاء الاصطناعي السحابي", fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = teacherEmail,
                        onValueChange = { 
                            teacherEmail = it
                            scope.launch {
                                config?.let { c ->
                                    viewModel.configDao.saveConfig(c.copy(userEmail = it))
                                }
                            }
                        },
                        label = { Text("البريد الإلكتروني للمدرس (Email)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تفعيل المساعد الذكي سحابياً", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("مرتبط ببريدك الإلكتروني تلقائياً", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = config?.isAiActivated ?: true,
                            onCheckedChange = { 
                                scope.launch {
                                    config?.let { c ->
                                        viewModel.configDao.saveConfig(c.copy(isAiActivated = it))
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Card 3: Status Message Alert
            if (statusMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isWarning) "تنبيه تعارض" else "حالة الاتصال",
                            fontWeight = FontWeight.Bold,
                            color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = statusMessage,
                            fontSize = 13.sp,
                            color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
