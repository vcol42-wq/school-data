package com.school.system.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.PdfPrint
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import com.school.system.data.model.Student
import com.school.system.data.model.StudentMarks
import com.school.system.data.model.DailyColumnSetting
import com.school.system.data.model.AbsenceRecord
import com.school.system.data.model.latestRecordedScore
import com.school.system.data.model.latestRecordedScoreInt
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.FileProvider
import com.school.system.data.model.SchoolConfig
import com.school.system.data.repository.SecureUploadResult
import com.school.system.utils.BiometricHelper
import com.school.system.utils.ImageTextExtractor
import com.school.system.utils.ProfessionalDocumentParser
import java.io.File

data class ProgressiveEvaluationResult(
    val stageName: String,
    val totalStudents: Int,
    val passedStudents: Int,
    val failedStudents: Int,
    val passRate: Int
)

/**
 * احتساب نسبة النجاح التدريجية وفق آخر عمود مدخل (تجريبي)
 * بدلاً من الحساب للعمود النهائي أو السعي السنوي عندما لا تزال غير مدخلة،
 * يتم الكشف عن آخر مرحلة فُعّلت فيها الدرجات (الشهر الأول -> الثاني -> الفصل الأول -> نصف السنة -> ...)
 */
fun calculateProgressiveStats(students: List<Student>): ProgressiveEvaluationResult {
    val total = students.size
    if (total == 0) {
        return ProgressiveEvaluationResult(
            stageName = "السجل فارغ",
            totalStudents = 0,
            passedStudents = 0,
            failedStudents = 0,
            passRate = 0
        )
    }

    // 1. فحص الامتحان النهائي / الدرجة النهائية (يتطلب وجود امتحان نهائي فعلي وسعي)
    val hasFinal = students.any { (it.marks.finalGrade > 0f && it.marks.annualAverage > 0f) || it.marks.finalExamTotal > 0f || it.marks.finalWrittenD1 > 0f }
    if (hasFinal) {
        val passed = students.count { 
            val score = if (it.marks.finalGrade > 0f) it.marks.finalGrade else it.marks.finalExamTotal
            score >= 50f 
        }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("النهائية", total, passed, failed, rate)
    }

    // 2. فحص السعي السنوي (يتطلب وجود الفصلين ونصف السنة)
    val hasAnnual = students.any { it.marks.annualAverage > 0f && it.marks.term1Avg > 0f && it.marks.term2Avg > 0f }
    if (hasAnnual) {
        val passed = students.count { it.marks.annualAverage >= 50f }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("السعي السنوي", total, passed, failed, rate)
    }

    // 3. فحص معدل الفصل الثاني (يتطلب إدخال الشهر الرابع)
    val hasTerm2 = students.any { it.marks.term2Avg > 0f && (it.marks.m4MonthAvg > 0f || it.marks.m4Written > 0f) }
    if (hasTerm2) {
        val passed = students.count { it.marks.term2Avg >= 50f }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("الفصل الثاني", total, passed, failed, rate)
    }

    // 4. فحص درجات الشهر الرابع
    val hasM4 = students.any { it.marks.m4MonthAvg > 0f || it.marks.m4Written > 0f || it.marks.m4Daily.any { d -> d > 0f } }
    if (hasM4) {
        val passed = students.count {
            val score = if (it.marks.m4MonthAvg > 0f) it.marks.m4MonthAvg else (it.marks.m4Written + it.marks.m4Daily.sum())
            score >= 50f
        }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("الشهر الرابع", total, passed, failed, rate)
    }

    // 5. فحص درجات الشهر الثالث
    val hasM3 = students.any { it.marks.m3MonthAvg > 0f || it.marks.m3Written > 0f || it.marks.m3Daily.any { d -> d > 0f } }
    if (hasM3) {
        val passed = students.count {
            val score = if (it.marks.m3MonthAvg > 0f) it.marks.m3MonthAvg else (it.marks.m3Written + it.marks.m3Daily.sum())
            score >= 50f
        }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("الشهر الثالث", total, passed, failed, rate)
    }

    // 6. فحص درجات نصف السنة
    val hasMidterm = students.any { it.marks.midtermFinalGrade > 0f || it.marks.midtermTotal > 0f || it.marks.midtermScore > 0f }
    if (hasMidterm) {
        val passed = students.count {
            val score = if (it.marks.midtermFinalGrade > 0f) it.marks.midtermFinalGrade else it.marks.midtermTotal
            score >= 50f
        }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("نصف السنة", total, passed, failed, rate)
    }

    // 7. فحص معدل الفصل الأول (يتطلب إدخال الشهر الثاني)
    val hasTerm1 = students.any { it.marks.term1Avg > 0f && (it.marks.m2MonthAvg > 0f || it.marks.m2Written > 0f || it.marks.m2Daily.any { d -> d > 0f }) }
    if (hasTerm1) {
        val passed = students.count { it.marks.term1Avg >= 50f }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("الفصل الأول", total, passed, failed, rate)
    }

    // 8. فحص درجات الشهر الثاني
    val hasM2 = students.any { it.marks.m2MonthAvg > 0f || it.marks.m2Written > 0f || it.marks.m2Daily.any { d -> d > 0f } }
    if (hasM2) {
        val passed = students.count {
            val score = if (it.marks.m2MonthAvg > 0f) it.marks.m2MonthAvg else (it.marks.m2Written + it.marks.m2Daily.sum())
            score >= 50f
        }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("الشهر الثاني", total, passed, failed, rate)
    }

    // 9. فحص درجات الشهر الأول
    val hasM1 = students.any { it.marks.m1MonthAvg > 0f || it.marks.m1Written > 0f || it.marks.m1Daily.any { d -> d > 0f } }
    if (hasM1) {
        val passed = students.count {
            val score = if (it.marks.m1MonthAvg > 0f) it.marks.m1MonthAvg else (it.marks.m1Written + it.marks.m1Daily.sum())
            score >= 50f
        }
        val failed = (total - passed).coerceAtLeast(0)
        val rate = ((passed.toFloat() / total) * 100).toInt()
        return ProgressiveEvaluationResult("الشهر الأول", total, passed, failed, rate)
    }

    // 10. لا توجد أي درجات مدخلة بعد
    return ProgressiveEvaluationResult("لم تُدخل درجات", total, 0, 0, 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeRegisterScreen(
    grade: String,
    section: String,
    subject: String,
    onBack: () -> Unit,
    viewModel: GradeViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsState()
    val absences by viewModel.absences.collectAsState()
    val columnSettings by viewModel.getDailyColumnSettings("${grade}_${section}_$subject").collectAsState(null)
    val config by viewModel.config.collectAsState()
    val gradeLocks by viewModel.gradeLocks.collectAsState()
    val isRosterProtected = remember(config) { config?.schoolId.orEmpty().isNotBlank() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showRosterProtectedDialog by remember { mutableStateOf(false) }
    var showPublishHomeworkDialog by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showShareRosterDialog by remember { mutableStateOf(false) }
    var showEditSchoolInfoDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var headerToEdit by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val coroutineScope = rememberCoroutineScope()
    var isUploadingGrades by remember { mutableStateOf(false) }
    var isRefreshingStudentsFromCloud by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var selectedPeriod by remember { mutableIntStateOf(1) }

    LaunchedEffect(config?.schoolId) {
        if (!config?.schoolId.isNullOrBlank()) {
            viewModel.refreshSchoolIdentityFromCloud()
        }
    }

    val isSpecial = remember(subject) { isSpecialSubject(subject) }

    var showPinDialog by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var showInvalidPinDialog by remember { mutableStateOf(false) }
    var invalidPinDialogMessage by remember { mutableStateOf("") }

    val fragmentActivity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is androidx.fragment.app.FragmentActivity) break
            ctx = ctx.baseContext
        }
        ctx as? androidx.fragment.app.FragmentActivity
    }

    val performUpload: (String) -> Unit = { pinToUse ->
        isUploadingGrades = true
        viewModel.uploadGradesSecurely(grade, section, subject, pinToUse) { result ->
            isUploadingGrades = false
            when (result) {
                is SecureUploadResult.Success -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
                is SecureUploadResult.InvalidPin -> {
                    invalidPinDialogMessage = result.reason
                    showInvalidPinDialog = true
                }
                is SecureUploadResult.ClassLocked -> {
                    Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                }
                is SecureUploadResult.Failure -> {
                    Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val startSecureUploadWithBiometric: (String) -> Unit = { pinToUse ->
        val avail = BiometricHelper.checkBiometricAvailability(context)
        if (fragmentActivity == null || avail !is BiometricHelper.BiometricStatus.Available) {
            performUpload(pinToUse)
        } else {
            BiometricHelper.authenticate(
                activity = fragmentActivity,
                title = "تأكيد رفع درجات: $subject ($section)",
                subtitle = "المصادقة البيومترية مطلوبة لاعتماد درجات الشعبة ورفعها للسحابة",
                onSuccess = {
                    performUpload(pinToUse)
                },
                onError = { err ->
                    if (err.contains("إلغاء")) {
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    } else {
                        performUpload(pinToUse)
                    }
                }
            )
        }
    }

    // Instant lock checking - only locked if official cloud seal token is present
    val isEditingLocked = remember(config) {
        config?.isVerified == true && config?.syncSealToken?.startsWith("SEAL-") == true
    }

    LaunchedEffect(grade, section, subject) {
        viewModel.loadStudents(grade, section, subject)
    }

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    // Smooth Unified Block Scroll Connection
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (headerHeightPx <= 0f) return Offset.Zero

                // When scrolling down (dragging up: delta < 0), smoothly collapse top block
                if (delta < 0f && headerOffsetPx > -headerHeightPx) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                // When scrolling up (dragging down: delta > 0) and at top of list, smoothly expand top block
                if (delta > 0f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 10) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        containerColor = currentTheme.backgroundColor,
        topBar = {
            Surface(
                color = currentTheme.surfaceColor,
                shadowElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, currentTheme.tableBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    // Tier 1: Geometric Action Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 40.dp else 46.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Geometric Navigation Group (الرجوع والمظهر)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = currentTheme.primaryColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = currentTheme.textPrimaryColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(currentTheme.tableBorderColor.copy(alpha = 0.6f)))
                                IconButton(
                                    onClick = { showThemeDialog = true },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = "تغيير الثيم والمظهر 🎨",
                                        tint = currentTheme.primaryColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        // Geometric Main Actions Group (إضافة طالب، طباعة، مشاركة، سحابة)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = currentTheme.primaryColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        if (isRosterProtected) {
                                            showRosterProtectedDialog = true
                                        } else {
                                            showAddStudentDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        if (isRosterProtected) Icons.Default.Shield else Icons.Default.PersonAdd,
                                        contentDescription = if (isRosterProtected) "قائمة الطلبة معتمدة 🛡️" else "إضافة طالب",
                                        tint = if (isRosterProtected) Color(0xFF0284C7) else currentTheme.primaryColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(currentTheme.tableBorderColor.copy(alpha = 0.6f)))
                                IconButton(
                                    onClick = { showPublishHomeworkDialog = true },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Campaign,
                                        contentDescription = "إرسال واجب وتوجيهات يومية للشعبة 📝",
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(currentTheme.tableBorderColor.copy(alpha = 0.6f)))
                                IconButton(
                                    onClick = {
                                        if (isRefreshingStudentsFromCloud) return@IconButton
                                        isRefreshingStudentsFromCloud = true
                                        viewModel.refreshStudentsFromCloud(grade, section, subject) { success, msg ->
                                            isRefreshingStudentsFromCloud = false
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    if (isRefreshingStudentsFromCloud) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF0EA5E9)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "استدعاء وتحديث قائمة الأسماء والبيانات من السحابة 🔄",
                                            tint = Color(0xFF0EA5E9),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(currentTheme.tableBorderColor.copy(alpha = 0.6f)))
                                IconButton(
                                    onClick = { 
                                        if (students.isEmpty()) {
                                            Toast.makeText(context, "لا توجد أسماء طلاب في هذه الشعبة للطباعة", Toast.LENGTH_SHORT).show()
                                        } else {
                                            printRegister(context, grade, section, subject, selectedTab, students, absences, config, includeCover = true, columnSettings = columnSettings)
                                        }
                                    },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = "طباعة السجل الرسمي A4",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(currentTheme.tableBorderColor.copy(alpha = 0.6f)))
                                IconButton(
                                    onClick = { 
                                        if (students.isEmpty()) {
                                            Toast.makeText(context, "لا توجد أسماء طلاب في هذه الشعبة للمشاركة", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showShareRosterDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "مشاركة السجل وقائمة الأسماء",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).height(16.dp).background(currentTheme.tableBorderColor.copy(alpha = 0.6f)))
                                IconButton(
                                    onClick = {
                                        if (isUploadingGrades) return@IconButton
                                        val storedPin = viewModel.getStoredPin(grade, section, subject)
                                        if (storedPin != null) {
                                            startSecureUploadWithBiometric(storedPin)
                                        } else {
                                            inputPin = ""
                                            showPinDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    if (isUploadingGrades) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = currentTheme.primaryColor
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = "رفع الدرجات المشفر بالبصمة",
                                            tint = currentTheme.primaryColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tier 2: Dedicated Slim Title Ribbon
                Surface(
                    modifier = Modifier.fillMaxWidth().height(if (isLandscape) 26.dp else 32.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(currentTheme.ribbonGradient)
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 1. المادة
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("المادة:", color = Color(0xFFFBBF24), fontSize = if (isLandscape) 10.5.sp else 11.5.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(4.dp))
                                Text(subject, color = Color.White, fontSize = if (isLandscape) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.35f)))

                            // 2. الصف
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("الصف:", color = Color(0xFF93C5FD), fontSize = if (isLandscape) 10.5.sp else 11.5.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(4.dp))
                                Text(grade, color = Color.White, fontSize = if (isLandscape) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.35f)))

                            // 3. الشعبة
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("الشعبة:", color = Color(0xFF86EFAC), fontSize = if (isLandscape) 10.5.sp else 11.5.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(4.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = section,
                                        color = Color.White,
                                        fontSize = if (isLandscape) 11.sp else 12.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 0.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            val progStats = remember(students) { calculateProgressiveStats(students) }
            val totalStudents = progStats.totalStudents
            val passedStudents = progStats.passedStudents
            val failedStudents = progStats.failedStudents
            val passRate = progStats.passRate
            val currentStageName = progStats.stageName

            // Top Header: Stats + Tabs move smoothly as a single unified block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        if (headerHeightPx == 0f) {
                            headerHeightPx = coordinates.size.height.toFloat()
                        }
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val height = (placeable.height + headerOffsetPx).roundToInt().coerceAtLeast(0)
                        layout(placeable.width, height) {
                            placeable.placeRelative(0, headerOffsetPx.roundToInt())
                        }
                    }
                    .clipToBounds()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isEditingLocked) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "تم قفل السجل سحابياً بشكل نهائي من قبل إدارة المدرسة.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 4 Decorated 3D Embossed Stat Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = if (isLandscape) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // عدد الطلاب
                        EmbossedStatBox3D(
                            title = "عدد الطلاب",
                            value = "$totalStudents",
                            bgGradient = if (currentTheme.isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A)) else listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)),
                            borderColor = currentTheme.tableBorderColor,
                            textColor = currentTheme.textPrimaryColor,
                            isCompact = isLandscape,
                            modifier = Modifier.weight(1f)
                        )

                        // الناجحين
                        EmbossedStatBox3D(
                            title = "الناجحين",
                            value = "$passedStudents",
                            bgGradient = if (currentTheme.isDark) listOf(Color(0xFF064E3B), Color(0xFF022C22)) else listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)),
                            borderColor = Color(0xFF10B981),
                            textColor = if (currentTheme.isDark) Color(0xFF34D399) else Color(0xFF15803D),
                            isCompact = isLandscape,
                            modifier = Modifier.weight(1f)
                        )

                        // الراسبين
                        EmbossedStatBox3D(
                            title = "الراسبين",
                            value = "$failedStudents",
                            bgGradient = if (currentTheme.isDark) listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)) else listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2)),
                            borderColor = Color(0xFFEF4444),
                            textColor = if (currentTheme.isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                            isCompact = isLandscape,
                            modifier = Modifier.weight(1f)
                        )

                        // نسبة النجاح
                        EmbossedStatBox3D(
                            title = if (currentStageName == "لم تُدخل درجات" || currentStageName == "السجل فارغ") "نسبة النجاح" else "النسبة ($currentStageName)",
                            value = "$passRate%",
                            bgGradient = if (passRate >= 50) {
                                if (currentTheme.isDark) listOf(Color(0xFF064E3B), Color(0xFF042F2E)) else listOf(Color(0xFFECFDF5), Color(0xFFA7F3D0))
                            } else {
                                if (currentTheme.isDark) listOf(Color(0xFF7F1D1D), Color(0xFF4C0519)) else listOf(Color(0xFFFFF1F2), Color(0xFFFECDD3))
                            },
                            borderColor = if (passRate >= 50) Color(0xFF10B981) else Color(0xFFF43F5E),
                            textColor = if (passRate >= 50) {
                                if (currentTheme.isDark) Color(0xFF34D399) else Color(0xFF047857)
                            } else {
                                if (currentTheme.isDark) Color(0xFFFB7185) else Color(0xFFBE123C)
                            },
                            isCompact = isLandscape,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = currentTheme.surfaceColor,
                        contentColor = currentTheme.primaryColor
                    ) {
                        val tabPadding = if (isLandscape) 6.dp else 12.dp
                        Tab(
                            selected = selectedTab == 0, 
                            onClick = { selectedTab = 0 },
                            selectedContentColor = currentTheme.primaryColor,
                            unselectedContentColor = currentTheme.textSecondaryColor
                        ) { Text("سجل اليومي", modifier = Modifier.padding(tabPadding), fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Normal, fontSize = if (isLandscape) 12.sp else 13.5.sp) }
                        Tab(
                            selected = selectedTab == 1, 
                            onClick = { selectedTab = 1 },
                            selectedContentColor = currentTheme.primaryColor,
                            unselectedContentColor = currentTheme.textSecondaryColor
                        ) { Text("سجل المدرس", modifier = Modifier.padding(tabPadding), fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Normal, fontSize = if (isLandscape) 12.sp else 13.5.sp) }
                        Tab(
                            selected = selectedTab == 2, 
                            onClick = { selectedTab = 2 },
                            selectedContentColor = currentTheme.primaryColor,
                            unselectedContentColor = currentTheme.textSecondaryColor
                        ) { Text("سجل الإدارة", modifier = Modifier.padding(tabPadding), fontWeight = if (selectedTab == 2) FontWeight.Black else FontWeight.Normal, fontSize = if (isLandscape) 12.sp else 13.5.sp) }
                        Tab(
                            selected = selectedTab == 3, 
                            onClick = { selectedTab = 3 },
                            selectedContentColor = currentTheme.primaryColor,
                            unselectedContentColor = currentTheme.textSecondaryColor
                        ) { Text("سجل الغيابات", modifier = Modifier.padding(tabPadding), fontWeight = if (selectedTab == 3) FontWeight.Black else FontWeight.Normal, fontSize = if (isLandscape) 12.sp else 13.5.sp) }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (students.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        color = currentTheme.surfaceColor,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = currentTheme.primaryColor.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.GroupAdd,
                                        contentDescription = null,
                                        tint = currentTheme.primaryColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "لا يوجد طلاب مسجلين في هذه الشعبة بعد",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = currentTheme.textPrimaryColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "يمكنك إضافة طالب مفرد أو لصق قائمة الأسماء كاملة من ملف Word أو Excel بسهولة.",
                                fontSize = 12.sp,
                                color = currentTheme.textSecondaryColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    if (isRosterProtected) {
                                        showRosterProtectedDialog = true
                                    } else {
                                        showAddStudentDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isRosterProtected) Color(0xFF0284C7) else currentTheme.primaryColor)
                            ) {
                                Icon(if (isRosterProtected) Icons.Default.Shield else Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isRosterProtected) "قائمة الطلبة معتمدة ومحمية 🛡️" else "إضافة واستيراد الطلبة 📋", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> DailyRegisterTable(
                            students = students,
                            settings = columnSettings ?: DailyColumnSetting("${grade}_${section}_$subject"),
                            listState = listState,
                            onUpdateMarks = { s, m -> viewModel.updateStudentMarks(s, m) },
                            onEditHeader = { headerToEdit = it },
                            onEditStudentName = { 
                                if (isRosterProtected) {
                                    Toast.makeText(context, "تعديل أسماء الطلاب مقتصر على إدارة المدرسة لحماية سجل القيد 🛡️", Toast.LENGTH_LONG).show()
                                } else {
                                    studentToEdit = it
                                }
                            },
                            isEditable = !isEditingLocked,
                            isSpecial = isSpecial,
                            gradeLocks = gradeLocks
                        )
                        1 -> TeacherRegisterTable(
                            students = students, 
                            listState = listState, 
                            onUpdate = { viewModel.updateStudentMarks(it.first, it.second) }, 
                            onEditStudentName = { 
                                if (isRosterProtected) {
                                    Toast.makeText(context, "تعديل أسماء الطلاب مقتصر على إدارة المدرسة لحماية سجل القيد 🛡️", Toast.LENGTH_LONG).show()
                                } else {
                                    studentToEdit = it
                                }
                            },
                            isEditable = !isEditingLocked, 
                            isSpecial = isSpecial,
                            gradeLocks = gradeLocks
                        )
                        2 -> AdminRegisterTable(
                            students = students, 
                            listState = listState, 
                            onUpdate = { viewModel.updateStudentMarks(it.first, it.second) }, 
                            onEditStudentName = { 
                                if (isRosterProtected) {
                                    Toast.makeText(context, "تعديل أسماء الطلاب مقتصر على إدارة المدرسة لحماية سجل القيد 🛡️", Toast.LENGTH_LONG).show()
                                } else {
                                    studentToEdit = it
                                }
                            },
                            isEditable = !isEditingLocked, 
                            isSpecial = isSpecial
                        )
                        3 -> AbsencesRegisterTable(
                            students = students,
                            listState = listState,
                            absences = absences,
                            selectedDate = selectedDate.toString(),
                            onToggleAbsence = { student, isAbsent ->
                                viewModel.toggleDailyAbsence(student, selectedDate.toString(), isAbsent)
                            },
                            onDateChange = { selectedDate = it },
                            onEditStudentName = { 
                                if (isRosterProtected) {
                                    Toast.makeText(context, "تعديل أسماء الطلاب مقتصر على إدارة المدرسة لحماية سجل القيد 🛡️", Toast.LENGTH_LONG).show()
                                } else {
                                    studentToEdit = it
                                }
                            },
                            isEditable = !isEditingLocked
                        )
                    }
                }
            }
        }

        if (showShareRosterDialog) {
            AlertDialog(
                onDismissRequest = { showShareRosterDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "مشاركة سجل وأسماء الشعبة 📤",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "اختر صيغة المشاركة لشعبة $grade ($section) - (${students.size} طالب):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1. Share Official PDF directly (1-page A4)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF059669).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showShareRosterDialog = false
                                    exportAndSharePdfWithNativePdf(
                                        context,
                                        grade,
                                        section,
                                        subject,
                                        selectedTab,
                                        students,
                                        absences,
                                        config,
                                        includeCover = true,
                                        columnSettings = columnSettings
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "مشاركة السجل كملف PDF رسمي (مع صفحة الغلاف) 📄",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "كشف درجات وأسماء جاهز للطباعة والمشاركة عبر واتساب أو تليغرام",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 2. Share Student Names List as formatted text (WhatsApp)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showShareRosterDialog = false
                                    shareStudentNamesAsText(context, grade, section, subject, students)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "مشاركة قائمة الأسماء نصياً (واتساب / تليغرام) 💬",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = "إرسال قائمة أسماء طلاب الشعبة مرقمة كرسالة نصية فورية",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 3. Copy Student Names List to Clipboard
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showShareRosterDialog = false
                                    copyStudentNamesToClipboard(context, grade, section, subject, students)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "نسخ قائمة الأسماء إلى الحافظة 📋",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF5B21B6)
                                    )
                                    Text(
                                        text = "نسخ قائمة الأسماء للصقها في أي تطبيق أو مستند فوراً",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 4. Edit Official School Name & Directorate for Printing
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showShareRosterDialog = false
                                    showEditSchoolInfoDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "تعديل اسم المدرسة والمديرية في الطباعة 🏫✏️",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0369A1)
                                    )
                                    Text(
                                        text = "تغيير اسم المدرسة التي تظهر في ترويسة السجلات والـ PDF رسمياً",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showShareRosterDialog = false }) {
                        Text("إغلاق", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        if (showEditSchoolInfoDialog) {
            val resolved = remember(config) { resolveSchoolInfo(context, config) }
            var editSchoolNameInput by remember(showEditSchoolInfoDialog) { mutableStateOf(resolved.schoolName) }
            var editDirectorateInput by remember(showEditSchoolInfoDialog) { mutableStateOf(resolved.directorateName) }
            var editManagerNameInput by remember(showEditSchoolInfoDialog) { mutableStateOf(resolved.managerName) }
            var isCloudFetching by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showEditSchoolInfoDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("بيانات المدرسة والمدير في الطباعة 🏫", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "اسم المدرسة والمديرية والمدير المدخلين هنا سيظهران في الترويسة والتواقيع الرسمية للطباعة والمشاركة PDF:",
                            fontSize = 11.5.sp,
                            color = currentTheme.textSecondaryColor,
                            lineHeight = 16.sp
                        )

                        OutlinedButton(
                            onClick = {
                                isCloudFetching = true
                                viewModel.refreshSchoolIdentityFromCloud { success, msg ->
                                    isCloudFetching = false
                                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        val updated = resolveSchoolInfo(context, config)
                                        editSchoolNameInput = updated.schoolName
                                        editDirectorateInput = updated.directorateName
                                        editManagerNameInput = updated.managerName
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCloudFetching
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isCloudFetching) "جاري الجلب من السحابة... ⏳" else "جلب بيانات المدرسة والمدير من السحابة ☁️", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                        }

                        OutlinedTextField(
                            value = editSchoolNameInput,
                            onValueChange = { editSchoolNameInput = it },
                            label = { Text("اسم المدرسة الرسمي 🏫") },
                            placeholder = { Text("اتركه فارغاً إذا لم ترغب بظهوره") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editDirectorateInput,
                            onValueChange = { editDirectorateInput = it },
                            label = { Text("المديرية العامة للتربية 🏛️") },
                            placeholder = { Text("مثال: المديرية العامة لتربية ديالى") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editManagerNameInput,
                            onValueChange = { editManagerNameInput = it },
                            label = { Text("اسم مدير المدرسة 👨‍🏫") },
                            placeholder = { Text("اسم المدير للتوقيع الرسمي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanName = editSchoolNameInput.trim()
                            val cleanDir = editDirectorateInput.trim().ifBlank { "المديرية العامة للتربية" }
                            val cleanMgr = editManagerNameInput.trim()
                            coroutineScope.launch {
                                val currentCfg = viewModel.configDao.getConfig().first() ?: com.school.system.data.model.SchoolConfig()
                                viewModel.configDao.saveConfig(currentCfg.copy(
                                    schoolName = cleanName,
                                    directorateName = cleanDir,
                                    managerName = cleanMgr
                                ))
                            }
                            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                if (cleanName.isNotBlank()) putString("school_name", cleanName) else remove("school_name")
                                putString("directorate_name", cleanDir)
                                if (cleanMgr.isNotBlank()) putString("manager_name", cleanMgr) else remove("manager_name")
                                apply()
                            }
                            val bossPrefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                            bossPrefs.edit().apply {
                                if (cleanName.isNotBlank()) putString("school_name", cleanName) else remove("school_name")
                                putString("directorate_name", cleanDir)
                                if (cleanMgr.isNotBlank()) putString("manager_name", cleanMgr) else remove("manager_name")
                                apply()
                            }
                            try {
                                val sessionManager = com.school.system.data.local.SessionManager(context)
                                if (cleanName.isNotBlank()) {
                                    sessionManager.saveSchoolSession(
                                        schoolId = config?.schoolId.orEmpty(),
                                        schoolCode = config?.pairingCode.orEmpty(),
                                        schoolName = cleanName,
                                        managerName = cleanMgr
                                    )
                                }
                            } catch (_: Exception) {}
                            Toast.makeText(context, "تم حفظ بيانات المدرسة والمدير بنجاح ✓", Toast.LENGTH_SHORT).show()
                            showEditSchoolInfoDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ واعتماد في الطباعة 💾", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditSchoolInfoDialog = false }) {
                        Text("إلغاء", color = Color(0xFF64748B))
                    }
                }
            )
        }

        if (showAddStudentDialog) {
            var selectedTab by remember { mutableIntStateOf(0) }
            var singleName by remember { mutableStateOf("") }
            var multiNamesText by remember { mutableStateOf("") }
            // File Launcher for Excel / CSV / TXT / Word offline import
            val fileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        val names = ProfessionalDocumentParser.parseStudentNamesFromUri(context, uri)
                        if (names.isNotEmpty()) {
                            multiNamesText = names.joinToString("\n")
                            selectedTab = 1
                            Toast.makeText(context, "تم استخراج (${names.size}) اسم طالب احترافياً من الملف! 📄🚀", Toast.LENGTH_LONG).show()
                        } else {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                            if (content.isNotBlank()) {
                                multiNamesText = content
                                selectedTab = 1
                                Toast.makeText(context, "تم قراءة محتوى الملف أوفلاين", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تعذر استخراج الأسماء، يرجى التأكد من محتوى الملف", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "تعذر قراءة الملف المحدد أوفلاين", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showAddStudentDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إضافة واستيراد الطلبة 📋", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                Text("طالب مفرد", modifier = Modifier.padding(vertical = 8.dp), fontSize = 12.sp)
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("استيراد ملف / لصق 📄", modifier = Modifier.padding(vertical = 8.dp), fontSize = 12.sp)
                            }
                        }

                        if (selectedTab == 0) {
                            OutlinedTextField(
                                value = singleName,
                                onValueChange = { singleName = it },
                                label = { Text("اسم الطالب الرباعي") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "الصق القائمة أو اختر ملفاً:",
                                        fontSize = 11.5.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = { fileLauncher.launch("*/*") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("فتح ملف (Excel/Word/CSV/TXT) 📂", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = multiNamesText,
                                    onValueChange = { multiNamesText = it },
                                    placeholder = { Text("أحمد علي حسن محمد\nزيد كريم جاسم\nعلي حسين صالح") },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    maxLines = 12
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                if (singleName.isNotBlank()) {
                                    viewModel.addMockStudent(grade, section, subject, singleName.trim())
                                    Toast.makeText(context, "تمت إضافة الطالب بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (multiNamesText.isNotBlank()) {
                                    viewModel.importMultipleStudents(grade, section, subject, multiNamesText) { count ->
                                        Toast.makeText(context, "تم استيراد $count طالب بنجاح! 🚀", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            showAddStudentDialog = false
                        }
                    ) {
                        Text(if (selectedTab == 0) "إضافة" else "استيراد القائمة 🚀")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStudentDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        if (showPublishHomeworkDialog) {
            val hwPrefs = remember(context) { context.getSharedPreferences("homework_drafts", android.content.Context.MODE_PRIVATE) }
            val classKey = "${grade}_${section}_$subject"

            val initialTitle = remember(showPublishHomeworkDialog) {
                hwPrefs.getString("title_$classKey", null) ?: hwPrefs.getString("title_global", "") ?: ""
            }
            val initialDesc = remember(showPublishHomeworkDialog) {
                hwPrefs.getString("desc_$classKey", null) ?: hwPrefs.getString("desc_global", "") ?: ""
            }
            val initialDue = remember(showPublishHomeworkDialog) {
                hwPrefs.getString("due_$classKey", null) ?: hwPrefs.getString("due_global", "غداً") ?: "غداً"
            }

            var hwTitle by remember(showPublishHomeworkDialog) { mutableStateOf(initialTitle) }
            var hwDesc by remember(showPublishHomeworkDialog) { mutableStateOf(initialDesc) }
            var hwDueDate by remember(showPublishHomeworkDialog) { mutableStateOf(if (initialDue.isNotBlank()) initialDue else "غداً") }
            var isSendingHw by remember { mutableStateOf(false) }

            val saveCurrentDraft: (String, String, String) -> Unit = { titleToSave, descToSave, dueToSave ->
                hwPrefs.edit()
                    .putString("title_$classKey", titleToSave)
                    .putString("desc_$classKey", descToSave)
                    .putString("due_$classKey", dueToSave)
                    .putString("title_global", titleToSave)
                    .putString("desc_global", descToSave)
                    .putString("due_global", dueToSave)
                    .apply()
            }

            val clearCurrentDraft: () -> Unit = {
                hwTitle = ""
                hwDesc = ""
                hwDueDate = "غداً"
                hwPrefs.edit()
                    .remove("title_$classKey")
                    .remove("desc_$classKey")
                    .remove("due_$classKey")
                    .remove("title_global")
                    .remove("desc_global")
                    .remove("due_global")
                    .apply()
                Toast.makeText(context, "تم تصفير محتوى الواجب بنجاح 🧹", Toast.LENGTH_SHORT).show()
            }

            var selectedDueDateChips by remember { mutableStateOf(setOf("الدرس القادم")) }

            val toggleDueDateChip: (String) -> Unit = { chip ->
                val newSet = selectedDueDateChips.toMutableSet()
                if (newSet.contains(chip)) {
                    newSet.remove(chip)
                } else {
                    newSet.add(chip)
                }
                selectedDueDateChips = newSet

                val hasNextLesson = newSet.contains("الدرس القادم")
                val daysOrder = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
                val days = daysOrder.filter { newSet.contains(it) }

                val newDue = when {
                    hasNextLesson && days.isNotEmpty() -> "الدرس القادم يوم " + days.joinToString(" و ")
                    hasNextLesson -> "الدرس القادم"
                    days.isNotEmpty() -> "يوم " + days.joinToString(" و ")
                    else -> ""
                }
                hwDueDate = newDue
                saveCurrentDraft(hwTitle, hwDesc, newDue)
            }

            AlertDialog(
                onDismissRequest = { 
                    saveCurrentDraft(hwTitle, hwDesc, hwDueDate)
                    showPublishHomeworkDialog = false 
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("بطاقة الواجب والتعليمات 📝", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        // زر تصفير الواجب بلمسة واحدة
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.clickable { clearCurrentDraft() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "تصفير الواجب", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تصفير الواجب", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Prominent Lesson & Class Info Card
                        Surface(
                            color = Color(0xFFF3E8FF),
                            border = BorderStroke(1.dp, Color(0xFFC084FC)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "المادة والدرس: $subject 📖",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF581C87)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "موجّه لكافة طلاب: $grade ($section)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7E22CE)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = hwTitle,
                            onValueChange = { 
                                hwTitle = it
                                saveCurrentDraft(it, hwDesc, hwDueDate)
                            },
                            label = { Text("عنوان الواجب أو التوجيهات") },
                            placeholder = { Text("مثال: حل تمارين ص 42 + مراجعة الفصل الثاني") },
                            textStyle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = currentTheme.textPrimaryColor),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = hwDesc,
                            onValueChange = { 
                                hwDesc = it
                                saveCurrentDraft(hwTitle, it, hwDueDate)
                            },
                            label = { Text("تفاصيل وملاحظات إضافية للطلاب") },
                            placeholder = { Text("مثال: كتابة التمارين في دفتر الواجبات وإحضاره غداً") },
                            textStyle = TextStyle(fontSize = 13.sp, color = currentTheme.textPrimaryColor),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Due Date Input & Multi-Select Quick Preset Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = hwDueDate,
                                onValueChange = { 
                                    hwDueDate = it
                                    saveCurrentDraft(hwTitle, hwDesc, it)
                                },
                                label = { Text("موعد الإنجاز والتسليم 📅") },
                                placeholder = { Text("اكتب الموعد أو اختر اختصارات...") },
                                textStyle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = currentTheme.textPrimaryColor),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                text = "اختصارات الموعد (يمكنك اختيار أكثر من خيار للدمج):",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )

                            // Row 1: الدرس القادم | الأحد | الإثنين
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("الدرس القادم", "الأحد", "الإثنين").forEach { chip ->
                                    val isSelected = selectedDueDateChips.contains(chip)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(if (chip == "الدرس القادم") 1.2f else 1f)
                                            .clickable { toggleDueDateChip(chip) }
                                    ) {
                                        Text(
                                            text = if (chip == "الدرس القادم") "الدرس القادم ⏱️" else chip,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }

                            // Row 2: الثلاثاء | الأربعاء | الخميس
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("الثلاثاء", "الأربعاء", "الخميس").forEach { chip ->
                                    val isSelected = selectedDueDateChips.contains(chip)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF7C3AED) else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { toggleDueDateChip(chip) }
                                    ) {
                                        Text(
                                            text = chip,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (hwTitle.isNotBlank()) {
                                isSendingHw = true
                                saveCurrentDraft(hwTitle, hwDesc, hwDueDate)
                                viewModel.publishDailyHomework(grade, section, subject, hwTitle, hwDesc, hwDueDate) { success, msg ->
                                    isSendingHw = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        showPublishHomeworkDialog = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "يرجى كتابة عنوان الواجب أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSendingHw,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        if (isSendingHw) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("جاري الإرسال...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إرسال التعليمات للشعبة 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        saveCurrentDraft(hwTitle, hwDesc, hwDueDate)
                        showPublishHomeworkDialog = false 
                    }) {
                        Text("إغلاق وحفظ المسودة")
                    }
                }
            )
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "اعتماد مادة: $subject ($section)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "أدخل الرمز السري للمادة والشعبة، أو كود المشرف العام المعتمد من الإدارة، أو اضغط رفع مباشر أدناه.",
                            fontSize = 12.5.sp,
                            color = currentTheme.textSecondaryColor,
                            lineHeight = 18.sp
                        )
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = { inputPin = it },
                            label = { Text("الرمز السري للمادة أو رمز المشرف") },
                            placeholder = { Text("أدخل رمز المادة أو SUP-xxxx...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(Modifier.height(4.dp))

                        // الزر الأول: رفع مباشر فوري بدون رمز
                        Button(
                            onClick = {
                                showPinDialog = false
                                startSecureUploadWithBiometric("DIRECT")
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("رفع مباشر فوري للسحابة ⚡", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                        }

                        // الزر الثاني: تأكيد الرمز والرفع بالبصمة
                        OutlinedButton(
                            onClick = {
                                val pin = inputPin.trim()
                                if (pin.isEmpty()) {
                                    android.widget.Toast.makeText(context, "يرجى إدخال الرمز السري", android.widget.Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                showPinDialog = false
                                startSecureUploadWithBiometric(pin)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryColor),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, currentTheme.primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تأكيد الرمز والرفع بالبصمة 🔒", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("إلغاء", color = currentTheme.textSecondaryColor)
                    }
                }
            )
        }

        if (showInvalidPinDialog) {
            AlertDialog(
                onDismissRequest = { showInvalidPinDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "تأكيد الرفع والمصادقة",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = invalidPinDialogMessage.ifEmpty {
                                "يمكنك المتابعة بالرفع المباشر الفوري إلى السحابة أو إدخال رمز آخر."
                            },
                            fontSize = 13.sp,
                            color = currentTheme.textPrimaryColor,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = {
                                showInvalidPinDialog = false
                                startSecureUploadWithBiometric("DIRECT")
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تجاوز ورفع مباشر فوري للسحابة ⚡", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                showInvalidPinDialog = false
                                inputPin = ""
                                showPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إدخال رمز جديد 🔑", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showInvalidPinDialog = false }) {
                        Text("إلغاء", color = currentTheme.textSecondaryColor)
                    }
                }
            )
        }

        headerToEdit?.let { index ->
            var newName by remember { mutableStateOf(columnSettings?.columnNames?.get(index) ?: "") }
            AlertDialog(
                onDismissRequest = { headerToEdit = null },
                title = { Text("تسمية العمود") },
                text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("اسم العمود") }) },
                confirmButton = {
                    Button(onClick = {
                        val current = columnSettings ?: DailyColumnSetting("${grade}_${section}_$subject")
                        val newList = current.columnNames.toMutableList()
                        newList[index] = newName
                        viewModel.updateDailyColumnSettings(current.copy(columnNames = newList))
                        headerToEdit = null
                    }) { Text("حفظ") }
                }
            )
        }

        if (showHelpGuideDialog) {
            com.school.system.ui.components.HelpGuideDialog(
                onDismiss = { showHelpGuideDialog = false }
            )
        }

        if (showThemeDialog) {
            com.school.system.ui.theme.ThemeSelectionDialog(
                onDismiss = { showThemeDialog = false }
            )
        }

        if (studentToEdit != null) {
            val targetStudent = studentToEdit!!
            var editedName by remember(targetStudent) { mutableStateOf(targetStudent.fullName) }

            AlertDialog(
                onDismissRequest = { studentToEdit = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = currentTheme.primaryColor, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تعديل اسم الطالب ✏️", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "يمكنك تعديل وتصحيح اسم الطالب وسيتم تحديثه فورياً في جميع السجلات:",
                            fontSize = 12.sp,
                            color = currentTheme.textSecondaryColor
                        )
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("الاسم الرباعي واللقب") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                viewModel.updateStudentName(targetStudent, editedName.trim())
                                studentToEdit = null
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor)
                    ) {
                        Text("حفظ التعديل ✓", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { studentToEdit = null }) {
                        Text("إلغاء", color = Color(0xFF64748B))
                    }
                }
            )
        }

        if (showRosterProtectedDialog) {
            AlertDialog(
                onDismissRequest = { showRosterProtectedDialog = false },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.12f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        "قائمة الطلبة معتمدة ومحمية 🛡️",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "التطبيق مقترن حالياً بالنظام المركزي للمدرسة. لحماية سجل القيد العام وتفادي أي أخطاء أو تكرار بالبيانات في السحابة، يمنع إضافة أو حذف الطلاب من هاتف المعلم.",
                            fontSize = 12.5.sp,
                            color = currentTheme.textPrimaryColor,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "💡 لإضافة أي طالب جديد أو منقول: يرجى التنسيق مع إدارة المدرسة لإضافته في النظام المكتبي، ثم الضغط على زر (استدعاء وتحديث 🔄) في الشريط العلوي لاستدعائه فوراً.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF0369A1),
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showRosterProtectedDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حسناً، فهمت ذلك ✓", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

private fun isSpecialSubject(subject: String): Boolean {
    val s = subject.trim().lowercase()
    return s.contains("عرب") ||
           s.contains("انكل") || s.contains("إنكل") ||
           s.contains("انجلي") || s.contains("إنجلي") ||
           s.contains("انكلش") || s.contains("انجلش") ||
           s.contains("english") || s.contains("engl") || s == "e" || s == "eng" || s == "en" || s == "el" ||
           s.contains("اسلام") || s.contains("إسلام") ||
           s.contains("قرآن") || s.contains("قران") ||
           s.contains("دين") ||
           s.contains("فرنس") || s.contains("french") || s == "f" ||
           s.contains("كرد") || s.contains("kurd") ||
           s.contains("تركم") || s.contains("turk") ||
           s.contains("سريان") || s.contains("syriac") ||
           s.contains("المان") || s.contains("ألمان") || s.contains("german") ||
           s.contains("اسبان") || s.contains("إسبان") || s.contains("spanish") ||
           s.contains("لغة") || s.contains("لغات")
}

fun sanitizeRoleName(name: String?): String {
    if (name.isNullOrBlank()) return ""
    val t = name.trim()
    val isRole = t.contains("مدير المدرسة") ||
                 t.contains("الإشراف العام") ||
                 t.contains("الاشراف العام") ||
                 t.contains("المشرف العام") ||
                 t.contains("الإشراف") ||
                 t.contains("الاشراف") ||
                 t.contains("المشرف") ||
                 t.contains("المدير /") ||
                 t.contains("مدير /") ||
                 t.contains("إدارة المدرسة") ||
                 t.contains("ادارة المدرسة") ||
                 t == "مدير" ||
                 t == "المدير" ||
                 t == "مدرس المادة" ||
                 t == "أستاذ المادة" ||
                 t == "استاذ المادة" ||
                 t == "-"
    return if (isRole) "" else t
}

fun resolveTeacherName(
    context: Context,
    grade: String,
    section: String,
    subject: String,
    config: SchoolConfig?
): String {
    try {
        val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
        val rawSchedule = prefs.getString("synced_schedule", null)
            ?: context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).getString("synced_schedule", null)

        if (!rawSchedule.isNullOrBlank()) {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val rootObj = Gson().fromJson<Map<String, Any>>(rawSchedule, type)
            val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
            for (day in daysList) {
                val dayData = rootObj[day] as? List<*> ?: continue
                for (row in dayData) {
                    val r = row as? Map<*, *> ?: continue
                    val g = r["grade"]?.toString().orEmpty()
                    val s = r["subject"]?.toString().orEmpty()
                    val t = r["teacherName"]?.toString().orEmpty()

                    val gradeMatches = g.contains(grade) && (section.isBlank() || g.contains(section) || g.contains("($section)"))
                    val subjectMatches = s.isNotBlank() && (s.contains(subject) || subject.contains(s))
                    if (gradeMatches && subjectMatches && t.isNotBlank() && t != "-") {
                        val clean = sanitizeRoleName(t)
                        if (clean.isNotBlank()) return clean
                    }
                }
            }
        }

        val prefTeacher = sanitizeRoleName(prefs.getString("teacher_name", ""))
        if (prefTeacher.isNotBlank()) return prefTeacher

        val bossTeacher = sanitizeRoleName(prefs.getString("user_name", ""))
        if (bossTeacher.isNotBlank()) return bossTeacher
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ""
}

data class ResolvedSchoolInfo(
    val schoolName: String,
    val directorateName: String,
    val managerName: String
)

fun resolveSchoolInfo(
    context: Context,
    config: SchoolConfig?,
    teacherName: String? = null
): ResolvedSchoolInfo {
    try {
        val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
        val sessionManager = com.school.system.data.local.SessionManager(context)
        val bossPrefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

        fun isInvalid(s: String?): Boolean {
            if (s.isNullOrBlank()) return true
            val t = s.trim()
            return t == "المدرسة الذكية" ||
                   t == "مدرسة التميز للبنين" ||
                   t == "مدرسة سحابية" ||
                   t == "سجل مستقل (أوفلاين)" ||
                   t == "مدرسة مرتبطة" ||
                   t == "المدرسة" ||
                   t == "school_01"
        }

        fun isLegitManager(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val clean = sanitizeRoleName(name)
            if (clean.isBlank()) return false
            val cleanTeacher = sanitizeRoleName(teacherName)
            if (cleanTeacher.isNotBlank() && clean.equals(cleanTeacher, ignoreCase = true)) return false
            if (clean.contains("مدرس") || clean.contains("أستاذ") || clean.contains("استاذ")) return false
            return true
        }

        // 1. SharedPreferences (set by user or synced from cloud)
        val prefSchool = prefs.getString("school_name", null)?.trim()?.takeIf { !isInvalid(it) }
        val prefDir = prefs.getString("directorate_name", null)?.trim()?.takeIf { it.isNotBlank() }
        val prefMgr = prefs.getString("manager_name", null)?.trim()?.takeIf { isLegitManager(it) }
            ?: prefs.getString("school_manager_name", null)?.trim()?.takeIf { isLegitManager(it) }
            ?: prefs.getString("principal_name", null)?.trim()?.takeIf { isLegitManager(it) }

        // 2. Room database config
        val cfgSchool = config?.schoolName?.trim()?.takeIf { !isInvalid(it) }
        val cfgDir = config?.directorateName?.trim()?.takeIf { it.isNotBlank() }
        val cfgMgr = config?.managerName?.trim()?.takeIf { isLegitManager(it) }

        // 3. SessionManager
        val sessionSchool = sessionManager.getSchoolName()?.trim()?.takeIf { !isInvalid(it) }
        val sessionMgr = sessionManager.getManagerName()?.trim()?.takeIf { isLegitManager(it) }

        // 4. BossPrefs
        val bossSchool = bossPrefs.getString("school_name", null)?.trim()?.takeIf { !isInvalid(it) }
        val bossMgr = bossPrefs.getString("manager_name", null)?.trim()?.takeIf { isLegitManager(it) }
            ?: bossPrefs.getString("school_manager_name", null)?.trim()?.takeIf { isLegitManager(it) }

        val finalSchool = prefSchool
            ?: cfgSchool
            ?: sessionSchool
            ?: bossSchool
            ?: "" // ترك اسم المدرسة فارغاً تماماً إذا لم يكتب اسم

        val finalDir = prefDir ?: cfgDir ?: "المديرية العامة للتربية"

        val finalMgr = prefMgr ?: sessionMgr ?: bossMgr ?: cfgMgr ?: ""

        return ResolvedSchoolInfo(finalSchool, finalDir, finalMgr)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val fallbackMgr = config?.managerName?.trim()?.takeIf {
        val clean = sanitizeRoleName(it)
        val cleanTeacher = sanitizeRoleName(teacherName)
        clean.isNotBlank() && (cleanTeacher.isBlank() || !clean.equals(cleanTeacher, ignoreCase = true)) && !clean.contains("مدرس") && !clean.contains("أستاذ")
    } ?: ""

    return ResolvedSchoolInfo(
        config?.schoolName?.trim()?.takeIf { it.isNotBlank() && it != "مدرسة التميز للبنين" && it != "المدرسة الذكية" } ?: "",
        config?.directorateName?.takeIf { it.isNotBlank() } ?: "المديرية العامة للتربية",
        fallbackMgr
    )
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun copyStudentNamesToClipboard(
    context: Context,
    grade: String,
    section: String,
    subject: String,
    students: List<Student>
) {
    try {
        val text = buildStudentRosterText(grade, section, subject, students)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("قائمة أسماء الطلاب", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ قائمة الأسماء بنجاح (${students.size} طالب) 📋", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "تعذر نسخ الأسماء: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun shareStudentNamesAsText(
    context: Context,
    grade: String,
    section: String,
    subject: String,
    students: List<Student>
) {
    try {
        val text = buildStudentRosterText(grade, section, subject, students)
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "قائمة أسماء طلبة $grade ($section)")
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        val chooser = android.content.Intent.createChooser(sendIntent, "مشاركة قائمة الأسماء عبر:").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "تعذر مشاركة الأسماء: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun buildStudentRosterText(
    grade: String,
    section: String,
    subject: String,
    students: List<Student>
): String {
    val sb = StringBuilder()
    sb.append("📋 قائمة أسماء طلبة: $grade ($section)\n")
    if (subject.isNotBlank()) {
        sb.append("📚 المادة: $subject\n")
    }
    sb.append("👥 العدد الكلي: ${students.size} طالب\n")
    sb.append("-----------------------------\n")
    students.forEachIndexed { index, student ->
        sb.append("${index + 1}. ${student.fullName}\n")
    }
    sb.append("-----------------------------\n")
    sb.append("تم التصدير عبر منظومة الإدارة المدرسية الذكية 🏫")
    return sb.toString()
}

fun printRegister(
    context: Context,
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<Student>,
    absences: List<AbsenceRecord>,
    config: SchoolConfig?,
    explicitTeacherName: String? = null,
    includeCover: Boolean = true,
    columnSettings: DailyColumnSetting? = null
) {
    if (students.isEmpty()) {
        Toast.makeText(context, "لا توجد أسماء طلاب في هذه الشعبة للطباعة", Toast.LENGTH_SHORT).show()
        return
    }

    val activity = context.findActivity() ?: run {
        Toast.makeText(context, "تعذر تشغيل خدمة الطباعة (لم يتم العثور على واجهة النشاط)", Toast.LENGTH_SHORT).show()
        return
    }

    android.os.Handler(android.os.Looper.getMainLooper()).post {
        try {
            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(activity, "خدمة الطباعة غير متوفرة في هذا الجهاز", Toast.LENGTH_LONG).show()
                return@post
            }

            val teacher = explicitTeacherName ?: resolveTeacherName(activity, grade, section, subject, config)
            val (resolvedSchool, resolvedDirectorate, resolvedManager) = resolveSchoolInfo(activity, config, teacher)
            val resolvedColumnSettings = columnSettings ?: try {
                com.school.system.data.SchoolDatabase.getInstance(activity).dailyColumnDao().getSettingsSync("${grade}_${section}_$subject")
            } catch (e: Exception) { null }

            val webView = WebView(activity)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
            }

            val tabName = when (tabIndex) {
                0 -> "سجل اليومي"
                1 -> "سجل المدرس"
                2 -> "سجل الإدارة"
                else -> "سجل الغيابات"
            }
            val jobName = "$tabName - $grade $section - $subject"
            val html = generateGradesHtml(grade, section, subject, tabIndex, students, absences, config, teacher, includeCover, resolvedSchool, resolvedDirectorate, resolvedColumnSettings, explicitManagerName = resolvedManager)

            Toast.makeText(activity, "جاري تجهيز السجل للطباعة... 🖨️", Toast.LENGTH_SHORT).show()

            webView.webViewClient = object : WebViewClient() {
                private var printed = false
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (printed) return
                    printed = true
                    try {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(if (tabIndex == 0) PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4.asPortrait())
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager.print(jobName, printAdapter, printAttributes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(activity, "خطأ أثناء إرسال أمر الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    Toast.makeText(activity, "خطأ في تهيئة صفحة الطباعة", Toast.LENGTH_SHORT).show()
                }
            }

            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "تعذر بدء عملية الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

fun shareRegister(
    context: android.content.Context,
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<com.school.system.data.model.Student>,
    absences: List<com.school.system.data.model.AbsenceRecord>,
    config: com.school.system.data.model.SchoolConfig?,
    explicitTeacherName: String? = null,
    includeCover: Boolean = true,
    columnSettings: com.school.system.data.model.DailyColumnSetting? = null
) {
    exportAndSharePdfWithNativePdf(context, grade, section, subject, tabIndex, students, absences, config, explicitTeacherName, includeCover = includeCover, columnSettings = columnSettings)
}

fun exportAndSharePdfWithNativePdf(
    context: Context,
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<Student>,
    absences: List<AbsenceRecord>,
    config: SchoolConfig?,
    explicitTeacherName: String? = null,
    includeCover: Boolean = true,
    columnSettings: DailyColumnSetting? = null
) {
    if (students.isEmpty()) {
        Toast.makeText(context, "لا توجد أسماء طلاب في هذه الشعبة للتصدير", Toast.LENGTH_SHORT).show()
        return
    }

    val activity = context.findActivity() ?: run {
        Toast.makeText(context, "تعذر العثور على واجهة النشاط للتصدير", Toast.LENGTH_SHORT).show()
        return
    }

    android.os.Handler(android.os.Looper.getMainLooper()).post {
        try {
            val teacher = explicitTeacherName ?: resolveTeacherName(activity, grade, section, subject, config)
            val (resolvedSchool, resolvedDirectorate, resolvedManager) = resolveSchoolInfo(activity, config, teacher)
            val resolvedColumnSettings = columnSettings ?: try {
                com.school.system.data.SchoolDatabase.getInstance(activity).dailyColumnDao().getSettingsSync("${grade}_${section}_$subject")
            } catch (e: Exception) { null }
            val tabName = when (tabIndex) {
                0 -> "سجل اليومي"
                1 -> "سجل المدرس"
                2 -> "سجل الإدارة"
                else -> "سجل الغيابات"
            }
            val jobName = "$tabName - $grade $section - $subject"

            Toast.makeText(activity, "جاري تحضير ملف PDF للمشاركة... ⏳", Toast.LENGTH_SHORT).show()

            val webView = WebView(activity)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
            }

            val html = generateGradesHtml(
                grade, section, subject, tabIndex, students, absences, config, teacher,
                includeCover = includeCover, resolvedSchool, resolvedDirectorate, resolvedColumnSettings,
                explicitManagerName = resolvedManager
            )

            var isHandled = false

            fun finishProcess(pdfFile: File?) {
                if (isHandled) return
                isHandled = true
                if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0L) {
                    shareFile(activity, pdfFile, subject, grade, section)
                } else {
                    Toast.makeText(activity, "تعذر إنشاء ملف PDF للمشاركة", Toast.LENGTH_SHORT).show()
                }
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (isHandled) return
                    try {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(if (tabIndex == 0) PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4.asPortrait())
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()

                        val pdfPrint = PdfPrint(printAttributes)
                        val fileName = "${tabName}_${grade}_${section}_${System.currentTimeMillis()}.pdf"
                        pdfPrint.print(webView, printAdapter, activity.cacheDir, fileName, object : PdfPrint.Callback {
                            override fun onSuccess(file: File) {
                                finishProcess(file)
                            }

                            override fun onFailure(error: String?) {
                                if (!isHandled) {
                                    isHandled = true
                                    Toast.makeText(activity, "فشل حفظ ملف PDF: $error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (!isHandled) {
                            isHandled = true
                            val msg = e.localizedMessage ?: e.javaClass.simpleName
                            Toast.makeText(activity, "خطأ أثناء إنشاء ملف PDF: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    if (!isHandled) {
                        isHandled = true
                        Toast.makeText(activity, "خطأ في تهيئة صفحة PDF للمشاركة", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)

            // مهلة أمان قصوى لحماية العملية
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isHandled) {
                    isHandled = true
                    Toast.makeText(activity, "استغرقت عملية إعداد PDF وقتاً أطول من المتوقع", Toast.LENGTH_SHORT).show()
                }
            }, 10000)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "تعذر إنشاء ملف PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

fun shareFile(context: Context, pdfFile: File, subject: String, grade: String, section: String) {
    try {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            Toast.makeText(context, "ملف PDF غير متوفر للمشاركة", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "سجل الدرجات الرسمي - $subject - $grade ($section)")
            putExtra(android.content.Intent.EXTRA_TEXT, "سجل درجات وأسماء شعبة $grade ($section) - مادة $subject بصيغة PDF الرسمية.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(shareIntent, "مشاركة سجل الدرجات (PDF) عبر:").apply {
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "تعذر مشاركة السجل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun generateGradesHtml(
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<com.school.system.data.model.Student>,
    absences: List<com.school.system.data.model.AbsenceRecord>,
    config: com.school.system.data.model.SchoolConfig?,
    explicitTeacherName: String? = null,
    includeCover: Boolean = true,
    explicitSchoolName: String? = null,
    explicitDirectorateName: String? = null,
    columnSettings: com.school.system.data.model.DailyColumnSetting? = null,
    explicitManagerName: String? = null,
    onlyCover: Boolean = false
): String {
    val isInvalid = { s: String? ->
        s.isNullOrBlank() || s == "المدرسة الذكية" || s == "مدرسة التميز للبنين" || s == "مدرسة سحابية" || s == "سجل مستقل (أوفلاين)" || s == "مدرسة مرتبطة" || s == "المدرسة" || s == "school_01"
    }
    // إذا لم يكتب اسماً للمدرسة يترك اسم المدرسة فارغاً
    val schoolName = explicitSchoolName?.trim()?.takeIf { !isInvalid(it) }
        ?: config?.schoolName?.trim()?.takeIf { !isInvalid(it) }
        ?: ""
    val directorate = explicitDirectorateName?.trim()?.takeIf { it.isNotBlank() }
        ?: config?.directorateName?.trim()?.takeIf { it.isNotBlank() }
        ?: "المديرية العامة للتربية"
    val cleanExplicitTeacher = sanitizeRoleName(explicitTeacherName)
    val teacherName = cleanExplicitTeacher

    // حل مشكلة تكرار اسم الأستاذ في مكان المدير
    var cleanManagerName = sanitizeRoleName(explicitManagerName)
    if (cleanManagerName.isBlank()) {
        val cfgMgr = sanitizeRoleName(config?.managerName)
        if (cfgMgr.isNotBlank() && (teacherName.isBlank() || !cfgMgr.equals(teacherName, ignoreCase = true)) && !cfgMgr.contains("مدرس") && !cfgMgr.contains("أستاذ")) {
            cleanManagerName = cfgMgr
        }
    } else if (teacherName.isNotBlank() && cleanManagerName.equals(teacherName, ignoreCase = true)) {
        cleanManagerName = ""
    }
    
    // عنوان السجل يكون فقط سجل اليومي وسجل المدرس اسمه فقط سجل المدرس وكذلك سجل الإدارة
    val tabName = when (tabIndex) {
        0 -> "سجل اليومي"
        1 -> "سجل المدرس"
        2 -> "سجل الإدارة"
        else -> "سجل الغيابات"
    }

    // وإذا لم يكتب درجات اترك مكان الدرجات فارغاً
    fun fmtGrade(v: Float?): String {
        if (v == null || v <= 0f) return ""
        return if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(java.util.Locale.US, v)
    }

    fun fmtAvg(list: List<Float>?): String {
        if (list.isNullOrEmpty()) return ""
        val nonZero = list.filter { it > 0f }
        if (nonZero.isEmpty()) return ""
        val avg = nonZero.average().toFloat()
        return if (avg % 1f == 0f) avg.toInt().toString() else "%.1f".format(java.util.Locale.US, avg)
    }

    val isSpecial = isSpecialSubject(subject)

    // Build Daily Register (tabIndex == 0) columns matching Android app
    // بالنسبة للعلميات يقسم الشهر الأول إلى عمودين يومي وتحريري (+ معدل ش1)
    // وفي اللغات والتربية الإسلامية يقسم الشهر الأول إلى الأعمدة التي يخصصها الأستاذ في التطبيق وبنفس التسميات التي يختارها
    val m1Cols: List<Pair<Int, String>> = if (isSpecial) {
        val named = (0..4).mapNotNull { i ->
            val name = columnSettings?.columnNames?.getOrNull(i)?.trim()
            if (!name.isNullOrBlank()) Pair(i, name) else null
        }
        val maxMarkIdx = (0..4).indexOfLast { i -> students.any { (it.marks.m1Daily.getOrNull(i) ?: 0f) > 0f } }
        val count = maxOf(1, maxOf(named.maxOfOrNull { it.first } ?: -1, maxMarkIdx) + 1)
        (0 until count).map { i ->
            val custom = columnSettings?.columnNames?.getOrNull(i)?.trim()
            val label = if (!custom.isNullOrBlank()) custom else if (count == 1) "شفهي" else "شفهي ${i + 1}"
            Pair(i, label)
        }
    } else emptyList()

    val m2Cols: List<Pair<Int, String>> = if (isSpecial) {
        val named = (0..4).mapNotNull { i ->
            val name = columnSettings?.columnNames?.getOrNull(5 + i)?.trim()
            if (!name.isNullOrBlank()) Pair(i, name) else null
        }
        val maxMarkIdx = (0..4).indexOfLast { i -> students.any { (it.marks.m2Daily.getOrNull(i) ?: 0f) > 0f } }
        val count = maxOf(1, maxOf(named.maxOfOrNull { it.first } ?: -1, maxMarkIdx) + 1)
        (0 until count).map { i ->
            val custom = columnSettings?.columnNames?.getOrNull(5 + i)?.trim()
            val label = if (!custom.isNullOrBlank()) custom else if (count == 1) "شفهي" else "شفهي ${i + 1}"
            Pair(i, label)
        }
    } else emptyList()

    val midtermOralCols: List<Pair<Int, String>> = if (isSpecial) {
        val maxMarkIdx = (0..4).indexOfLast { i -> students.any { (it.marks.midtermOral.getOrNull(i) ?: 0f) > 0f } }
        val count = maxOf(1, maxMarkIdx + 1)
        (0 until count).map { i ->
            Pair(i, if (count == 1) "شفهي" else "شفهي ${i + 1}")
        }
    } else emptyList()

    val m3Cols: List<Pair<Int, String>> = if (isSpecial) {
        val named = (0..4).mapNotNull { i ->
            val name = columnSettings?.columnNames?.getOrNull(10 + i)?.trim()
            if (!name.isNullOrBlank()) Pair(i, name) else null
        }
        val maxMarkIdx = (0..4).indexOfLast { i -> students.any { (it.marks.m3Daily.getOrNull(i) ?: 0f) > 0f } }
        val count = maxOf(1, maxOf(named.maxOfOrNull { it.first } ?: -1, maxMarkIdx) + 1)
        (0 until count).map { i ->
            val custom = columnSettings?.columnNames?.getOrNull(10 + i)?.trim()
            val label = if (!custom.isNullOrBlank()) custom else if (count == 1) "شفهي" else "شفهي ${i + 1}"
            Pair(i, label)
        }
    } else emptyList()

    val m4Cols: List<Pair<Int, String>> = if (isSpecial) {
        val named = (0..4).mapNotNull { i ->
            val name = columnSettings?.columnNames?.getOrNull(15 + i)?.trim()
            if (!name.isNullOrBlank()) Pair(i, name) else null
        }
        val maxMarkIdx = (0..4).indexOfLast { i -> students.any { (it.marks.m4Daily.getOrNull(i) ?: 0f) > 0f } }
        val count = maxOf(1, maxOf(named.maxOfOrNull { it.first } ?: -1, maxMarkIdx) + 1)
        (0 until count).map { i ->
            val custom = columnSettings?.columnNames?.getOrNull(15 + i)?.trim()
            val label = if (!custom.isNullOrBlank()) custom else if (count == 1) "شفهي" else "شفهي ${i + 1}"
            Pair(i, label)
        }
    } else emptyList()

    val finalOralCols: List<Pair<Int, String>> = if (isSpecial) {
        val maxMarkIdx = (0..4).indexOfLast { i -> students.any { (it.marks.finalOral.getOrNull(i) ?: 0f) > 0f } }
        val count = maxOf(1, maxMarkIdx + 1)
        (0 until count).map { i ->
            Pair(i, if (count == 1) "شفهي" else "شفهي ${i + 1}")
        }
    } else emptyList()

    val m1ColSpan = if (!isSpecial) 3 else (m1Cols.size + (if (m1Cols.size > 1) 1 else 0) + 2)
    val m2ColSpan = if (!isSpecial) 3 else (m2Cols.size + (if (m2Cols.size > 1) 1 else 0) + 2)
    val midtermColSpan = if (!isSpecial) 1 else (midtermOralCols.size + (if (midtermOralCols.size > 1) 1 else 0) + 2)
    val m3ColSpan = if (!isSpecial) 3 else (m3Cols.size + (if (m3Cols.size > 1) 1 else 0) + 2)
    val m4ColSpan = if (!isSpecial) 3 else (m4Cols.size + (if (m4Cols.size > 1) 1 else 0) + 2)
    val finalColSpan = if (!isSpecial) 1 else (finalOralCols.size + (if (finalOralCols.size > 1) 1 else 0) + 2)

    val rowsHtml = java.lang.StringBuilder()
    students.forEachIndexed { index, student ->
        val marks = student.marks
        val totalAbs = student.historicalAbsences + absences.count { it.studentId == student.id }
        val d2Formatted = when {
            marks.finalWrittenD2 == null || marks.finalWrittenD2 == 0f -> ""
            isSpecial -> fmtGrade(marks.finalOral.sum() + marks.finalWrittenD2!!)
            else -> fmtGrade(marks.finalWrittenD2)
        }
        val scoreStr = if (marks.latestRecordedScore() > 0f) fmtGrade(marks.latestRecordedScore()) else ""

        rowsHtml.append("<tr class='data-row'>")
        rowsHtml.append("<td class='seq-col'>${index + 1}</td>")
        rowsHtml.append("<td class='name-col'>${student.fullName}</td>")

        if (tabIndex == 0) {
            // Month 1
            if (!isSpecial) {
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m1Daily.firstOrNull())}</td>")
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m1Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #eff6ff;'>${fmtGrade(marks.m1MonthAvg)}</td>")
            } else {
                m1Cols.forEach { col ->
                    rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m1Daily.getOrNull(col.first))}</td>")
                }
                if (m1Cols.size > 1) {
                    val vals = m1Cols.mapNotNull { marks.m1Daily.getOrNull(it.first) }.filter { it > 0f }
                    rowsHtml.append("<td class='cell-data'>${fmtAvg(vals)}</td>")
                }
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m1Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #eff6ff;'>${fmtGrade(marks.m1MonthAvg)}</td>")
            }

            // Month 2
            if (!isSpecial) {
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m2Daily.firstOrNull())}</td>")
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m2Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #f5f3ff;'>${fmtGrade(marks.m2MonthAvg)}</td>")
            } else {
                m2Cols.forEach { col ->
                    rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m2Daily.getOrNull(col.first))}</td>")
                }
                if (m2Cols.size > 1) {
                    val vals = m2Cols.mapNotNull { marks.m2Daily.getOrNull(it.first) }.filter { it > 0f }
                    rowsHtml.append("<td class='cell-data'>${fmtAvg(vals)}</td>")
                }
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m2Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #f5f3ff;'>${fmtGrade(marks.m2MonthAvg)}</td>")
            }

            // Term 1
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #fef3c7;'>${fmtGrade(marks.term1Avg)}</td>")

            // Midterm
            if (!isSpecial) {
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.midtermScore)}</td>")
            } else {
                midtermOralCols.forEach { col ->
                    rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.midtermOral.getOrNull(col.first))}</td>")
                }
                if (midtermOralCols.size > 1) {
                    val vals = midtermOralCols.mapNotNull { marks.midtermOral.getOrNull(it.first) }.filter { it > 0f }
                    rowsHtml.append("<td class='cell-data'>${fmtAvg(vals)}</td>")
                }
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.midtermScore)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #ccfbf1;'>${fmtGrade(marks.midtermFinalGrade)}</td>")
            }

            // Month 3
            if (!isSpecial) {
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m3Daily.firstOrNull())}</td>")
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m3Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #eff6ff;'>${fmtGrade(marks.m3MonthAvg)}</td>")
            } else {
                m3Cols.forEach { col ->
                    rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m3Daily.getOrNull(col.first))}</td>")
                }
                if (m3Cols.size > 1) {
                    val vals = m3Cols.mapNotNull { marks.m3Daily.getOrNull(it.first) }.filter { it > 0f }
                    rowsHtml.append("<td class='cell-data'>${fmtAvg(vals)}</td>")
                }
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m3Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #eff6ff;'>${fmtGrade(marks.m3MonthAvg)}</td>")
            }

            // Month 4
            if (!isSpecial) {
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m4Daily.firstOrNull())}</td>")
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m4Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #f5f3ff;'>${fmtGrade(marks.m4MonthAvg)}</td>")
            } else {
                m4Cols.forEach { col ->
                    rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m4Daily.getOrNull(col.first))}</td>")
                }
                if (m4Cols.size > 1) {
                    val vals = m4Cols.mapNotNull { marks.m4Daily.getOrNull(it.first) }.filter { it > 0f }
                    rowsHtml.append("<td class='cell-data'>${fmtAvg(vals)}</td>")
                }
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m4Written)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #f5f3ff;'>${fmtGrade(marks.m4MonthAvg)}</td>")
            }

            // Term 2
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #fef3c7;'>${fmtGrade(marks.term2Avg)}</td>")

            // Annual Pursuit
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #1e3a8a; background: #ffedd5;'>${fmtGrade(marks.annualAverage)}</td>")

            // Final Exam
            if (!isSpecial) {
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.finalWrittenD1)}</td>")
            } else {
                finalOralCols.forEach { col ->
                    rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.finalOral.getOrNull(col.first))}</td>")
                }
                if (finalOralCols.size > 1) {
                    val vals = finalOralCols.mapNotNull { marks.finalOral.getOrNull(it.first) }.filter { it > 0f }
                    rowsHtml.append("<td class='cell-data'>${fmtAvg(vals)}</td>")
                }
                rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.finalWrittenD1)}</td>")
                rowsHtml.append("<td class='cell-data' style='font-weight: bold; background: #d1fae5;'>${fmtGrade(marks.finalExamTotal)}</td>")
            }

            // Round 2
            rowsHtml.append("<td class='cell-data'>$d2Formatted</td>")

            // Final Score
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #15803d; background: #e0e7ff;'>$scoreStr</td>")
        } else if (tabIndex == 1) {
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m1MonthAvg)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m2MonthAvg)}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold;'>${fmtGrade(marks.term1Avg)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.midtermFinalGrade)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m3MonthAvg)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.m4MonthAvg)}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold;'>${fmtGrade(marks.term2Avg)}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #1e3a8a;'>${fmtGrade(marks.annualAverage)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.finalExamTotal)}</td>")
            rowsHtml.append("<td class='cell-data'>$d2Formatted</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #15803d;'>$scoreStr</td>")
        } else if (tabIndex == 2) {
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.term1Avg)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.midtermFinalGrade)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.term2Avg)}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #1e3a8a;'>${fmtGrade(marks.annualAverage)}</td>")
            rowsHtml.append("<td class='cell-data'>${fmtGrade(marks.finalExamTotal)}</td>")
            rowsHtml.append("<td class='cell-data'>$d2Formatted</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #15803d;'>$scoreStr</td>")
        } else {
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: ${if (totalAbs > 3) "#dc2626" else "#0f172a"};'>$totalAbs</td>")
            rowsHtml.append("<td class='cell-data'>${marks.status}</td>")
        }
        rowsHtml.append("</tr>")
    }

    val dailySubHeaders = java.lang.StringBuilder()
    // Month 1
    if (!isSpecial) {
        dailySubHeaders.append("<th class='cell-header'>يومي</th><th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش1</th>")
    } else {
        m1Cols.forEach { dailySubHeaders.append("<th class='cell-header'>${it.second}</th>") }
        if (m1Cols.size > 1) dailySubHeaders.append("<th class='cell-header'>م.شفهي</th>")
        dailySubHeaders.append("<th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش1</th>")
    }
    // Month 2
    if (!isSpecial) {
        dailySubHeaders.append("<th class='cell-header'>يومي</th><th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش2</th>")
    } else {
        m2Cols.forEach { dailySubHeaders.append("<th class='cell-header'>${it.second}</th>") }
        if (m2Cols.size > 1) dailySubHeaders.append("<th class='cell-header'>م.شفهي</th>")
        dailySubHeaders.append("<th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش2</th>")
    }
    // Midterm
    if (!isSpecial) {
        dailySubHeaders.append("<th class='cell-header'>درجة نصف</th>")
    } else {
        midtermOralCols.forEach { dailySubHeaders.append("<th class='cell-header'>${it.second}</th>") }
        if (midtermOralCols.size > 1) dailySubHeaders.append("<th class='cell-header'>م.شفهي</th>")
        dailySubHeaders.append("<th class='cell-header'>تحريري</th><th class='cell-header'>درجة نصف</th>")
    }
    // Month 3
    if (!isSpecial) {
        dailySubHeaders.append("<th class='cell-header'>يومي</th><th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش3</th>")
    } else {
        m3Cols.forEach { dailySubHeaders.append("<th class='cell-header'>${it.second}</th>") }
        if (m3Cols.size > 1) dailySubHeaders.append("<th class='cell-header'>م.شفهي</th>")
        dailySubHeaders.append("<th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش3</th>")
    }
    // Month 4
    if (!isSpecial) {
        dailySubHeaders.append("<th class='cell-header'>يومي</th><th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش4</th>")
    } else {
        m4Cols.forEach { dailySubHeaders.append("<th class='cell-header'>${it.second}</th>") }
        if (m4Cols.size > 1) dailySubHeaders.append("<th class='cell-header'>م.شفهي</th>")
        dailySubHeaders.append("<th class='cell-header'>تحريري</th><th class='cell-header'>معدل ش4</th>")
    }
    // Final Exam
    if (!isSpecial) {
        dailySubHeaders.append("<th class='cell-header'>تحريري د1</th>")
    } else {
        finalOralCols.forEach { dailySubHeaders.append("<th class='cell-header'>${it.second}</th>") }
        if (finalOralCols.size > 1) dailySubHeaders.append("<th class='cell-header'>م.شفهي</th>")
        dailySubHeaders.append("<th class='cell-header'>تحريري د1</th><th class='cell-header'>مجموع د1</th>")
    }

    val tableHeader = when (tabIndex) {
        0 -> """
            <tr style='background: #e2e8f0; font-weight: 900;'>
                <th rowspan='2' class='cell-header seq-col'>ت</th>
                <th rowspan='2' class='cell-header name-col'>اسم الطالب</th>
                <th colspan='$m1ColSpan' class='cell-header' style='background: #1e3a8a !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>الشهر الأول</th>
                <th colspan='$m2ColSpan' class='cell-header' style='background: #5b21b6 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>الشهر الثاني</th>
                <th rowspan='2' class='cell-header' style='background: #b45309 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>فص1</th>
                <th colspan='$midtermColSpan' class='cell-header' style='background: #0f766e !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>نصف السنة</th>
                <th colspan='$m3ColSpan' class='cell-header' style='background: #1e3a8a !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>الشهر الثالث</th>
                <th colspan='$m4ColSpan' class='cell-header' style='background: #5b21b6 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>الشهر الرابع</th>
                <th rowspan='2' class='cell-header' style='background: #b45309 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>فص2</th>
                <th rowspan='2' class='cell-header' style='background: #c2410c !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>السعي</th>
                <th colspan='$finalColSpan' class='cell-header' style='background: #047857 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>الامتحان النهائي</th>
                <th rowspan='2' class='cell-header' style='background: #334155 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>د2</th>
                <th rowspan='2' class='cell-header' style='background: #312e81 !important; color: #ffffff !important; border: 1.2px solid #000000 !important;'>الدرجة النهائية</th>
            </tr>
            <tr style='background: #f1f5f9; font-weight: 900;'>
                $dailySubHeaders
            </tr>
        """
        1 -> """
            <tr style='background: #e2e8f0; font-weight: 900;'>
                <th class='cell-header seq-col'>ت</th>
                <th class='cell-header name-col'>اسم الطالب</th>
                <th class='cell-header'>معدل ش1</th>
                <th class='cell-header'>معدل ش2</th>
                <th class='cell-header'>درجة فص1</th>
                <th class='cell-header'>درجة نصف</th>
                <th class='cell-header'>معدل ش3</th>
                <th class='cell-header'>معدل ش4</th>
                <th class='cell-header'>درجة فص2</th>
                <th class='cell-header'>السعي السنوي</th>
                <th class='cell-header'>امتحان نهائي د1</th>
                <th class='cell-header'>امتحان نهائي د2</th>
                <th class='cell-header'>الدرجة النهائية</th>
            </tr>
        """
        2 -> """
            <tr style='background: #e2e8f0; font-weight: 900;'>
                <th class='cell-header seq-col'>ت</th>
                <th class='cell-header name-col'>اسم الطالب</th>
                <th class='cell-header'>معدل الفصل 1</th>
                <th class='cell-header'>درجة نصف السنة</th>
                <th class='cell-header'>معدل الفصل 2</th>
                <th class='cell-header'>السعي السنوي</th>
                <th class='cell-header'>امتحان نهائي د1</th>
                <th class='cell-header'>امتحان نهائي د2</th>
                <th class='cell-header'>الدرجة النهائية</th>
            </tr>
        """
        else -> """
            <tr style='background: #e2e8f0; font-weight: 900;'>
                <th class='cell-header seq-col'>ت</th>
                <th class='cell-header name-col'>اسم الطالب</th>
                <th class='cell-header'>مجموع الغيابات</th>
                <th class='cell-header'>حالة الطالب</th>
            </tr>
        """
    }

    val cellPad = if (tabIndex == 0) "5px 2px" else "6px 3px"
    val headerPad = if (tabIndex == 0) "6px 2px" else "7px 3px"
    val cellFontSize = if (tabIndex == 0) "12px" else "13px"
    val headerFontSize = if (tabIndex == 0) "11px" else "12px"
    val nameFontSize = if (tabIndex == 0) "15.5px" else "16.5px"

    val progHtmlStats = calculateProgressiveStats(students)
    val totalSt = progHtmlStats.totalStudents
    val passedSt = progHtmlStats.passedStudents
    val failedSt = progHtmlStats.failedStudents
    val passRateVal = progHtmlStats.passRate
    val stageLabel = progHtmlStats.stageName

    return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Amiri:wght@400;700&family=Tajawal:wght@400;700;900&display=swap');
                @page {
                    size: ${if (tabIndex == 0) "A4 landscape" else "A4 portrait"};
                    margin: 0;
                }
                * {
                    box-sizing: border-box;
                }
                html, body {
                    font-family: 'Tajawal', 'Amiri', 'Segoe UI', Tahoma, sans-serif;
                    margin: 0;
                    padding: 0;
                    background: #ffffff;
                    color: #0f172a;
                    direction: rtl;
                    width: 100%;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }

                /* Front Cover Page - 3D Geometric Layout - Strictly Standalone Page */
                .cover-page {
                    page-break-before: avoid !important;
                    break-before: avoid !important;
                    page-break-after: always !important;
                    break-after: page !important;
                    page-break-inside: avoid !important;
                    break-inside: avoid !important;
                    width: 100vw !important;
                    height: 100vh !important;
                    min-height: 100vh !important;
                    max-height: 100vh !important;
                    box-sizing: border-box;
                    padding: ${if (tabIndex == 0) "10px 18px" else "18px 22px"} !important;
                    margin: 0 !important;
                    overflow: hidden;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                }
                .outer-frame {
                    width: 98% !important;
                    max-width: ${if (tabIndex == 0) "1040px" else "750px"} !important;
                    height: 96% !important;
                    max-height: ${if (tabIndex == 0) "730px" else "1050px"} !important;
                    border-radius: 18px;
                    padding: 6px;
                    box-sizing: border-box;
                    background: linear-gradient(145deg, #ffffff, #f1f5f9);
                    box-shadow: 
                        0 20px 40px -10px rgba(15, 23, 42, 0.25),
                        0 0 0 1px rgba(255, 255, 255, 0.9) inset,
                        0 0 0 4px #1e3a8a,
                        0 0 0 7px #b45309,
                        0 0 0 9px #1e3a8a;
                    display: flex;
                    flex-direction: column;
                    margin: auto !important;
                    position: relative;
                }
                .inner-frame {
                    width: 100%;
                    height: 100%;
                    border: 2px solid #b45309;
                    border-radius: 12px;
                    padding: ${if (tabIndex == 0) "8px 24px" else "18px 24px"};
                    box-sizing: border-box;
                    background: radial-gradient(circle at 50% 50%, #ffffff 0%, #fafaf9 70%, #f5f5f4 100%);
                    box-shadow: 
                        inset 0 3px 8px rgba(0, 0, 0, 0.08),
                        inset 0 0 0 1px rgba(180, 83, 9, 0.2);
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    align-items: center;
                    text-align: center;
                    position: relative;
                }
                .corner {
                    position: absolute;
                    font-size: ${if (tabIndex == 0) "18px" else "22px"};
                    color: #b45309;
                    line-height: 1;
                    font-weight: 900;
                    text-shadow: 1px 2px 3px rgba(0, 0, 0, 0.25), 0 0 1px #d97706;
                }
                .c-tr { top: 8px; right: 12px; }
                .c-tl { top: 8px; left: 12px; }
                .c-br { bottom: 8px; right: 12px; }
                .c-bl { bottom: 8px; left: 12px; }

                .header-box { width: 100%; }
                .country-title {
                    font-size: ${if (tabIndex == 0) "22px" else "26px"};
                    font-weight: 900;
                    color: #1e3a8a;
                    margin: 0;
                    text-shadow: 0 1px 2px rgba(30, 58, 138, 0.25);
                    letter-spacing: 0.5px;
                }
                .ministry-title {
                    font-size: ${if (tabIndex == 0) "13px" else "15px"};
                    font-weight: bold;
                    color: #475569;
                    margin: 2px 0 0 0;
                }
                .school-title {
                    font-size: ${if (tabIndex == 0) "17px" else "21px"};
                    font-weight: 900;
                    color: #0f172a;
                    margin: 2px 0 0 0;
                    min-height: 20px;
                    text-shadow: 0 1px 1px rgba(0, 0, 0, 0.15);
                }

                .ornate-line {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    width: 70%;
                    margin: ${if (tabIndex == 0) "2px auto" else "6px auto"};
                }
                .line-segment {
                    flex: 1;
                    height: 2px;
                    background: linear-gradient(to right, transparent, #b45309, #d97706, #b45309, transparent);
                    box-shadow: 0 1px 2px rgba(0,0,0,0.15);
                }
                .line-emblem {
                    padding: 0 10px;
                    color: #b45309;
                    font-size: 13px;
                    font-weight: 900;
                    text-shadow: 1px 1px 2px rgba(0,0,0,0.2);
                }

                .main-title-box {
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    margin: ${if (tabIndex == 0) "2px 0" else "6px 0"};
                }
                .title-banner {
                    background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 50%, #1e3a8a 100%);
                    color: #ffffff !important;
                    border-radius: 12px;
                    padding: ${if (tabIndex == 0) "4px 32px" else "8px 44px"};
                    border: 2px solid #fbbf24;
                    box-shadow: 
                        0 8px 18px -2px rgba(30, 58, 138, 0.4),
                        0 3px 6px -1px rgba(0, 0, 0, 0.15),
                        inset 0 1px 2px rgba(255, 255, 255, 0.5),
                        inset 0 -2px 4px rgba(0, 0, 0, 0.3);
                    display: inline-block;
                }
                .register-title {
                    font-size: ${if (tabIndex == 0) "24px" else "32px"};
                    font-weight: 900;
                    color: #ffffff !important;
                    margin: 0;
                    font-family: 'Amiri', 'Tajawal', serif;
                    text-shadow: 0 2px 5px rgba(0, 0, 0, 0.6);
                    letter-spacing: 0.5px;
                }
                .academic-year {
                    font-size: ${if (tabIndex == 0) "12.5px" else "14.5px"};
                    font-weight: bold;
                    color: #92400e;
                    margin: 4px 0 0 0;
                    text-shadow: 0 1px 1px rgba(255,255,255,0.8);
                }

                .info-cards-grid {
                    display: grid;
                    grid-template-columns: repeat(3, 1fr);
                    gap: ${if (tabIndex == 0) "8px" else "14px"};
                    width: 95%;
                    margin: ${if (tabIndex == 0) "4px auto" else "10px auto"};
                }
                .info-card {
                    background: linear-gradient(145deg, #ffffff, #f8fafc);
                    border: 1.5px solid #cbd5e1;
                    border-top: 4px solid #1e3a8a;
                    border-radius: 12px;
                    padding: ${if (tabIndex == 0) "5px 10px" else "10px 14px"};
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    box-shadow: 
                        0 8px 16px -2px rgba(0, 0, 0, 0.1),
                        0 3px 6px -2px rgba(0, 0, 0, 0.05),
                        inset 0 1px 0 rgba(255, 255, 255, 1);
                }
                .card-label {
                    font-size: ${if (tabIndex == 0) "11px" else "13px"};
                    font-weight: bold;
                    color: #64748b;
                    margin-bottom: 2px;
                }
                .card-val {
                    font-size: ${if (tabIndex == 0) "15px" else "18px"};
                    font-weight: 900;
                    color: #0f172a;
                    text-shadow: 0 1px 1px rgba(0, 0, 0, 0.08);
                }
                .val-subject { color: #b45309; }
                .val-teacher { color: #1e3a8a; }

                .cover-bottom {
                    display: flex;
                    justify-content: space-around;
                    align-items: center;
                    width: 92%;
                    margin-top: ${if (tabIndex == 0) "4px" else "10px"};
                    margin-bottom: ${if (tabIndex == 0) "2px" else "6px"};
                }
                .sig-card {
                    background: linear-gradient(145deg, #ffffff, #f8fafc);
                    border: 1.5px solid #e2e8f0;
                    border-radius: 12px;
                    box-shadow: 
                        0 6px 12px -2px rgba(0, 0, 0, 0.08),
                        inset 0 1px 0 #ffffff;
                    padding: ${if (tabIndex == 0) "5px 16px" else "8px 22px"};
                    width: 42%;
                    text-align: center;
                }
                .sig-title {
                    font-size: ${if (tabIndex == 0) "12px" else "14px"};
                    font-weight: bold;
                    color: #475569;
                }
                .sig-name {
                    font-size: ${if (tabIndex == 0) "14.5px" else "17px"};
                    font-weight: 900;
                    color: #0f172a;
                    margin-top: 2px;
                    min-height: 20px;
                }
                .sig-dots {
                    color: #94a3b8;
                    font-size: ${if (tabIndex == 0) "11px" else "12.5px"};
                    margin-top: 2px;
                }

                /* Official Table Page - Always Starts On Next Page */
                .table-page {
                    page-break-before: always !important;
                    break-before: page !important;
                    width: 100%;
                    box-sizing: border-box;
                    padding: ${if (tabIndex == 0) "8px 12px" else "12px 14px"};
                }
                .official-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 2px solid #1e3a8a;
                    padding-bottom: 3px;
                    margin-bottom: 3px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    text-align: center;
                    table-layout: auto;
                    margin-top: 2px;
                    page-break-inside: auto;
                    border: 1.5px solid #000000 !important;
                }
                tr {
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                th, td {
                    border: 1.2px solid #000000 !important;
                    box-sizing: border-box;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }
                .seq-col {
                    width: 28px;
                    font-weight: 900 !important;
                    font-size: ${if (tabIndex == 0) "12px" else "13px"};
                    padding: 3px 2px;
                    color: #000000 !important;
                }
                .name-col {
                    text-align: right;
                    font-weight: 900 !important;
                    padding-right: 8px !important;
                    padding-left: 6px !important;
                    font-size: $nameFontSize !important;
                    white-space: nowrap;
                    color: #000000 !important;
                    min-width: ${if (tabIndex == 0) "150px" else "165px"};
                    letter-spacing: -0.2px;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }
                .cell-data {
                    font-size: $cellFontSize !important;
                    padding: $cellPad !important;
                    color: #000000 !important;
                    font-weight: 900 !important;
                    border: 1.2px solid #000000 !important;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }
                .cell-header {
                    background-color: #e2e8f0 !important;
                    font-weight: 900 !important;
                    font-size: $headerFontSize !important;
                    padding: $headerPad !important;
                    color: #000000 !important;
                    border: 1.2px solid #000000 !important;
                    -webkit-print-color-adjust: exact !important;
                    print-color-adjust: exact !important;
                }
                .data-row:nth-child(even) {
                    background-color: #f1f5f9;
                }
                .data-row:nth-child(odd) {
                    background-color: #ffffff;
                }
                .stats-bar {
                    display: flex;
                    justify-content: space-around;
                    background-color: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    padding: 3px 8px;
                    margin-top: 6px;
                    margin-bottom: 4px;
                    font-size: 9.5px;
                    font-weight: bold;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
                .signatures {
                    margin-top: 8px;
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    text-align: center;
                    font-weight: bold;
                    font-size: 10px;
                    page-break-inside: avoid;
                    break-inside: avoid;
                }
            </style>
        </head>
        <body>
            ${if (includeCover) """
            <div class="cover-page">
                <div class="outer-frame">
                    <div class="inner-frame">
                        <div class="corner c-tr">❖</div>
                        <div class="corner c-tl">❖</div>
                        <div class="corner c-br">❖</div>
                        <div class="corner c-bl">❖</div>

                        <div class="header-box">
                            <h1 class="country-title">جمهورية العراق</h1>
                            <h2 class="ministry-title">وزارة التربية • ${directorate}</h2>
                            ${if (schoolName.isNotBlank()) """<h3 class="school-title">${schoolName}</h3>""" else """<h3 class="school-title" style="visibility: hidden;">&nbsp;</h3>"""}
                        </div>

                        <div class="ornate-line">
                            <span class="line-segment"></span>
                            <span class="line-emblem">✦ ❖ ✦</span>
                            <span class="line-segment"></span>
                        </div>

                        <div class="main-title-box">
                            <div class="title-banner">
                                <h1 class="register-title">${tabName}</h1>
                            </div>
                            <div class="academic-year">للعام الدراسي ( 2025 – 2026 م )</div>
                        </div>

                        <div class="info-cards-grid">
                            <div class="info-card">
                                <span class="card-label">المادة الدراسية</span>
                                <span class="card-val val-subject">${subject}</span>
                            </div>
                            <div class="info-card">
                                <span class="card-label">الصف والشعبة</span>
                                <span class="card-val">${grade} - (${section})</span>
                            </div>
                            <div class="info-card">
                                <span class="card-label">مدرس المادة المحترم</span>
                                <span class="card-val val-teacher">${if (teacherName.isNotBlank()) teacherName else "..................."}</span>
                            </div>
                        </div>

                        <div class="cover-bottom">
                            <div class="sig-card">
                                <div class="sig-title">أستاذ المادة</div>
                                <div class="sig-name">${if (teacherName.isNotBlank()) teacherName else "&nbsp;"}</div>
                                <div class="sig-dots">التوقيع: ...................</div>
                            </div>

                            <div class="sig-card">
                                <div class="sig-title">إدارة المدرسة</div>
                                <div class="sig-name">${if (cleanManagerName.isNotBlank()) cleanManagerName else "&nbsp;"}</div>
                                <div class="sig-dots">التوقيع: ...................</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """ else ""}

            ${if (!onlyCover) """
            <!-- Grades Table Page -->
            <div class="table-page">
                <div class="official-header">
                    <div>
                        ${if (schoolName.isNotBlank()) """<span style="font-weight: 900; font-size: 12px; color: #1e3a8a;">${schoolName}</span><br>""" else ""}
                        <span style="font-size: 9.5px; color: #64748b;">${directorate}</span>
                    </div>
                    <div style="text-align: center;">
                        <span style="font-weight: 900; font-size: 14px; color: #1e3a8a;">${tabName}</span><br>
                        <span style="font-size: 10px; font-weight: bold; color: #475569;">المادة: ${subject} | الصف: ${grade} (${section})${if (teacherName.isNotBlank()) " | مدرس المادة: $teacherName" else ""}</span>
                    </div>
                    <div style="text-align: left; font-size: 9.5px; color: #64748b;">
                        <span>العام الدراسي: 2025-2026</span><br>
                        <span>التاريخ: ${java.time.LocalDate.now()}</span>
                    </div>
                </div>

                <table>
                    <thead>
                        $tableHeader
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>

                <!-- Statistics Summary Bar -->
                <div class="stats-bar">
                    ${if (progHtmlStats.stageName == "لم تُدخل درجات" || totalSt == 0) """
                        <div style="text-align: center; width: 100%; color: #64748b; font-size: 10px; font-weight: bold;">
                            عدد الطلاب الكلي: $totalSt طالب &bull; لم يتم رصد أي درجات في هذا السجل بعد
                        </div>
                    """ else """
                        <div>
                            <span style="color: #64748b;">عدد الطلاب:</span>
                            <span style="color: #0f172a;">$totalSt</span>
                        </div>
                        <div>
                            <span style="color: #15803d;">الناجحين:</span>
                            <span style="color: #16a34a;">$passedSt</span>
                        </div>
                        <div>
                            <span style="color: #b91c1c;">المكملين / الراسبين:</span>
                            <span style="color: #dc2626;">$failedSt</span>
                        </div>
                        <div>
                            <span style="color: #1d4ed8;">نسبة النجاح:</span>
                            <span style="color: #2563eb;">$passRateVal%</span>
                        </div>
                        <div>
                            <span style="color: #b45309;">المرحلة الحالية:</span>
                            <span style="color: #92400e;">$stageLabel</span>
                        </div>
                    """}
                </div>

                <!-- Signatures -->
                <div class="signatures">
                    <div>
                        <span>مدرس المادة${if (teacherName.isNotBlank()) ": $teacherName" else ""}</span><br><br>
                        <span>التوقيع: .......................................</span>
                    </div>
                    <div>
                        <span>إدارة المدرسة${if (cleanManagerName.isNotBlank()) ": $cleanManagerName" else ""}</span><br><br>
                        <span>الختم والتوقيع الرسمي: .......................................</span>
                    </div>
                </div>
            </div>
            """ else ""}
        </body>
        </html>
    """.trimIndent()
}


@Composable
fun AdminRegisterTable(
    students: List<Student>,
    listState: LazyListState,
    onUpdate: (Pair<Student, StudentMarks>) -> Unit,
    onEditStudentName: (Student) -> Unit,
    isEditable: Boolean = false,
    isSpecial: Boolean = false
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val hScroll = rememberScrollState()
    val cellW = 58.dp
    val nameW = 135.dp
    val seqW = 28.dp
    val rowH = 40.dp
    val fontSize = 10.sp
    val nameFontSize = 13.sp
    val labelFontSize = 8.5.sp

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table 2-Tier Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(currentTheme.tableHeaderBg)
        ) {
            // Fixed Columns (ثابت: التسلسل ثم اسم الطالب)
            PaperHeaderCell("ت", seqW, rowH * 2, fontSize)
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, 11.5.sp)

            // Scrollable Columns (متحركة بالسحب: أعمدة الدرجات)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                Column {
                    // Row 1: Groups
                    Row {
                        PaperHeaderCell("الفصل الأول ونصف السنة", cellW * 2, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("الفصل الثاني والسعي", cellW * 2, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF5B21B6))
                        PaperHeaderCell("الامتحان النهائي والنتيجة", cellW * 3, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF047857))
                    }
                    // Row 2: Sub-headers
                    Row {
                        PaperHeaderCell("معدل ف1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("درجة نصف", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))

                        PaperHeaderCell("معدل ف2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("السعي السنوي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))

                        PaperHeaderCell("نهائي د1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF065F46))
                        PaperHeaderCell("نهائي د2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF065F46))
                        PaperHeaderCell("الدرجة النهائية", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF065F46))
                    }
                }
            }
        }

        // Table Rows
        LazyColumn(
            state = listState, 
            modifier = Modifier.weight(1f).imePadding(),
            contentPadding = PaddingValues(bottom = 350.dp)
        ) {
            itemsIndexed(students, key = { _, it -> it.id }, contentType = { _, _ -> "admin_row" }) { index, std ->
                val m = std.marks
                val isDuplicate = std.fullName in duplicateNames
                val d2Val = m.finalWrittenD2
                val d2Formatted = if (d2Val == null || d2Val == 0f) "-" else if (isSpecial) (m.finalOral.sum() + d2Val).toInt().toString() else d2Val.toInt().toString()
                val rowBg = if (index % 2 == 0) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                ) {
                    // Fixed Columns (التسلسل ثم اسم الطالب ثابتان على اليمين)
                    PaperTableCell("${index + 1}", seqW, rowH, fontSize, isBold = true, backgroundColor = rowBg)
                    PaperTableCell(
                        text = std.fullName, 
                        width = nameW, 
                        height = rowH, 
                        fontSize = nameFontSize, 
                        textColor = if (isDuplicate) Color(0xFFDC2626) else null, 
                        backgroundColor = rowBg, 
                        textAlign = TextAlign.Start,
                        onLongClick = if (isEditable) { { onEditStudentName(std) } } else null
                    )

                    // Scrollable Grade Cells
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(hScroll)
                    ) {
                        PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = rowBg)
                        PaperTableCell(m.finalExamTotal.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(d2Formatted, cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.latestRecordedScoreInt().toString(), cellW, rowH, fontSize, isBold = true, textColor = if (m.latestRecordedScore() < 50f) Color(0xFFDC2626) else null, backgroundColor = rowBg)
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherRegisterTable(
    students: List<Student>,
    listState: LazyListState,
    onUpdate: (Pair<Student, StudentMarks>) -> Unit,
    onEditStudentName: (Student) -> Unit,
    isEditable: Boolean = false,
    isSpecial: Boolean = false,
    gradeLocks: Map<String, Boolean> = emptyMap()
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val hScroll = rememberScrollState()
    val cellW = 52.dp
    val nameW = 135.dp
    val seqW = 28.dp
    val rowH = 40.dp
    val fontSize = 9.5.sp
    val nameFontSize = 13.sp
    val labelFontSize = 8.5.sp

    val isM1Locked = !isEditable || (gradeLocks["m1"] == true)
    val isM2Locked = !isEditable || (gradeLocks["m2"] == true)
    val isMidtermLocked = !isEditable || (gradeLocks["midterm"] == true)
    val isM3Locked = !isEditable || (gradeLocks["m3"] == true)
    val isM4Locked = !isEditable || (gradeLocks["m4"] == true)
    val isFinalLocked = !isEditable || (gradeLocks["final"] == true)

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table 2-Tier Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(currentTheme.tableHeaderBg)
        ) {
            // Fixed Columns (ثابت: التسلسل ثم اسم الطالب)
            PaperHeaderCell("ت", seqW, rowH * 2, fontSize)
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, 11.5.sp)

            // Scrollable Columns (متحركة بالسحب: أعمدة الدرجات)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                Column {
                    // Row 1: Groups
                    Row {
                        PaperHeaderCell("الفصل الأول (كورس 1)", cellW * 4, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("الفصل الثاني (كورس 2)", cellW * 4, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF5B21B6))
                        PaperHeaderCell("الامتحان النهائي والنتيجة", cellW * 3, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF047857))
                    }
                    // Row 2: Sub-headers
                    Row {
                        PaperHeaderCell(if (isM1Locked) "معدل ش1 🔒" else "معدل ش1", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM1Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                        PaperHeaderCell(if (isM2Locked) "معدل ش2 🔒" else "معدل ش2", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM2Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                        PaperHeaderCell("درجة فص1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell(if (isMidtermLocked) "درجة نصف 🔒" else "درجة نصف", cellW, rowH, labelFontSize, backgroundColor = if (isMidtermLocked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isMidtermLocked) Color(0xFF64748B) else Color(0xFF1E3A8A))

                        PaperHeaderCell(if (isM3Locked) "معدل ش3 🔒" else "معدل ش3", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM3Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                        PaperHeaderCell(if (isM4Locked) "معدل ش4 🔒" else "معدل ش4", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM4Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                        PaperHeaderCell("درجة فص2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("السعي السنوي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))

                        PaperHeaderCell(if (isFinalLocked) "نهائي د1 🔒" else "نهائي د1", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF065F46))
                        PaperHeaderCell(if (isFinalLocked) "د2 🔒" else "د2", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF065F46))
                        PaperHeaderCell(if (isFinalLocked) "النهائية 🔒" else "النهائية", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF065F46))
                    }
                }
            }
        }

        // Table Rows
        LazyColumn(
            state = listState, 
            modifier = Modifier.weight(1f).imePadding(),
            contentPadding = PaddingValues(bottom = 350.dp)
        ) {
            itemsIndexed(students, key = { _, it -> it.id }, contentType = { _, _ -> "teacher_row" }) { index, std ->
                val m = std.marks
                val isDuplicate = std.fullName in duplicateNames
                val d2Val = m.finalWrittenD2
                val d2Formatted = if (d2Val == null || d2Val == 0f) "-" else if (isSpecial) (m.finalOral.sum() + d2Val).toInt().toString() else d2Val.toInt().toString()
                val rowBg = if (index % 2 == 0) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                ) {
                    // Fixed Columns (التسلسل ثم اسم الطالب ثابتان على اليمين)
                    PaperTableCell("${index + 1}", seqW, rowH, fontSize, isBold = true, backgroundColor = rowBg)
                    PaperTableCell(
                        text = std.fullName, 
                        width = nameW, 
                        height = rowH, 
                        fontSize = nameFontSize, 
                        textColor = if (isDuplicate) Color(0xFFDC2626) else null, 
                        backgroundColor = rowBg, 
                        textAlign = TextAlign.Start,
                        onLongClick = if (isEditable) { { onEditStudentName(std) } } else null
                    )

                    // Scrollable Columns
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(hScroll)
                    ) {
                        PaperTableCell(m.m1MonthAvg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.m2MonthAvg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.m3MonthAvg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.m4MonthAvg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = rowBg)
                        PaperTableCell(m.finalExamTotal.toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(d2Formatted, cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperTableCell(m.latestRecordedScoreInt().toString(), cellW, rowH, fontSize, isBold = true, textColor = if (m.latestRecordedScore() < 50f) Color(0xFFDC2626) else null, backgroundColor = rowBg)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyRegisterTable(
    students: List<Student>,
    settings: DailyColumnSetting,
    listState: LazyListState,
    onUpdateMarks: (Student, StudentMarks) -> Unit,
    onEditHeader: (Int) -> Unit,
    onEditStudentName: (Student) -> Unit,
    isEditable: Boolean = true,
    isSpecial: Boolean = false,
    gradeLocks: Map<String, Boolean> = emptyMap()
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val hScroll = rememberScrollState()
    val cellW = 44.dp
    val nameW = 135.dp
    val seqW = 28.dp
    val rowH = 40.dp
    val fontSize = 9.5.sp
    val nameFontSize = 13.sp
    val labelFontSize = 8.sp

    val isM1Locked = !isEditable || (gradeLocks["m1"] == true)
    val isM2Locked = !isEditable || (gradeLocks["m2"] == true)
    val isMidtermLocked = !isEditable || (gradeLocks["midterm"] == true)
    val isM3Locked = !isEditable || (gradeLocks["m3"] == true)
    val isM4Locked = !isEditable || (gradeLocks["m4"] == true)
    val isFinalLocked = !isEditable || (gradeLocks["final"] == true)

    val initialM1Count = remember(settings, students, isSpecial) {
        if (!isSpecial) 1 else {
            val maxNamed = (0..4).indexOfLast { settings.columnNames.getOrElse(it) { "" }.isNotBlank() }
            val maxWithMark = (0..4).indexOfLast { i -> students.any { it.marks.m1Daily.getOrElse(i) { 0f } > 0f } }
            maxOf(1, maxOf(maxNamed, maxWithMark) + 1)
        }
    }
    var m1Count by remember(initialM1Count) { mutableIntStateOf(initialM1Count) }

    val initialM2Count = remember(settings, students, isSpecial) {
        if (!isSpecial) 1 else {
            val maxNamed = (0..4).indexOfLast { settings.columnNames.getOrElse(5 + it) { "" }.isNotBlank() }
            val maxWithMark = (0..4).indexOfLast { i -> students.any { it.marks.m2Daily.getOrElse(i) { 0f } > 0f } }
            maxOf(1, maxOf(maxNamed, maxWithMark) + 1)
        }
    }
    var m2Count by remember(initialM2Count) { mutableIntStateOf(initialM2Count) }

    val initialM3Count = remember(settings, students, isSpecial) {
        if (!isSpecial) 1 else {
            val maxNamed = (0..4).indexOfLast { settings.columnNames.getOrElse(10 + it) { "" }.isNotBlank() }
            val maxWithMark = (0..4).indexOfLast { i -> students.any { it.marks.m3Daily.getOrElse(i) { 0f } > 0f } }
            maxOf(1, maxOf(maxNamed, maxWithMark) + 1)
        }
    }
    var m3Count by remember(initialM3Count) { mutableIntStateOf(initialM3Count) }

    val initialM4Count = remember(settings, students, isSpecial) {
        if (!isSpecial) 1 else {
            val maxNamed = (0..4).indexOfLast { settings.columnNames.getOrElse(15 + it) { "" }.isNotBlank() }
            val maxWithMark = (0..4).indexOfLast { i -> students.any { it.marks.m4Daily.getOrElse(i) { 0f } > 0f } }
            maxOf(1, maxOf(maxNamed, maxWithMark) + 1)
        }
    }
    var m4Count by remember(initialM4Count) { mutableIntStateOf(initialM4Count) }

    val initialMidtermOralCount = remember(students, isSpecial) {
        if (!isSpecial) 1 else {
            val maxWithMark = (0..4).indexOfLast { i -> students.any { it.marks.midtermOral.getOrElse(i) { 0f } > 0f } }
            maxOf(1, maxWithMark + 1)
        }
    }
    var midtermOralCount by remember(initialMidtermOralCount) { mutableIntStateOf(initialMidtermOralCount) }

    val initialFinalOralCount = remember(students, isSpecial) {
        if (!isSpecial) 1 else {
            val maxWithMark = (0..4).indexOfLast { i -> students.any { it.marks.finalOral.getOrElse(i) { 0f } > 0f } }
            maxOf(1, maxWithMark + 1)
        }
    }
    var finalOralCount by remember(initialFinalOralCount) { mutableIntStateOf(initialFinalOralCount) }

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table 2-Tier Grouped Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(currentTheme.tableHeaderBg)
        ) {
            // Fixed Columns (ثابت: التسلسل ثم اسم الطالب)
            PaperHeaderCell("ت", seqW, rowH * 2, fontSize)
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, 11.5.sp)

            // Scrollable Columns (متحركة بالسحب: أعمدة الدرجات)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                Column {
                    // Row 1: Top Group Headers with Distinct Themes
                    Row {
                        // 1. الشهر الأول (Blue Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = if (isM1Locked) "الشهر الأول 🔒" else "الشهر الأول", 
                                width = cellW * (m1Count + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = if (isM1Locked) Color(0xFF475569) else Color(0xFF1E3A8A),
                                onAdd = { if (!isM1Locked && m1Count < 5) m1Count++ }, 
                                onRemove = { if (!isM1Locked && m1Count > 1) m1Count-- },
                                canAdd = !isM1Locked && m1Count < 5,
                                canRemove = !isM1Locked && m1Count > 1
                            )
                        } else {
                            PaperHeaderCell(
                                text = if (isM1Locked) "الشهر الأول 🔒" else "الشهر الأول", 
                                width = cellW * 3, 
                                height = rowH, 
                                fontSize = fontSize, 
                                textColor = Color.White, 
                                backgroundColor = if (isM1Locked) Color(0xFF475569) else Color(0xFF1E3A8A)
                            )
                        }

                        // 2. الشهر الثاني (Purple Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = if (isM2Locked) "الشهر الثاني 🔒" else "الشهر الثاني", 
                                width = cellW * (m2Count + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = if (isM2Locked) Color(0xFF475569) else Color(0xFF5B21B6),
                                onAdd = { if (!isM2Locked && m2Count < 5) m2Count++ }, 
                                onRemove = { if (!isM2Locked && m2Count > 1) m2Count-- },
                                canAdd = !isM2Locked && m2Count < 5,
                                canRemove = !isM2Locked && m2Count > 1
                            )
                        } else {
                            PaperHeaderCell(
                                text = if (isM2Locked) "الشهر الثاني 🔒" else "الشهر الثاني", 
                                width = cellW * 3, 
                                height = rowH, 
                                fontSize = fontSize, 
                                textColor = Color.White, 
                                backgroundColor = if (isM2Locked) Color(0xFF475569) else Color(0xFF5B21B6)
                            )
                        }

                        // 3. الفصل الأول (Amber Group)
                        PaperHeaderCell("فص1", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFFB45309))
                        
                        // 4. نصف السنة (Teal Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = if (isMidtermLocked) "نصف السنة 🔒" else "نصف السنة", 
                                width = cellW * (midtermOralCount + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = if (isMidtermLocked) Color(0xFF475569) else Color(0xFF0F766E),
                                onAdd = { if (!isMidtermLocked && midtermOralCount < 5) midtermOralCount++ }, 
                                onRemove = { if (!isMidtermLocked && midtermOralCount > 1) midtermOralCount-- },
                                canAdd = !isMidtermLocked && midtermOralCount < 5,
                                canRemove = !isMidtermLocked && midtermOralCount > 1
                            )
                        } else {
                            PaperHeaderCell(
                                text = if (isMidtermLocked) "نصف السنة 🔒" else "نصف السنة", 
                                width = cellW, 
                                height = rowH, 
                                fontSize = fontSize, 
                                textColor = Color.White, 
                                backgroundColor = if (isMidtermLocked) Color(0xFF475569) else Color(0xFF0F766E)
                            )
                        }
                        
                        // 5. الشهر الثالث (Blue Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = if (isM3Locked) "الشهر الثالث 🔒" else "الشهر الثالث", 
                                width = cellW * (m3Count + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = if (isM3Locked) Color(0xFF475569) else Color(0xFF1E3A8A),
                                onAdd = { if (!isM3Locked && m3Count < 5) m3Count++ }, 
                                onRemove = { if (!isM3Locked && m3Count > 1) m3Count-- },
                                canAdd = !isM3Locked && m3Count < 5,
                                canRemove = !isM3Locked && m3Count > 1
                            )
                        } else {
                            PaperHeaderCell(
                                text = if (isM3Locked) "الشهر الثالث 🔒" else "الشهر الثالث", 
                                width = cellW * 3, 
                                height = rowH, 
                                fontSize = fontSize, 
                                textColor = Color.White, 
                                backgroundColor = if (isM3Locked) Color(0xFF475569) else Color(0xFF1E3A8A)
                            )
                        }

                        // 6. الشهر الرابع (Purple Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = if (isM4Locked) "الشهر الرابع 🔒" else "الشهر الرابع", 
                                width = cellW * (m4Count + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = if (isM4Locked) Color(0xFF475569) else Color(0xFF5B21B6),
                                onAdd = { if (!isM4Locked && m4Count < 5) m4Count++ }, 
                                onRemove = { if (!isM4Locked && m4Count > 1) m4Count-- },
                                canAdd = !isM4Locked && m4Count < 5,
                                canRemove = !isM4Locked && m4Count > 1
                            )
                        } else {
                            PaperHeaderCell(
                                text = if (isM4Locked) "الشهر الرابع 🔒" else "الشهر الرابع", 
                                width = cellW * 3, 
                                height = rowH, 
                                fontSize = fontSize, 
                                textColor = Color.White, 
                                backgroundColor = if (isM4Locked) Color(0xFF475569) else Color(0xFF5B21B6)
                            )
                        }

                        // 7. الفصل الثاني والسعي (Amber & Orange Groups)
                        PaperHeaderCell("فص2", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFFB45309))
                        PaperHeaderCell("السعي", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFFC2410C))
                        
                        // 8. الامتحان النهائي (Emerald Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = if (isFinalLocked) "الامتحان النهائي 🔒" else "الامتحان النهائي", 
                                width = cellW * (finalOralCount + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = if (isFinalLocked) Color(0xFF475569) else Color(0xFF047857),
                                onAdd = { if (!isFinalLocked && finalOralCount < 5) finalOralCount++ }, 
                                onRemove = { if (!isFinalLocked && finalOralCount > 1) finalOralCount-- },
                                canAdd = !isFinalLocked && finalOralCount < 5,
                                canRemove = !isFinalLocked && finalOralCount > 1
                            )
                        } else {
                            PaperHeaderCell(
                                text = if (isFinalLocked) "الامتحان النهائي 🔒" else "الامتحان النهائي", 
                                width = cellW, 
                                height = rowH, 
                                fontSize = fontSize, 
                                textColor = Color.White, 
                                backgroundColor = if (isFinalLocked) Color(0xFF475569) else Color(0xFF047857)
                            )
                        }
                        
                        // 9. د2 والدرجة النهائية
                        PaperHeaderCell(if (isFinalLocked) "د2 🔒" else "د2", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF475569))
                        PaperHeaderCell("الدرجة النهائية", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF4338CA))
                    }
                    
                    // Row 2: Sub-headers Under Each Month / Section
                    Row {
                        // Month 1 Subheaders
                        if (isSpecial) {
                            repeat(m1Count) { i ->
                                val defaultName = if (m1Count == 1) "شفهي" else "شفهي ${i+1}"
                                val colName = settings.columnNames.getOrElse(i) { "" }.ifBlank { defaultName }
                                EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM1Locked) Color(0xFF64748B) else null) { 
                                    if (!isM1Locked) onEditHeader(i) 
                                }
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM1Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM1Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("معدل ش1", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFCBD5E1) else Color(0xFFBFDBFE), textColor = if (isM1Locked) Color(0xFF475569) else Color(0xFF1E3A8A))
                        } else {
                            PaperHeaderCell("يومي", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM1Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM1Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("معدل ش1", cellW, rowH, labelFontSize, backgroundColor = if (isM1Locked) Color(0xFFCBD5E1) else Color(0xFFBFDBFE), textColor = if (isM1Locked) Color(0xFF475569) else Color(0xFF1E3A8A))
                        }
                        
                        // Month 2 Subheaders
                        if (isSpecial) {
                            repeat(m2Count) { i ->
                                val defaultName = if (m2Count == 1) "شفهي" else "شفهي ${i+1}"
                                val colName = settings.columnNames.getOrElse(5 + i) { "" }.ifBlank { defaultName }
                                EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM2Locked) Color(0xFF64748B) else null) { 
                                    if (!isM2Locked) onEditHeader(5 + i) 
                                }
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM2Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM2Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("معدل ش2", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFCBD5E1) else Color(0xFFDDD6FE), textColor = if (isM2Locked) Color(0xFF475569) else Color(0xFF5B21B6))
                        } else {
                            PaperHeaderCell("يومي", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM2Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM2Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("معدل ش2", cellW, rowH, labelFontSize, backgroundColor = if (isM2Locked) Color(0xFFCBD5E1) else Color(0xFFDDD6FE), textColor = if (isM2Locked) Color(0xFF475569) else Color(0xFF5B21B6))
                        }
                        
                        // Term 1 Subheader
                        PaperHeaderCell("معدل ف1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFFEF3C7), textColor = Color(0xFFB45309))
                        
                        // Midterm Subheaders
                        if (isSpecial) {
                            repeat(midtermOralCount) { i ->
                                PaperHeaderCell("شفهي ${i+1}", cellW, rowH, labelFontSize, backgroundColor = if (isMidtermLocked) Color(0xFFE2E8F0) else Color(0xFFCCFBF1), textColor = if (isMidtermLocked) Color(0xFF64748B) else Color(0xFF0F766E))
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = if (isMidtermLocked) Color(0xFFE2E8F0) else Color(0xFFCCFBF1), textColor = if (isMidtermLocked) Color(0xFF64748B) else Color(0xFF0F766E))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isMidtermLocked) Color(0xFFE2E8F0) else Color(0xFFCCFBF1), textColor = if (isMidtermLocked) Color(0xFF64748B) else Color(0xFF0F766E))
                            PaperHeaderCell("درجة نصف", cellW, rowH, labelFontSize, backgroundColor = if (isMidtermLocked) Color(0xFFCBD5E1) else Color(0xFF99F6E4), textColor = if (isMidtermLocked) Color(0xFF475569) else Color(0xFF0F766E))
                        } else {
                            PaperHeaderCell("درجة نصف", cellW, rowH, labelFontSize, backgroundColor = if (isMidtermLocked) Color(0xFFE2E8F0) else Color(0xFFCCFBF1), textColor = if (isMidtermLocked) Color(0xFF64748B) else Color(0xFF0F766E))
                        }
                        
                        // Month 3 Subheaders
                        if (isSpecial) {
                            repeat(m3Count) { i ->
                                val defaultName = if (m3Count == 1) "شفهي" else "شفهي ${i+1}"
                                val colName = settings.columnNames.getOrElse(10 + i) { "" }.ifBlank { defaultName }
                                EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM3Locked) Color(0xFF64748B) else null) { 
                                    if (!isM3Locked) onEditHeader(10 + i) 
                                }
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM3Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM3Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("معدل ش3", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFCBD5E1) else Color(0xFFBFDBFE), textColor = if (isM3Locked) Color(0xFF475569) else Color(0xFF1E3A8A))
                        } else {
                            PaperHeaderCell("يومي", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM3Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), textColor = if (isM3Locked) Color(0xFF64748B) else Color(0xFF1E3A8A))
                            PaperHeaderCell("معدل ش3", cellW, rowH, labelFontSize, backgroundColor = if (isM3Locked) Color(0xFFCBD5E1) else Color(0xFFBFDBFE), textColor = if (isM3Locked) Color(0xFF475569) else Color(0xFF1E3A8A))
                        }
                        
                        // Month 4 Subheaders
                        if (isSpecial) {
                            repeat(m4Count) { i ->
                                val defaultName = if (m4Count == 1) "شفهي" else "شفهي ${i+1}"
                                val colName = settings.columnNames.getOrElse(15 + i) { "" }.ifBlank { defaultName }
                                EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM4Locked) Color(0xFF64748B) else null) { 
                                    if (!isM4Locked) onEditHeader(15 + i) 
                                }
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM4Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM4Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("معدل ش4", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFCBD5E1) else Color(0xFFDDD6FE), textColor = if (isM4Locked) Color(0xFF475569) else Color(0xFF5B21B6))
                        } else {
                            PaperHeaderCell("يومي", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM4Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFE2E8F0) else Color(0xFFEDE9FE), textColor = if (isM4Locked) Color(0xFF64748B) else Color(0xFF5B21B6))
                            PaperHeaderCell("معدل ش4", cellW, rowH, labelFontSize, backgroundColor = if (isM4Locked) Color(0xFFCBD5E1) else Color(0xFFDDD6FE), textColor = if (isM4Locked) Color(0xFF475569) else Color(0xFF5B21B6))
                        }
                        
                        // Term 2 & Annual Subheaders
                        PaperHeaderCell("معدل ف2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFFEF3C7), textColor = Color(0xFFB45309))
                        PaperHeaderCell("السعي السنوي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFFFEDD5), textColor = Color(0xFFC2410C))
                        
                        // Final Exam Subheaders
                        if (isSpecial) {
                            repeat(finalOralCount) { i ->
                                PaperHeaderCell("شفهي ${i+1}", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF047857))
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF047857))
                            PaperHeaderCell("تحريري د1", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF047857))
                            PaperHeaderCell("مجموع د1", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFCBD5E1) else Color(0xFFA7F3D0), textColor = if (isFinalLocked) Color(0xFF475569) else Color(0xFF047857))
                        } else {
                            PaperHeaderCell("تحريري د1", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFD1FAE5), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF047857))
                        }
                        
                        // Round 2 & Final Grade Subheaders
                        PaperHeaderCell(if (isFinalLocked) "امتحان د2 🔒" else "امتحان د2", cellW, rowH, labelFontSize, backgroundColor = if (isFinalLocked) Color(0xFFE2E8F0) else Color(0xFFF1F5F9), textColor = if (isFinalLocked) Color(0xFF64748B) else Color(0xFF475569))
                        PaperHeaderCell("النهائية", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFE0E7FF), textColor = Color(0xFF4338CA))
                    }
                }
            }
        }

        // Table Rows
        LazyColumn(
            state = listState, 
            modifier = Modifier.weight(1f).imePadding(),
            contentPadding = PaddingValues(bottom = 350.dp)
        ) {
            itemsIndexed(students, key = { _, it -> it.id }, contentType = { _, _ -> "daily_row" }) { index, std ->
                val m = std.marks
                val isDuplicate = std.fullName in duplicateNames
                val rowBg = if (index % 2 == 0) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                ) {
                    // Fixed Columns (التسلسل ثم اسم الطالب ثابتان على اليمين)
                    PaperTableCell("${index + 1}", seqW, rowH, fontSize, isBold = true, backgroundColor = rowBg)
                    PaperTableCell(
                        text = std.fullName, 
                        width = nameW, 
                        height = rowH, 
                        fontSize = nameFontSize, 
                        textColor = if (isDuplicate) Color(0xFFDC2626) else null, 
                        backgroundColor = rowBg, 
                        textAlign = TextAlign.Start,
                        onLongClick = if (isEditable) { { onEditStudentName(std) } } else null
                    )

                    // Scrollable Grade Cells - Perfectly Aligned Under Corresponding Month Headers
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(hScroll)
                    ) {
                        // Month 1 Data Cells
                        if (isSpecial) {
                            repeat(m1Count) { i ->
                                val v = m.m1Daily.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM1Locked) { valNew -> 
                                    val nl = m.m1Daily.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(m1Daily = nl)) 
                                }
                            }
                            PaperTableCell(m.m1Daily.take(m1Count).sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.m1Written, cellW, rowH, fontSize, isEditable = !isM1Locked) { onUpdateMarks(std, m.copy(m1Written = it)) }
                            PaperTableCell(m.m1MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFDBEAFE).copy(alpha = 0.5f))
                        } else {
                            val v = m.m1Daily.firstOrNull() ?: 0f
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM1Locked) { valNew ->
                                val nl = m.m1Daily.toMutableList()
                                if (nl.isEmpty()) nl.add(valNew) else nl[0] = valNew
                                onUpdateMarks(std, m.copy(m1Daily = nl))
                            }
                            PaperInputCell(m.m1Written, cellW, rowH, fontSize, isEditable = !isM1Locked) { onUpdateMarks(std, m.copy(m1Written = it)) }
                            PaperTableCell(m.m1MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFDBEAFE).copy(alpha = 0.5f))
                        }
                        
                        // Month 2 Data Cells
                        if (isSpecial) {
                            repeat(m2Count) { i ->
                                val v = m.m2Daily.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM2Locked) { valNew -> 
                                    val nl = m.m2Daily.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(m2Daily = nl)) 
                                }
                            }
                            PaperTableCell(m.m2Daily.take(m2Count).sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.m2Written, cellW, rowH, fontSize, isEditable = !isM2Locked) { onUpdateMarks(std, m.copy(m2Written = it)) }
                            PaperTableCell(m.m2MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFEDE9FE).copy(alpha = 0.5f))
                        } else {
                            val v = m.m2Daily.firstOrNull() ?: 0f
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM2Locked) { valNew ->
                                val nl = m.m2Daily.toMutableList()
                                if (nl.isEmpty()) nl.add(valNew) else nl[0] = valNew
                                onUpdateMarks(std, m.copy(m2Daily = nl))
                            }
                            PaperInputCell(m.m2Written, cellW, rowH, fontSize, isEditable = !isM2Locked) { onUpdateMarks(std, m.copy(m2Written = it)) }
                            PaperTableCell(m.m2MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFEDE9FE).copy(alpha = 0.5f))
                        }
                        
                        // Term 1 Data Cell
                        PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFFEF3C7).copy(alpha = 0.5f))
                        
                        // Midterm Data Cells
                        if (isSpecial) {
                            repeat(midtermOralCount) { i ->
                                val v = m.midtermOral.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isMidtermLocked) { valNew -> 
                                    val nl = m.midtermOral.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(midtermOral = nl)) 
                                }
                            }
                            PaperTableCell(m.midtermOral.take(midtermOralCount).sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.midtermScore, cellW, rowH, fontSize, isEditable = !isMidtermLocked) { onUpdateMarks(std, m.copy(midtermScore = it)) }
                            PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFCCFBF1).copy(alpha = 0.5f))
                        } else {
                            PaperInputCell(m.midtermScore, cellW, rowH, fontSize, isEditable = !isMidtermLocked) { onUpdateMarks(std, m.copy(midtermScore = it, midtermFinalGrade = it)) }
                        }
                        
                        // Month 3 Data Cells
                        if (isSpecial) {
                            repeat(m3Count) { i ->
                                val v = m.m3Daily.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM3Locked) { valNew -> 
                                    val nl = m.m3Daily.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(m3Daily = nl)) 
                                }
                            }
                            PaperTableCell(m.m3Daily.take(m3Count).sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.m3Written, cellW, rowH, fontSize, isEditable = !isM3Locked) { onUpdateMarks(std, m.copy(m3Written = it)) }
                            PaperTableCell(m.m3MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFDBEAFE).copy(alpha = 0.5f))
                        } else {
                            val v = m.m3Daily.firstOrNull() ?: 0f
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM3Locked) { valNew ->
                                val nl = m.m3Daily.toMutableList()
                                if (nl.isEmpty()) nl.add(valNew) else nl[0] = valNew
                                onUpdateMarks(std, m.copy(m3Daily = nl))
                            }
                            PaperInputCell(m.m3Written, cellW, rowH, fontSize, isEditable = !isM3Locked) { onUpdateMarks(std, m.copy(m3Written = it)) }
                            PaperTableCell(m.m3MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFDBEAFE).copy(alpha = 0.5f))
                        }
                        
                        // Month 4 Data Cells
                        if (isSpecial) {
                            repeat(m4Count) { i ->
                                val v = m.m4Daily.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM4Locked) { valNew -> 
                                    val nl = m.m4Daily.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(m4Daily = nl)) 
                                }
                            }
                            PaperTableCell(m.m4Daily.take(m4Count).sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.m4Written, cellW, rowH, fontSize, isEditable = !isM4Locked) { onUpdateMarks(std, m.copy(m4Written = it)) }
                            PaperTableCell(m.m4MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFEDE9FE).copy(alpha = 0.5f))
                        } else {
                            val v = m.m4Daily.firstOrNull() ?: 0f
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isM4Locked) { valNew ->
                                val nl = m.m4Daily.toMutableList()
                                if (nl.isEmpty()) nl.add(valNew) else nl[0] = valNew
                                onUpdateMarks(std, m.copy(m4Daily = nl))
                            }
                            PaperInputCell(m.m4Written, cellW, rowH, fontSize, isEditable = !isM4Locked) { onUpdateMarks(std, m.copy(m4Written = it)) }
                            PaperTableCell(m.m4MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFEDE9FE).copy(alpha = 0.5f))
                        }
                        
                        // Term 2 & Annual Average Data Cells
                        PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFFEF3C7).copy(alpha = 0.5f))
                        PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFFFEDD5).copy(alpha = 0.5f))
                        
                        // Final Exam Data Cells
                        if (isSpecial) {
                            repeat(finalOralCount) { i ->
                                val v = m.finalOral.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable = !isFinalLocked) { valNew -> 
                                    val nl = m.finalOral.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(finalOral = nl)) 
                                }
                            }
                            PaperTableCell(m.finalOral.take(finalOralCount).sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.finalWrittenD1, cellW, rowH, fontSize, isEditable = !isFinalLocked) { onUpdateMarks(std, m.copy(finalWrittenD1 = it)) }
                            PaperTableCell(m.finalExamTotal.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFD1FAE5).copy(alpha = 0.5f))
                        } else {
                            PaperInputCell(m.finalWrittenD1, cellW, rowH, fontSize, isEditable = !isFinalLocked) { onUpdateMarks(std, m.copy(finalWrittenD1 = it)) }
                        }
                        
                        // Round 2 & Final Grade Data Cells
                        PaperInputCell(m.finalWrittenD2 ?: 0f, cellW, rowH, fontSize, isEditable = !isFinalLocked) { onUpdateMarks(std, m.copy(finalWrittenD2 = if (it == 0f) null else it)) }
                        
                        PaperTableCell(m.finalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true, textColor = if (m.finalGrade < 50) Color(0xFFDC2626) else null, backgroundColor = Color(0xFFE0E7FF).copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun PaperHeaderCell(
    text: String,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
    textColor: Color? = null,
    backgroundColor: Color? = null
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val effectiveBg = if (currentTheme.isDark) {
        currentTheme.tableHeaderBg
    } else {
        backgroundColor ?: currentTheme.tableHeaderBg
    }
    val effectiveTextColor = if (currentTheme.isDark) {
        currentTheme.tableHeaderTextColor
    } else {
        textColor ?: currentTheme.tableHeaderTextColor
    }
    val borderColor = if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFF475569)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.8.dp, borderColor)
            .background(effectiveBg)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = effectiveTextColor,
            textAlign = TextAlign.Center,
            lineHeight = (fontSize.value + 3f).sp,
            softWrap = true,
            maxLines = 2
        )
    }
}

@Composable
fun PaperHeaderCellWithAdd(
    text: String,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    canAdd: Boolean = true,
    canRemove: Boolean = false,
    textColor: Color? = null,
    backgroundColor: Color? = null
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val effectiveBg = if (currentTheme.isDark) {
        currentTheme.tableHeaderBg
    } else {
        backgroundColor ?: currentTheme.tableHeaderBg
    }
    val effectiveTextColor = if (currentTheme.isDark) {
        currentTheme.tableHeaderTextColor
    } else {
        textColor ?: currentTheme.tableHeaderTextColor
    }
    val borderColor = currentTheme.tableBorderColor

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, borderColor)
            .background(effectiveBg),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            if (canRemove) {
                Surface(
                    color = Color(0xFFDC2626),
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(width = 18.dp, height = 18.dp)
                        .clickable { onRemove() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "−",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = effectiveTextColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize.value + 2.5f).sp,
                softWrap = true,
                maxLines = 2
            )
            if (canAdd) {
                Surface(
                    color = Color(0xFF16A34A),
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(width = 18.dp, height = 18.dp)
                        .clickable { onAdd() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditableHeaderCell(
    text: String, 
    width: Dp, 
    height: Dp, 
    fontSize: TextUnit, 
    textColor: Color? = null,
    backgroundColor: Color? = null,
    onClick: () -> Unit
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val effectiveBg = if (currentTheme.isDark) {
        currentTheme.tableHeaderBg
    } else {
        backgroundColor ?: currentTheme.tableHeaderBg
    }
    val effectiveTextColor = if (currentTheme.isDark) {
        currentTheme.tableHeaderTextColor
    } else {
        textColor ?: currentTheme.tableHeaderTextColor
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, currentTheme.tableBorderColor)
            .background(effectiveBg)
            .clickable { onClick() }
            .padding(horizontal = 2.dp), 
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (text.isEmpty()) "..." else text, 
            fontSize = fontSize, 
            fontWeight = FontWeight.Black, 
            color = effectiveTextColor, 
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaperTableCell(
    text: String,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
    isBold: Boolean = false,
    textColor: Color? = null,
    backgroundColor: Color? = null,
    textAlign: TextAlign = TextAlign.Center,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val isName = (textAlign == TextAlign.Start || textAlign == TextAlign.Right)
    val effectiveFontSize = if (isName && fontSize.value < 13f) 13.sp else fontSize
    val effectiveTextColor = if (currentTheme.isDark) {
        if (textColor == Color(0xFFDC2626) || textColor == Color.Red) Color(0xFFF87171)
        else currentTheme.textPrimaryColor
    } else {
        textColor ?: (if (isName) Color(0xFF000000) else Color(0xFF0F172A))
    }
    val effectiveBg = if (currentTheme.isDark) {
        if (backgroundColor == currentTheme.tableAltCellBg) currentTheme.tableAltCellBg
        else currentTheme.tableCellBg
    } else {
        backgroundColor ?: currentTheme.tableCellBg
    }
    val borderColor = if (currentTheme.isDark) currentTheme.tableBorderColor else Color(0xFF64748B)

    val clickModifier = when {
        onLongClick != null -> Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick() }
        )
        onClick != null -> Modifier.clickable { onClick() }
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.8.dp, borderColor)
            .background(effectiveBg)
            .then(clickModifier)
            .padding(start = 3.dp, end = 2.dp),
        contentAlignment = if (isName) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = effectiveFontSize,
            fontWeight = if (isBold || isName) FontWeight.Black else FontWeight.Bold,
            color = effectiveTextColor,
            textAlign = textAlign,
            maxLines = 2,
            softWrap = true,
            lineHeight = (effectiveFontSize.value + 3.5f).sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaperInputCell(
    value: Float,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
    isEditable: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val context = LocalContext.current
    var text by remember(value) { mutableStateOf(if(value == 0f) "" else value.toInt().toString()) }
    val borderColor = currentTheme.tableBorderColor
    val cellBg = if (!isEditable) {
        if (currentTheme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    } else {
        currentTheme.tableCellBg
    }
    val textColor = if (!isEditable) {
        if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    } else {
        currentTheme.textPrimaryColor
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, borderColor)
            .background(cellBg)
            .then(
                if (!isEditable) {
                    Modifier.clickable {
                        Toast.makeText(context, "درجات هذه الفترة معتمدة ومقفلة من إدارة المدرسة 🔒", Toast.LENGTH_SHORT).show()
                    }
                } else Modifier
            )
            .bringIntoViewRequester(bringIntoViewRequester),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = text,
            onValueChange = { input -> 
                if (!isEditable) return@BasicTextField
                val filtered = input.filter { it.isDigit() }
                if (filtered.length <= 3) {
                    text = filtered
                    val num = filtered.toFloatOrNull() ?: 0f
                    if (num != value) {
                        onValueChange(num)
                    }
                }
            },
            enabled = isEditable,
            textStyle = TextStyle(
                fontSize = fontSize, 
                textAlign = TextAlign.Center, 
                fontWeight = FontWeight.Bold, 
                color = textColor
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
    }
}

@Composable
fun AbsencesRegisterTable(
    students: List<Student>,
    listState: LazyListState,
    absences: List<AbsenceRecord>,
    selectedDate: String,
    onToggleAbsence: (Student, Boolean) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    onEditStudentName: (Student) -> Unit = {},
    isEditable: Boolean = true
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val date = try { java.time.LocalDate.parse(selectedDate) } catch (e: Exception) { java.time.LocalDate.now() }
    val cellW = 110.dp
    val nameW = 135.dp
    val seqW = 28.dp
    val rowH = 44.dp
    val fontSize = 10.5.sp
    val nameFontSize = 13.sp

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    // Fast O(1) Pre-computed Absence Lookups for selected date
    val absentStudentIds = remember(absences, selectedDate) {
        absences.filter { it.dateString == selectedDate }
            .map { it.studentId }
            .toSet()
    }
    val totalAbsencesCountMap = remember(absences) {
        absences.groupBy { it.studentId }.mapValues { it.value.size }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        // Date Selector Bar (شريط التحكم بالتاريخ)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = currentTheme.surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onDateChange(date.minusDays(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "اليوم السابق", tint = currentTheme.primaryColor)
                }

                Surface(
                    color = currentTheme.primaryColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp), tint = currentTheme.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التاريخ: $selectedDate",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = currentTheme.textPrimaryColor
                        )
                    }
                }

                IconButton(onClick = { onDateChange(date.plusDays(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "اليوم التالي", tint = currentTheme.primaryColor)
                }
            }
        }

        // Table Content
        val hScroll = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.tableHeaderBg)
            ) {
                // Fixed Columns (ثابت: التسلسل ثم اسم الطالب)
                PaperHeaderCell("ت", seqW, 40.dp, fontSize)
                PaperHeaderCell("اسم الطالب", nameW, 40.dp, 11.5.sp)

                // Scrollable Columns (متحركة بالسحب)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(hScroll)
                ) {
                    PaperHeaderCell("تسجيل الغياب لليوم", cellW, 40.dp, fontSize)
                    PaperHeaderCell("مجموع أيام الغياب", cellW, 40.dp, fontSize)
                }
            }

            LazyColumn(
                state = listState, 
                modifier = Modifier.weight(1f).imePadding(),
                contentPadding = PaddingValues(bottom = 350.dp)
            ) {
                itemsIndexed(students, key = { _, it -> it.id }) { index, std ->
                    val isAbsent = std.id in absentStudentIds
                    val activeCount = totalAbsencesCountMap[std.id] ?: 0
                    val totalAbsences = std.historicalAbsences + activeCount
                    val isDuplicate = std.fullName in duplicateNames
                    val rowBg = if (index % 2 == 0) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .then(if (isEditable) Modifier.clickable { onToggleAbsence(std, !isAbsent) } else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fixed Columns (التسلسل ثم اسم الطالب ثابتان على اليمين)
                        PaperTableCell("${index + 1}", seqW, rowH, fontSize, isBold = true, backgroundColor = rowBg)
                        PaperTableCell(
                            text = std.fullName, 
                            width = nameW, 
                            height = rowH, 
                            fontSize = nameFontSize, 
                            textColor = if (isDuplicate) Color(0xFFDC2626) else null,
                            backgroundColor = rowBg,
                            textAlign = TextAlign.Start,
                            onLongClick = if (isEditable) { { onEditStudentName(std) } } else null
                        )

                        // Scrollable Columns
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(hScroll),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Today's Absence Toggle Pill
                            Box(
                                modifier = Modifier
                                    .width(cellW)
                                    .height(rowH)
                                    .border(0.5.dp, currentTheme.tableBorderColor)
                                    .background(rowBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = if (isAbsent) Color(0xFFDC2626) else Color(0xFF16A34A).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clickable(enabled = isEditable) {
                                            onToggleAbsence(std, !isAbsent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAbsent) "غائب ❌" else "حاضر ✓",
                                            color = if (isAbsent) Color.White else Color(0xFF15803D),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Total Days Absent Badge
                            Box(
                                modifier = Modifier
                                    .width(cellW)
                                    .height(rowH)
                                    .border(0.5.dp, currentTheme.tableBorderColor)
                                    .background(rowBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$totalAbsences ${if (totalAbsences == 1) "يوم" else if (totalAbsences == 2) "يومان" else "أيام"}",
                                    fontSize = 11.sp,
                                    fontWeight = if (totalAbsences > 0) FontWeight.Black else FontWeight.Normal,
                                    color = if (totalAbsences >= 3) Color(0xFFDC2626) else currentTheme.textPrimaryColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * مربعات الإحصاء والنسب المزخرفة والمضللة بتأثير ثلاثي الأبعاد
 */
@Composable
fun EmbossedStatBox3D(
    title: String,
    value: String,
    bgGradient: List<Color>,
    borderColor: Color,
    textColor: Color,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = if (isCompact) 34.dp else 64.dp),
        shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
        shadowElevation = if (isCompact) 2.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(bgGradient)),
            contentAlignment = Alignment.Center
        ) {
            // شريط إضاءة زجاجي ثلاثي الأبعاد بالأعلى
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.7f))
            )

            if (isCompact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                    Text(
                        text = value.ifBlank { "0" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        maxLines = 1
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = value.ifBlank { "0" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

