package com.school.system.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlin.math.roundToInt
import com.school.system.data.model.Student
import com.school.system.data.model.StudentMarks
import com.school.system.data.model.DailyColumnSetting
import com.school.system.data.model.AbsenceRecord
import com.school.system.data.model.latestRecordedScore
import com.school.system.data.model.latestRecordedScoreInt
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.school.system.data.repository.SecureUploadResult
import com.school.system.utils.BiometricHelper
import com.school.system.utils.ImageTextExtractor

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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
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

    val startSecureUploadWithBiometric: (String) -> Unit = { pinToUse ->
        if (fragmentActivity == null) {
            Toast.makeText(context, "تعذر تشغيل المصادقة البيومترية في النشاط الحالي", Toast.LENGTH_LONG).show()
        } else {
            BiometricHelper.authenticate(
                activity = fragmentActivity,
                title = "تأكيد رفع درجات: $subject ($section)",
                subtitle = "المصادقة البيومترية مطلوبة لاعتماد درجات الشعبة ورفعها للسحابة",
                onSuccess = {
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
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tier 1: Geometric Action Controls Bar
                Surface(
                    color = currentTheme.surfaceColor,
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, currentTheme.tableBorderColor)
                ) {
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
                                    onClick = { showAddStudentDialog = true },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = "إضافة طالب",
                                        tint = currentTheme.primaryColor,
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
                                        printRegister(context, grade, section, subject, selectedTab, students, absences, config)
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
                                        exportAndSharePdfWithIText7(context, grade, section, subject, selectedTab, students, absences, config)
                                    },
                                    modifier = Modifier.size(if (isLandscape) 30.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "مشاركة السجل الرسمي كـ PDF",
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
                                onClick = { showAddStudentDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("إضافة واستيراد الطلبة 📋", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            onEditStudentName = { studentToEdit = it },
                            isEditable = !isEditingLocked,
                            isSpecial = isSpecial
                        )
                        1 -> TeacherRegisterTable(
                            students = students, 
                            listState = listState, 
                            onUpdate = { viewModel.updateStudentMarks(it.first, it.second) }, 
                            onEditStudentName = { studentToEdit = it },
                            isEditable = !isEditingLocked, 
                            isSpecial = isSpecial
                        )
                        2 -> AdminRegisterTable(
                            students = students, 
                            listState = listState, 
                            onUpdate = { viewModel.updateStudentMarks(it.first, it.second) }, 
                            onEditStudentName = { studentToEdit = it },
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
                            onEditStudentName = { studentToEdit = it },
                            isEditable = !isEditingLocked
                        )
                    }
                }
            }
        }

        if (showAddStudentDialog) {
            var selectedTab by remember { mutableIntStateOf(0) }
            var singleName by remember { mutableStateOf("") }
            var multiNamesText by remember { mutableStateOf("") }
            var ocrNamesText by remember { mutableStateOf("") }
            var isScanningImage by remember { mutableStateOf(false) }

            val coroutineScope = rememberCoroutineScope()

            // Function to process bitmap for OCR & Gemini AI
            val processImageBitmap: (Bitmap?) -> Unit = { bitmap ->
                if (bitmap != null) {
                    isScanningImage = true
                    viewModel.extractStudentNamesFromPhoto(bitmap) { names, rawText ->
                        isScanningImage = false
                        if (names.isNotEmpty()) {
                            ocrNamesText = names.joinToString("\n")
                            Toast.makeText(context, "تم استخراج ${names.size} اسم طالب بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                        } else if (rawText.isNotBlank()) {
                            ocrNamesText = rawText
                            Toast.makeText(context, "تم استخراج النص من الصورة", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "لم يتم العثور على أسماء واضحة، تأكد من وضوح الصورة والتركيز على القائمة", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            // Gallery Launcher
            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                        processImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "تعذر قراءة الصورة المختارة", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Camera Launcher
            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    processImageBitmap(bitmap)
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
                                Text("طالب مفرد", modifier = Modifier.padding(vertical = 8.dp), fontSize = 11.5.sp)
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("لصق قائمة", modifier = Modifier.padding(vertical = 8.dp), fontSize = 11.5.sp)
                            }
                            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                                Text("مسح صورة 📷", modifier = Modifier.padding(vertical = 8.dp), fontSize = 11.5.sp)
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
                        } else if (selectedTab == 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "الصق قائمة الأسماء هنا (سطر لكل طالب من ملف Word أو Excel):",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                OutlinedTextField(
                                    value = multiNamesText,
                                    onValueChange = { multiNamesText = it },
                                    placeholder = { Text("أحمد علي حسن محمد\nزيد كريم جاسم\nعلي حسين صالح") },
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    maxLines = 10
                                )
                            }
                        } else {
                            // Tab 2: OCR Image Scan
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "استخراج أسماء الطلاب أوفلاين مجاناً من صور القوائم الورقية 📷:",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF1E3A8A),
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("الكاميرا 📸", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("المعرض 🖼️", fontSize = 11.sp)
                                    }
                                }

                                if (isScanningImage) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("جاري استخراج الأسماء من الصورة أوفلاين...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = ocrNamesText,
                                    onValueChange = { ocrNamesText = it },
                                    label = { Text("الأسماء المستخرجة من الصورة (يمكنك مراجعتها وتعديلها)") },
                                    placeholder = { Text("تظهر الأسماء هنا بعد تصوير القائمة...") },
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
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
                            } else if (selectedTab == 1) {
                                if (multiNamesText.isNotBlank()) {
                                    viewModel.importMultipleStudents(grade, section, subject, multiNamesText) { count ->
                                        Toast.makeText(context, "تم استيراد $count طالب بنجاح!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                if (ocrNamesText.isNotBlank()) {
                                    viewModel.importMultipleStudents(grade, section, subject, ocrNamesText) { count ->
                                        Toast.makeText(context, "تم استيراد $count طالب من الصورة بنجاح! 🚀", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            showAddStudentDialog = false
                        }
                    ) {
                        Text(if (selectedTab == 0) "إضافة" else "استيراد القائمة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStudentDialog = false }) {
                        Text("إلغاء")
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

                        val savedSup = remember { viewModel.getSavedSupervisorCode() }
                        if (!savedSup.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                onClick = { inputPin = savedSup },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "اضغط هنا لاستخدام كود المشرف المحفوظ ($savedSup) 🔑",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // أزرار عريضة وواضحة تمتد على كامل العرض لمنع تقطع الأحرف وضغط الأزرار
                        Button(
                            onClick = {
                                if (inputPin.isNotBlank()) {
                                    val pin = inputPin.trim()
                                    showPinDialog = false
                                    startSecureUploadWithBiometric(pin)
                                } else {
                                    Toast.makeText(context, "يرجى كتابة الرمز السري أو اختيار رفع مباشر أدناه", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تأكيد الرمز والرفع بالبصمة 🔒", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                showPinDialog = false
                                startSecureUploadWithBiometric("DIRECT")
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("رفع مباشر فوري بدون رمز ⚡", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                            "تنبيه أمني من الإدارة",
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
                                "تم تحديث أو تغيير رمز اعتماد هذه المادة من الإدارة، يرجى إدخال الرمز الجديد أو التجاوز بالرفع المباشر."
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
                                inputPin = ""
                                showPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("إدخال الرمز الجديد 🔑", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                showInvalidPinDialog = false
                                startSecureUploadWithBiometric("DIRECT")
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تجاوز ورفع مباشر فوري ⚡", fontWeight = FontWeight.Black, fontSize = 13.sp)
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

fun exportAndSharePdfWithIText7(
    context: android.content.Context,
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<com.school.system.data.model.Student>,
    absences: List<com.school.system.data.model.AbsenceRecord>,
    config: com.school.system.data.model.SchoolConfig?
) {
    try {
        val schoolName = config?.schoolName ?: "مدرسة التميز"
        val teacherName = config?.managerName ?: "مدرس المادة"
        val directorate = config?.directorateName ?: "مديرية التربية"
        val tabName = when (tabIndex) {
            0 -> "السجل اليومي والنشاط"
            1 -> "سجل المدرس التفصيلي"
            2 -> "سجل الإدارة الختامي"
            else -> "سجل حضور وغيابات الطلاب"
        }

        val fileName = "Grade_Register_${System.currentTimeMillis()}.pdf"
        val pdfFile = java.io.File(context.cacheDir, fileName)
        val writer = com.itextpdf.kernel.pdf.PdfWriter(pdfFile)
        val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(writer)
        pdfDoc.defaultPageSize = if (tabIndex == 0) com.itextpdf.kernel.geom.PageSize.A4.rotate() else com.itextpdf.kernel.geom.PageSize.A4
        val document = com.itextpdf.layout.Document(pdfDoc)
        document.setMargins(15f, 15f, 15f, 15f)

        // Header Table
        val headerTable = com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(floatArrayOf(30f, 40f, 30f))).useAllAvailableWidth()
        headerTable.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
        
        val p1 = com.itextpdf.layout.element.Paragraph("جمهورية العراق\nوزارة التربية\n$directorate\n$schoolName").setBold().setFontSize(9.5f)
        val p2 = com.itextpdf.layout.element.Paragraph("سجل الدرجات الرسمي ($tabName)\nالمادة: $subject\nالصف: $grade ($section)").setBold().setFontSize(11f)
        val p3 = com.itextpdf.layout.element.Paragraph("العام الدراسي: 2025-2026\nالمدرس: $teacherName\nالتاريخ: ${java.time.LocalDate.now()}").setFontSize(9.5f)

        headerTable.addCell(com.itextpdf.layout.element.Cell().add(p1).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        headerTable.addCell(com.itextpdf.layout.element.Cell().add(p2).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        headerTable.addCell(com.itextpdf.layout.element.Cell().add(p3).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        document.add(headerTable)
        document.add(com.itextpdf.layout.element.Paragraph("\n"))

        // Data Table
        val dataTable = when (tabIndex) {
            0 -> com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(floatArrayOf(4f, 22f, 6.5f, 7.5f, 6.5f, 7.5f, 6.5f, 7.5f, 6.5f, 7.5f, 8f, 10f))).useAllAvailableWidth()
            1 -> com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(floatArrayOf(3.5f, 18.5f, 6.5f, 6.5f, 6.5f, 6.5f, 6.5f, 6.5f, 6.5f, 7.5f, 8.5f, 8.5f, 9.5f))).useAllAvailableWidth()
            2 -> com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(floatArrayOf(4f, 20f, 9f, 9.5f, 9f, 9.5f, 12f, 12f, 15f))).useAllAvailableWidth()
            else -> com.itextpdf.layout.element.Table(com.itextpdf.layout.properties.UnitValue.createPercentArray(floatArrayOf(7f, 53f, 20f, 20f))).useAllAvailableWidth()
        }
        dataTable.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)

        // Headers
        val headerTitles = when (tabIndex) {
            0 -> listOf("ت", "اسم الطالب", "يومي ش1", "تحريري ش1", "يومي ش2", "تحريري ش2", "يومي ش3", "تحريري ش3", "يومي ش4", "تحريري ش4", "شفهي نص", "تحريري نص")
            1 -> listOf("ت", "اسم الطالب", "معدل ش1", "معدل ش2", "درجة فص1", "درجة نصف", "معدل ش3", "معدل ش4", "درجة فص2", "السعي السنوي", "امتحان نهائي د1", "امتحان نهائي د2", "الدرجة النهائية")
            2 -> listOf("ت", "اسم الطالب", "معدل الفصل 1", "درجة نصف السنة", "معدل الفصل 2", "السعي السنوي", "امتحان نهائي د1", "امتحان نهائي د2", "الدرجة النهائية")
            else -> listOf("ت", "اسم الطالب", "مجموع الغيابات", "الحالة")
        }

        val headerFontSize = when (tabIndex) {
            0 -> 7f
            1 -> 6.5f
            2 -> 7.5f
            else -> 9f
        }
        val cellFontSize = when (tabIndex) {
            0 -> 6.5f
            1 -> 6.5f
            2 -> 7f
            else -> 8.5f
        }

        headerTitles.forEach { title ->
            dataTable.addHeaderCell(
                com.itextpdf.layout.element.Cell()
                    .setPadding(3f)
                    .add(com.itextpdf.layout.element.Paragraph(title).setBold().setFontSize(headerFontSize))
                    .setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(241, 245, 249))
            )
        }

        val isSpecial = isSpecialSubject(subject)

        // Rows
        students.forEachIndexed { index, student ->
            val m = student.marks
            val totalAbs = student.historicalAbsences + absences.count { it.studentId == student.id }
            val d2Formatted = if (m.finalWrittenD2 == null || m.finalWrittenD2 == 0f) "-" else if (isSpecial) "${(m.finalOral.sum() + m.finalWrittenD2!!).toInt()}" else "${m.finalWrittenD2!!.toInt()}"
            val cells = when (tabIndex) {
                0 -> listOf(
                    "${index + 1}",
                    student.fullName,
                    "${m.m1Daily.average().toInt()}",
                    "${m.m1Written.toInt()}",
                    "${m.m2Daily.average().toInt()}",
                    "${m.m2Written.toInt()}",
                    "${m.m3Daily.average().toInt()}",
                    "${m.m3Written.toInt()}",
                    "${m.m4Daily.average().toInt()}",
                    "${m.m4Written.toInt()}",
                    "${m.midtermOral.average().toInt()}",
                    "${m.midtermScore.toInt()}"
                )
                1 -> listOf(
                    "${index + 1}",
                    student.fullName,
                    "${m.m1MonthAvg.toInt()}",
                    "${m.m2MonthAvg.toInt()}",
                    "${m.term1Avg.toInt()}",
                    "${m.midtermFinalGrade.toInt()}",
                    "${m.m3MonthAvg.toInt()}",
                    "${m.m4MonthAvg.toInt()}",
                    "${m.term2Avg.toInt()}",
                    "${m.annualAverage.toInt()}",
                    "${m.finalExamTotal.toInt()}",
                    d2Formatted,
                    "${m.latestRecordedScoreInt()}"
                )
                2 -> listOf(
                    "${index + 1}",
                    student.fullName,
                    "${m.term1Avg.toInt()}",
                    "${m.midtermFinalGrade.toInt()}",
                    "${m.term2Avg.toInt()}",
                    "${m.annualAverage.toInt()}",
                    "${m.finalExamTotal.toInt()}",
                    d2Formatted,
                    "${m.latestRecordedScoreInt()}"
                )
                else -> listOf(
                    "${index + 1}",
                    student.fullName,
                    "$totalAbs",
                    m.status
                )
            }

            cells.forEachIndexed { cellIdx, cellText ->
                val cell = com.itextpdf.layout.element.Cell()
                    .setPadding(2.5f)
                    .add(com.itextpdf.layout.element.Paragraph(cellText).setFontSize(cellFontSize))
                if (cellIdx == 1) {
                    cell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)
                }
                dataTable.addCell(cell)
            }
        }

        document.add(dataTable)

        // Statistics Summary Bar in PDF (At bottom of page)
        val progPdfStats = calculateProgressiveStats(students)
        val totalSt = progPdfStats.totalStudents
        val passedSt = progPdfStats.passedStudents
        val failedSt = progPdfStats.failedStudents
        val passRateVal = progPdfStats.passRate
        val stageLabel = progPdfStats.stageName

        document.add(com.itextpdf.layout.element.Paragraph("\n"))
        val statsTable = com.itextpdf.layout.element.Table(floatArrayOf(25f, 25f, 25f, 25f)).useAllAvailableWidth()
        statsTable.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
        
        val cell1 = com.itextpdf.layout.element.Cell().add(com.itextpdf.layout.element.Paragraph("عدد الطلاب\n$totalSt").setBold().setFontSize(9f)).setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252)).setPadding(4f)
        val cell2 = com.itextpdf.layout.element.Cell().add(com.itextpdf.layout.element.Paragraph("الناجحين\n$passedSt").setBold().setFontSize(9f)).setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(220, 252, 231)).setPadding(4f)
        val cell3 = com.itextpdf.layout.element.Cell().add(com.itextpdf.layout.element.Paragraph("الراسبين\n$failedSt").setBold().setFontSize(9f)).setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(254, 226, 226)).setPadding(4f)
        val cell4 = com.itextpdf.layout.element.Cell().add(com.itextpdf.layout.element.Paragraph("نسبة النجاح ($stageLabel)\n$passRateVal%").setBold().setFontSize(9f)).setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(241, 245, 249)).setPadding(4f)
        
        statsTable.addCell(cell1)
        statsTable.addCell(cell2)
        statsTable.addCell(cell3)
        statsTable.addCell(cell4)
        document.add(statsTable)

        // Signatures
        document.add(com.itextpdf.layout.element.Paragraph("\n"))
        val signTable = com.itextpdf.layout.element.Table(2).useAllAvailableWidth()
        signTable.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
        signTable.addCell(com.itextpdf.layout.element.Cell().add(com.itextpdf.layout.element.Paragraph("توقيع مدرس المادة: .......................................").setBold().setFontSize(10f)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        signTable.addCell(com.itextpdf.layout.element.Cell().add(com.itextpdf.layout.element.Paragraph("توقيع وختم مدير المدرسة: .......................................").setBold().setFontSize(10f)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
        document.add(signTable)

        document.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "سجل الدرجات الرسمي - $subject - $grade ($section)")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة سجل الدرجات (PDF) عبر:"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "فشل إنشاء ملف PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun printRegister(
    context: android.content.Context,
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<com.school.system.data.model.Student>,
    absences: List<com.school.system.data.model.AbsenceRecord>,
    config: com.school.system.data.model.SchoolConfig?
) {
    (context as? android.app.Activity)?.runOnUiThread {
        val webView = android.webkit.WebView(context)
        val html = generateGradesHtml(grade, section, subject, tabIndex, students, absences, config)
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val jobName = "سجل درجات - $subject - $grade ($section)"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = android.print.PrintAttributes.Builder()
                    .setMediaSize(if (tabIndex == 0) android.print.PrintAttributes.MediaSize.ISO_A4.asLandscape() else android.print.PrintAttributes.MediaSize.ISO_A4.asPortrait())
                    .build()
                printManager.print(jobName, printAdapter, printAttributes)
            }
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
    config: com.school.system.data.model.SchoolConfig?
) {
    // Share exact official formatted PDF document identical to print
    exportAndSharePdfWithIText7(context, grade, section, subject, tabIndex, students, absences, config)
}

fun generateGradesHtml(
    grade: String,
    section: String,
    subject: String,
    tabIndex: Int,
    students: List<com.school.system.data.model.Student>,
    absences: List<com.school.system.data.model.AbsenceRecord>,
    config: com.school.system.data.model.SchoolConfig?
): String {
    val schoolName = config?.schoolName ?: "مدرسة التميز"
    val directorate = config?.directorateName ?: "مديرية التربية"
    val teacherName = config?.managerName ?: "مدرس المادة"
    val tabName = when (tabIndex) {
        0 -> "السجل اليومي والنشاط"
        1 -> "سجل المدرس التفصيلي"
        2 -> "سجل الإدارة الختامي"
        else -> "سجل حضور وغيابات الطلاب"
    }

    val isSpecial = isSpecialSubject(subject)
    val rowsHtml = java.lang.StringBuilder()
    students.forEachIndexed { index, student ->
        val marks = student.marks
        val totalAbs = student.historicalAbsences + absences.count { it.studentId == student.id }
        val d2Formatted = if (marks.finalWrittenD2 == null || marks.finalWrittenD2 == 0f) "-" else if (isSpecial) (marks.finalOral.sum() + marks.finalWrittenD2!!).toInt().toString() else marks.finalWrittenD2!!.toInt().toString()
        rowsHtml.append("<tr class='data-row'>")
        rowsHtml.append("<td class='seq-col'>${index + 1}</td>")
        rowsHtml.append("<td class='name-col'>${student.fullName}</td>")

        if (tabIndex == 0) {
            rowsHtml.append("<td class='cell-data'>${marks.m1Daily.average().toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m1Written}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m2Daily.average().toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m2Written}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m3Daily.average().toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m3Written}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m4Daily.average().toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m4Written}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.midtermOral.average().toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.midtermScore}</td>")
        } else if (tabIndex == 1) {
            rowsHtml.append("<td class='cell-data'>${marks.m1MonthAvg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m2MonthAvg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold;'>${marks.term1Avg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.midtermFinalGrade.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m3MonthAvg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.m4MonthAvg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold;'>${marks.term2Avg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #1e3a8a;'>${marks.annualAverage.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.finalExamTotal.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>$d2Formatted</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #15803d;'>${marks.latestRecordedScoreInt()}</td>")
        } else if (tabIndex == 2) {
            rowsHtml.append("<td class='cell-data'>${marks.term1Avg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.midtermFinalGrade.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.term2Avg.toInt()}</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #1e3a8a;'>${marks.annualAverage.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>${marks.finalExamTotal.toInt()}</td>")
            rowsHtml.append("<td class='cell-data'>$d2Formatted</td>")
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: #15803d;'>${marks.latestRecordedScoreInt()}</td>")
        } else {
            rowsHtml.append("<td class='cell-data' style='font-weight: bold; color: ${if (totalAbs > 3) "#dc2626" else "#0f172a"};'>$totalAbs</td>")
            rowsHtml.append("<td class='cell-data'>${marks.status}</td>")
        }
        rowsHtml.append("</tr>")
    }

    val tableHeader = when (tabIndex) {
        0 -> """
            <tr style='background: #f1f5f9; font-weight: bold;'>
                <th class='cell-header seq-col'>ت</th>
                <th class='cell-header name-col'>اسم الطالب</th>
                <th class='cell-header'>يومي1</th>
                <th class='cell-header'>تحريري1</th>
                <th class='cell-header'>يومي2</th>
                <th class='cell-header'>تحريري2</th>
                <th class='cell-header'>يومي3</th>
                <th class='cell-header'>تحريري3</th>
                <th class='cell-header'>يومي4</th>
                <th class='cell-header'>تحريري4</th>
                <th class='cell-header'>يومي نصف</th>
                <th class='cell-header'>تحريري نصف</th>
            </tr>
        """
        1 -> """
            <tr style='background: #f1f5f9; font-weight: bold;'>
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
            <tr style='background: #f1f5f9; font-weight: bold;'>
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
            <tr style='background: #f1f5f9; font-weight: bold;'>
                <th class='cell-header seq-col'>ت</th>
                <th class='cell-header name-col'>اسم الطالب</th>
                <th class='cell-header'>مجموع الغيابات</th>
                <th class='cell-header'>حالة الطالب</th>
            </tr>
        """
    }

    val cellPad = if (tabIndex == 0) "3px 1px" else if (tabIndex == 1) "4px 2px" else "5px 3px"
    val headerPad = if (tabIndex == 0) "4px 1px" else if (tabIndex == 1) "5px 2px" else "6px 3px"
    val cellFontSize = if (tabIndex == 0) "8px" else if (tabIndex == 1) "8px" else "9.5px"
    val headerFontSize = if (tabIndex == 0) "7.5px" else if (tabIndex == 1) "7.5px" else "9px"
    val nameFontSize = if (tabIndex == 0) "8.5px" else if (tabIndex == 1) "8.5px" else "10px"

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
            <style>
                @import url('https://fonts.googleapis.com/css2?family=Amiri:wght@400;700&family=Tajawal:wght@400;700;900&display=swap');
                @page {
                    size: A4 portrait;
                    margin: 6mm 6mm;
                }
                body {
                    font-family: 'Tajawal', 'Amiri', 'Segoe UI', Tahoma, sans-serif;
                    margin: 0;
                    padding: 0;
                    background: #ffffff;
                    color: #0f172a;
                    direction: rtl;
                }
                .cover-page {
                    page-break-after: always;
                    height: 265mm;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    box-sizing: border-box;
                    padding: 6mm;
                }
                .border-frame {
                    border: 8px double #1e3a8a;
                    border-radius: 16px;
                    width: 100%;
                    height: 100%;
                    padding: 12mm 10mm;
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-around;
                    align-items: center;
                    text-align: center;
                    position: relative;
                    background-color: #fafaf9;
                }
                .corner {
                    position: absolute;
                    font-size: 22px;
                    color: #d97706;
                    font-weight: bold;
                }
                .c-tr { top: 12px; right: 16px; }
                .c-tl { top: 12px; left: 16px; }
                .c-br { bottom: 12px; right: 16px; }
                .c-bl { bottom: 12px; left: 16px; }
                
                .school-title {
                    font-size: 22px;
                    font-weight: 900;
                    color: #1e3a8a;
                    margin: 0;
                }
                .ministry-title {
                    font-size: 15px;
                    font-weight: bold;
                    color: #475569;
                    margin: 4px 0 0 0;
                }
                .divider {
                    width: 60%;
                    border: 0;
                    height: 2px;
                    background-image: linear-gradient(to right, rgba(0,0,0,0), #d97706, rgba(0,0,0,0));
                    margin: 12px auto;
                }
                .main-title {
                    font-size: 18px;
                    color: #475569;
                    font-weight: bold;
                    margin: 0;
                }
                .subject-name {
                    font-size: 36px;
                    font-weight: 900;
                    color: #d97706;
                    margin: 8px 0;
                }
                .class-section {
                    font-size: 20px;
                    font-weight: bold;
                    color: #1e3a8a;
                    margin: 4px 0;
                }
                .teacher-title {
                    font-size: 18px;
                    font-weight: bold;
                    color: #0f172a;
                    margin: 0;
                }
                .footer-text {
                    font-size: 13px;
                    color: #64748b;
                }
                
                .table-page {
                    page-break-before: always;
                    padding-top: 2mm;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    text-align: center;
                    table-layout: auto;
                    margin-top: 10px;
                }
                th, td {
                    border: 1px solid #cbd5e1;
                    box-sizing: border-box;
                }
                .seq-col {
                    width: 24px;
                    font-weight: bold;
                    font-size: $headerFontSize;
                }
                .name-col {
                    text-align: right;
                    font-weight: bold;
                    padding-right: 4px !important;
                    font-size: $nameFontSize;
                    white-space: nowrap;
                }
                .cell-data {
                    font-size: $cellFontSize;
                    padding: $cellPad;
                }
                .cell-header {
                    background-color: #f1f5f9;
                    font-weight: bold;
                    font-size: $headerFontSize;
                    padding: $headerPad;
                }
                .data-row:nth-child(even) {
                    background-color: #f8fafc;
                }
                .stats-bar {
                    display: flex;
                    justify-content: space-around;
                    background-color: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    padding: 5px 10px;
                    margin-bottom: 8px;
                    font-size: 11px;
                    font-weight: bold;
                }
                .signatures {
                    margin-top: 25px;
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    text-align: center;
                    font-weight: bold;
                    font-size: 11px;
                }
            </style>
        </head>
        <body>
            <!-- Page 1: Cover Page -->
            <div class="cover-page">
                <div class="border-frame">
                    <div class="corner c-tr">❖</div>
                    <div class="corner c-tl">❖</div>
                    <div class="corner c-br">❖</div>
                    <div class="corner c-bl">❖</div>
                    
                    <div>
                        <h1 class="school-title">جمهورية العراق</h1>
                        <h2 class="ministry-title">وزارة التربية / ${directorate}</h2>
                    </div>
                    
                    <hr class="divider">
                    
                    <div>
                        <h3 class="main-title">سجل درجات الطلاب الرسمي (${tabName})</h3>
                        <h1 class="subject-name">${subject}</h1>
                        <h2 class="class-section">${grade} - الشعبة (${section})</h2>
                    </div>
                    
                    <hr class="divider">
                    
                    <div>
                        <h3 class="teacher-title">المدرسة: ${schoolName}</h3>
                        <h3 class="teacher-title" style="margin-top: 5px; color: #1e3a8a;">أستاذ المادة: ${teacherName}</h3>
                    </div>
                    
                    <div class="footer-text">العام الدراسي: 2025-2026</div>
                </div>
            </div>
            
            <!-- Page 2: Table -->
            <div class="table-page">
                <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #1e3a8a; padding-bottom: 8px; margin-bottom: 8px;">
                    <div>
                        <span style="font-weight: 900; font-size: 13px; color: #1e3a8a;">${schoolName}</span><br>
                        <span style="font-size: 10px; color: #64748b;">${directorate}</span>
                    </div>
                    <div style="text-align: center;">
                        <span style="font-weight: bold; font-size: 14px;">كشف الدرجات الرسمي - ${tabName}</span><br>
                        <span style="font-size: 11px; font-weight: bold; color: #d97706;">المادة: ${subject} | الصف: ${grade} (${section})</span>
                    </div>
                    <div style="text-align: left; font-size: 10px; color: #64748b;">
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
                
                <!-- Statistics Summary Bar at the bottom of the page -->
                <div class="stats-bar" style="margin-top: 15px; margin-bottom: 15px;">
                    <div style="text-align: center;">
                        <span style="font-size: 10px; color: #64748b;">عدد الطلاب</span><br>
                        <span style="font-size: 14px; font-weight: bold; color: #0f172a;">$totalSt</span>
                    </div>
                    <div style="text-align: center;">
                        <span style="font-size: 10px; color: #15803d;">الناجحين</span><br>
                        <span style="font-size: 14px; font-weight: bold; color: #16a34a;">$passedSt</span>
                    </div>
                    <div style="text-align: center;">
                        <span style="font-size: 10px; color: #b91c1c;">الراسبين</span><br>
                        <span style="font-size: 14px; font-weight: bold; color: #dc2626;">$failedSt</span>
                    </div>
                    <div style="text-align: center;">
                        <span style="font-size: 10px; color: #047857;">نسبة النجاح ($stageLabel)</span><br>
                        <span style="font-size: 14px; font-weight: bold; color: #059669;">$passRateVal%</span>
                    </div>
                </div>

                <div class="signatures">
                    <div>توقيع مدرس المادة: .......................................</div>
                    <div>توقيع وختم مدير المدرسة: .......................................</div>
                </div>
            </div>
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
    val nameW = 115.dp
    val seqW = 28.dp
    val rowH = 40.dp
    val fontSize = 10.sp
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
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, fontSize)

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
                        fontSize = fontSize, 
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
    isSpecial: Boolean = false
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val hScroll = rememberScrollState()
    val cellW = 52.dp
    val nameW = 115.dp
    val seqW = 28.dp
    val rowH = 40.dp
    val fontSize = 9.5.sp
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
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, fontSize)

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
                        PaperHeaderCell("معدل ش1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("معدل ش2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("درجة فص1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("درجة نصف", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))

                        PaperHeaderCell("معدل ش3", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("معدل ش4", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("درجة فص2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("السعي السنوي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))

                        PaperHeaderCell("نهائي د1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF065F46))
                        PaperHeaderCell("د2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF065F46))
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
                        fontSize = fontSize, 
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
    isSpecial: Boolean = false
) {
    val currentTheme = com.school.system.ui.theme.LocalAppTheme.current
    val hScroll = rememberScrollState()
    val cellW = 44.dp
    val nameW = 115.dp
    val seqW = 28.dp
    val rowH = 40.dp
    val fontSize = 9.5.sp
    val labelFontSize = 8.sp

    var m1Count by remember { mutableIntStateOf(1) }
    var m2Count by remember { mutableIntStateOf(1) }
    var m3Count by remember { mutableIntStateOf(1) }
    var m4Count by remember { mutableIntStateOf(1) }
    var midtermOralCount by remember { mutableIntStateOf(1) }
    var finalOralCount by remember { mutableIntStateOf(1) }

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
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, fontSize)

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
                        PaperHeaderCellWithAdd(
                            text = "الشهر الأول", 
                            width = cellW * (m1Count + 3), 
                            height = rowH, 
                            fontSize = fontSize,
                            textColor = Color.White,
                            backgroundColor = Color(0xFF1E3A8A),
                            onAdd = { if (m1Count < 5) m1Count++ }, 
                            onRemove = { if (m1Count > 1) m1Count-- },
                            canAdd = m1Count < 5,
                            canRemove = m1Count > 1
                        )

                        // 2. الشهر الثاني (Purple Group)
                        PaperHeaderCellWithAdd(
                            text = "الشهر الثاني", 
                            width = cellW * (m2Count + 3), 
                            height = rowH, 
                            fontSize = fontSize,
                            textColor = Color.White,
                            backgroundColor = Color(0xFF5B21B6),
                            onAdd = { if (m2Count < 5) m2Count++ }, 
                            onRemove = { if (m2Count > 1) m2Count-- },
                            canAdd = m2Count < 5,
                            canRemove = m2Count > 1
                        )

                        // 3. الفصل الأول (Amber Group)
                        PaperHeaderCell("فص1", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFFB45309))
                        
                        // 4. نصف السنة (Teal Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = "نصف السنة", 
                                width = cellW * (midtermOralCount + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = Color(0xFF0F766E),
                                onAdd = { if (midtermOralCount < 5) midtermOralCount++ }, 
                                onRemove = { if (midtermOralCount > 1) midtermOralCount-- },
                                canAdd = midtermOralCount < 5,
                                canRemove = midtermOralCount > 1
                            )
                        } else {
                            PaperHeaderCell("نصف السنة", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF0F766E))
                        }
                        
                        // 5. الشهر الثالث (Blue Group)
                        PaperHeaderCellWithAdd(
                            text = "الشهر الثالث", 
                            width = cellW * (m3Count + 3), 
                            height = rowH, 
                            fontSize = fontSize,
                            textColor = Color.White,
                            backgroundColor = Color(0xFF1E3A8A),
                            onAdd = { if (m3Count < 5) m3Count++ }, 
                            onRemove = { if (m3Count > 1) m3Count-- },
                            canAdd = m3Count < 5,
                            canRemove = m3Count > 1
                        )

                        // 6. الشهر الرابع (Purple Group)
                        PaperHeaderCellWithAdd(
                            text = "الشهر الرابع", 
                            width = cellW * (m4Count + 3), 
                            height = rowH, 
                            fontSize = fontSize,
                            textColor = Color.White,
                            backgroundColor = Color(0xFF5B21B6),
                            onAdd = { if (m4Count < 5) m4Count++ }, 
                            onRemove = { if (m4Count > 1) m4Count-- },
                            canAdd = m4Count < 5,
                            canRemove = m4Count > 1
                        )

                        // 7. الفصل الثاني والسعي (Amber & Orange Groups)
                        PaperHeaderCell("فص2", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFFB45309))
                        PaperHeaderCell("السعي", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFFC2410C))
                        
                        // 8. الامتحان النهائي (Emerald Group)
                        if (isSpecial) {
                            PaperHeaderCellWithAdd(
                                text = "الامتحان النهائي", 
                                width = cellW * (finalOralCount + 3), 
                                height = rowH, 
                                fontSize = fontSize,
                                textColor = Color.White,
                                backgroundColor = Color(0xFF047857),
                                onAdd = { if (finalOralCount < 5) finalOralCount++ }, 
                                onRemove = { if (finalOralCount > 1) finalOralCount-- },
                                canAdd = finalOralCount < 5,
                                canRemove = finalOralCount > 1
                            )
                        } else {
                            PaperHeaderCell("الامتحان النهائي", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF047857))
                        }
                        
                        // 9. د2 والدرجة النهائية
                        PaperHeaderCell("د2", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF475569))
                        PaperHeaderCell("الدرجة النهائية", cellW, rowH, fontSize, textColor = Color.White, backgroundColor = Color(0xFF4338CA))
                    }
                    
                    // Row 2: Sub-headers Under Each Month / Section
                    Row {
                        // Month 1 Subheaders
                        repeat(m1Count) { i ->
                            val colName = settings.columnNames.getOrElse(i) { "يومية ${i+1}" }
                            EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE)) { onEditHeader(i) }
                        }
                        PaperHeaderCell("م.يومي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("معدل ش1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFBFDBFE), textColor = Color(0xFF1E3A8A))
                        
                        // Month 2 Subheaders
                        repeat(m2Count) { i ->
                            val colName = settings.columnNames.getOrElse(5 + i) { "يومية ${i+1}" }
                            EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE)) { onEditHeader(5 + i) }
                        }
                        PaperHeaderCell("م.يومي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("معدل ش2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDDD6FE), textColor = Color(0xFF5B21B6))
                        
                        // Term 1 Subheader
                        PaperHeaderCell("معدل ف1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFFEF3C7), textColor = Color(0xFFB45309))
                        
                        // Midterm Subheaders
                        if (isSpecial) {
                            repeat(midtermOralCount) { i ->
                                PaperHeaderCell("شفهي ${i+1}", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFCCFBF1), textColor = Color(0xFF0F766E))
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFCCFBF1), textColor = Color(0xFF0F766E))
                            PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFCCFBF1), textColor = Color(0xFF0F766E))
                            PaperHeaderCell("درجة نصف", cellW, rowH, labelFontSize, backgroundColor = Color(0xFF99F6E4), textColor = Color(0xFF0F766E))
                        } else {
                            PaperHeaderCell("درجة نصف", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFCCFBF1), textColor = Color(0xFF0F766E))
                        }
                        
                        // Month 3 Subheaders
                        repeat(m3Count) { i ->
                            val colName = settings.columnNames.getOrElse(10 + i) { "يومية ${i+1}" }
                            EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE)) { onEditHeader(10 + i) }
                        }
                        PaperHeaderCell("م.يومي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E3A8A))
                        PaperHeaderCell("معدل ش3", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFBFDBFE), textColor = Color(0xFF1E3A8A))
                        
                        // Month 4 Subheaders
                        repeat(m4Count) { i ->
                            val colName = settings.columnNames.getOrElse(15 + i) { "يومية ${i+1}" }
                            EditableHeaderCell(colName, cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE)) { onEditHeader(15 + i) }
                        }
                        PaperHeaderCell("م.يومي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("تحريري", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFEDE9FE), textColor = Color(0xFF5B21B6))
                        PaperHeaderCell("معدل ش4", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFDDD6FE), textColor = Color(0xFF5B21B6))
                        
                        // Term 2 & Annual Subheaders
                        PaperHeaderCell("معدل ف2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFFEF3C7), textColor = Color(0xFFB45309))
                        PaperHeaderCell("السعي السنوي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFFFEDD5), textColor = Color(0xFFC2410C))
                        
                        // Final Exam Subheaders
                        if (isSpecial) {
                            repeat(finalOralCount) { i ->
                                PaperHeaderCell("شفهي ${i+1}", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF047857))
                            }
                            PaperHeaderCell("م.شفهي", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF047857))
                            PaperHeaderCell("تحريري د1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF047857))
                            PaperHeaderCell("مجموع د1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFA7F3D0), textColor = Color(0xFF047857))
                        } else {
                            PaperHeaderCell("تحريري د1", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFD1FAE5), textColor = Color(0xFF047857))
                        }
                        
                        // Round 2 & Final Grade Subheaders
                        PaperHeaderCell("امتحان د2", cellW, rowH, labelFontSize, backgroundColor = Color(0xFFF1F5F9), textColor = Color(0xFF475569))
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
                        fontSize = fontSize, 
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
                        repeat(m1Count) { i ->
                            val v = m.m1Daily.getOrElse(i) { 0f }
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable) { valNew -> 
                                val nl = m.m1Daily.toMutableList()
                                while (nl.size < 5) nl.add(0f)
                                nl[i] = valNew
                                onUpdateMarks(std, m.copy(m1Daily = nl)) 
                            }
                        }
                        PaperTableCell(m.m1Daily.sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperInputCell(m.m1Written, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(m1Written = it)) }
                        PaperTableCell(m.m1MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFDBEAFE).copy(alpha = 0.5f))
                        
                        // Month 2 Data Cells
                        repeat(m2Count) { i ->
                            val v = m.m2Daily.getOrElse(i) { 0f }
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable) { valNew -> 
                                val nl = m.m2Daily.toMutableList()
                                while (nl.size < 5) nl.add(0f)
                                nl[i] = valNew
                                onUpdateMarks(std, m.copy(m2Daily = nl)) 
                            }
                        }
                        PaperTableCell(m.m2Daily.sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperInputCell(m.m2Written, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(m2Written = it)) }
                        PaperTableCell(m.m2MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFEDE9FE).copy(alpha = 0.5f))
                        
                        // Term 1 Data Cell
                        PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFFEF3C7).copy(alpha = 0.5f))
                        
                        // Midterm Data Cells
                        if (isSpecial) {
                            repeat(midtermOralCount) { i ->
                                val v = m.midtermOral.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable) { valNew -> 
                                    val nl = m.midtermOral.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(midtermOral = nl)) 
                                }
                            }
                            PaperTableCell(m.midtermOral.sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.midtermScore, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(midtermScore = it)) }
                            PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFCCFBF1).copy(alpha = 0.5f))
                        } else {
                            PaperInputCell(m.midtermScore, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(midtermScore = it, midtermFinalGrade = it)) }
                        }
                        
                        // Month 3 Data Cells
                        repeat(m3Count) { i ->
                            val v = m.m3Daily.getOrElse(i) { 0f }
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable) { valNew -> 
                                val nl = m.m3Daily.toMutableList()
                                while (nl.size < 5) nl.add(0f)
                                nl[i] = valNew
                                onUpdateMarks(std, m.copy(m3Daily = nl)) 
                            }
                        }
                        PaperTableCell(m.m3Daily.sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperInputCell(m.m3Written, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(m3Written = it)) }
                        PaperTableCell(m.m3MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFDBEAFE).copy(alpha = 0.5f))
                        
                        // Month 4 Data Cells
                        repeat(m4Count) { i ->
                            val v = m.m4Daily.getOrElse(i) { 0f }
                            PaperInputCell(v, cellW, rowH, fontSize, isEditable) { valNew -> 
                                val nl = m.m4Daily.toMutableList()
                                while (nl.size < 5) nl.add(0f)
                                nl[i] = valNew
                                onUpdateMarks(std, m.copy(m4Daily = nl)) 
                            }
                        }
                        PaperTableCell(m.m4Daily.sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                        PaperInputCell(m.m4Written, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(m4Written = it)) }
                        PaperTableCell(m.m4MonthAvg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFEDE9FE).copy(alpha = 0.5f))
                        
                        // Term 2 & Annual Average Data Cells
                        PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFFEF3C7).copy(alpha = 0.5f))
                        PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFFFEDD5).copy(alpha = 0.5f))
                        
                        // Final Exam Data Cells
                        if (isSpecial) {
                            repeat(finalOralCount) { i ->
                                val v = m.finalOral.getOrElse(i) { 0f }
                                PaperInputCell(v, cellW, rowH, fontSize, isEditable) { valNew -> 
                                    val nl = m.finalOral.toMutableList()
                                    while (nl.size < 5) nl.add(0f)
                                    nl[i] = valNew
                                    onUpdateMarks(std, m.copy(finalOral = nl)) 
                                }
                            }
                            PaperTableCell(m.finalOral.sum().toInt().toString(), cellW, rowH, fontSize, backgroundColor = rowBg)
                            PaperInputCell(m.finalWrittenD1, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(finalWrittenD1 = it)) }
                            PaperTableCell(m.finalExamTotal.toInt().toString(), cellW, rowH, fontSize, isBold = true, backgroundColor = Color(0xFFD1FAE5).copy(alpha = 0.5f))
                        } else {
                            PaperInputCell(m.finalWrittenD1, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(finalWrittenD1 = it)) }
                        }
                        
                        // Round 2 & Final Grade Data Cells
                        PaperInputCell(m.finalWrittenD2 ?: 0f, cellW, rowH, fontSize, isEditable) { onUpdateMarks(std, m.copy(finalWrittenD2 = if (it == 0f) null else it)) }
                        
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
    val borderColor = currentTheme.tableBorderColor

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, borderColor)
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
    val effectiveTextColor = if (currentTheme.isDark) {
        if (textColor == Color(0xFFDC2626) || textColor == Color.Red) Color(0xFFF87171)
        else currentTheme.textPrimaryColor
    } else {
        textColor ?: Color(0xFF0F172A)
    }
    val effectiveBg = if (currentTheme.isDark) {
        if (backgroundColor == currentTheme.tableAltCellBg) currentTheme.tableAltCellBg
        else currentTheme.tableCellBg
    } else {
        backgroundColor ?: currentTheme.tableCellBg
    }
    val borderColor = currentTheme.tableBorderColor

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
            .border(0.5.dp, borderColor)
            .background(effectiveBg)
            .then(clickModifier)
            .padding(start = 3.dp, end = 2.dp),
        contentAlignment = if (textAlign == TextAlign.Start || textAlign == TextAlign.Right) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Medium,
            color = effectiveTextColor,
            textAlign = textAlign,
            maxLines = 2,
            softWrap = true,
            lineHeight = (fontSize.value + 2.5f).sp
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
    var text by remember(value) { mutableStateOf(if(value == 0f) "" else value.toInt().toString()) }
    val borderColor = currentTheme.tableBorderColor
    val cellBg = currentTheme.tableCellBg
    val textColor = currentTheme.textPrimaryColor
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, borderColor)
            .background(cellBg)
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
    val nameW = 115.dp
    val seqW = 28.dp
    val rowH = 44.dp
    val fontSize = 10.5.sp

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
                PaperHeaderCell("اسم الطالب", nameW, 40.dp, fontSize)

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
                            fontSize = fontSize, 
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

