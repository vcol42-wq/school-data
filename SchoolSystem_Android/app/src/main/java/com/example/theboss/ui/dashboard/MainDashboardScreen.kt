package com.example.theboss.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.local.AppDao
import com.example.theboss.data.local.AssignmentEntity
import com.example.theboss.data.local.AttendanceEntity
import com.example.theboss.data.local.SubjectEntity
import com.example.theboss.data.repository.StudentRepository
import android.content.Context
import androidx.compose.foundation.BorderStroke
import com.example.theboss.data.remote.DirectiveDto
import com.example.theboss.ui.workspace.WorkspaceToolsScreen
import com.example.theboss.utils.NotificationHelper
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppDao,
    private val repository: StudentRepository
) : ViewModel() {
    val subjects: StateFlow<List<SubjectEntity>> = dao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<AssignmentEntity>> = dao.getAllAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<AttendanceEntity>> = dao.getAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directives: StateFlow<List<DirectiveDto>> = repository.directives
    val syncedSchedule: StateFlow<String> = repository.syncedSchedule

    val schoolName = repository.getSchoolName() ?: "ثانوية كعب بن مالك المسائية"

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        refreshData()
        startPeriodicDirectivesSync()
    }

    private fun startPeriodicDirectivesSync() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000L)
                val schoolId = repository.getSchoolId() ?: "SCH-KAB2-9359"
                try {
                    repository.syncDirectives(schoolId)
                } catch (e: Exception) {
                    // ignore background network issues
                }
            }
        }
    }

    fun refreshData() {
        val schoolId = repository.getSchoolId() ?: "SCH-KAB2-9359"
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // مزامنة الجدول والتعاميم بالدرجة الأولى لتحديث واجهة الطالب فوراً
                repository.syncSchedule(schoolId)
                repository.syncDirectives(schoolId)
                repository.syncDailyAssignments(schoolId)
                repository.syncTimetableAndInstructions(schoolId)
                repository.syncDirectMessages()
                val deviceId = repository.getDeviceId()
                repository.syncAttendance(schoolId, deviceId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun updateStudentAndSchool(
        name: String,
        grade: String,
        section: String,
        schoolCode: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (schoolCode.isNotBlank()) {
                    repository.verifySchoolCode(schoolCode)
                }
                val finalSchoolId = repository.getSchoolId() ?: "SCH-KAB2-9359"
                repository.syncSchedule(finalSchoolId)
                repository.syncDirectives(finalSchoolId)
                repository.syncDailyAssignments(finalSchoolId)
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun syncScheduleManual(onComplete: (Boolean) -> Unit = {}) {
        val schoolId = repository.getSchoolId() ?: "SCH-KAB2-9359"
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val res = repository.syncSchedule(schoolId)
                repository.syncDirectives(schoolId)
                onComplete(res.isSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleAssignment(assignment: AssignmentEntity) {
        viewModelScope.launch {
            val newStatus = !assignment.isCompleted
            dao.updateAssignmentStatus(assignment.id, newStatus)
            if (newStatus) {
                val notifId = (assignment.subjectName + assignment.title).hashCode()
                NotificationHelper.cancelNotification(context, notifId)
            }
            try {
                val allActive = dao.getAllAssignments().firstOrNull()?.filter { !it.isCompleted }?.map { it.subjectName }?.toSet() ?: emptySet()
                val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("active_homework_subjects", Gson().toJson(allActive)).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSubjectClick: (String) -> Unit,
    onScheduleClick: () -> Unit,
    onExamsClick: () -> Unit,
    onTutoringClick: () -> Unit = {},
    onParentAttendanceClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val subjects by viewModel.subjects.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val directives by viewModel.directives.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var compactAttendanceWidget by rememberSaveable { mutableStateOf(false) }
    var compactScheduleWidget by rememberSaveable { mutableStateOf(false) }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (currentTab) {
                                0 -> viewModel.schoolName
                                else -> "مساحة الأدوات المدرسية"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }
                },
                actions = {
                    if (currentTab == 0) {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "تحديث البيانات", tint = Color(0xFF059669))
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات والربط", tint = Color(0xFF475569))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "أدواتي") },
                    label = { Text("أدواتي") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                0 -> {
                    val pendingAssignments = assignments.filter { !it.isCompleted }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Class Timetable & Schedule Hub (جدول دروس الشعبة الأسبوعي في الأعلى تماماً)
                        item(span = { GridItemSpan(2) }) {
                            StudentScheduleViewerCard(
                                onClick = onScheduleClick,
                                compact = compactScheduleWidget,
                                onToggleCompact = { compactScheduleWidget = !compactScheduleWidget }
                            )
                        }

                        // 2. Directives & Instructions (بطاقة توجيهات الإدارة المدرسية تحت الجدول مباشرة)
                        if (directives.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                StudentDirectivesCard(directives = directives)
                            }
                        }

                        // 3. Urgent Homework Banner (الواجبات والمهام العاجلة)
                        if (pendingAssignments.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                UrgentHomeworkCenter(
                                    assignments = pendingAssignments,
                                    onToggleComplete = { viewModel.toggleAssignment(it) }
                                )
                            }
                        }

                        // 4. Parent Guardian Attendance Hub (متابعة ولي أمر الطالب)
                        item(span = { GridItemSpan(2) }) {
                            ParentGuardianAttendanceHubCard(
                                attendanceList = attendance,
                                onClick = onParentAttendanceClick,
                                compact = compactAttendanceWidget,
                                onToggleCompact = { compactAttendanceWidget = !compactAttendanceWidget }
                            )
                        }

                        // Smart Study Priority Advisor (المستشار الذكي ومؤشر التفوق)
                        item(span = { GridItemSpan(2) }) {
                            val nextUrgent = pendingAssignments.firstOrNull()
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Text("مؤشر إنجاز الواجبات والتفوق 🎯", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                        }
                                        Surface(
                                            color = Color(0xFFEDE9FE),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("مؤشر الأداء", color = Color(0xFF6D28D9), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Text(
                                        text = if (nextUrgent != null) {
                                            "أولوية المذاكرة المقترحة لك اليوم هي مادة (${nextUrgent.subjectName}) لإنجاز واجب: ${nextUrgent.title}."
                                        } else {
                                            "أحسنت! جميع واجباتك مكتملة حتى الآن. يمكنك استثمار الوقت بمراجعة الدروس القادمة أو طرح استفسار في قناة التقوية."
                                        },
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF334155),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // 4. Motivational Card
                        item(span = { GridItemSpan(2) }) {
                            MotivationalCard()
                        }

                        // 5. Section Header for Subjects
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المواد الدراسية المقررة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${subjects.size} مواد",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (subjects.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("لا توجد مواد مسجلة حالياً في حساب المدرسة", color = Color.Gray, fontSize = 12.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Button(onClick = { viewModel.refreshData() }) {
                                            Text("تحديث ومزامنة المواد من المدرسة")
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Subjects Cards
                        items(subjects) { subject ->
                            SubjectCard(subject = subject, onClick = { onSubjectClick(subject.id) })
                        }
                    }
                }
                1 -> {
                    WorkspaceToolsScreen()
                }
            }
        }
    }
}

/**
 * Directives & Instructions Card (توجيهات الإدارة المدرسية المعتمدة المبنية حديثاً تحت الجدول)
 */
@Composable
private fun StudentDirectivesCard(directives: List<DirectiveDto>) {
    if (directives.isEmpty()) return
    var isExpanded by remember { mutableStateOf(false) }

    // Auto-expand automatically when directives arrive or update
    val directivesFingerprint = remember(directives) {
        directives.map { it.id }.sorted().joinToString(",")
    }
    LaunchedEffect(directivesFingerprint) {
        if (directives.isNotEmpty()) {
            isExpanded = true
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "توجيهات الإدارة المدرسية 📢",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399))
                            ) {
                                Text(
                                    text = "${directives.size}",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (directives.size == 1) "تعميم جديد اعتماد رسمي" else "${directives.size} تعاميم إدارية رسمية",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "طي" else "توسيع",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF334155))

                    directives.forEachIndexed { idx, directive ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (idx == 0) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (idx == 0) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF475569).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, if (idx == 0) Color(0xFF38BDF8) else Color(0xFF64748B))
                                    ) {
                                        Text(
                                            text = if (idx == 0) "⭐ أحدث تعميم" else "تعميم مدرسي",
                                            color = if (idx == 0) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = directive.title,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = directive.content,
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 18.sp
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
 * Urgent Homework Banner with Interactive Emerald Glow Border & Hot Badge 🔥
 */
@Composable
fun UrgentHomeworkCenter(
    assignments: List<AssignmentEntity>,
    onToggleComplete: (AssignmentEntity) -> Unit
) {
    // Infinite glow pulse transition
    val infiniteTransition = rememberInfiniteTransition(label = "emerald_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val emeraldBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF10B981).copy(alpha = glowAlpha),
            Color(0xFF059669),
            Color(0xFF34D399).copy(alpha = glowAlpha),
            Color(0xFF10B981)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0xFF10B981).copy(alpha = 0.4f))
            .border(width = 2.dp, brush = emeraldBrush, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0FDF4) // Light emerald surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Hot Badge and Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFDC2626),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "واجب جديد عاجل 🔥",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${assignments.size} مهام مطلوبة",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Carousel or List of Hot Assignments
            assignments.take(3).forEach { assignment ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (assignment.isPrivateTutoring) Color(0xFF8B5CF6) else Color(0xFF059669),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (assignment.isPrivateTutoring) "تقوية خاصة ⭐" else assignment.subjectName.ifEmpty { "واجب مدرسي" },
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "تسليم: ${assignment.dueDateString.ifEmpty { "اليوم" }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = assignment.title,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (assignment.description.isNotBlank()) {
                                Text(
                                    text = assignment.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleComplete(assignment) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF10B981))
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = "إنجاز الواجب",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parent Guardian Attendance Hub Card (متابعة ولي أمر الطالب 👨‍👩‍👧‍👦)
 */
@Composable
fun ParentGuardianAttendanceHubCard(
    attendanceList: List<AttendanceEntity>,
    onClick: () -> Unit,
    compact: Boolean = false,
    onToggleCompact: () -> Unit = {}
) {
    val absentRecords = attendanceList.filter { it.status.equals("absent", ignoreCase = true) || it.status == "غياب" }
    val totalAbsentDays = absentRecords.map { it.dateString }.distinct().filter { it.isNotBlank() }.size
    val totalAbsentPeriods = absentRecords.size

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (totalAbsentDays > 0) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(19.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("متابعة ولي الأمر", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text(if (totalAbsentDays == 0) "السجل منضبط" else "$totalAbsentDays أيام غياب • $totalAbsentPeriods حصص", fontSize = 10.sp, color = Color.Gray)
                }
                IconButton(onClick = onToggleCompact, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.UnfoldMore, contentDescription = "توسيع الودجت", tint = Color(0xFF0284C7))
                }
            }
        } else Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("متابعة ولي أمر الطالب 👨‍👩‍👧‍👦", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text("سجل غيابات الحصص والانضباط الدراسي", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (totalAbsentDays == 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (totalAbsentDays == 0) "ملتزم وممتاز 🌟" else "$totalAbsentDays أيام غياب ⚠️",
                        color = if (totalAbsentDays == 0) Color(0xFF065F46) else Color(0xFF991B1B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onToggleCompact, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.UnfoldLess, contentDescription = "تصغير الودجت", tint = Color(0xFF0284C7))
                }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick Info Strip
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مجموع الحصص الفائتة: $totalAbsentPeriods حصة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("عرض تفاصيل الحصص", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Class Timetable & Lessons Viewer Hub Card (جدول دروس الشعبة الأسبوعي والتوقيتات 📅)
 */
@Composable
fun StudentScheduleViewerCard(
    onClick: () -> Unit,
    compact: Boolean = false,
    onToggleCompact: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Modern dark slate
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (compact) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("الجدول اليومي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("الحصص والتوقيتات الأسبوعية", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
                IconButton(onClick = onToggleCompact, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.UnfoldMore, contentDescription = "توسيع الودجت", tint = Color(0xFF38BDF8))
                }
                return@Card
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF38BDF8),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "الجدول اليومي 📅",
                            color = Color(0xFF0F172A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "جدول دروس الشعبة والتوقيتات",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "استعراض جدول الحصص اليومية والأسبوعية وتوزيع المواد والتوقيتات المعتمدة",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                color = Color(0xFF0284C7),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = onToggleCompact, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.UnfoldLess, contentDescription = "تصغير الودجت", tint = Color(0xFF38BDF8))
            }
        }
    }
}

@Composable
fun SubjectCard(subject: SubjectEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(subject.teacherName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "المهمة القادمة:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subject.lastAssignment ?: "لا توجد مهام",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFFFFB300)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("الدرجة: ${subject.lastGrade ?: "N/A"}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun MotivationalCard() {
    val quotes = listOf(
        "النجاح هو مجموع جهود صغيرة تتكرر يوماً بعد يوم.",
        "ابدأ من حيث أنت، واستخدم ما لديك، وافعل ما تستطيع.",
        "كلما اجتهدت أكثر، كلما شعرت بسعادة أكبر عند النجاح.",
        "لا تتوقف عندما تتعب، توقف عندما تنتهي.",
        "آمن بنفسك وبكل ما أنت عليه، واعلم أن هناك شيئاً بداخلك أعظم من أي عائق."
    )
    val quote = remember { quotes.random() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "بطاقة التحفيز اليومية",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quote,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                lineHeight = 26.sp
            )
        }
    }
}
