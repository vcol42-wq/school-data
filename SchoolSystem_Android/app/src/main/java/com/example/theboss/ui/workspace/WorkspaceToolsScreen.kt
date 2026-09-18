package com.example.theboss.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.theboss.data.local.AlarmEntity
import com.example.theboss.data.local.ExamEntity
import com.example.theboss.data.local.NoteEntity
import com.example.theboss.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkspaceToolsScreen(
    viewModel: WorkspaceToolsViewModel = hiltViewModel()
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf("الملاحظات", "المهام والامتحانات", "المنبهات بومودورو", "حاسبة المعدل")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedPageIndex = selectedSubTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedSubTab) {
                0 -> NotesTab(viewModel)
                1 -> TasksExamsTab(viewModel)
                2 -> AlarmsTab(viewModel)
                3 -> GpaCalculatorTab(viewModel)
            }
        }
    }
}

// 1. تبويب الملاحظات النصية
@Composable
fun NotesTab(viewModel: WorkspaceToolsViewModel) {
    val notes by viewModel.notes.collectAsState()

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var isAddingTextNote by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Toggle Manual Text Note
        if (isAddingTextNote) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("إضافة ملاحظة نصية", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("العنوان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("المحتوى") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (noteTitle.isNotBlank() && noteContent.isNotBlank()) {
                                    viewModel.addTextNote(noteTitle, noteContent)
                                    noteTitle = ""
                                    noteContent = ""
                                    isAddingTextNote = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ")
                        }
                        OutlinedButton(
                            onClick = { isAddingTextNote = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = { isAddingTextNote = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "أضف")
                Spacer(modifier = Modifier.width(8.dp))
                Text("كتابة ملاحظة نصية")
            }
        }

        // Note List
        Text(
            text = "دفتر الملاحظات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                NoteCard(
                    note = note,
                    onDeleteClick = { viewModel.deleteNote(note) }
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(note.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.createdAt))
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// 2. تبويب المهام والامتحانات والعد التنازلي
@Composable
fun TasksExamsTab(viewModel: WorkspaceToolsViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val exams by viewModel.exams.collectAsState()

    var taskTitle by remember { mutableStateOf("") }
    var taskSubject by remember { mutableStateOf("") }
    var isAddingTask by remember { mutableStateOf(false) }

    var examSubject by remember { mutableStateOf("") }
    var examHoursOut by remember { mutableStateOf("") }
    var isAddingExam by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("قائمة المهام والواجبات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isAddingTask = !isAddingTask }) {
                    Icon(if (isAddingTask) Icons.Default.Close else Icons.Default.Add, contentDescription = "إضافة")
                }
            }
        }

        if (isAddingTask) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("عنوان الواجب أو المهمة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = taskSubject,
                            onValueChange = { taskSubject = it },
                            label = { Text("المادة (اختياري)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (taskTitle.isNotBlank()) {
                                    viewModel.addTask(
                                        title = taskTitle,
                                        dueDate = System.currentTimeMillis() + (24 * 3600 * 1000L), // تسليم غداً
                                        priority = 2,
                                        subject = if (taskSubject.isBlank()) null else taskSubject
                                    )
                                    taskTitle = ""
                                    taskSubject = ""
                                    isAddingTask = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إضافة المهمة")
                        }
                    }
                }
            }
        }

        items(tasks) { task ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.setTaskCompletion(task.id, it) }
                        )
                        Column {
                            Text(
                                task.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (task.subjectTag != null) {
                                Text("المادة: ${task.subjectTag}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.deleteTask(task) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Section 2: Exams
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مواعيد الامتحانات (العداد التنازلي)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isAddingExam = !isAddingExam }) {
                    Icon(if (isAddingExam) Icons.Default.Close else Icons.Default.Add, contentDescription = "إضافة")
                }
            }
        }

        if (isAddingExam) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = examSubject,
                            onValueChange = { examSubject = it },
                            label = { Text("المادة الدراسية") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = examHoursOut,
                            onValueChange = { examHoursOut = it },
                            label = { Text("بعد كم ساعة من الآن؟") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val hours = examHoursOut.toLongOrNull()
                                if (examSubject.isNotBlank() && hours != null) {
                                    val examTime = System.currentTimeMillis() + (hours * 3600 * 1000L)
                                    viewModel.addExam(
                                        subjectName = examSubject,
                                        timestamp = examTime,
                                        targetGrade = 100,
                                        notes = "مراجعة شاملة للمنهج"
                                    )
                                    examSubject = ""
                                    examHoursOut = ""
                                    isAddingExam = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("جدولة الامتحان")
                        }
                    }
                }
            }
        }

        items(exams) { exam ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(exam.subjectName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        val remainingMillis = exam.examTimestamp - System.currentTimeMillis()
                        val countdownText = if (remainingMillis <= 0) {
                            "بدأ الامتحان أو انتهى"
                        } else {
                            val days = remainingMillis / (24 * 3600 * 1000L)
                            val hours = (remainingMillis % (24 * 3600 * 1000L)) / (3600 * 1000L)
                            "متبقي: $days يوم و $hours ساعة"
                        }
                        Text(countdownText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.deleteExam(exam) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// 3. تبويب التنبيهات وبومودورو
@Composable
fun AlarmsTab(viewModel: WorkspaceToolsViewModel) {
    val alarms by viewModel.alarms.collectAsState()

    var alarmTitle by remember { mutableStateOf("") }
    var alarmMinutes by remember { mutableStateOf("") }
    var isAddingAlarm by remember { mutableStateOf(false) }

    var pSubject by remember { mutableStateOf("") }
    var pMinutes by remember { mutableStateOf("25") }
    var isStartingPomodoro by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pomodoro Session
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مؤقت بومودورو للتركيز (Pomodoro)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("يساعدك على التركيز في المذاكرة لمدة 25 دقيقة متبوعة باستراحة.", style = MaterialTheme.typography.bodySmall)

                    if (isStartingPomodoro) {
                        OutlinedTextField(
                            value = pSubject,
                            onValueChange = { pSubject = it },
                            label = { Text("المادة الدراسية") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = pMinutes,
                            onValueChange = { pMinutes = it },
                            label = { Text("المدة بالدقائق") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val mins = pMinutes.toIntOrNull()
                                if (pSubject.isNotBlank() && mins != null) {
                                    viewModel.startPomodoro(pSubject, mins)
                                    isStartingPomodoro = false
                                    pSubject = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("بدأ جلسة التركيز")
                        }
                    } else {
                        Button(
                            onClick = { isStartingPomodoro = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = "بومودورو")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل مؤقت بومودورو")
                        }
                    }
                }
            }
        }

        // Custom Alarms
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المنبهات والتذكيرات المجدولة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { isAddingAlarm = !isAddingAlarm }) {
                    Icon(if (isAddingAlarm) Icons.Default.Close else Icons.Default.Add, contentDescription = "إضافة")
                }
            }
        }

        if (isAddingAlarm) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = alarmTitle,
                            onValueChange = { alarmTitle = it },
                            label = { Text("نص التذكير الدراسي") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = alarmMinutes,
                            onValueChange = { alarmMinutes = it },
                            label = { Text("ينطلق بعد كم دقيقة من الآن؟") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val mins = alarmMinutes.toLongOrNull()
                                if (alarmTitle.isNotBlank() && mins != null) {
                                    val trigger = System.currentTimeMillis() + (mins * 60 * 1000L)
                                    viewModel.addExactAlarm(
                                        title = "تذكير دراسي مجدول",
                                        message = alarmTitle,
                                        triggerTime = trigger
                                    )
                                    alarmTitle = ""
                                    alarmMinutes = ""
                                    isAddingAlarm = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ضبط التنبيه الدقيق")
                        }
                    }
                }
            }
        }

        items(alarms) { alarm ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(alarm.message, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(alarm.triggerTime))
                        Text("وقت الرنين: $date", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                        Icon(Icons.Default.Delete, contentDescription = "إلغاء التنبيه", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// 4. تبويب حاسبة المعدل التراكمي
@Composable
fun GpaCalculatorTab(viewModel: WorkspaceToolsViewModel) {
    val courses by viewModel.gpaCourses.collectAsState()
    val calculatedGpa by viewModel.calculatedGpa.collectAsState()

    var cName by remember { mutableStateOf("") }
    var cGrade by remember { mutableStateOf("") }
    var cCredits by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GPA Output display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("المعدل التراكمي الفصلي المتوقع (GPA)", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("%.2f / 4.0", calculatedGpa),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Add course Form
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إدخال المواد لتقدير المعدل", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cName,
                        onValueChange = { cName = it },
                        label = { Text("المادة") },
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = cGrade,
                        onValueChange = { cGrade = it },
                        label = { Text("الدرجة 100") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cCredits,
                        onValueChange = { cCredits = it },
                        label = { Text("الساعات") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        val gradeVal = cGrade.toDoubleOrNull()
                        val creditVal = cCredits.toIntOrNull()
                        if (cName.isNotBlank() && gradeVal != null && creditVal != null) {
                            viewModel.addGpaCourse(cName, gradeVal, creditVal)
                            cName = ""
                            cGrade = ""
                            cCredits = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة للمعدل")
                }
            }
        }

        // Clear All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("المواد المدرجة: ${courses.size}", fontWeight = FontWeight.Bold)
            if (courses.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearGpaCourses() }) {
                    Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // List of courses
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(courses) { course ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(course.name, fontWeight = FontWeight.Bold)
                            Text("الدرجة: ${course.grade} | الساعات: ${course.credits}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.removeGpaCourse(course) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// Simple Helper for Tab implementation since TabRow needs selectedPageIndex on this Compose version
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabRow(
    selectedPageIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedPageIndex,
        modifier = modifier,
        edgePadding = 0.dp
    ) {
        content()
    }
}
