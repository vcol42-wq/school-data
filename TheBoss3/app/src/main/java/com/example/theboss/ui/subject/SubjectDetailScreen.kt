package com.example.theboss.ui.subject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.local.*
import com.example.theboss.data.remote.GeminiStudyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val dao: AppDao,
    private val gemini: GeminiStudyService
) : ViewModel() {
    
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse

    fun getGrades(subjectId: String) = dao.getGradesForSubject(subjectId)
    fun getAssignments(subjectId: String) = dao.getAssignmentsForSubject(subjectId)

    fun askAi(subject: String, topic: String) {
        viewModelScope.launch {
            _aiResponse.value = "جاري التفكير..."
            _aiResponse.value = gemini.explainTopic(subject, topic)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    subjectName: String,
    viewModel: SubjectDetailViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الدرجات", "الواجبات", "المساعد الذكي")

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
                2 -> AiHelperTab(subjectName, viewModel)
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

@Composable
fun AiHelperTab(subjectName: String, viewModel: SubjectDetailViewModel) {
    var topic by remember { mutableStateOf("") }
    val response by viewModel.aiResponse.collectAsState()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("عن ماذا تريد أن تسأل؟") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.askAi(subjectName, topic) }, modifier = Modifier.fillMaxWidth()) {
            Text("اسأل المساعد الذكي")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(response)
    }
}
