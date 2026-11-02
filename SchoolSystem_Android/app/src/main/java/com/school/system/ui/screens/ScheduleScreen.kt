package com.school.system.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.school.system.data.SyncManager
import com.school.system.data.model.ClassPackage
import com.school.system.ui.theme.LocalAppTheme
import com.school.system.widget.WidgetScheduleHelper
import kotlinx.coroutines.launch

data class TeacherLessonInfo(
    val day: String,
    val lessonNumber: Int,
    val className: String,
    val subject: String,
    val teacherName: String
)

fun parseGradeAndSection(rawCls: String): Pair<String, String> {
    var text = rawCls.trim().replace("الصف ", "").replace("صف ", "")
    var section = ""
    val regex = "\\((.*?)\\)".toRegex()
    val match = regex.find(text)
    if (match != null) {
        section = match.groupValues[1].trim()
        text = text.replace(match.value, "").trim()
    } else {
        val parts = text.split(" ").filter { it.isNotBlank() }
        if (parts.size > 1) {
            section = parts.last().trim()
            text = parts.dropLast(1).joinToString(" ").trim()
        }
    }
    val grade = text.ifBlank { rawCls }
    val sec = section.ifBlank { "أ" }
    return grade to sec
}

fun normalizeArabicSchedule(str: String): String {
    return str.trim()
        .replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
        .replace("ـ", "")
        .replace("  ", " ")
        .lowercase()
}

fun isRosterDownloadedForLesson(
    className: String,
    subject: String,
    packages: List<ClassPackage>
): Boolean {
    if (packages.isEmpty()) return false
    val (grade, section) = parseGradeAndSection(className)
    val normG = normalizeArabicSchedule(grade)
    val normS = normalizeArabicSchedule(section)
    val normSubj = normalizeArabicSchedule(WidgetScheduleHelper.cleanSubjectName(subject))

    return packages.any { pkg ->
        val pkgG = normalizeArabicSchedule(pkg.grade)
        val pkgS = normalizeArabicSchedule(pkg.section)
        val pkgSubj = normalizeArabicSchedule(WidgetScheduleHelper.cleanSubjectName(pkg.subject))

        val gradeMatches = pkgG.contains(normG) || normG.contains(pkgG)
        val sectionMatches = pkgS.isEmpty() || normS.isEmpty() || pkgS == normS
        val subjectMatches = normSubj.isEmpty() || pkgSubj.isEmpty() || pkgSubj == normSubj || pkgSubj.contains(normSubj) || normSubj.contains(pkgSubj)

        gradeMatches && sectionMatches && subjectMatches
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    syncManager: SyncManager,
    onBack: () -> Unit,
    onNavigateToGrades: ((grade: String, section: String, subject: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val currentTheme = LocalAppTheme.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedLessonForInspection by remember { mutableStateOf<TeacherLessonInfo?>(null) }

    // Read synced schedule from SharedPreferences
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    var rawScheduleJson by remember { mutableStateOf(prefs.getString("synced_schedule", "{}") ?: "{}") }

    // 0: جدولي الخاص (Personal Table & Cards), 1: الجدول العام لجميع الصفوف (School General Grid)
    var selectedModeTab by remember { mutableIntStateOf(0) }

    // Display Format for Personal Schedule: 0: جدول أسبوعي شبكي (Matrix Table), 1: بطاقات يومية (Cards)
    var personalViewType by remember { mutableIntStateOf(0) }
    var isFullscreenTable by remember { mutableStateOf(false) }

    // Teacher Name for Personal Schedule filter
    var teacherNameInput by remember {
        mutableStateOf(prefs.getString("teacher_name", "") ?: "")
    }

    val localPackages by syncManager.packageDao.getAllPackages().collectAsState(initial = emptyList())
    var showTeacherPicker by remember { mutableStateOf(false) }
    var teacherSearchQuery by remember { mutableStateOf("") }

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val gson = remember { Gson() }

    var timingRefreshTrigger by remember { mutableIntStateOf(0) }
    var showTimingsDialog by remember { mutableStateOf(false) }

    // Dynamic schedule timings (configured by principal in desktop system or customized locally)
    val timingInfo = remember(rawScheduleJson, timingRefreshTrigger) {
        WidgetScheduleHelper.getFullTimingFromScheduleJson(context)
    }
    val timingStartHour = timingInfo.startHour
    val timingLessonDur = timingInfo.lessonDuration
    val timingBreakDur = timingInfo.breakDuration
    val customLessonDurations = timingInfo.customLessons
    val customBreakDurations = timingInfo.customBreaks
    
    // Parse structured schedule items
    val parsedScheduleByDay: Map<String, List<TeacherLessonInfo>> = remember(rawScheduleJson) {
        val result = mutableMapOf<String, MutableList<TeacherLessonInfo>>()
        daysList.forEach { result[it] = mutableListOf() }

        try {
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            if (rootObj != null) {
                for (day in daysList) {
                    val dayData = rootObj[day]
                    if (dayData is List<*>) {
                        for (row in dayData) {
                            if (row is Map<*, *>) {
                                val grade = row["grade"]?.toString()?.trim().orEmpty()
                                val section = row["section"]?.toString()?.trim().orEmpty()
                                val className = row["className"]?.toString()?.trim()
                                    ?.takeIf { it.isNotEmpty() }
                                    ?: listOf(grade, section)
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" ")
                                val lessons = row["lessons"] as? Map<*, *>
                                if (lessons != null) {
                                    for (i in 1..7) {
                                        val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                        val subj = lessonObj?.get("subject")?.toString() ?: ""
                                        val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                        val isOff = lessonObj?.get("isOff") as? Boolean ?: false
                                        if (!isOff && (subj.isNotEmpty() || teacher.isNotEmpty())) {
                                            result[day]?.add(
                                                TeacherLessonInfo(
                                                    day = day,
                                                    lessonNumber = i,
                                                    className = className,
                                                    subject = subj,
                                                    teacherName = teacher
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (dayData is Map<*, *>) {
                        for ((clsKey, lessonsList) in dayData) {
                            val className = clsKey.toString()
                            if (lessonsList is List<*>) {
                                lessonsList.forEachIndexed { idx, item ->
                                    val itemStr = item?.toString() ?: ""
                                    if (itemStr.isNotEmpty() && itemStr != "-") {
                                        result[day]?.add(
                                            TeacherLessonInfo(
                                                day = day,
                                                lessonNumber = idx + 1,
                                                className = className,
                                                subject = itemStr,
                                                teacherName = ""
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    // Filter personal lessons for teacher
    val myPersonalLessons: List<TeacherLessonInfo> = remember(parsedScheduleByDay, teacherNameInput) {
        val query = teacherNameInput.trim().lowercase()
        val allLessons = parsedScheduleByDay.values.flatten()
        if (query.isEmpty()) {
            allLessons
        } else {
            allLessons.filter {
                val cleanTeacher = WidgetScheduleHelper.cleanTeacherFirstName(it.teacherName).lowercase()
                val cleanSubj = WidgetScheduleHelper.cleanSubjectName(it.subject).lowercase()
                it.teacherName.lowercase().contains(query) ||
                it.subject.lowercase().contains(query) ||
                cleanTeacher.contains(query) ||
                cleanSubj.contains(query)
            }
        }
    }

    // Available classes for general schedule selector
    val availableClasses = remember(parsedScheduleByDay) {
        parsedScheduleByDay.values.flatten()
            .map { it.className }
            .distinct()
            .sortedWith { c1, c2 -> WidgetScheduleHelper.compareClassNames(c1, c2) }
    }

    val allTeachers = remember(parsedScheduleByDay) {
        parsedScheduleByDay.values.flatten()
            .map { it.teacherName.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    var selectedGeneralDay by remember { mutableStateOf("الأحد") }
    val lazyListState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 25 }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentTheme.backgroundColor)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // TopBar - Collapsible on Scroll
                AnimatedVisibility(
                    visible = !isScrolled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = currentTheme.primaryColor,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isLandscape) 34.dp else 46.dp)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(if (isLandscape) 28.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack, 
                                        contentDescription = "رجوع", 
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isLandscape) 17.dp else 20.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "الجدول المدرسي 📅",
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isLandscape) 12.5.sp else 14.sp,
                                    color = Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showTimingsDialog = true },
                                    modifier = Modifier.size(if (isLandscape) 28.dp else 34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = "تخصيص أوقات الحصص والفرص ⏱️",
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(if (isLandscape) 17.dp else 20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isRefreshing = true
                                        coroutineScope.launch {
                                            val success = syncManager.downloadSchedule(context)
                                            isRefreshing = false
                                            if (success) {
                                                rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                                                timingRefreshTrigger++
                                                Toast.makeText(context, "تمت مزامنة الجدول وتحديث الودجت بنجاح! ⚡", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "تعذر الاتصال بالسحابة أو لم يتم رفع جدول جديد بعد", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isRefreshing,
                                    modifier = Modifier.size(if (isLandscape) 28.dp else 34.dp)
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = "مزامنة السحابة ⚡",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isLandscape) 17.dp else 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Teacher Selection Bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = currentTheme.surfaceColor,
                    border = BorderStroke(1.dp, currentTheme.tableBorderColor),
                    shadowElevation = 1.dp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = if (isLandscape) 2.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).clickable { showTeacherPicker = true }
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (teacherNameInput.isNotBlank()) currentTheme.primaryColor else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "المعلم:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = currentTheme.textSecondaryColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (teacherNameInput.isNotBlank()) teacherNameInput else "الجدول العام (اختر معلماً لتظليل حصصه)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (teacherNameInput.isNotBlank()) currentTheme.primaryColor else Color.Gray,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (teacherNameInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        teacherNameInput = ""
                                        prefs.edit().remove("teacher_name").apply()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "إلغاء التحديد",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = { showTeacherPicker = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("تحديد المعلم ▾", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Day Selector Bar (الأحد -> الخميس) - Fixed/Pinned at Top Edge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = currentTheme.surfaceColor,
                    border = BorderStroke(1.dp, currentTheme.tableBorderColor),
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        daysList.forEach { day ->
                            val isSelected = selectedGeneralDay == day
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) currentTheme.primaryColor else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedGeneralDay = day }
                            ) {
                                Text(
                                    text = day,
                                    modifier = Modifier.padding(vertical = if (isLandscape) 3.dp else 6.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = if (isLandscape) 11.sp else 12.sp,
                                    color = if (isSelected) Color.White else currentTheme.textPrimaryColor
                                )
                            }
                        }
                    }
                }

                // Full-Screen Main Schedule Table directly
                Box(modifier = Modifier.weight(1f)) {
                    GeneralDayScheduleGrid(
                        day = selectedGeneralDay,
                        classes = availableClasses,
                        allLessons = parsedScheduleByDay[selectedGeneralDay] ?: emptyList(),
                        teacherName = teacherNameInput,
                        localPackages = localPackages,
                        lazyListState = lazyListState,
                        startHourStr = timingStartHour,
                        lessonDuration = timingLessonDur,
                        breakDuration = timingBreakDur,
                        customLessons = customLessonDurations,
                        customBreaks = customBreakDurations,
                        onLessonSelect = { lesson ->
                            selectedLessonForInspection = lesson
                        }
                    )
                }
            }
        }

        // Supervisor / Teacher Lesson Inspection Dialog
        selectedLessonForInspection?.let { lesson ->
            val (parsedGrade, parsedSection) = parseGradeAndSection(lesson.className)
            var isSyncingLesson by remember { mutableStateOf(false) }
            val isRosterDownloaded = remember(lesson, localPackages) {
                isRosterDownloadedForLesson(lesson.className, lesson.subject, localPackages)
            }
            val breakAfter = if (lesson.lessonNumber < 6) (customBreakDurations.getOrNull(lesson.lessonNumber - 1) ?: timingBreakDur) else null

            AlertDialog(
                onDismissRequest = { selectedLessonForInspection = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏫", fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("استعراض وتدقيق الحصة 🏫", fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text("الدرس ${lesson.lessonNumber} | مادة ${lesson.subject}", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Class & Subject Details
                        Surface(
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("الصف والشعبة: $parsedGrade ($parsedSection)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0369A1))
                                Text("المادة الدراسية: ${lesson.subject}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                                if (lesson.teacherName.isNotBlank()) {
                                    Text("أستاذ المادة: ${lesson.teacherName}", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color(0xFF475569))
                                }
                            }
                        }

                        // Break or Merge Indicator Card (علامة الدمج أو الفرصة في البطاقة)
                        if (breakAfter == 0) {
                            Surface(
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔗", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("حصة مدمجة (استراحة 0 دقيقة) 🔗", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF6B21A8))
                                        Text("الدرسان متصلان ويبدأ الدرس التالي فوراً بدون استراحة.", fontSize = 10.5.sp, color = Color(0xFF7E22CE))
                                    }
                                }
                            }
                        } else if ((breakAfter ?: 0) > 0) {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("☕", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("تلي الحصة فرصة / استراحة ($breakAfter دقيقة) ☕", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF92400E))
                                        Text("فترة استراحة بين الحصة ${lesson.lessonNumber} والحصة ${lesson.lessonNumber + 1}.", fontSize = 10.5.sp, color = Color(0xFFB45309))
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🏁", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("الحصة الأخيرة في الدوام 🏁", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF166534))
                                        Text("نهاية الدوام المدرسي وانصراف التلاميذ.", fontSize = 10.5.sp, color = Color(0xFF15803D))
                                    }
                                }
                            }
                        }

                        // Grade Register Local Status Card (حالة سجل الدرجات)
                        if (isRosterDownloaded) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📋✓", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("سجل الدرجات متوفر محلياً وجاهز للرصد ✓", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF065F46))
                                        Text("تم تنزيل أسماء ودرجات هذا الصف مسبقاً على جهازك.", fontSize = 10.5.sp, color = Color(0xFF047857))
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("☁️", fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("سجل درجات هذا الصف غير منزل بعد", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                                        Text("اضغط على الزر أدناه لتنزيل ومزامنة درجات هذا الصف من السحابة.", fontSize = 10.5.sp, color = Color(0xFF2563EB))
                                    }
                                }
                            }
                        }

                        Text("اختر الإجراء المطلوب لهذا الدرس:", fontSize = 11.5.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Button 1: فتح سجل الأسماء والدرجات
                        Button(
                            onClick = {
                                val targetGrade = parsedGrade
                                val targetSec = parsedSection
                                val targetSubj = lesson.subject
                                selectedLessonForInspection = null
                                if (onNavigateToGrades != null) {
                                    onNavigateToGrades(targetGrade, targetSec, targetSubj)
                                } else {
                                    Toast.makeText(context, "فتح سجل $targetGrade ($targetSec) - $targetSubj", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRosterDownloaded) Color(0xFF059669) else Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isRosterDownloaded) "فتح سجل الأسماء والدرجات المخزونة 📋✓" else "فتح سجل الأسماء والدرجات 📋",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Button 2: تحديث ومزامنة الدرجات من السحابة
                        OutlinedButton(
                            onClick = {
                                isSyncingLesson = true
                                coroutineScope.launch {
                                    val ok = syncManager.downloadSimpleRosterForClass(parsedGrade, parsedSection, lesson.subject)
                                    isSyncingLesson = false
                                    if (ok) {
                                        Toast.makeText(context, "تم تحديث ومزامنة درجات $parsedGrade ($parsedSection) من السحابة بنجاح! ⚡", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تعذر التحديث من السحابة. تأكد من توفر الاتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSyncingLesson,
                            border = BorderStroke(1.2.dp, Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncingLesson) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF0EA5E9))
                                Spacer(Modifier.width(6.dp))
                                Text("جاري المزامنة...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تحديث ومزامنة الدرجات من السحابة 🔄", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedLessonForInspection = null }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // Teacher Picker Dialog
        if (showTeacherPicker) {
            AlertDialog(
                onDismissRequest = { showTeacherPicker = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = currentTheme.primaryColor)
                        Spacer(Modifier.width(8.dp))
                        Text("اختر المعلم لتظليل حصصه 👨‍🏫", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = teacherSearchQuery,
                            onValueChange = { teacherSearchQuery = it },
                            placeholder = { Text("بحث عن اسم المعلم...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        val filteredTeachers = remember(allTeachers, teacherSearchQuery) {
                            if (teacherSearchQuery.isBlank()) allTeachers
                            else allTeachers.filter { it.contains(teacherSearchQuery.trim(), ignoreCase = true) }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (teacherNameInput.isEmpty()) currentTheme.primaryColor.copy(alpha = 0.15f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (teacherNameInput.isEmpty()) currentTheme.primaryColor else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        teacherNameInput = ""
                                        prefs.edit().remove("teacher_name").apply()
                                        showTeacherPicker = false
                                    }
                                ) {
                                    Text(
                                        text = "الكل (إلغاء التحديد وتظليل الجدول العام)",
                                        modifier = Modifier.padding(10.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (teacherNameInput.isEmpty()) currentTheme.primaryColor else Color(0xFF475569)
                                    )
                                }
                            }

                            items(filteredTeachers) { teacher ->
                                val isSelected = (teacher == teacherNameInput)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) currentTheme.primaryColor.copy(alpha = 0.15f) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) currentTheme.primaryColor else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        teacherNameInput = teacher
                                        prefs.edit().putString("teacher_name", teacher).apply()
                                        showTeacherPicker = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = teacher,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = if (isSelected) currentTheme.primaryColor else Color(0xFF1E293B)
                                        )
                                        if (isSelected) {
                                            Text("✓ محدد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = currentTheme.primaryColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTeacherPicker = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        if (showTimingsDialog) {
            ScheduleTimingsDialog(
                initialStartHour = timingStartHour,
                initialLessonDur = timingLessonDur,
                initialBreakDur = timingBreakDur,
                initialCustomLessons = customLessonDurations,
                initialCustomBreaks = customBreakDurations,
                onDismiss = { showTimingsDialog = false },
                onSave = { newStart, newLessons, newBreaks ->
                    saveTimingPreferences(context, newStart, newLessons, newBreaks) {
                        showTimingsDialog = false
                        timingRefreshTrigger++
                        rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                        Toast.makeText(context, "تم حفظ وتطبيق التوقيتات وتحديث الودجت بنجاح! ⏱️", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

/**
 * جدول الحصص الأسبوعي الخاص بالمعلم (يبدأ من اليمين: اليوم ثم الحصص 1 إلى 6 بالتوقيت)
 */
@Composable
fun WeeklyPersonalScheduleTable(
    daysList: List<String>,
    lessons: List<TeacherLessonInfo>,
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10,
    customLessons: List<Int> = emptyList(),
    customBreaks: List<Int> = emptyList(),
    localPackages: List<ClassPackage> = emptyList(),
    onLessonSelect: ((TeacherLessonInfo) -> Unit)? = null
) {
    val currentTheme = LocalAppTheme.current
    val hScroll = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val totalAvailableW = this.maxWidth
        val minDayColW = 80.dp
        val minLessonColW = 105.dp
        val totalMinW = minDayColW + (minLessonColW * 6)

        val (dayColW, lessonColW) = if (totalAvailableW > totalMinW) {
            val lessonW = (totalAvailableW - minDayColW) / 6
            minDayColW to lessonW
        } else {
            minDayColW to minLessonColW
        }

        val headerH = 50.dp
        val rowH = 60.dp

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = currentTheme.surfaceColor,
            border = BorderStroke(1.dp, currentTheme.tableBorderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Fixed Days Column on Right
                Column(
                    modifier = Modifier
                        .width(dayColW)
                        .border(
                            BorderStroke(
                                0.5.dp,
                                currentTheme.tableBorderColor
                            )
                        )
                ) {
                    // Header Cell for Days
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerH)
                            .background(currentTheme.tableHeaderBg)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اليوم 📅",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = currentTheme.textSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Day Cells
                    daysList.forEachIndexed { index, day ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowH)
                                .background(if (index % 2 == 0) currentTheme.surfaceColor else currentTheme.tableAltCellBg)
                                .border(
                                    BorderStroke(
                                        0.5.dp,
                                        currentTheme.tableBorderColor.copy(alpha = 0.5f)
                                    )
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = currentTheme.primaryColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 2. Scrollable Lessons Matrix on Left
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(hScroll)
                ) {
                    // Header Row: Lessons 1 to 6
                    Row(
                        modifier = Modifier.background(currentTheme.tableHeaderBg)
                    ) {
                        for (i in 1..6) {
                            val bAfter = if (i < 6) (customBreaks.getOrNull(i - 1) ?: breakDuration) else null
                            val bBefore = if (i > 1) (customBreaks.getOrNull(i - 2) ?: breakDuration) else null
                            val isMergedWithNext = (bAfter == 0)
                            val isMergedWithPrev = (bBefore == 0)
                            ScheduleHeaderCellWithTiming(
                                lessonTitle = "الدرس $i",
                                timing = getLessonShortTiming(i, startHourStr, lessonDuration, breakDuration, customLessons, customBreaks),
                                isMergedWithNext = isMergedWithNext,
                                isMergedWithPrev = isMergedWithPrev,
                                width = lessonColW,
                                height = headerH
                            )
                        }
                    }

                    // Days Rows
                    daysList.forEachIndexed { index, day ->
                        val dayLessonsMap = remember(lessons, day) {
                            lessons.filter { it.day == day }.associateBy { it.lessonNumber }
                        }
                        val isEven = index % 2 == 0
                        val rowBg = if (isEven) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                        Row(
                            modifier = Modifier.background(rowBg)
                        ) {
                            for (i in 1..6) {
                                val item = dayLessonsMap[i]
                                val isRosterDownloaded = remember(item, localPackages) {
                                    if (item != null && localPackages.isNotEmpty()) {
                                        isRosterDownloadedForLesson(item.className, item.subject, localPackages)
                                    } else false
                                }
                                val breakAfter = if (i < 6) (customBreaks.getOrNull(i - 1) ?: breakDuration) else null
                                val isMerged = (breakAfter == 0)

                                val cellBoxColor = when {
                                    item != null && isRosterDownloaded -> {
                                        if (currentTheme.isDark) Color(0xFF064E3B).copy(alpha = 0.75f) else Color(0xFFDCFCE7)
                                    }
                                    item != null -> {
                                        if (currentTheme.isDark) Color(0xFF1E3A8A).copy(alpha = 0.65f) else Color(0xFFDBEAFE)
                                    }
                                    else -> if (isEven) currentTheme.tableCellBg else currentTheme.tableAltCellBg
                                }

                                val cellBorderColor = when {
                                    item != null && isRosterDownloaded -> Color(0xFF059669)
                                    item != null -> Color(0xFF2563EB)
                                    else -> currentTheme.tableBorderColor.copy(alpha = 0.4f)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = cellBoxColor,
                                    border = BorderStroke(if (item != null) 1.5.dp else 0.5.dp, cellBorderColor),
                                    modifier = Modifier
                                        .width(lessonColW)
                                        .height(rowH)
                                        .padding(1.5.dp)
                                        .clickable(enabled = (item != null && onLessonSelect != null)) {
                                            if (item != null && onLessonSelect != null) {
                                                onLessonSelect(item)
                                            }
                                        }
                                ) {
                                    if (item != null) {
                                        val cleanSubj = WidgetScheduleHelper.cleanSubjectName(item.subject)
                                        val cleanCls = WidgetScheduleHelper.cleanClassName(item.className)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = cleanSubj.ifEmpty { "درس مقرر" },
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.5.sp,
                                                color = if (isRosterDownloaded) Color(0xFF047857) else Color(0xFF1D4ED8),
                                                maxLines = 1,
                                                softWrap = false,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.height(1.5.dp))
                                            Text(
                                                text = cleanCls,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = currentTheme.textPrimaryColor.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                softWrap = false,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "شاغر",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * جدول المدرسة العام لكافة الصفوف والشعب في اليوم المحدد (مرتب من اليمين لليسار)
 */
@Composable
fun GeneralDayScheduleGrid(
    day: String,
    classes: List<String>,
    allLessons: List<TeacherLessonInfo>,
    teacherName: String = "",
    localPackages: List<ClassPackage> = emptyList(),
    lazyListState: LazyListState = rememberLazyListState(),
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10,
    customLessons: List<Int> = emptyList(),
    customBreaks: List<Int> = emptyList(),
    onLessonSelect: ((TeacherLessonInfo) -> Unit)? = null
) {
    val currentTheme = LocalAppTheme.current
    val hScroll = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    val activeTeacher = teacherName.ifBlank { prefs.getString("teacher_name", "")?.trim() ?: "" }

    if (classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد بيانات جدول لصفوف المدرسة", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalAvailableW = this.maxWidth
        val minClassColW = if (isLandscape) 80.dp else 100.dp
        val minLessonColW = if (isLandscape) 95.dp else 110.dp
        val totalMinW = minClassColW + (minLessonColW * 6)

        val (classColW, lessonColW) = if (totalAvailableW > totalMinW) {
            val lessonW = (totalAvailableW - minClassColW) / 6
            minClassColW to lessonW
        } else {
            minClassColW to minLessonColW
        }

        val rowH = if (isLandscape) 40.dp else 56.dp
        val headerH = if (isLandscape) 46.dp else 56.dp

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = currentTheme.surfaceColor,
            border = BorderStroke(1.dp, currentTheme.tableBorderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: الصف والشعبة على اليمين ثم الحصص 1 إلى 6 بالتوقيت وعلامة الدمج/الفرصة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentTheme.tableHeaderBg)
                        .border(0.5.dp, currentTheme.tableBorderColor)
                ) {
                    ScheduleHeaderCell("الصف / الشعبة 🏫", classColW, headerH)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(hScroll)
                    ) {
                        for (i in 1..6) {
                            val bAfter = if (i < 6) (customBreaks.getOrNull(i - 1) ?: breakDuration) else null
                            val bBefore = if (i > 1) (customBreaks.getOrNull(i - 2) ?: breakDuration) else null
                            val isMergedWithNext = (bAfter == 0)
                            val isMergedWithPrev = (bBefore == 0)
                            ScheduleHeaderCellWithTiming(
                                lessonTitle = "الدرس $i",
                                timing = getLessonShortTiming(i, startHourStr, lessonDuration, breakDuration, customLessons, customBreaks),
                                isMergedWithNext = isMergedWithNext,
                                isMergedWithPrev = isMergedWithPrev,
                                width = lessonColW,
                                height = headerH
                            )
                        }
                    }
                }

                // Rows for each class
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = if (isLandscape) 80.dp else 40.dp)
                ) {
                    items(classes) { clsName ->
                        val classLessonsMap = remember(allLessons, clsName) {
                            allLessons.filter { it.className == clsName }.associateBy { it.lessonNumber }
                        }
                        val isEven = classes.indexOf(clsName) % 2 == 0
                        val rowBg = if (isEven) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .border(0.5.dp, currentTheme.tableBorderColor)
                        ) {
                            // Class Label on Right
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentTheme.primaryColor.copy(alpha = 0.08f),
                                border = BorderStroke(0.5.dp, currentTheme.tableBorderColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .width(classColW)
                                    .height(rowH)
                                    .padding(1.5.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = WidgetScheduleHelper.cleanClassName(clsName),
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (isLandscape) 10.5.sp else 11.5.sp,
                                        color = currentTheme.textPrimaryColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }

                            // Lessons 1 to 6
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(hScroll)
                            ) {
                                for (i in 1..6) {
                                    val item = classLessonsMap[i]
                                    val isTeacherLesson = remember(item, activeTeacher) {
                                        if (item != null && activeTeacher.isNotEmpty()) {
                                            WidgetScheduleHelper.isLessonMatchingTeacher(activeTeacher, item.teacherName, item.subject)
                                        } else false
                                    }

                                    val isRosterDownloaded = remember(item, localPackages) {
                                        if (item != null && localPackages.isNotEmpty()) {
                                            isRosterDownloadedForLesson(item.className, item.subject, localPackages)
                                        } else false
                                    }

                                    val breakAfter = if (i < 6) (customBreaks.getOrNull(i - 1) ?: breakDuration) else null
                                    val isMerged = (breakAfter == 0)

                                    val cellBoxColor = when {
                                        // درس الأستاذ مع سجل درجات منزل (تظليل أخضر زمردي مميز)
                                        isTeacherLesson && isRosterDownloaded -> {
                                            if (currentTheme.isDark) Color(0xFF064E3B).copy(alpha = 0.75f) else Color(0xFFDCFCE7)
                                        }
                                        // درس الأستاذ بدون سجل درجات منزل (تظليل أزرق سماوي)
                                        isTeacherLesson -> {
                                            if (currentTheme.isDark) Color(0xFF1E3A8A).copy(alpha = 0.65f) else Color(0xFFDBEAFE)
                                        }
                                        item != null -> if (currentTheme.isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                                        else -> if (currentTheme.isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                                    }

                                    val cellBorderColor = when {
                                        isTeacherLesson && isRosterDownloaded -> Color(0xFF059669)
                                        isTeacherLesson -> Color(0xFF2563EB)
                                        item != null -> currentTheme.tableBorderColor
                                        else -> currentTheme.tableBorderColor.copy(alpha = 0.3f)
                                    }

                                    val cellBorderWidth = when {
                                        isTeacherLesson && isRosterDownloaded -> 1.8.dp
                                        isTeacherLesson -> 1.3.dp
                                        else -> 0.8.dp
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = cellBoxColor,
                                        border = BorderStroke(
                                            cellBorderWidth,
                                            cellBorderColor
                                        ),
                                        modifier = Modifier
                                            .width(lessonColW)
                                            .height(rowH)
                                            .padding(1.5.dp)
                                            .clickable(enabled = (item != null && onLessonSelect != null)) {
                                                if (item != null && onLessonSelect != null) {
                                                    onLessonSelect(item)
                                                }
                                            }
                                    ) {
                                        if (item != null) {
                                            val cleanSubj = WidgetScheduleHelper.cleanSubjectName(item.subject)
                                            val dynamicFontSize = when {
                                                cleanSubj == "اجتماعيات" -> if (isLandscape) 7.5.sp else 8.5.sp
                                                cleanSubj.length > 8 -> if (isLandscape) 8.sp else 9.sp
                                                cleanSubj.length > 5 -> if (isLandscape) 9.sp else 10.sp
                                                else -> if (isLandscape) 10.5.sp else 11.5.sp
                                            }

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = cleanSubj.ifEmpty { "درس مقرر" },
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = dynamicFontSize,
                                                    color = when {
                                                        isTeacherLesson && isRosterDownloaded -> if (currentTheme.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                                        isTeacherLesson -> if (currentTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                                        currentTheme.isDark -> Color(0xFFCBD5E1)
                                                        else -> Color(0xFF1E3A8A)
                                                    },
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "شاغر",
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = if (isLandscape) 9.sp else 10.sp,
                                                    color = Color(0xFF94A3B8),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleHeaderCell(text: String, width: Dp, height: Dp) {
    val currentTheme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, currentTheme.tableBorderColor)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            fontSize = 11.5.sp,
            color = currentTheme.textPrimaryColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScheduleHeaderCellWithTiming(
    lessonTitle: String,
    timing: String,
    isMergedWithNext: Boolean = false,
    isMergedWithPrev: Boolean = false,
    width: Dp,
    height: Dp
) {
    val currentTheme = LocalAppTheme.current
    val isMerged = isMergedWithNext || isMergedWithPrev
    val mergeLineColor = if (currentTheme.isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, currentTheme.tableBorderColor),
        contentAlignment = Alignment.Center
    ) {
        // خط الدمج في الأعلى بين الدروس المدمجة فقط (والبقية بدون خط لمعرفة أنها بدون دمج)
        if (isMerged) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.TopCenter)
            ) {
                val strokeW = 4.dp.toPx()
                val tickH = 4.dp.toPx()
                val w = size.width
                val midX = w / 2f

                // في نظام RTL:
                // الحصة التالية (Next) تقع على اليسار x = 0f
                // الحصة السابقة (Prev) تقع على اليمين x = w
                val nextEdge = if (isRtl) 0f else w
                val prevEdge = if (isRtl) w else 0f

                if (isMergedWithPrev && isMergedWithNext) {
                    // مدمج من الجانبين: خط متصل يغطي كامل أعلى الخلية
                    drawLine(
                        color = mergeLineColor,
                        start = Offset(0f, strokeW / 2),
                        end = Offset(w, strokeW / 2),
                        strokeWidth = strokeW
                    )
                } else if (isMergedWithNext) {
                    // بداية الدمج: خط من المنتصف إلى الحافة المتصلة بالدرس التالي
                    drawLine(
                        color = mergeLineColor,
                        start = Offset(midX, strokeW / 2),
                        end = Offset(nextEdge, strokeW / 2),
                        strokeWidth = strokeW
                    )
                    // علامة طرف الرابط (Tick نزول ناعم)
                    drawLine(
                        color = mergeLineColor,
                        start = Offset(midX, 0f),
                        end = Offset(midX, tickH),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                } else if (isMergedWithPrev) {
                    // نهاية الدمج: خط من الحافة المشتركة إلى المنتصف
                    drawLine(
                        color = mergeLineColor,
                        start = Offset(prevEdge, strokeW / 2),
                        end = Offset(midX, strokeW / 2),
                        strokeWidth = strokeW
                    )
                    // علامة طرف الرابط (Tick نزول ناعم)
                    drawLine(
                        color = mergeLineColor,
                        start = Offset(midX, 0f),
                        end = Offset(midX, tickH),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            Text(
                text = lessonTitle,
                fontWeight = FontWeight.Black,
                fontSize = 11.5.sp,
                color = if (isMerged) mergeLineColor else currentTheme.textPrimaryColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = timing,
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp,
                color = if (currentTheme.isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                textAlign = TextAlign.Center
            )
        }
    }
}

fun calculateSlotTiming(
    lessonNumber: Int,
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10,
    isShort: Boolean = false,
    customLessons: List<Int> = emptyList(),
    customBreaks: List<Int> = emptyList()
): String {
    if (lessonNumber < 1) return ""
    val parts = startHourStr.split(":").mapNotNull { it.toIntOrNull() }
    val startH = if (parts.isNotEmpty()) parts[0] else 8
    val startM = if (parts.size > 1) parts[1] else 0

    var currentTotalMinutes = startH * 60 + startM
    for (i in 1 until lessonNumber) {
        val lDur = customLessons.getOrNull(i - 1) ?: lessonDuration
        val bDur = customBreaks.getOrNull(i - 1) ?: breakDuration
        currentTotalMinutes += lDur + bDur
    }

    val lDurCurrent = customLessons.getOrNull(lessonNumber - 1) ?: lessonDuration
    val lessonStartMin = currentTotalMinutes
    val lessonEndMin = currentTotalMinutes + lDurCurrent

    fun formatMin(min: Int, short: Boolean): String {
        var h = min / 60
        val m = min % 60
        val period = if (h in 12..23) "م" else "ص"
        h %= 12
        if (h == 0) h = 12
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", h, m)
        return if (short) timeStr else "$timeStr $period"
    }

    val startFormatted = formatMin(lessonStartMin, isShort)
    val endFormatted = formatMin(lessonEndMin, isShort)
    return "$startFormatted - $endFormatted"
}

fun getLessonShortTiming(
    lessonNumber: Int,
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10,
    customLessons: List<Int> = emptyList(),
    customBreaks: List<Int> = emptyList()
): String {
    return calculateSlotTiming(lessonNumber, startHourStr, lessonDuration, breakDuration, isShort = true, customLessons = customLessons, customBreaks = customBreaks)
}

fun getLessonTiming(
    lessonNumber: Int,
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10,
    customLessons: List<Int> = emptyList(),
    customBreaks: List<Int> = emptyList()
): String {
    return calculateSlotTiming(lessonNumber, startHourStr, lessonDuration, breakDuration, isShort = false, customLessons = customLessons, customBreaks = customBreaks)
}

@Composable
fun PersonalLessonCardRtl(
    lesson: TeacherLessonInfo,
    localPackages: List<ClassPackage> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentTheme = LocalAppTheme.current
    val tInfo = remember(lesson.lessonNumber) {
        WidgetScheduleHelper.getFullTimingFromScheduleJson(context)
    }
    val timing = remember(lesson.lessonNumber, tInfo) {
        getLessonTiming(
            lesson.lessonNumber,
            tInfo.startHour,
            tInfo.lessonDuration,
            tInfo.breakDuration,
            tInfo.customLessons,
            tInfo.customBreaks
        )
    }
    val breakAfter = if (lesson.lessonNumber < 6) (tInfo.customBreaks.getOrNull(lesson.lessonNumber - 1) ?: tInfo.breakDuration) else null
    val isMerged = (breakAfter == 0)
    val isRosterDownloaded = remember(lesson, localPackages) {
        if (localPackages.isNotEmpty()) {
            isRosterDownloadedForLesson(lesson.className, lesson.subject, localPackages)
        } else false
    }

    val cleanCls = remember(lesson.className) { WidgetScheduleHelper.cleanClassName(lesson.className) }
    val cleanSubj = remember(lesson.subject) { WidgetScheduleHelper.cleanSubjectName(lesson.subject) }
    val cleanTeacher = remember(lesson.teacherName) { WidgetScheduleHelper.cleanTeacherFirstName(lesson.teacherName) }

    val cardBg = when {
        isRosterDownloaded -> if (currentTheme.isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFF0FDF4)
        else -> currentTheme.surfaceColor
    }
    val cardBorder = when {
        isRosterDownloaded -> Color(0xFF059669)
        else -> currentTheme.tableBorderColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(if (isRosterDownloaded) 1.5.dp else 1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right Column: Lesson Tag & Class Name (البداية من اليمين)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = currentTheme.primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "الدرس ${lesson.lessonNumber}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = currentTheme.primaryColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp
                            )
                        }

                        Surface(
                            color = if (currentTheme.isDark) Color(0xFF78350F) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (currentTheme.isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = timing,
                                    color = if (currentTheme.isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "الصف والشعبة: $cleanCls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = currentTheme.textPrimaryColor
                    )
                }

                // Left Side: Subject Name & Status
                Column(
                    horizontalAlignment = Alignment.End, 
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = cleanSubj.ifEmpty { "درس مقرر" },
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isRosterDownloaded) Color(0xFF059669) else if (currentTheme.isDark) Color(0xFF34D399) else Color(0xFF0284C7)
                    )
                    if (cleanTeacher.isNotEmpty()) {
                        Text(
                            text = cleanTeacher,
                            fontSize = 11.sp,
                            color = currentTheme.textSecondaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom Badges Row: علامات الدمج أو الفرصة + حالة سجل الدرجات في البطاقة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // علامة الفرصة أو الدمج في البطاقة
                if (isMerged) {
                    Surface(
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🔗 دمج (0د)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF7E22CE),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if ((breakAfter ?: 0) > 0) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "☕ فرصة (${breakAfter}د)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🏁 نهاية الدوام",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // علامة سجل الدرجات في البطاقة
                if (isRosterDownloaded) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "📋 سجل الدرجات جاهز ✓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "☁️ غير منزل محلياً",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * حفظ توقيتات الحصص والفرص في التفضيلات وتحديث الودجت
 */
fun saveTimingPreferences(
    context: Context,
    startHour: String,
    lessonDurations: List<Int>,
    breakDurations: List<Int>,
    onComplete: () -> Unit
) {
    try {
        val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
        val bossPrefs = context.getSharedPreferences("com.example.theboss_preferences", Context.MODE_PRIVATE)
        val gson = Gson()
        val customLessonsJson = gson.toJson(lessonDurations)
        val customBreaksJson = gson.toJson(breakDurations)
        val avgLesson = lessonDurations.firstOrNull() ?: 45
        val avgBreak = breakDurations.firstOrNull() ?: 10

        listOf(prefs, bossPrefs).forEach { p ->
            p.edit()
                .putString("school_start_hour", startHour)
                .putInt("lesson_duration_minutes", avgLesson)
                .putInt("break_duration_minutes", avgBreak)
                .putString("custom_lesson_durations", customLessonsJson)
                .putString("custom_break_durations", customBreaksJson)
                .apply()
        }

        // Also update _timing inside synced_schedule so getFullTimingFromScheduleJson reads it instantly
        val rawJson = prefs.getString("synced_schedule", "{}") ?: "{}"
        try {
            val type = object : TypeToken<MutableMap<String, Any>>() {}.type
            val rootObj: MutableMap<String, Any>? = gson.fromJson(rawJson, type)
            if (rootObj != null) {
                val timingMap = mutableMapOf<String, Any>(
                    "schoolStartHour" to startHour,
                    "lessonDurationMinutes" to avgLesson,
                    "breakDurationMinutes" to avgBreak,
                    "customLessonDurations" to lessonDurations,
                    "customBreakDurations" to breakDurations
                )
                rootObj["_timing"] = timingMap
                val updatedJson = gson.toJson(rootObj)
                prefs.edit().putString("synced_schedule", updatedJson).apply()
                bossPrefs.edit().putString("synced_schedule", updatedJson).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Update all home screen widgets immediately
        WidgetScheduleHelper.updateAllWidgets(context)

        onComplete()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "حدث خطأ أثناء حفظ التوقيتات: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * نافذة تخصيص مواقيت الحصص والفرص والدمج في تطبيق الهاتف
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimingsDialog(
    initialStartHour: String,
    initialLessonDur: Int,
    initialBreakDur: Int,
    initialCustomLessons: List<Int>,
    initialCustomBreaks: List<Int>,
    onDismiss: () -> Unit,
    onSave: (startHour: String, lessons: List<Int>, breaks: List<Int>) -> Unit
) {
    val context = LocalContext.current
    var startHour by remember { mutableStateOf(initialStartHour) }
    val lessons = remember {
        mutableStateListOf(
            *Array(6) { idx ->
                initialCustomLessons.getOrNull(idx) ?: initialLessonDur
            }
        )
    }
    val breaks = remember {
        mutableStateListOf(
            *Array(5) { idx ->
                initialCustomBreaks.getOrNull(idx) ?: initialBreakDur
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.92f),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "تخصيص أوقات الحصص والفرص ⏱️",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "تحديد زمن كل درس على حدة، وتخصيص الفرص، ودمج درسين متتاليين بجعل الفرصة صفر (0د).",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: School Start Hour
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("بداية الدوام المدرسي:", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF1E3A8A))
                                    Text("وقت انطلاق الحصة الأولى", fontSize = 11.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        val parts = startHour.split(":").mapNotNull { it.toIntOrNull() }
                                        val h = if (parts.isNotEmpty()) parts[0] else 8
                                        val m = if (parts.size > 1) parts[1] else 0
                                        TimePickerDialog(context, { _, nh, nm ->
                                            startHour = String.format(Locale.US, "%02d:%02d", nh, nm)
                                        }, h, m, true).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        startHour,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Section 2: Lessons Durations (1 to 6)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مدد الحصص الدراسية الستة (بالدقائق):", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                            }
                            Text("يمكنك تخصيص مدة مختلفة لكل حصة دراسية على حدة.", fontSize = 11.sp, color = Color(0xFF64748B))

                            for (idx in 0 until 6) {
                                val lessonNum = idx + 1
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "الدرس $lessonNum:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1E3A8A)
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    lessons[idx] = maxOf(15, lessons[idx] - 5)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "إنقاص 5 دقائق", tint = Color(0xFF2563EB))
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    "${lessons[idx]} دقيقة",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF0F172A),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    lessons[idx] = minOf(90, lessons[idx] + 5)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "زيادة 5 دقائق", tint = Color(0xFF2563EB))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 3: Break Durations (1 to 5) with Merged Lessons Support (0 min)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FreeBreakfast, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مدد الفرص بين الدروس وإمكانية دمج الحصص:", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                            }
                            Text("وضع مدة الفرصة 0 دقيقة يعني دمج الحصتين متتاليتين معاً بدون فاصل استراحة.", fontSize = 11.sp, color = Color(0xFF92400E))

                            for (bIdx in 0 until 5) {
                                val bNum = bIdx + 1
                                val isMerged = breaks[bIdx] == 0

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMerged) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.2.dp, if (isMerged) Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "فرصة $bNum (بين الدرس $bNum والدرس ${bNum + 1}):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = if (isMerged) Color(0xFFB45309) else Color(0xFF334155)
                                            )

                                            if (isMerged) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFFEF3C7),
                                                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                                ) {
                                                    Text(
                                                        "🔗 درس مدمج (بدون فرصة)",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFB45309),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Merge Button
                                            if (isMerged) {
                                                OutlinedButton(
                                                    onClick = { breaks[bIdx] = 10 },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF059669))
                                                ) {
                                                    Text("إلغاء الدمج (10د)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                }
                                            } else {
                                                Button(
                                                    onClick = { breaks[bIdx] = 0 },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("دمج الحصتين (0د)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                                }
                                            }

                                            // Plus / Minus Duration
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        breaks[bIdx] = maxOf(0, breaks[bIdx] - 5)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "إنقاص 5 دقائق", tint = Color(0xFFD97706))
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, if (isMerged) Color(0xFFF59E0B) else Color(0xFFCBD5E1)),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        if (breaks[bIdx] == 0) "0 د (دمج)" else "${breaks[bIdx]} دقيقة",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 12.5.sp,
                                                        color = if (isMerged) Color(0xFFD97706) else Color(0xFF0F172A),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        breaks[bIdx] = minOf(60, breaks[bIdx] + 5)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "زيادة 5 دقائق", tint = Color(0xFFD97706))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Reset button
                        OutlinedButton(
                            onClick = {
                                startHour = "08:00"
                                for (i in 0 until 6) lessons[i] = 45
                                for (i in 0 until 5) breaks[i] = 10
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("استعادة التوقيتات الافتراضية (45د / 10د)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Bottom Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSave(startHour, lessons.toList(), breaks.toList())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(2f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ وتطبيق التوقيتات ⚡", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    )
}
