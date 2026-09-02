package com.principal.system.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.principal.system.data.local.AttendanceSummaryEntity
import com.principal.system.data.local.CriticalStudentAbsenceEntity
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
class AttendanceRadarViewModel @Inject constructor(
    private val repository: PrincipalRepository
) : ViewModel() {

    val criticalAbsences: StateFlow<List<CriticalStudentAbsenceEntity>> = repository.criticalAbsences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todaySummary: StateFlow<AttendanceSummaryEntity?> = repository.getTodayAttendanceSummary(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncAllExecutiveMetrics()
            _isRefreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRadarScreen(
    onBack: () -> Unit,
    viewModel: AttendanceRadarViewModel = hiltViewModel()
) {
    val criticalList by viewModel.criticalAbsences.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val isRefreshing by viewModel.isRefreshing
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("رادار الحضور وغيابات الحصص", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث")
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
            // 1. Today's Period Breakdown (غيابات الحصص من 1 إلى 7)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("توزيع غيابات اليوم حسب الحصص", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(12.dp))

                        val summary = todaySummary
                        val periodsData = listOf(
                            "ح1" to (summary?.period1Absences ?: 0),
                            "ح2" to (summary?.period2Absences ?: 0),
                            "ح3" to (summary?.period3Absences ?: 0),
                            "ح4" to (summary?.period4Absences ?: 0),
                            "ح5" to (summary?.period5Absences ?: 0),
                            "ح6" to (summary?.period6Absences ?: 0),
                            "ح7" to (summary?.period7Absences ?: 0)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(periodsData) { (periodName, count) ->
                                Surface(
                                    color = if (count > 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (count > 0) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(periodName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "$count غائب",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (count > 0) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Red Zone Section Header (المنطقة الحمراء)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = CircleShape,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text("المنطقة الحمراء للغيابات الحرجة (3+ أيام)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    Text("${criticalList.size} طلاب", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // 3. Critical Absences List
            if (criticalList.isEmpty()) {
                item {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("ممتاز! لا يوجد طلاب في المنطقة الحرجة للغيابات حالياً", color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(criticalList, key = { it.studentRecordNumber }) { student ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(student.fullName, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFDC2626),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${student.totalAbsenceDays} أيام غياب",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "الصف: ${student.className} (${student.section}) • رقم القيد: ${student.studentRecordNumber}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                if (!student.guardianPhone.isNullOrBlank() && student.guardianPhone != "غير مسجل") {
                                    Text(
                                        text = "ولي الأمر: ${student.guardianPhone}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!student.guardianPhone.isNullOrBlank() && student.guardianPhone != "غير مسجل") {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.guardianPhone}"))
                                        context.startActivity(intent)
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
