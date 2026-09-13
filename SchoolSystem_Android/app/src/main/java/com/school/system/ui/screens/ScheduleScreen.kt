package com.school.system.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    syncManager: SyncManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme = LocalAppTheme.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

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

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val gson = remember { Gson() }

    // Dynamic schedule timings (configured by principal in desktop system)
    val (timingStartHour, timingLessonDur, timingBreakDur) = remember(rawScheduleJson) {
        try {
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            val timingObj = rootObj?.get("_timing") as? Map<*, *>
            val startHour = timingObj?.get("schoolStartHour")?.toString()
                ?: prefs.getString("school_start_hour", "08:00")
                ?: "08:00"
            val lessonDur = (timingObj?.get("lessonDurationMinutes") as? Number)?.toInt()
                ?: prefs.getInt("lesson_duration_minutes", 45)
            val breakDur = (timingObj?.get("breakDurationMinutes") as? Number)?.toInt()
                ?: prefs.getInt("break_duration_minutes", 10)
            Triple(startHour, lessonDur, breakDur)
        } catch (e: Exception) {
            Triple("08:00", 45, 10)
        }
    }
    
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

    var selectedGeneralDay by remember { mutableStateOf("الأحد") }
    val lazyListState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 25 }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = !isScrolled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = currentTheme.primaryColor,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 36.dp else 46.dp)
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

                            IconButton(
                                onClick = {
                                    isRefreshing = true
                                    coroutineScope.launch {
                                        val success = syncManager.downloadSchedule(context)
                                        isRefreshing = false
                                        if (success) {
                                            rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
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
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(currentTheme.backgroundColor)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Day Selector Bar (الأحد -> الخميس) - Collapsible on Scroll
                AnimatedVisibility(
                    visible = !isScrolled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = currentTheme.surfaceColor,
                        border = BorderStroke(1.dp, currentTheme.tableBorderColor),
                        shadowElevation = 1.dp
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
                                        modifier = Modifier.padding(vertical = if (isLandscape) 4.dp else 6.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = if (isLandscape) 11.sp else 12.sp,
                                        color = if (isSelected) Color.White else currentTheme.textPrimaryColor
                                    )
                                }
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
                        lazyListState = lazyListState,
                        startHourStr = timingStartHour,
                        lessonDuration = timingLessonDur,
                        breakDuration = timingBreakDur
                    )
                }
            }
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
    breakDuration: Int = 10
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

        val headerH = 48.dp
        val rowH = 58.dp

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
                            ScheduleHeaderCellWithTiming("الدرس $i", getLessonShortTiming(i, startHourStr, lessonDuration, breakDuration), lessonColW, headerH)
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
                                Box(
                                    modifier = Modifier
                                        .width(lessonColW)
                                        .height(rowH)
                                        .border(0.5.dp, currentTheme.tableBorderColor)
                                        .padding(horizontal = 4.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item != null) {
                                        val cleanSubj = WidgetScheduleHelper.cleanSubjectName(item.subject)
                                        val cleanCls = WidgetScheduleHelper.cleanClassName(item.className)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = cleanSubj.ifEmpty { "درس مقرر" },
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = Color(0xFF059669),
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = cleanCls,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = currentTheme.textPrimaryColor,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
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

/**
 * جدول المدرسة العام لكافة الصفوف والشعب في اليوم المحدد (مرتب من اليمين لليسار)
 */
@Composable
fun GeneralDayScheduleGrid(
    day: String,
    classes: List<String>,
    allLessons: List<TeacherLessonInfo>,
    lazyListState: LazyListState = rememberLazyListState(),
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10
) {
    val currentTheme = LocalAppTheme.current
    val hScroll = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    val teacherNameInput = remember { prefs.getString("teacher_name", "")?.trim() ?: "" }

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

        val rowH = if (isLandscape) 38.dp else 52.dp
        val headerH = if (isLandscape) 36.dp else 46.dp

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = currentTheme.surfaceColor,
            border = BorderStroke(1.dp, currentTheme.tableBorderColor),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: الصف والشعبة على اليمين ثم الحصص 1 إلى 6 بالتوقيت
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
                            ScheduleHeaderCellWithTiming("الدرس $i", getLessonShortTiming(i, startHourStr, lessonDuration, breakDuration), lessonColW, headerH)
                        }
                    }
                }

                // Rows for each class
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
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
                                    val isTeacherLesson = remember(item, teacherNameInput) {
                                        if (item != null && teacherNameInput.isNotEmpty()) {
                                            WidgetScheduleHelper.isLessonMatchingTeacher(teacherNameInput, item.teacherName, item.subject)
                                        } else false
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isTeacherLesson) currentTheme.primaryColor.copy(alpha = 0.2f) else Color.Transparent,
                                        border = BorderStroke(
                                            0.8.dp,
                                            if (isTeacherLesson) currentTheme.primaryColor else currentTheme.tableBorderColor.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .width(lessonColW)
                                            .height(rowH)
                                            .padding(1.5.dp)
                                    ) {
                                        if (item != null) {
                                            val cleanSubj = WidgetScheduleHelper.cleanSubjectName(item.subject)
                                            val dynamicFontSize = when {
                                                cleanSubj == "اجتماعيات" -> if (isLandscape) 7.5.sp else 8.5.sp
                                                cleanSubj.length > 8 -> if (isLandscape) 8.5.sp else 9.5.sp
                                                cleanSubj.length > 5 -> if (isLandscape) 9.5.sp else 10.5.sp
                                                else -> if (isLandscape) 11.sp else 12.5.sp
                                            }
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = cleanSubj.ifEmpty { "درس مقرر" },
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = dynamicFontSize,
                                                    color = if (isTeacherLesson) currentTheme.primaryColor else if (currentTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A),
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
                                                    fontSize = if (isLandscape) 9.5.sp else 10.5.sp,
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
fun ScheduleHeaderCellWithTiming(lessonTitle: String, timing: String, width: Dp, height: Dp) {
    val currentTheme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, currentTheme.tableBorderColor)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = lessonTitle,
                fontWeight = FontWeight.Black,
                fontSize = 11.5.sp,
                color = currentTheme.textPrimaryColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = timing,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
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
    isShort: Boolean = false
): String {
    if (lessonNumber < 1) return ""
    val parts = startHourStr.split(":").mapNotNull { it.toIntOrNull() }
    val startH = if (parts.isNotEmpty()) parts[0] else 8
    val startM = if (parts.size > 1) parts[1] else 0

    var currentTotalMinutes = startH * 60 + startM
    for (i in 1 until lessonNumber) {
        currentTotalMinutes += lessonDuration + breakDuration
    }

    val lessonStartMin = currentTotalMinutes
    val lessonEndMin = currentTotalMinutes + lessonDuration

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
    breakDuration: Int = 10
): String {
    return calculateSlotTiming(lessonNumber, startHourStr, lessonDuration, breakDuration, isShort = true)
}

fun getLessonTiming(
    lessonNumber: Int,
    startHourStr: String = "08:00",
    lessonDuration: Int = 45,
    breakDuration: Int = 10
): String {
    return calculateSlotTiming(lessonNumber, startHourStr, lessonDuration, breakDuration, isShort = false)
}

@Composable
fun PersonalLessonCardRtl(lesson: TeacherLessonInfo) {
    val currentTheme = LocalAppTheme.current
    val timing = remember(lesson.lessonNumber) { getLessonTiming(lesson.lessonNumber) }
    val cleanCls = remember(lesson.className) { WidgetScheduleHelper.cleanClassName(lesson.className) }
    val cleanSubj = remember(lesson.subject) { WidgetScheduleHelper.cleanSubjectName(lesson.subject) }
    val cleanTeacher = remember(lesson.teacherName) { WidgetScheduleHelper.cleanTeacherFirstName(lesson.teacherName) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = currentTheme.surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    color = if (currentTheme.isDark) Color(0xFF34D399) else Color(0xFF059669)
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
    }
}
