package com.school.system.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Read synced schedule from SharedPreferences
    val prefs = remember { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    var rawScheduleJson by remember { mutableStateOf(prefs.getString("synced_schedule", "{}") ?: "{}") }

    // 0: جدولي الخاص (Personal Table & Cards), 1: الجدول العام لجميع الصفوف (School General Grid)
    var selectedModeTab by remember { mutableIntStateOf(0) }

    // Display Format for Personal Schedule: 0: جدول أسبوعي شبكي (Matrix Table), 1: بطاقات يومية (Cards)
    var personalViewType by remember { mutableIntStateOf(0) }

    // Teacher Name for Personal Schedule filter
    var teacherNameInput by remember {
        mutableStateOf(prefs.getString("teacher_name", "") ?: "")
    }

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val gson = remember { Gson() }
    
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
                                val grade = row["grade"]?.toString() ?: ""
                                val section = row["section"]?.toString() ?: ""
                                val className = "$grade ($section)".trim()
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
        if (query.isEmpty()) {
            parsedScheduleByDay.values.flatten()
        } else {
            parsedScheduleByDay.values.flatten().filter {
                it.teacherName.lowercase().contains(query) || it.subject.lowercase().contains(query)
            }
        }
    }

    // Available classes for general schedule selector
    val availableClasses = remember(parsedScheduleByDay) {
        parsedScheduleByDay.values.flatten().map { it.className }.distinct().sorted()
    }

    var selectedGeneralDay by remember { mutableStateOf("الأحد") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Surface(
                    color = currentTheme.primaryColor,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "رجوع", 
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "جدول الحصص والتوقيتات الأسبوعي 📅",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
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
                                        Toast.makeText(context, "تمت مزامنة الجدول من السحابة بنجاح! ⚡", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "تأكد من الاتصال بالسحابة أو السيرفر", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "مزامنة", tint = Color.White)
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
            ) {
                
                // Mode Switch Tabs (1. جدولي الخاص من اليمين | 2. الجدول العام للمدرسة)
                Surface(
                    color = currentTheme.surfaceColor,
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, currentTheme.tableBorderColor)
                ) {
                    TabRow(
                        selectedTabIndex = selectedModeTab,
                        containerColor = currentTheme.surfaceColor,
                        contentColor = currentTheme.primaryColor
                    ) {
                        Tab(
                            selected = selectedModeTab == 0,
                            onClick = { selectedModeTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("جدولي الأسبوعي (حصصي)", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedModeTab == 1,
                            onClick = { selectedModeTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("جدول المدرسة العام (كافة الصفوف)", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                                }
                            }
                        )
                    }
                }

                // ==========================================
                // MODE 0: PERSONAL SCHEDULE (جدول المعلم الخاص)
                // ==========================================
                if (selectedModeTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Teacher Name Filter & Display Controls
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = currentTheme.surfaceColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
                            shadowElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "اسم المعلم في الجدول المدرسي:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = currentTheme.textPrimaryColor
                                    )

                                    // View Type Toggle: Grid vs Cards
                                    Row(
                                        modifier = Modifier
                                            .background(currentTheme.tableHeaderBg, RoundedCornerShape(8.dp))
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (personalViewType == 0) currentTheme.primaryColor else Color.Transparent,
                                            modifier = Modifier.clickable { personalViewType = 0 }
                                        ) {
                                            Text(
                                                text = "جدول شبكي 📊",
                                                color = if (personalViewType == 0) Color.White else currentTheme.textSecondaryColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (personalViewType == 1) currentTheme.primaryColor else Color.Transparent,
                                            modifier = Modifier.clickable { personalViewType = 1 }
                                        ) {
                                            Text(
                                                text = "قائمة بطاقات 📋",
                                                color = if (personalViewType == 1) Color.White else currentTheme.textSecondaryColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = teacherNameInput,
                                    onValueChange = { 
                                        teacherNameInput = it
                                        prefs.edit().putString("teacher_name", it).apply()
                                    },
                                    placeholder = { Text("اكتب اسمك لتصفية الحصص الموكلة لك...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        if (myPersonalLessons.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.EventBusy, 
                                        contentDescription = null, 
                                        tint = Color.Gray, 
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد حصص مسندة لهذا الاسم في الجدول.\nتأكد من كتابة اسمك كما هو مسجل في جدول المدرسة.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // View Type 0: WEEKLY RTL MATRIX TABLE (جدول شبكي أسبوعي متكامل يبدأ من اليمين)
                            if (personalViewType == 0) {
                                WeeklyPersonalScheduleTable(
                                    daysList = daysList,
                                    lessons = myPersonalLessons
                                )
                            } else {
                                // View Type 1: DETAILED CARDS LIST (بطاقات مرتبة من اليمين)
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    daysList.forEach { day ->
                                        val dayLessons = myPersonalLessons.filter { it.day == day }.sortedBy { it.lessonNumber }
                                        if (dayLessons.isNotEmpty()) {
                                            item {
                                                Surface(
                                                    color = currentTheme.primaryColor,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                    ) {
                                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = "يوم $day (${dayLessons.size} حصص مسندة)",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                            items(dayLessons) { lesson ->
                                                PersonalLessonCardRtl(lesson)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // MODE 1: GENERAL SCHOOL SCHEDULE (الجدول العام الشامل لكافة الصفوف)
                // ==========================================
                else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day Selector Tabs (من اليمين لليسار: الأحد -> الخميس)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = currentTheme.surfaceColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysList.forEach { day ->
                                    val isSelected = selectedGeneralDay == day
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) currentTheme.primaryColor else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedGeneralDay = day }
                                    ) {
                                        Text(
                                            text = day,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else currentTheme.textPrimaryColor
                                        )
                                    }
                                }
                            }
                        }

                        // Comprehensive General Timetable Grid for Selected Day
                        GeneralDayScheduleGrid(
                            day = selectedGeneralDay,
                            classes = availableClasses,
                            allLessons = parsedScheduleByDay[selectedGeneralDay] ?: emptyList()
                        )
                    }
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
    lessons: List<TeacherLessonInfo>
) {
    val currentTheme = LocalAppTheme.current
    val hScroll = rememberScrollState()
    
    val dayColW = 85.dp
    val lessonColW = 105.dp
    val rowH = 52.dp

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = currentTheme.surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Table Header (من اليمين إلى اليسار)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.tableHeaderBg)
                    .border(0.5.dp, currentTheme.tableBorderColor)
            ) {
                // Rightmost Fixed Header: اليوم
                ScheduleHeaderCell("اليوم 📅", dayColW, 46.dp)

                // Scrollable Lesson Columns (1 إلى 6 من اليمين لليسار)
                Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
                    for (i in 1..6) {
                        ScheduleHeaderCellWithTiming("الدرس $i", getLessonShortTiming(i), lessonColW, 46.dp)
                    }
                }
            }

            // Table Rows for 5 Days
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(daysList) { day ->
                    val dayLessonsMap = remember(lessons, day) {
                        lessons.filter { it.day == day }.associateBy { it.lessonNumber }
                    }
                    val isEven = daysList.indexOf(day) % 2 == 0
                    val rowBg = if (isEven) currentTheme.tableCellBg else currentTheme.tableAltCellBg

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .border(0.5.dp, currentTheme.tableBorderColor)
                    ) {
                        // Day Label on Right
                        Box(
                            modifier = Modifier
                                .width(dayColW)
                                .height(rowH)
                                .background(currentTheme.primaryColor.copy(alpha = 0.08f))
                                .border(0.5.dp, currentTheme.tableBorderColor)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = currentTheme.primaryColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Lesson Cells 1 to 6 (من اليمين إلى اليسار)
                        Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
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
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = item.subject,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF059669),
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = item.className,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = currentTheme.textPrimaryColor,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "شاغر",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 10.sp,
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
    allLessons: List<TeacherLessonInfo>
) {
    val currentTheme = LocalAppTheme.current
    val hScroll = rememberScrollState()
    
    val classColW = 100.dp
    val lessonColW = 110.dp
    val rowH = 50.dp

    if (classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد بيانات جدول لصفوف المدرسة", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = currentTheme.surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.tableBorderColor),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: الصف والشعبة على اليمين ثم الحصص 1 إلى 6
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.tableHeaderBg)
                    .border(0.5.dp, currentTheme.tableBorderColor)
            ) {
                ScheduleHeaderCell("الصف / الشعبة 🏫", classColW, 44.dp)

                Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
                    for (i in 1..6) {
                        ScheduleHeaderCellWithTiming("الدرس $i", getLessonShortTiming(i), lessonColW, 44.dp)
                    }
                }
            }

            // Rows for each class
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        Box(
                            modifier = Modifier
                                .width(classColW)
                                .height(rowH)
                                .background(currentTheme.primaryColor.copy(alpha = 0.06f))
                                .border(0.5.dp, currentTheme.tableBorderColor)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = clsName,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = currentTheme.textPrimaryColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }

                        // Lessons 1 to 6
                        Row(modifier = Modifier.weight(1f).horizontalScroll(hScroll)) {
                            for (i in 1..6) {
                                val item = classLessonsMap[i]
                                Box(
                                    modifier = Modifier
                                        .width(lessonColW)
                                        .height(rowH)
                                        .border(0.5.dp, currentTheme.tableBorderColor)
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = item.subject.ifEmpty { "درس مقرر" },
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (currentTheme.isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A),
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                            if (item.teacherName.isNotEmpty()) {
                                                Text(
                                                    text = item.teacherName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.5.sp,
                                                    color = if (currentTheme.isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "شاغر",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 10.sp,
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

fun getLessonShortTiming(lessonNumber: Int): String {
    return when (lessonNumber) {
        1 -> "08:00 ص"
        2 -> "08:55 ص"
        3 -> "09:50 ص"
        4 -> "10:45 ص"
        5 -> "11:40 ص"
        6 -> "12:35 م"
        7 -> "01:30 م"
        else -> ""
    }
}

fun getLessonTiming(lessonNumber: Int): String {
    return when (lessonNumber) {
        1 -> "08:00 ص - 08:45 ص"
        2 -> "08:55 ص - 09:40 ص"
        3 -> "09:50 ص - 10:35 ص"
        4 -> "10:45 ص - 11:30 ص"
        5 -> "11:40 ص - 12:25 م"
        6 -> "12:35 م - 01:20 م"
        7 -> "01:30 م - 02:15 م"
        else -> "الحصة $lessonNumber"
    }
}

@Composable
fun PersonalLessonCardRtl(lesson: TeacherLessonInfo) {
    val currentTheme = LocalAppTheme.current
    val timing = remember(lesson.lessonNumber) { getLessonTiming(lesson.lessonNumber) }

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
                    text = "الصف والشعبة: ${lesson.className}",
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
                    text = lesson.subject.ifEmpty { "درس مقرر" },
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (currentTheme.isDark) Color(0xFF34D399) else Color(0xFF059669)
                )
                if (lesson.teacherName.isNotEmpty()) {
                    Text(
                        text = lesson.teacherName,
                        fontSize = 11.sp,
                        color = currentTheme.textSecondaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
