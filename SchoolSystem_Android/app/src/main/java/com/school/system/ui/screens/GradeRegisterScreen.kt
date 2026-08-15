package com.school.system.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.system.data.model.Student
import com.school.system.data.model.StudentMarks
import com.school.system.data.model.DailyColumnSetting
import com.school.system.data.model.AbsenceRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeRegisterScreen(
    grade: String,
    section: String,
    subject: String,
    onBack: () -> Unit,
    viewModel: GradeViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsState()
    val columnSettings by viewModel.getDailyColumnSettings("${grade}_${section}_$subject").collectAsState(null)
    val absences by viewModel.absences.collectAsState()
    val config by viewModel.config.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isEditingLocked = remember(config) {
        val hasCloudUrl = config?.cloudUrl?.isNotEmpty() == true
        val isNotVerified = config?.isVerified == false
        val isFinalSyncSeal = config?.syncSealToken?.startsWith("SEAL-") == true
        isFinalSyncSeal || (hasCloudUrl && isNotVerified)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var headerToEdit by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadStudents(grade, section, subject)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("$subject - $grade ($section)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع") }
                },
                actions = {
                    if (selectedTab == 3) {
                        IconButton(
                            onClick = {
                                viewModel.syncAndArchiveAbsences(grade, section, subject) { message ->
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isEditingLocked
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = "مزامنة وأرشفة الغيابات")
                        }
                    }
                    IconButton(
                        onClick = { showAddStudentDialog = true },
                        enabled = !isEditingLocked
                    ) { 
                        Icon(Icons.Default.PersonAdd, contentDescription = "إضافة طالب") 
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isEditingLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (config?.syncSealToken?.startsWith("SEAL-") == true) 
                                "تم قفل السجل سحابياً بشكل نهائي (نقطة اللا عودة) ولا يمكن التعديل." 
                            else 
                                "تعديل السجل متوقف! يرجى إدخال الرمز الموحد وتدقيق الربط السحابي في الإعدادات للتمكن من الرصد.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("سجل الإدارة", modifier = Modifier.padding(12.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("سجل المدرس", modifier = Modifier.padding(12.dp)) }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("سجل اليومي", modifier = Modifier.padding(12.dp)) }
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) { Text("سجل الغيابات", modifier = Modifier.padding(12.dp)) }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (selectedTab) {
                    0 -> AdminRegisterTable(students, listState, { viewModel.updateStudentMarks(it.first, it.second) }, !isEditingLocked)
                    1 -> TeacherRegisterTable(students, listState, { viewModel.updateStudentMarks(it.first, it.second) }, !isEditingLocked)
                    2 -> DailyRegisterTable(
                        students = students,
                        settings = columnSettings ?: DailyColumnSetting("${grade}_${section}_$subject"),
                        listState = listState,
                        onUpdateMarks = { s, m -> viewModel.updateStudentMarks(s, m) },
                        onEditHeader = { headerToEdit = it },
                        isEditable = !isEditingLocked
                    )
                    3 -> AbsencesRegisterTable(
                        students = students,
                        listState = listState,
                        absences = absences,
                        selectedDate = selectedDate.toString(),
                        onToggleAbsence = { student, isAbsent ->
                            viewModel.toggleAbsence(student, selectedDate.toString(), isAbsent)
                        },
                        onDateChange = { selectedDate = it },
                        isEditable = !isEditingLocked
                    )
                }
            }
        }

        if (showAddStudentDialog) {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddStudentDialog = false },
                title = { Text("إضافة طالب") },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }) },
                confirmButton = { Button(onClick = { viewModel.addMockStudent(grade, section, subject, name); showAddStudentDialog = false }) { Text("إضافة") } }
            )
        }

        headerToEdit?.let { index ->
            var newName by remember { mutableStateOf(columnSettings?.columnNames?.get(index) ?: "") }
            AlertDialog(
                onDismissRequest = { headerToEdit = null },
                title = { Text("تسمية العمود") },
                text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("اسم العمود") }) },
                confirmButton = {
                    Button(onClick = {
                        val current = columnSettings ?: DailyColumnSetting("${grade}_${section}_$subject")
                        val newList = current.columnNames.toMutableList()
                        newList[index] = newName
                        viewModel.updateDailyColumnSettings(current.copy(columnNames = newList))
                        headerToEdit = null
                    }) { Text("حفظ") }
                }
            )
        }
    }
}

@Composable
fun AdminRegisterTable(students: List<Student>, listState: LazyListState, onUpdate: (Pair<Student, StudentMarks>) -> Unit, isEditable: Boolean = true) {
    val hScroll = rememberScrollState()
    val cellW = 58.dp
    val nameW = 140.dp
    val rowH = 42.dp
    val fontSize = 11.sp

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column {
        Row(modifier = Modifier.background(Color(0xFFE2EFE9))) {
            PaperHeaderCell("الاسم", nameW, rowH * 2, fontSize)
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                Column {
                    Row {
                        PaperHeaderCell("الفصل 1", cellW, rowH, fontSize)
                        PaperHeaderCell("نصف السنة", cellW, rowH, fontSize)
                        PaperHeaderCell("الفصل 2", cellW, rowH, fontSize)
                        PaperHeaderCell("السعي", cellW, rowH, fontSize)
                        PaperHeaderCell("د1 (نهائي)", cellW, rowH, fontSize)
                        PaperHeaderCell("د2 (نهائي)", cellW, rowH, fontSize)
                        PaperHeaderCell("الدرجة", cellW, rowH, fontSize)
                    }
                    Row {
                        PaperHeaderCell("معدل", cellW, rowH, fontSize)
                        PaperHeaderCell("درجة", cellW, rowH, fontSize)
                        PaperHeaderCell("معدل", cellW, rowH, fontSize)
                        PaperHeaderCell("سنوي", cellW, rowH, fontSize)
                        PaperHeaderCell("رصد", cellW, rowH, fontSize)
                        PaperHeaderCell("رصد", cellW, rowH, fontSize)
                        PaperHeaderCell("نهائية", cellW, rowH, fontSize)
                    }
                }
            }
        }
        LazyColumn(state = listState) {
            items(students, key = { it.id }) { std ->
                val m = std.marks
                val isDuplicate = std.fullName in duplicateNames
                Row {
                    PaperTableCell(std.fullName, nameW, rowH, fontSize, textColor = if (isDuplicate) Color(0xFFDC2626) else Color.Black)
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        if (isEditable) {
                            PaperInputCell(m.term1Avg, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(term1Avg = valNew))) }
                            PaperInputCell(m.midtermFinalGrade, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(midtermFinalGrade = valNew))) }
                            PaperInputCell(m.term2Avg, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(term2Avg = valNew))) }
                            PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                            PaperInputCell(m.finalWrittenD1, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(finalWrittenD1 = valNew))) }
                            PaperInputCell(m.finalWrittenD2 ?: 0f, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(finalWrittenD2 = if (valNew == 0f) null else valNew))) }
                            PaperTableCell(m.finalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true, textColor = if(m.finalGrade < 50) Color.Red else Color.Black)
                        } else {
                            PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                            PaperTableCell(m.finalWrittenD1.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(if (m.finalWrittenD2 == null) "-" else m.finalWrittenD2!!.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.finalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherRegisterTable(students: List<Student>, listState: LazyListState, onUpdate: (Pair<Student, StudentMarks>) -> Unit, isEditable: Boolean = true) {
    val hScroll = rememberScrollState()
    val cellW = 50.dp
    val nameW = 130.dp
    val rowH = 40.dp
    val fontSize = 10.5.sp

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column {
        Row(modifier = Modifier.background(Color(0xFFF1F5F9))) {
            PaperHeaderCell("الاسم", nameW, rowH * 2, fontSize)
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                Column {
                    Row {
                        PaperHeaderCell("شهر 1", cellW, rowH, fontSize)
                        PaperHeaderCell("شهر 2", cellW, rowH, fontSize)
                        PaperHeaderCell("فص1", cellW, rowH, fontSize)
                        PaperHeaderCell("نصف السنة", cellW, rowH, fontSize)
                        PaperHeaderCell("شهر 3", cellW, rowH, fontSize)
                        PaperHeaderCell("شهر 4", cellW, rowH, fontSize)
                        PaperHeaderCell("فص2", cellW, rowH, fontSize)
                        PaperHeaderCell("السعي", cellW, rowH, fontSize)
                    }
                    Row {
                        repeat(8) { PaperHeaderCell("درجة", cellW, rowH, fontSize) }
                    }
                }
            }
        }
        LazyColumn(state = listState) {
            items(students, key = { it.id }) { std ->
                val m = std.marks
                val isDuplicate = std.fullName in duplicateNames
                Row {
                    PaperTableCell(std.fullName, nameW, rowH, fontSize, textColor = if (isDuplicate) Color(0xFFDC2626) else Color.Black)
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        if (isEditable) {
                            PaperInputCell(m.m1MonthAvg, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(m1MonthAvg = valNew))) }
                            PaperInputCell(m.m2MonthAvg, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(m2MonthAvg = valNew))) }
                            PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize)
                            PaperInputCell(m.midtermFinalGrade, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(midtermFinalGrade = valNew))) }
                            PaperInputCell(m.m3MonthAvg, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(m3MonthAvg = valNew))) }
                            PaperInputCell(m.m4MonthAvg, cellW, rowH, fontSize) { valNew -> onUpdate(Pair(std, m.copy(m4MonthAvg = valNew))) }
                            PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                        } else {
                            PaperTableCell(m.m1MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.m2MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.m3MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.m4MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize)
                            PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyRegisterTable(
    students: List<Student>,
    settings: DailyColumnSetting,
    listState: LazyListState,
    onUpdateMarks: (Student, StudentMarks) -> Unit,
    onEditHeader: (Int) -> Unit,
    isEditable: Boolean = true
) {
    val hScroll = rememberScrollState()
    val cellW = 42.dp
    val nameW = 120.dp
    val rowH = 38.dp
    val fontSize = 10.sp

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column {
        Row(modifier = Modifier.background(Color(0xFFFFF7ED))) {
            PaperHeaderCell("اسم الطالب", nameW, rowH * 2, fontSize)
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                Column {
                    Row {
                        PaperHeaderCell("الشهر الأول", cellW * 8, rowH, fontSize)
                        PaperHeaderCell("الشهر الثاني", cellW * 8, rowH, fontSize)
                        PaperHeaderCell("فص1", cellW, rowH, fontSize)
                        PaperHeaderCell("نصف السنة", cellW * 8, rowH, fontSize)
                        PaperHeaderCell("الشهر الثالث", cellW * 8, rowH, fontSize)
                        PaperHeaderCell("الشهر الرابع", cellW * 8, rowH, fontSize)
                        PaperHeaderCell("فص2", cellW, rowH, fontSize)
                        PaperHeaderCell("السعي", cellW, rowH, fontSize)
                        PaperHeaderCell("الامتحان النهائي", cellW * 8, rowH, fontSize)
                        PaperHeaderCell("الدرجة النهائية", cellW, rowH, fontSize)
                    }
                    Row {
                        for (i in 0 until 5) {
                            val headerText = settings.columnNames.getOrElse(i) { "" }
                            if (isEditable) {
                                EditableHeaderCell(headerText, cellW, rowH, fontSize) { onEditHeader(i) }
                            } else {
                                PaperHeaderCell(headerText, cellW, rowH, fontSize)
                            }
                        }
                        PaperHeaderCell("م", cellW, rowH, fontSize)
                        PaperHeaderCell("تحر", cellW, rowH, fontSize)
                        PaperHeaderCell("مجم", cellW, rowH, fontSize)
                        for (i in 5 until 10) {
                            val headerText = settings.columnNames.getOrElse(i) { "" }
                            if (isEditable) {
                                EditableHeaderCell(headerText, cellW, rowH, fontSize) { onEditHeader(i) }
                            } else {
                                PaperHeaderCell(headerText, cellW, rowH, fontSize)
                            }
                        }
                        PaperHeaderCell("م", cellW, rowH, fontSize)
                        PaperHeaderCell("تحر", cellW, rowH, fontSize)
                        PaperHeaderCell("مجم", cellW, rowH, fontSize)
                        PaperHeaderCell("فص1", cellW, rowH, fontSize)
                        for (i in 10 until 15) {
                            val headerText = settings.columnNames.getOrElse(i) { "" }
                            if (isEditable) {
                                EditableHeaderCell(headerText, cellW, rowH, fontSize) { onEditHeader(i) }
                            } else {
                                PaperHeaderCell(headerText, cellW, rowH, fontSize)
                            }
                        }
                        PaperHeaderCell("م", cellW, rowH, fontSize)
                        PaperHeaderCell("درجة", cellW, rowH, fontSize)
                        PaperHeaderCell("(نص)", cellW, rowH, fontSize)
                        for (i in 15 until 20) {
                            val headerText = settings.columnNames.getOrElse(i) { "" }
                            if (isEditable) {
                                EditableHeaderCell(headerText, cellW, rowH, fontSize) { onEditHeader(i) }
                            } else {
                                PaperHeaderCell(headerText, cellW, rowH, fontSize)
                            }
                        }
                        PaperHeaderCell("م", cellW, rowH, fontSize)
                        PaperHeaderCell("تحر", cellW, rowH, fontSize)
                        PaperHeaderCell("مجم", cellW, rowH, fontSize)
                        for (i in 20 until 25) {
                            val headerText = settings.columnNames.getOrElse(i) { "" }
                            if (isEditable) {
                                EditableHeaderCell(headerText, cellW, rowH, fontSize) { onEditHeader(i) }
                            } else {
                                PaperHeaderCell(headerText, cellW, rowH, fontSize)
                            }
                        }
                        PaperHeaderCell("م", cellW, rowH, fontSize)
                        PaperHeaderCell("تحر", cellW, rowH, fontSize)
                        PaperHeaderCell("مجم", cellW, rowH, fontSize)
                        PaperHeaderCell("فص2", cellW, rowH, fontSize)
                        PaperHeaderCell("سعي", cellW, rowH, fontSize)
                        for (i in 25 until 30) {
                            val headerText = settings.columnNames.getOrElse(i) { "" }
                            if (isEditable) {
                                EditableHeaderCell(headerText, cellW, rowH, fontSize) { onEditHeader(i) }
                            } else {
                                PaperHeaderCell(headerText, cellW, rowH, fontSize)
                            }
                        }
                        PaperHeaderCell("م", cellW, rowH, fontSize)
                        PaperHeaderCell("درجة", cellW, rowH, fontSize)
                        PaperHeaderCell("دور2", cellW, rowH, fontSize)
                        PaperHeaderCell("(د نهائية)", cellW, rowH, fontSize)
                    }
                }
            }
        }
        LazyColumn(state = listState) {
            items(students, key = { it.id }) { std ->
                val m = std.marks
                val isDuplicate = std.fullName in duplicateNames
                Row {
                    PaperTableCell(std.fullName, nameW, rowH, fontSize, textColor = if (isDuplicate) Color(0xFFDC2626) else Color.Black)
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        for (i in 0 until 5) {
                            val v = m.m1Daily.getOrElse(i) { 0f }
                            if (isEditable) {
                                PaperInputCell(v, cellW, rowH, fontSize) { valNew -> 
                                    val nl = m.m1Daily.toMutableList(); nl[i] = valNew; onUpdateMarks(std, m.copy(m1Daily = nl)) 
                                }
                            } else {
                                PaperTableCell(if (v == 0f) "" else v.toInt().toString(), cellW, rowH, fontSize)
                            }
                        }
                        PaperTableCell(m.m1Daily.sum().toInt().toString(), cellW, rowH, fontSize)
                        if (isEditable) {
                            PaperInputCell(m.m1Written, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(m1Written = valNew)) }
                        } else {
                            PaperTableCell(if (m.m1Written == 0f) "" else m.m1Written.toInt().toString(), cellW, rowH, fontSize)
                        }
                        PaperTableCell(m.m1MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                        
                        for (i in 0 until 5) {
                            val v = m.m2Daily.getOrElse(i) { 0f }
                            if (isEditable) {
                                PaperInputCell(v, cellW, rowH, fontSize) { valNew -> 
                                    val nl = m.m2Daily.toMutableList(); nl[i] = valNew; onUpdateMarks(std, m.copy(m2Daily = nl)) 
                                }
                            } else {
                                PaperTableCell(if (v == 0f) "" else v.toInt().toString(), cellW, rowH, fontSize)
                            }
                        }
                        PaperTableCell(m.m2Daily.sum().toInt().toString(), cellW, rowH, fontSize)
                        if (isEditable) {
                            PaperInputCell(m.m2Written, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(m2Written = valNew)) }
                        } else {
                            PaperTableCell(if (m.m2Written == 0f) "" else m.m2Written.toInt().toString(), cellW, rowH, fontSize)
                        }
                        PaperTableCell(m.m2MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                        
                        PaperTableCell(m.term1Avg.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                        
                        for (i in 0 until 5) {
                            val v = m.midtermOral.getOrElse(i) { 0f }
                            if (isEditable) {
                                PaperInputCell(v, cellW, rowH, fontSize) { valNew -> 
                                    val nl = m.midtermOral.toMutableList(); nl[i] = valNew; onUpdateMarks(std, m.copy(midtermOral = nl)) 
                                }
                            } else {
                                PaperTableCell(if (v == 0f) "" else v.toInt().toString(), cellW, rowH, fontSize)
                            }
                        }
                        PaperTableCell(m.midtermOral.sum().toInt().toString(), cellW, rowH, fontSize)
                        if (isEditable) {
                            PaperInputCell(m.midtermScore, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(midtermScore = valNew)) }
                        } else {
                            PaperTableCell(if (m.midtermScore == 0f) "" else m.midtermScore.toInt().toString(), cellW, rowH, fontSize)
                        }
                        PaperTableCell(m.midtermFinalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true)
 
                        for (i in 0 until 5) {
                            val v = m.m3Daily.getOrElse(i) { 0f }
                            if (isEditable) {
                                PaperInputCell(v, cellW, rowH, fontSize) { valNew -> 
                                    val nl = m.m3Daily.toMutableList(); nl[i] = valNew; onUpdateMarks(std, m.copy(m3Daily = nl)) 
                                }
                            } else {
                                PaperTableCell(if (v == 0f) "" else v.toInt().toString(), cellW, rowH, fontSize)
                            }
                        }
                        PaperTableCell(m.m3Daily.sum().toInt().toString(), cellW, rowH, fontSize)
                        if (isEditable) {
                            PaperInputCell(m.m3Written, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(m3Written = valNew)) }
                        } else {
                            PaperTableCell(if (m.m3Written == 0f) "" else m.m3Written.toInt().toString(), cellW, rowH, fontSize)
                        }
                        PaperTableCell(m.m3MonthAvg.toInt().toString(), cellW, rowH, fontSize)
 
                        for (i in 0 until 5) {
                            val v = m.m4Daily.getOrElse(i) { 0f }
                            if (isEditable) {
                                PaperInputCell(v, cellW, rowH, fontSize) { valNew -> 
                                    val nl = m.m4Daily.toMutableList(); nl[i] = valNew; onUpdateMarks(std, m.copy(m4Daily = nl)) 
                                }
                            } else {
                                PaperTableCell(if (v == 0f) "" else v.toInt().toString(), cellW, rowH, fontSize)
                            }
                        }
                        PaperTableCell(m.m4Daily.sum().toInt().toString(), cellW, rowH, fontSize)
                        if (isEditable) {
                            PaperInputCell(m.m4Written, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(m4Written = valNew)) }
                        } else {
                            PaperTableCell(if (m.m4Written == 0f) "" else m.m4Written.toInt().toString(), cellW, rowH, fontSize)
                        }
                        PaperTableCell(m.m4MonthAvg.toInt().toString(), cellW, rowH, fontSize)
                        
                        PaperTableCell(m.term2Avg.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                        PaperTableCell(m.annualAverage.toInt().toString(), cellW, rowH, fontSize, isBold = true)
 
                        for (i in 0 until 5) {
                            val v = m.finalOral.getOrElse(i) { 0f }
                            if (isEditable) {
                                PaperInputCell(v, cellW, rowH, fontSize) { valNew -> 
                                    val nl = m.finalOral.toMutableList(); nl[i] = valNew; onUpdateMarks(std, m.copy(finalOral = nl)) 
                                }
                            } else {
                                PaperTableCell(if (v == 0f) "" else v.toInt().toString(), cellW, rowH, fontSize)
                            }
                        }
                        PaperTableCell(m.finalOral.sum().toInt().toString(), cellW, rowH, fontSize)
                        if (isEditable) {
                            PaperInputCell(m.finalWrittenD1, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(finalWrittenD1 = valNew)) }
                            PaperInputCell(m.finalWrittenD2 ?: 0f, cellW, rowH, fontSize) { valNew -> onUpdateMarks(std, m.copy(finalWrittenD2 = if (valNew == 0f) null else valNew)) }
                        } else {
                            PaperTableCell(if (m.finalWrittenD1 == 0f) "" else m.finalWrittenD1.toInt().toString(), cellW, rowH, fontSize)
                            val fd2 = m.finalWrittenD2
                            PaperTableCell(if (fd2 == null || fd2 == 0f) "" else fd2.toInt().toString(), cellW, rowH, fontSize)
                        }
                        PaperTableCell(m.finalGrade.toInt().toString(), cellW, rowH, fontSize, isBold = true)
                    }
                }
            }
        }
    }
}

@Composable
fun PaperHeaderCell(text: String, width: Dp, height: Dp, fontSize: TextUnit) {
    Box(modifier = Modifier.width(width).height(height).border(0.5.dp, Color.Gray).background(Color(0xFFE2EFE9)), contentAlignment = Alignment.Center) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun EditableHeaderCell(text: String, width: Dp, height: Dp, fontSize: TextUnit, onClick: () -> Unit) {
    Box(modifier = Modifier.width(width).height(height).border(0.5.dp, Color.Gray).background(Color(0xFFE2EFE9)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(if(text.isEmpty()) "..." else text, fontSize = fontSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun PaperTableCell(text: String, width: Dp, height: Dp, fontSize: TextUnit, isBold: Boolean = false, textColor: Color = Color.Black) {
    Box(modifier = Modifier.width(width).height(height).border(0.5.dp, Color.LightGray), contentAlignment = Alignment.Center) {
        Text(text, fontSize = fontSize, fontWeight = if(isBold) FontWeight.Bold else FontWeight.Normal, color = textColor, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun PaperInputCell(value: Float, width: Dp, height: Dp, fontSize: TextUnit, onValueChange: (Float) -> Unit) {
    var text by remember(value) { mutableStateOf(if(value == 0f) "" else value.toInt().toString()) }
    Box(modifier = Modifier.width(width).height(height).border(0.5.dp, Color.LightGray), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = text,
            onValueChange = { input -> 
                val filtered = input.filter { it.isDigit() }
                if (filtered.length <= 3) {
                    text = filtered
                    onValueChange(filtered.toFloatOrNull() ?: 0f)
                }
            },
            textStyle = TextStyle(fontSize = fontSize, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AbsencesRegisterTable(
    students: List<Student>,
    listState: LazyListState,
    absences: List<AbsenceRecord>,
    selectedDate: String,
    onToggleAbsence: (Student, Boolean) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    isEditable: Boolean = true
) {
    val date = java.time.LocalDate.parse(selectedDate)
    val cellW = 80.dp
    val nameW = 150.dp
    val rowH = 45.dp
    val fontSize = 12.sp

    val duplicateNames = remember(students) {
        students.groupBy { it.fullName }.filter { it.value.size > 1 }.keys
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateChange(date.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "اليوم السابق")
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = { onDateChange(date.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "اليوم التالي")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PaperHeaderCell("اسم الطالب", nameW, rowH, fontSize)
            PaperHeaderCell("الحالة (غائب)", cellW, rowH, fontSize)
            PaperHeaderCell("الغيابات الكلية", cellW + 40.dp, rowH, fontSize)
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(students, key = { it.id }) { std ->
                val isAbsent = absences.any { it.studentId == std.id && it.dateString == selectedDate }
                val activeCount = absences.count { it.studentId == std.id }
                val totalAbsences = std.historicalAbsences + activeCount
                val isDuplicate = std.fullName in duplicateNames

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isEditable) Modifier.clickable { onToggleAbsence(std, !isAbsent) } else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaperTableCell(std.fullName, nameW, rowH, fontSize, textColor = if (isDuplicate) Color(0xFFDC2626) else Color.Black)
                    Box(modifier = Modifier.width(cellW).height(rowH).border(0.5.dp, Color.LightGray), contentAlignment = Alignment.Center) {
                        Checkbox(
                            checked = isAbsent,
                            onCheckedChange = { onToggleAbsence(std, it) },
                            enabled = isEditable
                        )
                    }
                    PaperTableCell("$totalAbsences أيام غياب", cellW + 40.dp, rowH, fontSize, isBold = totalAbsences > 0, textColor = if (totalAbsences > 3) Color.Red else Color.Black)
                }
            }
        }
    }
}
