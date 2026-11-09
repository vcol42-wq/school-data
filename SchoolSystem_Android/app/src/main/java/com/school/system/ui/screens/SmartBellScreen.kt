package com.school.system.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import org.json.JSONArray
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.system.data.model.LessonAlarm
import com.school.system.ui.components.HelpGuideDialog
import com.school.system.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SmartBellScreen(
    onBack: () -> Unit,
    viewModel: SmartBellViewModel = hiltViewModel()
) {
    val allAlarms by viewModel.allAlarms.collectAsState()
    val config by viewModel.config.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val theme = LocalAppTheme.current
    
    // 0: جدول وضبط الحصص, 1: إعدادات التوقيت ورنين الجرس
    var mainTabSelected by remember { mutableIntStateOf(0) }
    var isGridView by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val daysValues = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
    val pagerState = rememberPagerState(pageCount = { daysList.size })

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = theme.primaryColor.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = theme.primaryColor
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("منبه وجرس الحصص الذكي ⏰", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(
                                text = if (config?.bellRingTeacherOnly == true) "الوضع: رنين لحصصي المقررة فقط" else "الوضع: رنين لكافة الحصص المدرسية",
                                fontSize = 11.sp,
                                color = theme.primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.surfaceColor,
                    titleContentColor = theme.textPrimaryColor,
                    navigationIconContentColor = theme.textPrimaryColor
                ),
                actions = {
                    IconButton(onClick = { showHelpGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "دليل الاستخدام والتعليمات", tint = theme.primaryColor)
                    }
                    if (mainTabSelected == 0) {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewDay else Icons.Default.GridView,
                                contentDescription = "تبديل طريقة العرض",
                                tint = theme.primaryColor
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.applyAutoSchedule()
                        Toast.makeText(context, "تم حساب وتوليد مواعيد الحصص تلقائياً ⚡", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.AutoMode, contentDescription = "حساب تلقائي", tint = theme.secondaryColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Primary Tabs: (1. جدول الحصص والتنبيهات) | (2. إعدادات التوقيت ورنين الجرس)
            TabRow(
                selectedTabIndex = mainTabSelected,
                containerColor = theme.surfaceColor,
                contentColor = theme.primaryColor
            ) {
                Tab(
                    selected = mainTabSelected == 0,
                    onClick = { mainTabSelected = 0 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("جدول الحصص والتنبيهات 📅", fontWeight = if (mainTabSelected == 0) FontWeight.Black else FontWeight.Medium)
                        }
                    }
                )
                Tab(
                    selected = mainTabSelected == 1,
                    onClick = { mainTabSelected = 1 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إعدادات التوقيت والرنين ⚙️", fontWeight = if (mainTabSelected == 1) FontWeight.Black else FontWeight.Medium)
                        }
                    }
                )
            }

            if (mainTabSelected == 0) {
                // TAB 1: SCHEDULE & ALARMS (Full screen, comfortable height)
                if (isGridView) {
                    WeeklyGridView(allAlarms, viewModel)
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Day Selector Tabs
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = theme.surfaceColor
                        ) {
                            daysList.forEachIndexed { index, day ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                    text = {
                                        Text(
                                            day,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }
                        }

                        // Top Quick Actions inside Period Tab
                        Surface(
                            color = theme.tableAltCellBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Import from timetable button
                                Button(
                                    onClick = {
                                        viewModel.importFromSchedule(context) { count ->
                                            if (count > 0) {
                                                Toast.makeText(context, "تم استيراد $count حصة من جدولك المعتمد بنجاح 📥✓", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "تم تطبيق الجدول الأساسي بنجاح", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("استيراد من جدول الحصص 📥", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val currentDayValue = daysValues[pagerState.currentPage]
                                val currentDayAlarms = allAlarms.filter { it.dayOfWeek == currentDayValue }
                                val allEnabled = currentDayAlarms.isNotEmpty() && currentDayAlarms.all { it.isEnabled }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.enableAllForDay(currentDayValue, !allEnabled)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(if (allEnabled) "تعطيل الكل ✕" else "تفعيل الكل ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) { page ->
                            val dayValue = daysValues[page]
                            val dayAlarms = allAlarms.filter { it.dayOfWeek == dayValue }
                            
                            if (dayAlarms.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color(0xFF94A3B8))
                                        Spacer(Modifier.height(12.dp))
                                        Text("لا توجد حصص مضافة لهذا اليوم", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.applyAutoSchedule() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.AutoMode, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("توليد الحصص الـ 6 تلقائياً ⚡")
                                        }
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 280.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(dayAlarms) { alarm ->
                                        val previous = dayAlarms.find { it.lessonIndex == alarm.lessonIndex - 1 }
                                        val next = dayAlarms.find { it.lessonIndex == alarm.lessonIndex + 1 }
                                        AlarmItemCard(
                                            alarm = alarm,
                                            isMergedWithPrevious = areAdjacentLessons(previous, alarm),
                                            isMergedWithNext = areAdjacentLessons(alarm, next),
                                            onToggle = { viewModel.toggleAlarm(alarm, it) },
                                            onUpdate = { name, start, end -> viewModel.updateAlarmDetails(alarm, name, start, end) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 2: BELL SETTINGS & RING MODE (Spacious & Clean)
                if (config != null) {
                    BellConfigurationScreen(
                        config = config!!,
                        onUpdateSettings = { sh, sm, ld, bd, ring -> viewModel.updateBellSettings(sh, sm, ld, bd, ring) },
                        onUpdateRingMode = { ringTeacherOnly -> viewModel.updateBellRingMode(ringTeacherOnly) },
                        onApplyAuto = { useCustomTimings ->
                            viewModel.applyAutoSchedule(useCustomTimings)
                            Toast.makeText(context, "تم حساب وتحديث كافة مواعيد الحصص بنجاح ⚡", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (showHelpGuideDialog) {
            HelpGuideDialog(onDismiss = { showHelpGuideDialog = false })
        }
    }
}

@Composable
fun BellConfigurationScreen(
    config: com.school.system.data.model.SchoolConfig,
    onUpdateSettings: (startH: Int, startM: Int, lessonD: Int, breakD: Int, ringtoneUri: String?) -> Unit,
    onUpdateRingMode: (ringTeacherOnly: Boolean) -> Unit,
    onApplyAuto: (useCustomTimings: Boolean) -> Unit
) {
    val context = LocalContext.current
    val theme = LocalAppTheme.current
    val surfaceColor = theme.surfaceColor
    val backgroundColor = theme.backgroundColor
    val textColor = theme.textPrimaryColor
    val secondaryTextColor = theme.textSecondaryColor
    val borderColor = theme.tableBorderColor
    val sectionColor = theme.tableAltCellBg
    val selectedColor = theme.primaryColor.copy(alpha = if (theme.isDark) 0.22f else 0.10f)
    var showCustomTimingsDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val prefs = remember(context, refreshKey) { context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE) }
    val customLessons = remember(showCustomTimingsDialog, refreshKey) {
        try {
            val raw = prefs.getString("custom_lesson_durations", null)
            if (!raw.isNullOrEmpty()) {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getInt(it) }
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }
    val customBreaks = remember(showCustomTimingsDialog, refreshKey) {
        try {
            val raw = prefs.getString("custom_break_durations", null)
            if (!raw.isNullOrEmpty()) {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getInt(it) }
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }
    val startHourStr = remember(config.bellStartHour, config.bellStartMinute) {
        String.format(Locale.US, "%02d:%02d", config.bellStartHour, config.bellStartMinute)
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onUpdateSettings(config.bellStartHour, config.bellStartMinute, config.bellLessonDuration, config.bellBreakDuration, uri?.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section 1: Ring Mode Selector (رنين لجميع الحصص أم لحصص المعلم فقط)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (theme.isDark) 0.dp else 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = theme.primaryColor)
                    Spacer(Modifier.width(8.dp))
                    Text("نطاق تشغيل رنين الجرس", fontWeight = FontWeight.Black, fontSize = 15.sp, color = textColor)
                }
                Text("اختر الحصص التي تريد أن يعمل فيها الجرس.", fontSize = 12.sp, color = secondaryTextColor)

                // Option A: All Periods
                Surface(
                    color = if (!config.bellRingTeacherOnly) selectedColor else sectionColor,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (!config.bellRingTeacherOnly) 1.5.dp else 1.dp,
                        if (!config.bellRingTeacherOnly) theme.primaryColor else borderColor
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { onUpdateRingMode(false) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !config.bellRingTeacherOnly,
                            onClick = { onUpdateRingMode(false) },
                            colors = RadioButtonDefaults.colors(selectedColor = theme.primaryColor),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("جميع الحصص", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor, maxLines = 1)
                            Text("يرن عند بدايات الحصص والاستراحات في الجدول العام.", fontSize = 11.sp, color = secondaryTextColor, lineHeight = 15.sp)
                        }
                    }
                }

                // Option B: Teacher's Lessons Only
                Surface(
                    color = if (config.bellRingTeacherOnly) selectedColor else sectionColor,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (config.bellRingTeacherOnly) 1.5.dp else 1.dp,
                        if (config.bellRingTeacherOnly) theme.primaryColor else borderColor
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { onUpdateRingMode(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.bellRingTeacherOnly,
                            onClick = { onUpdateRingMode(true) },
                            colors = RadioButtonDefaults.colors(selectedColor = theme.primaryColor),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("حصصي المقررة فقط", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor, maxLines = 1)
                            Text("يرن في أوقات حصصك وموادك المحددة في الجدول.", fontSize = 11.sp, color = secondaryTextColor, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Timing Configuration
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (theme.isDark) 0.dp else 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = theme.primaryColor)
                    Spacer(Modifier.width(8.dp))
                    Text("التوقيت والمدد", fontWeight = FontWeight.Black, fontSize = 15.sp, color = textColor)
                }

                // Start of school day
                Surface(
                    color = sectionColor,
                    shape = RoundedCornerShape(11.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("بداية الدوام المدرسي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                            Text("موعد انطلاق الحصة الأولى", fontSize = 11.sp, color = secondaryTextColor)
                        }
                        Button(
                            onClick = {
                                TimePickerDialog(context, { _, h, m -> 
                                    onUpdateSettings(h, m, config.bellLessonDuration, config.bellBreakDuration, config.bellRingtoneUri)
                                }, config.bellStartHour, config.bellStartMinute, true).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(
                                String.format(Locale.US, "%02d:%02d", config.bellStartHour, config.bellStartMinute),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                style = TextStyle(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                }

                // Lesson duration
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مدة الحصة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Text("${config.bellLessonDuration} دقيقة", fontWeight = FontWeight.Black, color = theme.primaryColor, fontSize = 14.sp)
                    }
                    Slider(
                        value = config.bellLessonDuration.toFloat(),
                        onValueChange = { onUpdateSettings(config.bellStartHour, config.bellStartMinute, it.toInt(), config.bellBreakDuration, config.bellRingtoneUri) },
                        valueRange = 30f..60f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primaryColor,
                            activeTrackColor = theme.primaryColor,
                            inactiveTrackColor = borderColor
                        )
                    )
                }

                // Break duration
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مدة الاستراحة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Text("${config.bellBreakDuration} دقيقة", fontWeight = FontWeight.Black, color = theme.secondaryColor, fontSize = 14.sp)
                    }
                    Slider(
                        value = config.bellBreakDuration.toFloat(),
                        onValueChange = { onUpdateSettings(config.bellStartHour, config.bellStartMinute, config.bellLessonDuration, it.toInt(), config.bellRingtoneUri) },
                        valueRange = 5f..30f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.secondaryColor,
                            activeTrackColor = theme.secondaryColor,
                            inactiveTrackColor = borderColor
                        )
                    )
                }

                // Advanced Timing Customization & Merge Lessons (0 min break)
                Surface(
                    color = selectedColor,
                    shape = RoundedCornerShape(11.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "تخصيص أوقات الحصص والفرص",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    color = surfaceColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "دمج 🔗",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primaryColor,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "خصص مدة الحصص والاستراحات، واجعل الاستراحة 0 دقيقة لدمج حصتين.",
                            fontSize = 11.sp,
                            color = secondaryTextColor,
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { showCustomTimingsDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "ضبط أوقات الحصص والدمج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Ringtone picker
                Surface(
                    color = sectionColor,
                    shape = RoundedCornerShape(11.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("نغمة الجرس", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                            Text(
                                if (config.bellRingtoneUri != null) "نغمة مخصصة" else "نغمة النظام الافتراضية",
                                fontSize = 11.sp,
                                color = secondaryTextColor,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة جرس الحصص")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, config.bellRingtoneUri?.let { Uri.parse(it) })
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primaryColor)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تغيير النغمة", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Apply Button
        Button(
            onClick = { onApplyAuto(false) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor),
            shape = RoundedCornerShape(11.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.AutoMode, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("تطبيق وحساب المواعيد لجميع الأيام ⚡", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }

        if (showCustomTimingsDialog) {
            ScheduleTimingsDialog(
                initialStartHour = startHourStr,
                initialLessonDur = config.bellLessonDuration,
                initialBreakDur = config.bellBreakDuration,
                initialCustomLessons = customLessons,
                initialCustomBreaks = customBreaks,
                onDismiss = { showCustomTimingsDialog = false },
                onSave = { newStartHour, newLessons, newBreaks ->
                    saveTimingPreferences(context, newStartHour, newLessons, newBreaks) {
                        val parts = newStartHour.split(":")
                        val sh = parts.getOrNull(0)?.toIntOrNull() ?: config.bellStartHour
                        val sm = parts.getOrNull(1)?.toIntOrNull() ?: config.bellStartMinute
                        val avgL = newLessons.firstOrNull() ?: config.bellLessonDuration
                        val avgB = newBreaks.firstOrNull() ?: config.bellBreakDuration
                        onUpdateSettings(sh, sm, avgL, avgB, config.bellRingtoneUri)
                        onApplyAuto(true)
                        refreshKey++
                        showCustomTimingsDialog = false
                        Toast.makeText(context, "تم حفظ وتطبيق التوقيتات المخصصة ودمج الحصص بنجاح ⚡", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: LessonAlarm,
    isMergedWithPrevious: Boolean,
    isMergedWithNext: Boolean,
    onToggle: (Boolean) -> Unit,
    onUpdate: (String, String, String) -> Unit
) {
    var name by remember(alarm.lessonName) { mutableStateOf(alarm.lessonName) }
    val context = LocalContext.current
    val theme = LocalAppTheme.current

    Surface(
        color = theme.surfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (alarm.isEnabled) theme.primaryColor.copy(alpha = 0.55f) else theme.tableBorderColor
        ),
        shadowElevation = if (alarm.isEnabled && !theme.isDark) 1.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (alarm.isEnabled) theme.primaryColor.copy(alpha = 0.14f) else theme.tableAltCellBg,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${alarm.lessonIndex}",
                        color = if (alarm.isEnabled) theme.primaryColor else theme.textSecondaryColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الحصة ${alarm.lessonIndex}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) theme.textPrimaryColor else theme.textSecondaryColor
                    )
                    if (isMergedWithPrevious || isMergedWithNext) {
                        Text(
                            text = "🔗 مدمجة",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                }

                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(it, alarm.startTime, alarm.endTime)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) theme.textPrimaryColor else theme.textSecondaryColor
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (name.isEmpty()) {
                                Text("اسم المادة/الشعبة", color = theme.textSecondaryColor, fontSize = 10.sp)
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = theme.tableAltCellBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable {
                            val (h, m) = alarm.startTime.split(":").map { it.toInt() }
                            TimePickerDialog(context, { _, nh, nm ->
                                onUpdate(name, String.format(Locale.US, "%02d:%02d", nh, nm), alarm.endTime)
                            }, h, m, true).show()
                        }
                    ) {
                        Text(
                            text = alarm.startTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Text("–", fontSize = 10.sp, color = theme.textSecondaryColor, modifier = Modifier.padding(horizontal = 3.dp))

                    Surface(
                        color = theme.tableAltCellBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable {
                            val (h, m) = alarm.endTime.split(":").map { it.toInt() }
                            TimePickerDialog(context, { _, nh, nm ->
                                onUpdate(name, alarm.startTime, String.format(Locale.US, "%02d:%02d", nh, nm))
                            }, h, m, true).show()
                        }
                    ) {
                        Text(
                            text = alarm.endTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.secondaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.surfaceColor,
                    checkedTrackColor = theme.primaryColor,
                    uncheckedThumbColor = theme.textSecondaryColor,
                    uncheckedTrackColor = theme.tableBorderColor
                ),
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

private fun areAdjacentLessons(previous: LessonAlarm?, next: LessonAlarm?): Boolean =
    previous != null &&
        next != null &&
        previous.lessonIndex + 1 == next.lessonIndex &&
        previous.endTime.trim() == next.startTime.trim()

@Composable
fun WeeklyGridView(alarms: List<LessonAlarm>, viewModel: SmartBellViewModel) {
    val scrollState = rememberScrollState()
    val theme = LocalAppTheme.current
    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val daysValues = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(scrollState)
    ) {
        // Header Row
        Row {
            Box(Modifier.width(80.dp).height(44.dp).background(theme.tableHeaderBg), contentAlignment = Alignment.Center) {
                Text("اليوم", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = theme.textPrimaryColor)
            }
            for (i in 1..6) {
                Box(Modifier.width(110.dp).height(44.dp).background(theme.tableHeaderBg), contentAlignment = Alignment.Center) {
                    Text("حصة $i", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = theme.textPrimaryColor)
                }
            }
        }

        // Data Rows
        daysValues.forEachIndexed { index, dayVal ->
            val dayName = daysList[index]
            Row {
                Box(Modifier.width(80.dp).height(65.dp).background(theme.tableAltCellBg), contentAlignment = Alignment.Center) {
                    Text(dayName, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = theme.textPrimaryColor)
                }
                for (i in 1..6) {
                    val alarm = alarms.find { it.dayOfWeek == dayVal && it.lessonIndex == i }
                    if (alarm != null) {
                        val dayAlarms = alarms.filter { it.dayOfWeek == dayVal }
                        val previous = dayAlarms.find { it.lessonIndex == i - 1 }
                        val next = dayAlarms.find { it.lessonIndex == i + 1 }
                        GridCell(
                            alarm = alarm,
                            isMerged = areAdjacentLessons(previous, alarm) || areAdjacentLessons(alarm, next),
                            onToggle = { viewModel.toggleAlarm(alarm, it) },
                            onUpdate = { name -> viewModel.updateAlarmDetails(alarm, name, alarm.startTime, alarm.endTime) }
                        )
                    } else {
                        Box(Modifier.width(110.dp).height(65.dp).border(0.5.dp, theme.tableBorderColor))
                    }
                }
            }
        }
    }
}

@Composable
fun GridCell(alarm: LessonAlarm, isMerged: Boolean, onToggle: (Boolean) -> Unit, onUpdate: (String) -> Unit) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(65.dp)
            .border(0.5.dp, theme.tableBorderColor)
            .background(if (alarm.isEnabled) theme.primaryColor.copy(alpha = if (theme.isDark) 0.18f else 0.08f) else theme.surfaceColor)
            .clickable { onToggle(!alarm.isEnabled) }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${alarm.startTime} - ${alarm.endTime}", 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                color = if (alarm.isEnabled) theme.primaryColor else theme.textSecondaryColor
            )
            if (isMerged) {
                Text("🔗 مدمجة", fontSize = 8.sp, color = theme.primaryColor, fontWeight = FontWeight.Bold)
            }
            BasicTextField(
                value = alarm.lessonName,
                onValueChange = { onUpdate(it) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp, 
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) theme.textPrimaryColor else theme.textSecondaryColor
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (alarm.lessonName.isEmpty()) {
                            Text("الحصة ${alarm.lessonIndex}", fontSize = 10.sp, color = theme.textSecondaryColor)
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
