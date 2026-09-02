package com.principal.system.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.principal.system.data.local.TeacherOverviewEntity
import com.principal.system.data.repository.PrincipalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherActivityViewModel @Inject constructor(
    private val repository: PrincipalRepository
) : ViewModel() {

    val teachers: StateFlow<List<TeacherOverviewEntity>> = repository.teachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
fun TeacherActivityRadarScreen(
    onBack: () -> Unit,
    viewModel: TeacherActivityViewModel = hiltViewModel()
) {
    val teachers by viewModel.teachers.collectAsState()
    val isRefreshing by viewModel.isRefreshing
    var searchQuery by remember { mutableStateOf("") }

    val filteredTeachers = remember(teachers, searchQuery) {
        if (searchQuery.isBlank()) teachers
        else teachers.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.subject.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("رادار نشاط المعلمين والواجبات", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المعلم أو المادة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Summary Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("إجمالي المعلمين: ${teachers.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("نشروا واجبات: ${teachers.count { !it.lastAssignmentDate.isNullOrBlank() }}", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (filteredTeachers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد معلمين مسجلين أو مطابقين للبحث", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTeachers, key = { it.id }) { teacher ->
                        TeacherRadarCard(teacher = teacher)
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherRadarCard(teacher: TeacherOverviewEntity) {
    val completionPercent = (teacher.gradeCompletionRate * 100).toInt()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name and Subject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(teacher.fullName, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("المادة: ${teacher.subject}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Surface(
                    color = if (completionPercent >= 80) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "رصد الدرجات: $completionPercent%",
                        color = if (completionPercent >= 80) Color(0xFF065F46) else Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { teacher.gradeCompletionRate },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (completionPercent >= 80) Color(0xFF059669) else Color(0xFFD97706),
                trackColor = Color(0xFFE2E8F0),
            )

            Spacer(Modifier.height(12.dp))

            // Homework Tracker Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (!teacher.lastAssignmentTitle.isNullOrBlank()) Color(0xFF059669) else Color.Gray
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (!teacher.lastAssignmentTitle.isNullOrBlank()) {
                            "آخر واجب: ${teacher.lastAssignmentTitle}"
                        } else {
                            "لم ينشر واجبات مؤخراً"
                        },
                        fontSize = 11.sp,
                        color = if (!teacher.lastAssignmentTitle.isNullOrBlank()) Color(0xFF0F172A) else Color.Gray,
                        fontWeight = if (!teacher.lastAssignmentTitle.isNullOrBlank()) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (!teacher.lastAssignmentDate.isNullOrBlank()) {
                    Text(teacher.lastAssignmentDate, fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            }

            // Pending Q&A from Students
            if (teacher.pendingMessagesCount > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "يوجد ${teacher.pendingMessagesCount} أسئلة معلقة من الطلاب بانتظار رد الأستاذ",
                            fontSize = 11.sp,
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
