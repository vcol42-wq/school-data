package com.example.theboss.ui.subject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(grades) { grade ->
            ListItem(
                headlineContent = { Text(grade.type) },
                supportingContent = { Text("الدرجة: ${grade.score} / ${grade.maxScore}") }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun AssignmentsTab(assignments: List<AssignmentEntity>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(assignments) { assignment ->
            ListItem(
                headlineContent = { Text(assignment.title) },
                supportingContent = { Text(assignment.description) },
                trailingContent = { Checkbox(checked = assignment.isCompleted, onCheckedChange = {}) }
            )
            HorizontalDivider()
        }
    }
}
