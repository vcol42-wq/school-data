package com.school.system.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.school.system.utils.RoleManager
import kotlinx.coroutines.launch

import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onActivationComplete: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToRoleSelection: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }

    var teacherNameInput by remember { mutableStateOf(prefs.getString("teacher_name", "") ?: "") }
    var teacherSubjectInput by remember { mutableStateOf(prefs.getString("teacher_subject", "اللغة العربية") ?: "اللغة العربية") }
    var selectedGender by remember { mutableStateOf(prefs.getString("school_gender", "بنين") ?: "بنين") }
    var selectedGrade by remember { mutableStateOf(prefs.getString("teacher_grade", "الأول المتوسط") ?: "الأول المتوسط") }
    var selectedSection by remember { mutableStateOf(prefs.getString("teacher_section", "أ") ?: "أ") }
    var teacherEmailInput by remember { mutableStateOf("") }
    var pairingCodeInput by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var showManualFields by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showPairWarningDialog by remember { mutableStateOf(false) }
    var showManualCodeDialog by remember { mutableStateOf(false) }
    var showDeterrentDialog by remember { mutableStateOf(false) }
    var directCodeInput by remember { mutableStateOf("") }

    val quickSubjects = listOf(
        "اللغة العربية", "الرياضيات", "التربية الإسلامية", "اللغة الإنكليزية",
        "العلوم", "الاجتماعيات", "الأحياء", "الكيمياء", "الفيزياء",
        "الحاسوب", "التربية الأخلاقية", "التربية الفنية", "النشاط البدني", "النشيد والموسيقى", "اللغة الفرنسية",
        "قراءة وكتابة", "علم الأرض"
    )

    val schoolGenders = listOf("بنين", "بنات", "مختلط")

    val gradesList = listOf(
        // المرحلة الابتدائية (من الأول إلى السادس)
        "الأول الابتدائي", "الثاني الابتدائي", "الثالث الابتدائي",
        "الرابع الابتدائي", "الخامس الابتدائي", "السادس الابتدائي",
        // المرحلة المتوسطة
        "الأول المتوسط", "الثاني المتوسط", "الثالث المتوسط",
        // المرحلة الإعدادية
        "الرابع العلمي", "الرابع الأدبي",
        "الخامس العلمي", "الخامس الأدبي",
        "السادس العلمي", "السادس الأدبي"
    )

    val sectionsList = listOf("أ", "ب", "ج", "د", "هـ", "و", "ز", "ح", "ط", "ي")
    
    var subjectExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

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
                .height(220.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
                    ),
                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!RoleManager.isRoleLocked(context)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            RoleManager.resetRoleBeforeLock(context)
                            onNavigateToRoleSelection()
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع لاختيار البوابات",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "تغيير البوابة ↩️",
                        color = Color(0xFFC7D2FE),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    text = "نظام إدارة المدرسة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "بوابة المعلم والملازم الذكية",
                    fontSize = 12.5.sp,
                    color = Color(0xFFE0E7FF)
                )
            }
        }

        // Main Action Container (الباركود، الرمز، أو العمل مستقل)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .offset(y = (-20).dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CARD 1: مسح باركود المدرسة (QR 📷)
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF2563EB).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("1. مسح باركود المدرسة (الباركود) 📷", fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                            Text("ربط مباشر وسريع عبر كاميرا الهاتف", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "وجّه الكاميرا نحو باركود المدرسة المعروض في شاشة الحاسبة ليتم تفعيل الحساب واستيراد الحصص فوراً.",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { showPairWarningDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        enabled = !isLoading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("مسح الباركود الآن 📷", fontSize = 13.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // CARD 2: الربط بواسطة كود المدرسة (الرمز 🔢)
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFD97706).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("2. الربط بواسطة كود المدرسة (الرمز) 🔢", fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                            Text("إدخال الرمز السحابي المعتمد", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = directCodeInput,
                        onValueChange = { directCodeInput = it },
                        label = { Text("أدخل رمز أو كود المدرسة") },
                        placeholder = { Text("مثال: SCH-123456 أو كود الربط") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val codeToVerify = directCodeInput.trim()
                            if (codeToVerify.isNotBlank()) {
                                isLoading = true
                                statusText = "جاري الفحص والتحقق الأمني..."
                                scope.launch {
                                    val ok = viewModel.syncManager.connectAndPairQr(codeToVerify)
                                    isLoading = false
                                    if (ok) {
                                        prefs.edit().putInt("pairing_failed_attempts", 0).apply()
                                        if (teacherNameInput.isNotBlank()) {
                                            prefs.edit().putString("teacher_name", teacherNameInput.trim()).apply()
                                        }
                                        Toast.makeText(context, "تم الربط والاقتران بالرمز بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        onActivationComplete()
                                    } else {
                                        statusText = "تعذر الاقتران بالرمز، تحقق من صحة الرمز"
                                        Toast.makeText(context, "رمز غير مصرح به", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = directCodeInput.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تأكيد والربط بواسطة الرمز 🚀", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }

            // CARD 3: العمل كحساب مستقل (أوفلاين 👤)
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF059669).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("3. العمل مستقل (أوفلاين) 👤", fontWeight = FontWeight.Black, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                            Text("دخول فوري ومباشر بدون ربط سحابي", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "يمكنك فتح السجل والبدء فوراً بشكل مستقل بدون إنترنت، وستتمكن من إضافة وتعديل كافة بياناتك لاحقاً.",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.activateStandaloneWithSubject(
                                    name = teacherNameInput.ifBlank { "أستاذ المادة" },
                                    email = teacherEmailInput.ifBlank { "teacher@local.edu" },
                                    subject = teacherSubjectInput.ifBlank { "اللغة العربية" },
                                    grade = selectedGrade,
                                    section = selectedSection
                                ) {
                                    Toast.makeText(context, "تم فتح السجل كعمل مستقل بنجاح 👤✓", Toast.LENGTH_SHORT).show()
                                    onActivationComplete()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("دخول مباشر وبدء السجل المستقل 🚀", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                        }
                    }
                }
            }

            // Information Notice Card about Settings
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "💡 ملاحظة: بعد فتح التطبيق، يمكنك في أي وقت الانتقال إلى شاشة (الضبط ⚙️) لإضافة أو تعديل اسمك والمادة التدريسية والصفوف والشعب متى رغبت.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Optional Expandable Section for Personal Profile
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualFields = !showManualFields },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إضافة معلوماتك الشخصية الآن (اختياري 📝)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF475569))
                        }
                        Icon(
                            if (showManualFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }

                    AnimatedVisibility(visible = showManualFields) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = teacherNameInput,
                                onValueChange = {
                                    teacherNameInput = it
                                    prefs.edit().putString("teacher_name", it.trim()).apply()
                                },
                                label = { Text("اسم الأستاذ الكامل") },
                                placeholder = { Text("اكتب اسمك الثلاثي") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = teacherSubjectInput,
                                onValueChange = {
                                    teacherSubjectInput = it
                                    prefs.edit().putString("teacher_subject", it.trim()).apply()
                                },
                                label = { Text("المادة التدريسية") },
                                placeholder = { Text("مثال: اللغة العربية") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

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

        if (showPairWarningDialog) {
            AlertDialog(
                onDismissRequest = { showPairWarningDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(Modifier.width(8.dp))
                        Text("تنبيه تفعيل الربط السحابي", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                },
                text = {
                    Text(
                        "تنبيه: سيؤدي مسح باركود المدرسة إلى تفعيل الربط المباشر مع سحابة المدرسة واستيراد الشعب والطلاب المخصصين لحسابك. هل ترغب في المتابعة وتشغيل الكاميرا؟",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPairWarningDialog = false
                            onNavigateToQrScanner()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("نعم، فتح الكاميرا ومسح الرمز", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPairWarningDialog = false }) {
                        Text("إلغاء", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showManualCodeDialog) {
            val failedAttempts = remember { prefs.getInt("pairing_failed_attempts", 0) }
            val isLockedOut = failedAttempts >= 3

            AlertDialog(
                onDismissRequest = { showManualCodeDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(8.dp))
                        Text("الربط برمز المدرسة 🔑", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isLockedOut) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🔒 حظر أمني: تكررت المحاولات الخاطئة! لحماية المنظومة من التخمين، يرجى تفعيل الربط عبر مسح باركود الـ QR بالكاميرا مباشرة.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            Text(
                                "أدخل رمز التحقق والاقتران المعتمد لمدرستك:",
                                fontSize = 12.5.sp,
                                color = Color(0xFF475569),
                                lineHeight = 18.sp
                            )
                            OutlinedTextField(
                                value = directCodeInput,
                                onValueChange = { directCodeInput = it },
                                label = { Text("رمز التحقق والاقتران") },
                                placeholder = { Text("أدخل الرمز المعتمد...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    if (isLockedOut) {
                        Button(
                            onClick = {
                                showManualCodeDialog = false
                                onNavigateToQrScanner()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("مسح الباركود الآن 📷", fontWeight = FontWeight.Black)
                        }
                    } else {
                        Button(
                            onClick = {
                                val codeToVerify = directCodeInput.trim()
                                if (codeToVerify.isNotBlank()) {
                                    showManualCodeDialog = false
                                    isLoading = true
                                    statusText = "جاري الفحص والتحقق الأمني..."
                                    scope.launch {
                                        val ok = viewModel.syncManager.connectAndPairQr(codeToVerify)
                                        isLoading = false
                                        if (ok) {
                                            prefs.edit().putInt("pairing_failed_attempts", 0).apply()
                                            if (teacherNameInput.isNotBlank()) {
                                                prefs.edit().putString("teacher_name", teacherNameInput.trim()).apply()
                                            }
                                            Toast.makeText(context, "تم الربط والاقتران الموثق بنجاح ✓", Toast.LENGTH_SHORT).show()
                                            onActivationComplete()
                                        } else {
                                            val currentFails = prefs.getInt("pairing_failed_attempts", 0) + 1
                                            prefs.edit().putInt("pairing_failed_attempts", currentFails).apply()
                                            statusText = "تحذير: رمز غير مصرح به!"
                                            showDeterrentDialog = true
                                        }
                                    }
                                }
                            },
                            enabled = directCodeInput.isNotBlank() && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تأكيد والتحقق 🔑", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showManualCodeDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Security Deterrent Alert Dialog ⚠️
        if (showDeterrentDialog) {
            AlertDialog(
                onDismissRequest = { showDeterrentDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "⚠️ تحذير أمني مسجل (الرقابة المركزية)",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF991B1B),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "الرمز المدخل غير صحيح! تم رصد محاولة دخول غير مصرح بها وتسجيل معلومات هذا الجهاز وعنوان الاتصال (IP / Device ID) وسوف ترسل إلى إدارة المدرسة والرقابة المركزية للتحقق ومطابقة السجلات.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 18.sp
                        )
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنبيه صارم: لا تعبث بإدخال رموز خاطئة حتى لا تقع تحت طائلة المساءلة التأديبية والقانونية وحظر الجهاز نهائياً من الشبكة.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDeterrentDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("أقر وأتعهد بالالتزام", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showHelpGuideDialog) {
            com.school.system.ui.components.HelpGuideDialog(
                onDismiss = { showHelpGuideDialog = false }
            )
        }
    }
}
