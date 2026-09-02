package com.school.system.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.system.data.model.LessonAlarm
import com.school.system.ui.components.HelpGuideDialog
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
    
    // 0: جدول وضبط الحصص, 1: إعدادات التوقيت ورنين الجرس
    var mainTabSelected by remember { mutableIntStateOf(0) }
    var isGridView by remember { mutableStateOf(false) }
    var showHelpGuideDialog by remember { mutableStateOf(false) }

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val daysValues = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
    val pagerState = rememberPagerState(pageCount = { daysList.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF2563EB).copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF2563EB)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("منبه وجرس الحصص الذكي ⏰", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(
                                text = if (config?.bellRingTeacherOnly == true) "الوضع: رنين لحصصي المقررة فقط" else "الوضع: رنين لكافة الحصص المدرسية",
                                fontSize = 11.sp,
                                color = Color(0xFF2563EB),
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
                actions = {
                    IconButton(onClick = { showHelpGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "دليل الاستخدام والتعليمات", tint = Color(0xFF2563EB))
                    }
                    if (mainTabSelected == 0) {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewDay else Icons.Default.GridView,
                                contentDescription = "تبديل طريقة العرض",
                                tint = Color(0xFF2563EB)
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.applyAutoSchedule()
                        Toast.makeText(context, "تم حساب وتوليد مواعيد الحصص تلقائياً ⚡", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.AutoMode, contentDescription = "حساب تلقائي", tint = Color(0xFF059669))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Primary Tabs: (1. جدول الحصص والتنبيهات) | (2. إعدادات التوقيت ورنين الجرس)
            TabRow(
                selectedTabIndex = mainTabSelected,
                containerColor = Color(0xFFF8FAFC),
                contentColor = Color(0xFF2563EB)
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
                            containerColor = Color.White
                        ) {
                            daysList.forEachIndexed { index, day ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                    text = { Text(day, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        // Top Quick Actions inside Period Tab
                        Surface(
                            color = Color(0xFFF1F5F9),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
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
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(dayAlarms) { alarm ->
                                        AlarmItemCard(
                                            alarm = alarm,
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
                        onApplyAuto = { 
                            viewModel.applyAutoSchedule()
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
    onApplyAuto: () -> Unit
) {
    val context = LocalContext.current
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Ring Mode Selector (رنين لجميع الحصص أم لحصص المعلم فقط)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF2563EB))
                    Spacer(Modifier.width(8.dp))
                    Text("نطاق تشغيل رنين الجرس 🔔", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF0F172A))
                }
                Text("حدد متى ترغب بسماع جرس المنبه أثناء اليوم الدراسي:", fontSize = 12.sp, color = Color(0xFF64748B))

                // Option A: All Periods
                Surface(
                    color = if (!config.bellRingTeacherOnly) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp, 
                        if (!config.bellRingTeacherOnly) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { onUpdateRingMode(false) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !config.bellRingTeacherOnly,
                            onClick = { onUpdateRingMode(false) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("رنين لجميع الحصص (الجدول العام) 🔔", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("تنبيه برنين الجرس مع بداية ونهاية كل حصة وفترة استراحة في المدرسة.", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Option B: Teacher's Lessons Only
                Surface(
                    color = if (config.bellRingTeacherOnly) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp, 
                        if (config.bellRingTeacherOnly) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { onUpdateRingMode(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.bellRingTeacherOnly,
                            onClick = { onUpdateRingMode(true) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("رنين لحصصي المقررة فقط (خاص بالأستاذ) 👨‍🏫", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("تنبيه برنين الجرس فقط في أوقات الحصص والمواد التي تدرّسها أنت حسب جدولك.", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        // Section 2: Timing Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(Modifier.width(8.dp))
                    Text("ضبط التوقيت والمدد الزمنية ⏱️", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF0F172A))
                }

                // Start of school day
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("بداية الدوام المدرسي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("وقت انطلاق الحصة الأولى", fontSize = 11.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                TimePickerDialog(context, { _, h, m -> 
                                    onUpdateSettings(h, m, config.bellLessonDuration, config.bellBreakDuration, config.bellRingtoneUri)
                                }, config.bellStartHour, config.bellStartMinute, true).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                String.format(Locale.US, "%02d:%02d", config.bellStartHour, config.bellStartMinute),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Lesson duration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مدة الحصة الدراسية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${config.bellLessonDuration} دقيقة", fontWeight = FontWeight.Black, color = Color(0xFF2563EB), fontSize = 14.sp)
                    }
                    Slider(
                        value = config.bellLessonDuration.toFloat(),
                        onValueChange = { onUpdateSettings(config.bellStartHour, config.bellStartMinute, it.toInt(), config.bellBreakDuration, config.bellRingtoneUri) },
                        valueRange = 30f..60f,
                        steps = 5
                    )
                }

                // Break duration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مدة الفرصة / الاستراحة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${config.bellBreakDuration} دقيقة", fontWeight = FontWeight.Black, color = Color(0xFF059669), fontSize = 14.sp)
                    }
                    Slider(
                        value = config.bellBreakDuration.toFloat(),
                        onValueChange = { onUpdateSettings(config.bellStartHour, config.bellStartMinute, config.bellLessonDuration, it.toInt(), config.bellRingtoneUri) },
                        valueRange = 5f..30f,
                        steps = 4
                    )
                }

                // Ringtone picker
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("نغمة تنبيه الجرس:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(if (config.bellRingtoneUri != null) "تم تخصيص نغمة ✓" else "النغمة الافتراضية للنظام", fontSize = 11.sp, color = Color.Gray)
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تغيير النغمة", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Apply Button
        Button(
            onClick = onApplyAuto,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AutoMode, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("تطبيق وحساب المواعيد لجميع الأيام ⚡", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: LessonAlarm, 
    onToggle: (Boolean) -> Unit, 
    onUpdate: (String, String, String) -> Unit
) {
    var name by remember(alarm.lessonName) { mutableStateOf(alarm.lessonName) }
    val context = LocalContext.current

    Surface(
        color = if (alarm.isEnabled) Color.White else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp, 
            if (alarm.isEnabled) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (alarm.isEnabled) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period Number Badge
            Surface(
                color = if (alarm.isEnabled) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${alarm.lessonIndex}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الحصة ${alarm.lessonIndex}", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) Color(0xFF1E3A8A) else Color.Gray
                    )
                }

                // Lesson name / subject note
                BasicTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        onUpdate(it, alarm.startTime, alarm.endTime) 
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) Color(0xFF0F172A) else Color.Gray
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (name.isEmpty()) {
                                Text("اسم المادة/الشعبة (اختياري)", color = Color.LightGray, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                // Time Span Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            val (h, m) = alarm.startTime.split(":").map { it.toInt() }
                            TimePickerDialog(context, { _, nh, nm -> 
                                onUpdate(name, String.format(Locale.US, "%02d:%02d", nh, nm), alarm.endTime)
                            }, h, m, true).show()
                        }
                    ) {
                        Text(
                            text = alarm.startTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(" إلى ", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            val (h, m) = alarm.endTime.split(":").map { it.toInt() }
                            TimePickerDialog(context, { _, nh, nm -> 
                                onUpdate(name, alarm.startTime, String.format(Locale.US, "%02d:%02d", nh, nm))
                            }, h, m, true).show()
                        }
                    ) {
                        Text(
                            text = alarm.endTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled, 
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2563EB)
                )
            )
        }
    }
}

@Composable
fun WeeklyGridView(alarms: List<LessonAlarm>, viewModel: SmartBellViewModel) {
    val scrollState = rememberScrollState()
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
            Box(Modifier.width(80.dp).height(44.dp).background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                Text("اليوم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            for (i in 1..6) {
                Box(Modifier.width(110.dp).height(44.dp).background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                    Text("حصة $i", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Data Rows
        daysValues.forEachIndexed { index, dayVal ->
            val dayName = daysList[index]
            Row {
                Box(Modifier.width(80.dp).height(65.dp).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                    Text(dayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                for (i in 1..6) {
                    val alarm = alarms.find { it.dayOfWeek == dayVal && it.lessonIndex == i }
                    if (alarm != null) {
                        GridCell(
                            alarm = alarm, 
                            onToggle = { viewModel.toggleAlarm(alarm, it) },
                            onUpdate = { name -> viewModel.updateAlarmDetails(alarm, name, alarm.startTime, alarm.endTime) }
                        )
                    } else {
                        Box(Modifier.width(110.dp).height(65.dp).border(0.5.dp, Color.LightGray))
                    }
                }
            }
        }
    }
}

@Composable
fun GridCell(alarm: LessonAlarm, onToggle: (Boolean) -> Unit, onUpdate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(65.dp)
            .border(0.5.dp, Color(0xFFE2E8F0))
            .background(if (alarm.isEnabled) Color(0xFFEFF6FF) else Color.White)
            .clickable { onToggle(!alarm.isEnabled) }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${alarm.startTime} - ${alarm.endTime}", 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                color = if (alarm.isEnabled) Color(0xFF2563EB) else Color.Gray
            )
            BasicTextField(
                value = alarm.lessonName,
                onValueChange = { onUpdate(it) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp, 
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) Color(0xFF0F172A) else Color.LightGray
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (alarm.lessonName.isEmpty()) {
                            Text("الحصة ${alarm.lessonIndex}", fontSize = 10.sp, color = Color.LightGray)
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
