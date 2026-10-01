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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

    var pairedSchoolName by remember { mutableStateOf("") }

    val scannedCode = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("scanned_code", null)
        ?.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

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
                    
                    schoolCode = if (pCode.isNotBlank()) pCode else sCode
                    pairedSchoolName = sName
                    
                    // ربط مباشر بالسحابة وحفظ الجلسة فوراً
                    viewModel.pairDirectlyFromQr(
                        schoolId = sCode,
                        schoolCode = schoolCode,
                        schoolName = if (sName.isNotBlank()) sName else "المدرسة الذكية",
                        url = url,
                        apiKey = apiKey
                    )
                    
                    val nameMsg = if (sName.isNotBlank()) " ($sName)" else ""
                    android.widget.Toast.makeText(context, "تم مسح باركود المدرسة وربطها بنجاح$nameMsg ✓", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    schoolCode = clean
                    android.widget.Toast.makeText(context, "تم مسح الرمز بنجاح: $schoolCode ✓", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                schoolCode = code.trim()
                android.widget.Toast.makeText(context, "تم مسح الرمز: $schoolCode", android.widget.Toast.LENGTH_SHORT).show()
            }
            navController?.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    LaunchedEffect(viewModel.isSuccess.value) {
        if (viewModel.isSuccess.value) {
            onNavigateToPending()
        }
    }

    LaunchedEffect(viewModel.error.value) {
        viewModel.error.value?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بوابة الطالب - تسجيل وانضمام 🎓", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController?.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else if (!RoleManager.isRoleLocked(context)) {
                            RoleManager.resetRoleBeforeLock(context)
                            onNavigateToRoleSelection()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (!RoleManager.isRoleLocked(context)) {
                        TextButton(
                            onClick = {
                                RoleManager.resetRoleBeforeLock(context)
                                onNavigateToRoleSelection()
                            }
                        ) {
                            Text("تغيير البوابة ↩️", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // PROMINENT QR BARCODE SCANNER CARD FOR STUDENTS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFF16A34A),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "قارئ الباركود السريع للطالب 📷",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF166534)
                        )
                    }

                    Text(
                        "وجّه الكاميرا نحو باركود الإدارة المدرسية المطبوع أو المعروض في شاشة الكمبيوتر ليتم ربطك تلقائياً.",
                        fontSize = 11.5.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF15803D),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onNavigateToQrScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("فتح الكاميرا ومسح الباركود الآن 📸", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFFE2E8F0))

            if (pairedSchoolName.isNotBlank()) {
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF22C55E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "تم الاقتران بنجاح مع: $pairedSchoolName ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF14532D)
                        )
                    }
                }
            }

            Text(
                "أو أدخل بيانات الطالب يدوياً:",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF64748B),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم الطالب الكامل") },
                placeholder = { Text("مثال: علي محمد حسن") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = schoolCode,
                onValueChange = { schoolCode = it },
                label = { Text("رمز أو كود المدرسة") },
                placeholder = { Text("يتم تعبئته آلياً عبر مسح الباركود") },
                trailingIcon = {
                    IconButton(onClick = onNavigateToQrScanner) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود", tint = androidx.compose.ui.graphics.Color(0xFF16A34A))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("الصف") },
                    placeholder = { Text("الأول المتوسط") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("الشعبة") },
                    placeholder = { Text("أ") },
                    singleLine = true,
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني (اختياري)") },
                placeholder = { Text("student@gmail.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val finalEmail = email.ifEmpty { "student_${System.currentTimeMillis()}@school.edu" }
                    val finalSchoolCode = schoolCode.trim()
                    viewModel.sendRequest(name, finalEmail, finalSchoolCode, stage.ifEmpty { "الصف الأول" }, grade.ifEmpty { "الأول" }, section.ifEmpty { "أ" })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2563EB)),
                enabled = name.isNotBlank() && !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("انضمام مباشر للجدول والصفوف 🚀", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

            OutlinedButton(
                onClick = {
                    viewModel.continueAsIndependentStudent(
                        name = name.ifBlank { "طالب مستقل" },
                        grade = grade.ifBlank { "الأول المتوسط" },
                        section = section.ifBlank { "أ" }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.ui.graphics.Color(0xFF0D9488)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color(0xFF0D9488))
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("المتابعة كطالب مستقل بدون ربط مدرسي 👤", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            viewModel.error.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
