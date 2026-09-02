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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onActivationComplete: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }

    var teacherNameInput by remember { mutableStateOf(prefs.getString("teacher_name", "") ?: "") }
    var teacherSubjectInput by remember { mutableStateOf("اللغة العربية") }
    var teacherEmailInput by remember { mutableStateOf("") }
    var pairingCodeInput by remember { mutableStateOf("112233") }
    
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var showManualFields by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }

    val quickSubjects = listOf(
        "اللغة العربية", "الرياضيات", "التربية الإسلامية", "اللغة الإنكليزية",
        "العلوم", "الفيزياء", "الكيمياء", "الأحياء", "الاجتماعيات",
        "الحاسوب", "التربية الفنية", "التربية الرياضية", "النشيد والموسيقى", "الفرنسية"
    )

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val scannedCode = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("scanned_code", null)
        ?.collectAsState()

    LaunchedEffect(scannedCode?.value) {
        scannedCode?.value?.let { code ->
            isLoading = true
            statusText = "جاري الاقتران عبر الباركود..."
            scope.launch {
                val ok = viewModel.syncManager.connectAndPairQr(code)
                isLoading = false
                if (ok) {
                    if (teacherNameInput.isNotBlank()) {
                        prefs.edit().putString("teacher_name", teacherNameInput.trim()).apply()
                    }
                    Toast.makeText(context, "تم قراءة باركود المدرسة والربط بنجاح ✓", Toast.LENGTH_SHORT).show()
                    onActivationComplete()
                } else {
                    statusText = "تعذر قراءة الباركود، يرجى إعادة المحاولة"
                    Toast.makeText(context, "تعذر قراءة الباركود", Toast.LENGTH_LONG).show()
                }
            }
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8FAFC))
    ) {
        // Dynamic Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
                    ),
                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "سجل المدرس الإلكتروني الذكي",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "استخدام مستقل أوفلاين أو ربط فوري مع سحابة المدرسة",
                    color = Color(0xFFC7D2FE),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Main Profile & Subject Setup Card
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .offset(y = (-20).dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات الأستاذ والمادة التدريسية 📝", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                    }

                    OutlinedTextField(
                        value = teacherNameInput,
                        onValueChange = { 
                            teacherNameInput = it
                            prefs.edit().putString("teacher_name", it.trim()).apply()
                        },
                        label = { Text("اسم الأستاذ الكامل") },
                        placeholder = { Text("اكتب اسمك الثلاثي أو الكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = teacherSubjectInput,
                        onValueChange = { teacherSubjectInput = it },
                        label = { Text("المادة أو الاختصاص التدريسي") },
                        placeholder = { Text("مثال: الرياضيات، اللغة العربية...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text(
                        text = "اختيار سريع للمادة / الاختصاص:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickSubjects) { s ->
                            val isSelected = teacherSubjectInput == s
                            Surface(
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.clickable { teacherSubjectInput = s }
                            ) {
                                Text(
                                    text = s,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = teacherEmailInput,
                        onValueChange = { teacherEmailInput = it },
                        label = { Text("البريد الإلكتروني (للنسخ السحابي مع Google Drive)") },
                        placeholder = { Text("teacher@gmail.com (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Standalone Offline Access Button (Primary & Easiest Option)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFECFDF5),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA7F3D0)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "الاستخدام كسجل مستقل أوفلاين (بدون ربط مدرسة) 👤",
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "يتيح لك إنشاء الشعب وإدخال الدرجات والغيابات والطباعة فوراً، مع ربط إيميلك للنسخ في Google Drive.",
                        color = Color(0xFF047857),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.edit().putString("teacher_name", teacherNameInput.trim()).apply()
                                viewModel.activateStandaloneWithSubject(
                                    name = teacherNameInput.ifBlank { "أستاذ المادة" },
                                    email = teacherEmailInput.ifBlank { "teacher@local.edu" },
                                    subject = teacherSubjectInput.ifBlank { "اللغة العربية" },
                                    grade = "الأول",
                                    section = "أ"
                                ) {
                                    Toast.makeText(context, "تم بدء السجل المستقل بنجاح 👤✓", Toast.LENGTH_SHORT).show()
                                    onActivationComplete()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بدء الاستخدام المستقل الآن 🚀", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                    }
                }
            }

            // Connection to School Cloud (Secondary Option)
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(6.dp))
                        Text("أو الاقتران مع سحابة المدرسة ☁️", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF1E3A8A))
                    }

                    OutlinedButton(
                        onClick = onNavigateToQrScanner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                        enabled = !isLoading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("مسح باركود شاشة الكمبيوتر (QR) 📷", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "يتم الربط التلقائي عبر مسح رمز الباركود المعروض على شاشة حاسبة إدارة المدرسة",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    if (statusText != null) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusText ?: "",
                                color = Color(0xFF92400E),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Guide Button in Onboarding
            TextButton(
                onClick = { showHelpGuideDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("دليل الاستخدام والتعليمات 📖", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showHelpGuideDialog) {
            com.school.system.ui.components.HelpGuideDialog(
                onDismiss = { showHelpGuideDialog = false }
            )
        }
    }
}
