package com.principal.system.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.principal.system.data.local.AttendanceSummaryEntity
import com.principal.system.data.local.CriticalStudentAbsenceEntity
import com.principal.system.data.local.TeacherOverviewEntity
import com.principal.system.data.repository.PrincipalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExecutiveDashboardViewModel @Inject constructor(
    private val repository: PrincipalRepository
) : ViewModel() {

    val schoolName = repository.getSchoolName()
    val schoolCode = repository.getSchoolCode()

    val teachers: StateFlow<List<TeacherOverviewEntity>> = repository.teachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val criticalAbsences: StateFlow<List<CriticalStudentAbsenceEntity>> = repository.criticalAbsences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayAttendance: StateFlow<AttendanceSummaryEntity?> = repository.getTodayAttendanceSummary(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    init {
        refreshMetrics()
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncAllExecutiveMetrics()
            _isRefreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveDashboardScreen(
    onNavigateToTeachers: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ExecutiveDashboardViewModel = hiltViewModel()
) {
    val teachers by viewModel.teachers.collectAsState()
    val criticalAbsences by viewModel.criticalAbsences.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val isRefreshing by viewModel.isRefreshing
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = viewModel.schoolName,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "لوحة القيادة التنفيذية للمدير • كود: ${viewModel.schoolCode}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshMetrics()
                        Toast.makeText(context, "جاري تحديث نبض المدرسة...", Toast.LENGTH_SHORT).show()
                    }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "تحديث", tint = Color(0xFF059669))
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات والربط", tint = Color(0xFF475569))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Attendance Rate Hero Card
            item {
                val rate = todayAttendance?.attendanceRatePercent ?: 100f
                val absentCount = todayAttendance?.absentStudentsCount ?: 0
                val totalCount = todayAttendance?.totalStudents ?: 0

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF059669).copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("نبض الحضور اليومي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Surface(
                                color = if (rate >= 90f) Color(0xFF059669) else Color(0xFFDC2626),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", rate)}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalCount", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("إجمالي الطلاب", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${totalCount - absentCount}", color = Color(0xFF34D399), fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("الطلاب الحاضرين", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$absentCount", color = Color(0xFFF87171), fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("غيابات اليوم", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. Critical Alert Banner (الإنذار المبكر)
            if (criticalAbsences.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAttendance() }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تنبيه المنطقة الحمراء للغيابات (${criticalAbsences.size} طلاب)",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF991B1B),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "تجاوز هؤلاء الطلاب حد الغياب الحرج (3+ أيام). اضغط لمعاينة السجل واتخاذ الإجراء.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // 3. Quick Action Hubs (مراكز الرقابة والقرارات)
            item {
                Text("المراكز التنفيذية والمراقبة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExecutiveHubCard(
                        title = "رادار نشاط المعلمين",
                        subtitle = "${teachers.size} معلماً مسجلاً",
                        icon = Icons.Default.SupervisedUserCircle,
                        accentColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTeachers
                    )

                    ExecutiveHubCard(
                        title = "رادار غيابات الحصص",
                        subtitle = "تتبع الحصص 1 إلى 7",
                        icon = Icons.Default.CalendarMonth,
                        accentColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAttendance
                    )
                }
            }

            // Smart Executive Insights Card (التوصيات الإدارية والتحليل الذكي)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("الرؤى والتحليل الذكي للمدير 🧠", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Surface(
                                color = Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("تحليل فوري", color = Color(0xFF1E40AF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        val summary = todayAttendance
                        val peakPeriod = listOf(
                            1 to (summary?.period1Absences ?: 0),
                            2 to (summary?.period2Absences ?: 0),
                            3 to (summary?.period3Absences ?: 0),
                            4 to (summary?.period4Absences ?: 0),
                            5 to (summary?.period5Absences ?: 0),
                            6 to (summary?.period6Absences ?: 0),
                            7 to (summary?.period7Absences ?: 0)
                        ).maxByOrNull { it.second }

                        val unassignedHomeworkTeachers = teachers.count { it.lastAssignmentDate.isNullOrBlank() }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (peakPeriod != null && peakPeriod.second > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "ذروة غياب الطلاب اليوم في الحصة رقم ${peakPeriod.first} بعدد ${peakPeriod.second} حالات غياب.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (unassignedHomeworkTeachers > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "يوجد $unassignedHomeworkTeachers معلمين لم ينشروا واجبات خلال هذا الأسبوع.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (criticalAbsences.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "يوصى بالتواصل الهاتفي مع أولياء أمور طلاب المنطقة الحمراء (${criticalAbsences.size} طلاب).",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    onClick = onNavigateToBroadcast,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)), // Royal Indigo
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF818CF8).copy(alpha = 0.25f),
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("إرسال تعميم إداري عاجل 📢", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("إشعار فوري لجميع المعلمين والطلاب", color = Color(0xFFC7D2FE), fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }

            // 4. Summary of Teachers Completion
            item {
                Text("ملخص تفاعل المعلمين", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                val totalTeachers = teachers.size
                val teachersWithAssignmentsToday = teachers.count { !it.lastAssignmentDate.isNullOrBlank() }
                val fullyGradedCount = teachers.count { it.gradeCompletionRate >= 0.8f }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اكتمال رصد الدرجات:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text("$fullyGradedCount من أصل $totalTeachers معلمين", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { if (totalTeachers > 0) fullyGradedCount.toFloat() / totalTeachers else 0f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF059669),
                            trackColor = Color(0xFFE2E8F0),
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المعلمون الذين نشروا واجبات:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text("$teachersWithAssignmentsToday من أصل $totalTeachers", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveHubCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
    }
}
