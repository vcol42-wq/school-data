package com.example.theboss.ui.subject

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.theboss.data.local.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val dao: AppDao
) : ViewModel() {
    fun getGrades(subjectId: String) = dao.getGradesForSubject(subjectId)
    fun getAssignments(subjectId: String) = dao.getAssignmentsForSubject(subjectId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    subjectName: String,
    viewModel: SubjectDetailViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الدرجات", "الواجبات والتحضير")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(subjectName) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> GradesTab(viewModel.getGrades(subjectId).collectAsState(initial = emptyList()).value)
                1 -> AssignmentsTab(viewModel.getAssignments(subjectId).collectAsState(initial = emptyList()).value)
            }
        }
    }
}

@Composable
fun GradesTab(grades: List<GradeEntity>) {
    if (grades.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.LightGray
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "لم يتم رصد درجات لهذه المادة بعد من قبل الأستاذ 📝",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "ستظهر درجاتك الرسمية هنا تلقائياً فور رفعها إلى السجل المدرسي السحابي",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(grades) { grade ->
            val isSuccess = grade.score >= 50.0
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = grade.type,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "من أصل ${grade.maxScore.toInt()}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${grade.score.toInt()} / ${grade.maxScore.toInt()}",
                            color = if (isSuccess) Color(0xFF166534) else Color(0xFF991B1B),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentsTab(assignments: List<AssignmentEntity>) {
    if (assignments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "لا توجد واجبات أو تحاضير مطلوبة لهذه المادة حالياً ✓",
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(assignments) { assignment ->
            ListItem(
                headlineContent = { Text(assignment.title, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(assignment.description) },
                trailingContent = { Checkbox(checked = assignment.isCompleted, onCheckedChange = {}) }
            )
            HorizontalDivider()
        }
    }
}
