package com.example.theboss.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.local.AppDao
import com.example.theboss.data.local.AttendanceEntity
import com.example.theboss.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentAttendanceViewModel @Inject constructor(
    private val dao: AppDao,
    private val repository: StudentRepository
) : ViewModel() {

    val attendance: StateFlow<List<AttendanceEntity>> = dao.getAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schoolName = repository.getSchoolName() ?: "المدرسة"

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    fun refresh() {
        val schoolId = repository.getSchoolId() ?: return
        val deviceId = repository.getDeviceId()
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncAttendance(schoolId, deviceId)
            _isRefreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAttendanceTrackerScreen(
    onBack: () -> Unit,
    viewModel: ParentAttendanceViewModel = hiltViewModel()
) {
    val attendanceList by viewModel.attendance.collectAsState()
    val isRefreshing by viewModel.isRefreshing

    // حساب الإحصائيات الدقيقة
    val absentRecords = remember(attendanceList) {
        attendanceList.filter { it.status.equals("absent", ignoreCase = true) || it.status == "غياب" }
    }

    val distinctAbsentDays = remember(absentRecords) {
        absentRecords.map { it.dateString }.distinct().filter { it.isNotBlank() }
    }

    val totalAbsentDaysCount = distinctAbsentDays.size
    val totalAbsentPeriodsCount = absentRecords.size

    // مجموع الغيابات في كل حصة من الحصص السبع
    val periodCounts = remember(absentRecords) {
        (1..7).map { period ->
            period to absentRecords.count { it.periodNumber == period }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("متابعة ولي أمر الطالب 👨‍👩‍👧‍👦", fontWeight = FontWeight.Black, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث السجل")
                        }
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
            // 1. Total Absences Hero Card (إجمالي غياب الطالب)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalAbsentDaysCount == 0) Color(0xFF065F46) else if (totalAbsentDaysCount < 3) Color(0xFF1E293B) else Color(0xFF991B1B)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("إجمالي غياب الطالب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (totalAbsentDaysCount == 0) "سجل مثالي 🌟" else if (totalAbsentDaysCount < 3) "مقبول 🟢" else "تنبيه غياب ⚠️",
                                    color = if (totalAbsentDaysCount >= 3) Color(0xFFDC2626) else Color(0xFF059669),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalAbsentDaysCount", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                                Text("إجمالي أيام الغياب", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.2f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalAbsentPeriodsCount", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                                Text("مجموع الحصص الفائتة", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Section Header: Per-period absence count (مجموع الغيابات في كل حصة)
            item {
                Text(
                    text = "مجموع غيابات الطالب في كل حصة (1 إلى 7)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 3. 7 Periods Attendance Grid Cards
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        periodCounts.forEach { (periodNum, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (count > 0) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (count > 0) Color(0xFFDC2626) else Color(0xFF059669),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$periodNum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text("الحصة رقم $periodNum", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Surface(
                                    color = if (count > 0) Color(0xFFFCA5A5).copy(alpha = 0.4f) else Color(0xFFD1FAE5),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (count == 0) "لا يوجد غياب (حاضر دائماً ✓)" else "$count مرات غياب 🔴",
                                        color = if (count > 0) Color(0xFF991B1B) else Color(0xFF065F46),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Detailed Absence Dates Log
            item {
                Text(
                    text = "سجل تواريخ الغياب المسجلة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (absentRecords.isEmpty()) {
                item {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("سجل الطالب نظيف تماماً ولا توجد غيابات مسجلة.", fontWeight = FontWeight.Bold, color = Color(0xFF065F46), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(absentRecords) { record ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(record.dateString.ifEmpty { "تاريخ غير محدد" }, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                                if (record.subject.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text("المادة: ${record.subject}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "غائب في الحصة ${record.periodNumber}",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
