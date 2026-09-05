package com.school.system.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.system.data.model.ClassPackage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToGrades: (grade: String, section: String, subject: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToSmartBell: () -> Unit,
    onNavigateToHomeworkHub: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val packages by viewModel.packages.collectAsState()
    var showSummonDialog by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showEditTeacherNameDialog by remember { mutableStateOf(false) }
    var packageToDelete by remember { mutableStateOf<ClassPackage?>(null) }
    var packageToSetup by remember { mutableStateOf<ClassPackage?>(null) }
    var packageToEdit by remember { mutableStateOf<ClassPackage?>(null) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    var teacherNameState by remember { mutableStateOf(prefs.getString("teacher_name", "") ?: "") }

    val coroutineScope = rememberCoroutineScope()
    var isUploadingGrades by remember { mutableStateOf(false) }

    val isOnlinePaired = (config?.isVerified == true || config?.isActivated == true) && !config?.schoolId.isNullOrEmpty()
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current

    Scaffold(
        containerColor = currentTheme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Surface(
                        color = currentTheme.primaryColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = "أيقونة التطبيق",
                                modifier = Modifier.size(22.dp),
                                tint = currentTheme.primaryColor
                            )
                        }
                    }
                },
                actions = {
                    // Theme Selector 🎨
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "تغيير الثيم والمظهر 🎨", tint = currentTheme.primaryColor)
                    }
                    // Help Guide Button 📖
                    IconButton(onClick = { showHelpGuideDialog = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "دليل الاستخدام والتعليمات",
                            tint = currentTheme.primaryColor
                        )
                    }
                    // Schedule (Calendar)
                    IconButton(onClick = onNavigateToSchedule) {
                        Icon(Icons.Default.DateRange, contentDescription = "جدول الدروس الأسبوعي", tint = currentTheme.primaryColor)
                    }
                    // Smart Bell (Alarm)
                    IconButton(onClick = onNavigateToSmartBell) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "منبه الجرس الذكي", tint = currentTheme.primaryColor)
                    }
                    // Settings (Gear)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = currentTheme.textSecondaryColor)
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
        ) {
            Spacer(Modifier.height(8.dp))

            // 1. ROW OF 3 CLOUD/PILL BADGES (غيمات التحكم السريعة الثلاث)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // غيمة 1: وضع الاتصال (متصل / غير متصل)
                CloudBadge(
                    title = if (isOnlinePaired) "متصل ☁️" else "غير متصل 👤",
                    subtitle = if (isOnlinePaired) (config?.schoolName?.take(12) ?: "السحابة") else "محلي",
                    backgroundColor = currentTheme.surfaceColor,
                    borderColor = currentTheme.tableBorderColor,
                    contentColor = if (isOnlinePaired) Color(0xFF16A34A) else currentTheme.primaryColor,
                    icon = if (isOnlinePaired) Icons.Default.CloudDone else Icons.Default.Person,
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                )

                // غيمة 2: استدعاء شعبة (متصل أو غير متصل)
                CloudBadge(
                    title = "استدعاء شعبة 📥",
                    subtitle = "إضافة صف ومادة",
                    backgroundColor = currentTheme.surfaceColor,
                    borderColor = currentTheme.tableBorderColor,
                    contentColor = currentTheme.primaryColor,
                    icon = Icons.Default.AddCircleOutline,
                    onClick = { showSummonDialog = true },
                    modifier = Modifier.weight(1f)
                )

                // غيمة 3: رفع الدرجات (بلون مختلف ومميز)
                CloudBadge(
                    title = if (isUploadingGrades) "جاري الرفع..." else "رفع الدرجات ↑",
                    subtitle = "مزامنة السحاب",
                    backgroundColor = currentTheme.primaryColor,
                    borderColor = currentTheme.secondaryColor,
                    contentColor = Color.White,
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        coroutineScope.launch {
                            isUploadingGrades = true
                            val success = viewModel.syncGradesOnly()
                            isUploadingGrades = false
                            if (success) {
                                Toast.makeText(context, "تم رفع ومزامنة كافة الدرجات للسحابة بنجاح ☁️✓", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context, 
                                    if (isOnlinePaired) "تعذر الرفع، يرجى مراجعة اتصال الإنترنت" else "التطبيق يعمل بالوضع المحلي (أوفلاين)", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(6.dp))

            // 2. مستطيل محدد ومضلل بشكل جميل لاسم الأستاذ
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, currentTheme.tableBorderColor),
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(currentTheme.ribbonGradient))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👨‍🏫", fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                val activeName = if (!config?.managerName.isNullOrBlank()) {
                                    config!!.managerName
                                } else if (teacherNameState.isNotBlank()) {
                                    teacherNameState
                                } else {
                                    "أستاذ المادة"
                                }
                                Text(
                                    text = "الأستاذ: $activeName",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isOnlinePaired) (config?.schoolName ?: "مدرسة متصلة بالسحاب") else "سجل محلي مستقل (أوفلاين)",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = { showEditTeacherNameDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل اسم الأستاذ",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3. قائمة السجلات (مستطيلات محددة ومضللة بلون مختلف عن الأرضية)
            if (packages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF2563EB)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "لا توجد سجلات أو شعب مضافة حتى الآن",
                            color = Color(0xFF1E293B),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "اضغط على غيمة (استدعاء شعبة 📥) لإضافة صفك ومادتك وتنزيل أو إضافة الطلاب",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showSummonDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة / استدعاء شعبة الآن 📥", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(packages) { pkg ->
                        RegisterCardItem(
                            pkg = pkg,
                            onClick = {
                                if (pkg.subject.isBlank() || pkg.subject == "المادة" || pkg.subject == "عام" || pkg.subject == "درس مقرر") {
                                    packageToSetup = pkg
                                } else {
                                    onNavigateToGrades(pkg.grade, pkg.section, pkg.subject)
                                }
                            },
                            onEdit = {
                                packageToEdit = pkg
                            },
                            onDelete = {
                                packageToDelete = pkg
                            }
                        )
                    }
                }
            }
        }

        // Dialog 1: حوار تأكيد الحذف
        if (packageToDelete != null) {
            val target = packageToDelete!!
            AlertDialog(
                onDismissRequest = { packageToDelete = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFDC2626))
                        Spacer(Modifier.width(8.dp))
                        Text("تأكيد حذف السجل 🗑️", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF991B1B))
                    }
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من حذف سجل مادة (${target.subject}) لشعبة ${target.grade} (${target.section})؟\n\nسيتم مسح هذا السجل من هاتفك، ويمكنك استدعاؤه لاحقاً.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePackage(target)
                            packageToDelete = null
                            Toast.makeText(context, "تم حذف السجل بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تأكيد الحذف 🗑️", fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { packageToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Dialog 2: حوار استدعاء الشعبة
        if (showSummonDialog) {
            SummonSectionDialog(
                onDismiss = { showSummonDialog = false },
                onConfirm = { grade, section, subject ->
                    val standardized = viewModel.syncRepository.standardizeSubjectName(subject)
                    viewModel.summonSectionDetailed(grade, section, standardized) { result ->
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                    showSummonDialog = false
                }
            )
        }

        // Dialog 3: حوار تعديل المادة والشعبة للسجل المحدد ✎
        if (packageToEdit != null) {
            val targetPkg = packageToEdit!!
            EditClassSubjectDialog(
                pkg = targetPkg,
                syncRepository = viewModel.syncRepository,
                onDismiss = { packageToEdit = null },
                onConfirm = { newGrade, newSection, newSubject ->
                    viewModel.updatePackage(targetPkg, newGrade, newSection, newSubject)
                    packageToEdit = null
                    Toast.makeText(context, "تم تعديل المادة والشعبة بنجاح ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dialog 4: حوار إعداد المادة للشعب غير المعينة
        if (packageToSetup != null) {
            val targetPkg = packageToSetup!!
            SetupClassSubjectDialog(
                pkg = targetPkg,
                syncRepository = viewModel.syncRepository,
                onDismiss = { packageToSetup = null },
                onConfirm = { updatedSubject, teacherName ->
                    viewModel.addPackage(targetPkg.grade, targetPkg.section, updatedSubject, "")
                    packageToSetup = null
                    onNavigateToGrades(targetPkg.grade, targetPkg.section, updatedSubject)
                }
            )
        }

        // Dialog 4: تعديل اسم الأستاذ
        if (showEditTeacherNameDialog) {
            var inputName by remember { mutableStateOf(teacherNameState) }
            AlertDialog(
                onDismissRequest = { showEditTeacherNameDialog = false },
                title = { Text("تعديل اسم الأستاذ 👨‍🏫", fontWeight = FontWeight.Black, fontSize = 16.sp) },
                text = {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("اسم الأستاذ الكامل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            teacherNameState = inputName.trim()
                            prefs.edit().putString("teacher_name", inputName.trim()).apply()
                            showEditTeacherNameDialog = false
                            Toast.makeText(context, "تم حفظ الاسم بنجاح ✓", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTeacherNameDialog = false }) { Text("إلغاء") }
                }
            )
        }

        // Dialog 5: دليل التعليمات
        if (showHelpGuideDialog) {
            com.school.system.ui.components.HelpGuideDialog(
                onDismiss = { showHelpGuideDialog = false }
            )
        }

        // Dialog 6: تخصيص الثيم والمظهر
        if (showThemeDialog) {
            com.school.system.ui.theme.ThemeSelectionDialog(
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}

/**
 * غيمة / كبسولة أنيقة للتحكم السريع في رأس الشاشة
 */
@Composable
fun CloudBadge(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    borderColor: Color,
    contentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = contentColor.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * مستطيل السجل المحدد والمضلل بشكل جميل بلون مختلف عن الأرضية
 */
@Composable
fun RegisterCardItem(
    pkg: ClassPackage,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current

    Surface(
        color = currentTheme.surfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, currentTheme.tableBorderColor),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Section Avatar + Subject & Class Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Section badge
                Surface(
                    color = currentTheme.primaryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = pkg.section,
                            color = currentTheme.primaryColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    // اسم المادة
                    Text(
                        text = if (pkg.subject.isBlank() || pkg.subject == "المادة" || pkg.subject == "عام" || pkg.subject == "درس مقرر") "انقر لتحديد المادة ✎" else pkg.subject,
                        color = if (pkg.subject.isBlank() || pkg.subject == "المادة" || pkg.subject == "عام" || pkg.subject == "درس مقرر") Color(0xFFD97706) else currentTheme.primaryColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    // الصف والشعبة والسعة
                    Text(
                        text = "الصف: ${pkg.grade} - شعبة (${pkg.section}) | سقف الطلاب: 60",
                        color = currentTheme.textSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Actions (Edit ✎ & Delete 🗑️)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // زر التعديل ✎
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل المادة والشعبة",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // زر الحذف 🗑️
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف السجل",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummonSectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (grade: String, section: String, subject: String) -> Unit
) {
    // 1. Grade Level (الأول إلى السادس)
    val gradesList = listOf("الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس")
    var selectedGradeLevel by remember { mutableStateOf("الأول") }

    // 2. Stage / Track / Type (ابتدائي، متوسط، علمي، أدبي، صناعة، تجارة، أخرى)
    val stagesList = listOf("ابتدائي", "متوسط", "علمي", "أدبي", "صناعة", "تجارة", "أخرى")
    var selectedStageType by remember { mutableStateOf("ابتدائي") }
    var customStageType by remember { mutableStateOf("") }

    // 3. Section (أ، ب، ج، ح، خ، أخرى)
    val sectionsList = listOf("أ", "ب", "ج", "ح", "خ", "أخرى")
    var selectedSection by remember { mutableStateOf("أ") }
    var customSection by remember { mutableStateOf("") }

    // 4. Subjects (التسميات المعروفة + لغة أخرى + أخرى)
    val standardSubjects = listOf(
        "تربية إسلامية",
        "اللغة العربية",
        "اللغة الإنكليزية",
        "لغة أخرى",
        "فيزياء",
        "كيمياء",
        "أحياء",
        "اجتماعيات",
        "رياضيات",
        "أخرى"
    )
    var selectedSubjectCategory by remember { mutableStateOf("اللغة العربية") }
    var customSubjectName by remember { mutableStateOf("") }
    var customLanguageName by remember { mutableStateOf("") }

    // Helper: compute unified grade name
    fun getUnifiedGradeName(): String {
        val branch = when (selectedStageType) {
            "ابتدائي" -> "الابتدائي"
            "متوسط" -> "المتوسط"
            "علمي" -> "العلمي"
            "أدبي" -> "الأدبي"
            "صناعة" -> "الصناعي"
            "تجارة" -> "التجاري"
            "أخرى" -> customStageType.trim().ifEmpty { "العام" }
            else -> selectedStageType
        }
        return "$selectedGradeLevel $branch".trim()
    }

    // Helper: compute unified section
    fun getUnifiedSectionName(): String {
        return if (selectedSection == "أخرى") customSection.trim().ifEmpty { "أ" } else selectedSection
    }

    // Helper: compute unified subject name
    fun getUnifiedSubjectName(): String {
        return when (selectedSubjectCategory) {
            "لغة أخرى" -> {
                val lang = customLanguageName.trim()
                if (lang.isNotEmpty()) (if (lang.startsWith("لغة") || lang.startsWith("اللغة")) lang else "لغة $lang") else "لغة أجنبية"
            }
            "أخرى" -> customSubjectName.trim().ifEmpty { "المادة العامة" }
            else -> selectedSubjectCategory
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("استدعاء أو إنشاء شعبة (التحديد الثلاثي) 📥", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1E3A8A))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info banner
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 نظام التحديد الثلاثي الموحد: يضمن مزامنة الصف والشعبة والمادة بدقة تامة مع السحابة والحاسبة بدون أي تداخل.",
                        color = Color(0xFF1E40AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // --- Part 1: Subject Selection (المادة الدراسية) ---
                Text("1. المادة الدراسية المقررة:", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(standardSubjects) { s ->
                        val isSelected = selectedSubjectCategory == s
                        Surface(
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { selectedSubjectCategory = s }
                        ) {
                            Text(
                                text = s,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // If "لغة أخرى" selected -> input name
                if (selectedSubjectCategory == "لغة أخرى") {
                    OutlinedTextField(
                        value = customLanguageName,
                        onValueChange = { customLanguageName = it },
                        label = { Text("اسم اللغة (مثل: فرنسي، كردي، تركماني، ألماني...)") },
                        placeholder = { Text("اكتب اسم اللغة...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        text = "ℹ️ ستُعامل هذه اللغة معاملة سجل اللغات والعربي والإسلامية (شفهي + تحريري).",
                        fontSize = 10.5.sp,
                        color = Color(0xFF0D9488),
                        fontWeight = FontWeight.Bold
                    )
                }

                // If "أخرى" selected -> input name
                if (selectedSubjectCategory == "أخرى") {
                    OutlinedTextField(
                        value = customSubjectName,
                        onValueChange = { customSubjectName = it },
                        label = { Text("اسم المادة المخصصة (مثل: حاسوب، علوم عامة...)") },
                        placeholder = { Text("اكتب اسم المادة...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // --- Part 2: Grade Level (الصف من الأول إلى السادس) ---
                Text("2. الصف الدراسي (من الأول إلى السادس):", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    gradesList.forEach { g ->
                        val isSelected = selectedGradeLevel == g
                        Surface(
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0369A1) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedGradeLevel = g }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                                Text(
                                    text = g,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B),
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // --- Part 3: Type / Stage (النوع: ابتدائي، متوسط، علمي، أدبي، صناعة، تجارة، أخرى) ---
                Text("3. النوع / المرحلة / الفرع:", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(stagesList) { st ->
                        val isSelected = selectedStageType == st
                        Surface(
                            color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF6D28D9) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { selectedStageType = st }
                        ) {
                            Text(
                                text = st,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (selectedStageType == "أخرى") {
                    OutlinedTextField(
                        value = customStageType,
                        onValueChange = { customStageType = it },
                        label = { Text("اكتب نوع المرحلة أو الفرع (مثل: مهني، زراعي، تطبيقي...)") },
                        placeholder = { Text("مثال: مهني") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // --- Part 4: Section (الشعبة: أ، ب، ج، ح، خ، أخرى) ---
                Text("4. الشعبة (أ، ب، ج، ح، خ، أخرى):", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    sectionsList.forEach { sec ->
                        val isSelected = selectedSection == sec
                        Surface(
                            color = if (isSelected) Color(0xFF16A34A) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF15803D) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSection = sec }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                                Text(
                                    text = sec,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B),
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (selectedSection == "أخرى") {
                    OutlinedTextField(
                        value = customSection,
                        onValueChange = { customSection = it },
                        label = { Text("رمز أو اسم الشعبة المخصصة (مثل: د، هـ، 1...)") },
                        placeholder = { Text("مثال: د") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Preview Box for Final Standardized String
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "📌 معاينة السجل الموحد:",
                            color = Color(0xFF92400E),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الصف: ${getUnifiedGradeName()} | الشعبة: (${getUnifiedSectionName()}) | المادة: ${getUnifiedSubjectName()}",
                            color = Color(0xFF78350F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalGrade = getUnifiedGradeName()
                    val finalSection = getUnifiedSectionName()
                    val finalSubject = getUnifiedSubjectName()
                    if (finalSubject.isNotBlank()) {
                        onConfirm(finalGrade, finalSection, finalSubject)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("تأكيد وتنزيل/إضافة الشعبة ✓", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupClassSubjectDialog(
    pkg: ClassPackage,
    syncRepository: com.school.system.data.SyncRepository,
    onDismiss: () -> Unit,
    onConfirm: (subject: String, teacherName: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    var teacherName by remember { mutableStateOf(prefs.getString("teacher_name", "") ?: "") }
    var subject by remember { mutableStateOf(if (pkg.subject == "المادة" || pkg.subject == "عام" || pkg.subject == "درس مقرر") "" else pkg.subject) }

    val standardized = remember(subject) {
        syncRepository.standardizeSubjectName(subject)
    }
    val isApproved = remember(subject) {
        syncRepository.isApprovedStandardSubject(standardized)
    }

    val quickSubjects = listOf(
        "التربية الإسلامية", "اللغة العربية", "اللغة الإنكليزية", "الرياضيات",
        "العلوم", "الفيزياء", "الكيمياء", "الأحياء", "الاجتماعيات",
        "الحاسوب", "التربية الرياضية", "التربية الفنية", "التربية الأخلاقية",
        "التاريخ", "الجغرافيا", "الاقتصاد", "الفلسفة وعلم النفس", "اللغة الفرنسية"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "إعداد المادة والمعلم 📝",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = "الشعبة: ${pkg.grade} - شعبة (${pkg.section})",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { 
                        teacherName = it
                        prefs.edit().putString("teacher_name", it).apply()
                    },
                    label = { Text("اسم الأستاذ") },
                    placeholder = { Text("اكتب اسمك الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة التي تدرّسها لهذه الشعبة") },
                    placeholder = { Text("اكتب اسم المادة المقررة أو مادة جديدة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Live Indicator Badge
                if (subject.isNotBlank()) {
                    if (isApproved) {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "مادة معتمدة وزارياً (يعاد للإملاء الرسمي)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = standardized,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "مادة دراسية جديدة / مخصصة (مسموح بها خارج المعتمد)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E22CE)
                                    )
                                    Text(
                                        text = standardized,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF581C87)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "اختيار سريع للمادة (انقر للاختيار):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    items(quickSubjects) { s ->
                        val isSelected = standardized == s
                        Surface(
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { subject = s }
                        ) {
                            Text(
                                text = s,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (standardized.isNotBlank()) {
                        onConfirm(standardized, teacherName.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("تأكيد وفتح السجل ✓", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClassSubjectDialog(
    pkg: ClassPackage,
    syncRepository: com.school.system.data.SyncRepository,
    onDismiss: () -> Unit,
    onConfirm: (grade: String, section: String, subject: String) -> Unit
) {
    var grade by remember { mutableStateOf(pkg.grade) }
    var section by remember { mutableStateOf(pkg.section) }
    var subject by remember { mutableStateOf(pkg.subject) }

    val standardized = remember(subject) {
        syncRepository.standardizeSubjectName(subject)
    }
    val isApproved = remember(subject) {
        syncRepository.isApprovedStandardSubject(standardized)
    }

    val quickSubjects = listOf(
        "التربية الإسلامية", "اللغة العربية", "اللغة الإنكليزية", "الرياضيات",
        "العلوم", "الفيزياء", "الكيمياء", "الأحياء", "الاجتماعيات",
        "الحاسوب", "التربية الرياضية", "التربية الفنية", "التربية الأخلاقية",
        "التاريخ", "الجغرافيا", "الاقتصاد", "الفلسفة وعلم النفس", "اللغة الفرنسية"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("تعديل بيانات السجل والمادة ✎", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1E3A8A))
                    Text("الشعبة الحالية: ${pkg.grade} (${pkg.section})", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Grade & Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("الصف الدراسي") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("الشعبة") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 2. Subject Name Input
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة التي يدرّسها") },
                    placeholder = { Text("اكتب اسم المادة المقررة أو مادة جديدة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // 3. Live Standardization Indicator Badge
                if (subject.isNotBlank()) {
                    if (isApproved) {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "مادة معتمدة وزارياً (تم الضبط للإملاء الرسمي)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = standardized,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFAF5FF),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "مادة دراسية جديدة / مخصصة (مسموح بها خارج المعتمد)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E22CE)
                                    )
                                    Text(
                                        text = standardized,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color(0xFF581C87)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Quick Selection Chips
                Text(
                    text = "اختيار سريع من المواد المعتمدة:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickSubjects) { s ->
                        val isSelected = standardized == s
                        Surface(
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { subject = s }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (grade.isNotBlank() && section.isNotBlank() && standardized.isNotBlank()) {
                        onConfirm(grade.trim(), section.trim(), standardized)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ التعديلات ✓", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
