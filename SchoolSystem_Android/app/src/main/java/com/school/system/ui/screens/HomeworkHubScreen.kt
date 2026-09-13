package com.school.system.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.school.system.data.SyncRepository
import com.school.system.data.SupabaseDailyAssignmentDto
import com.school.system.data.SupabaseDirectMessageDto
import com.school.system.data.dao.ClassPackageDao
import com.school.system.data.dao.ConfigDao
import com.school.system.data.model.ClassPackage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeworkHubViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val packageDao: ClassPackageDao,
    private val configDao: ConfigDao
) : ViewModel() {

    val config = configDao.getConfig().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val packages = packageDao.getAllPackages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _assignments = MutableStateFlow<List<SupabaseDailyAssignmentDto>>(emptyList())
    val assignments: StateFlow<List<SupabaseDailyAssignmentDto>> = _assignments

    private val _messages = MutableStateFlow<List<SupabaseDirectMessageDto>>(emptyList())
    val messages: StateFlow<List<SupabaseDirectMessageDto>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadData() {
        val conf = config.value ?: return
        val schoolId = conf.schoolId
        val teacherId = conf.syncSealToken ?: conf.managerName

        viewModelScope.launch {
            _isLoading.value = true
            val fetchedAssignments = syncRepository.getDailyAssignments(schoolId, teacherId)
            _assignments.value = fetchedAssignments

            val fetchedMessages = syncRepository.getDirectMessages(schoolId, teacherId)
            _messages.value = fetchedMessages
            _isLoading.value = false
        }
    }

    fun publishAssignment(
        className: String,
        section: String,
        subjectName: String,
        title: String,
        description: String,
        dueDate: String,
        isPrivateTutoring: Boolean,
        studentRecord: String?,
        onComplete: (Boolean) -> Unit
    ) {
        val conf = config.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val dto = SupabaseDailyAssignmentDto(
                school_id = conf.schoolId,
                teacher_id = conf.syncSealToken ?: conf.managerName,
                class_name = className,
                section = section,
                subject_name = subjectName,
                title = title,
                description = description,
                due_date = dueDate,
                is_private_tutoring = isPrivateTutoring,
                student_record_number = if (studentRecord.isNullOrBlank()) null else studentRecord
            )
            val success = syncRepository.publishDailyAssignment(dto)
            if (success) {
                loadData()
            }
            _isLoading.value = false
            onComplete(success)
        }
    }

    fun replyToStudent(
        receiverId: String,
        subjectName: String,
        replyText: String,
        onComplete: (Boolean) -> Unit
    ) {
        val conf = config.value ?: return
        viewModelScope.launch {
            val dto = SupabaseDirectMessageDto(
                school_id = conf.schoolId,
                sender_id = conf.syncSealToken ?: conf.managerName,
                sender_role = "teacher",
                receiver_id = receiverId,
                subject_name = subjectName,
                message_text = replyText
            )
            val success = syncRepository.sendDirectMessage(dto)
            if (success) {
                loadData()
            }
            onComplete(success)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkHubScreen(
    onBack: () -> Unit,
    viewModel: HomeworkHubViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<SupabaseDirectMessageDto?>(null) }
    val context = LocalContext.current

    LaunchedEffect(config) {
        if (config != null) {
            viewModel.loadData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مركز الواجبات وقنوات التقوية", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color(0xFF059669),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PostAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إرسال واجب جديد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الواجبات المنشورة (${assignments.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("رسائل واستفسارات الطلاب (${messages.size})") }
                )
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (selectedTab) {
                0 -> {
                    if (assignments.isEmpty() && !isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("لم تقم بإرسال واجبات بعد.", color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("اضغط على الزر بالأسفل لإرسال واجب للطلاب فورياً", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(assignments) { item ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = if (item.is_private_tutoring) Color(0xFF8B5CF6) else Color(0xFF059669),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (item.is_private_tutoring) "تقوية خاصة ⭐" else "${item.class_name} (${item.section})",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = item.due_date ?: "بدون موعد تسليم",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                        if (!item.description.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = item.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "المادة: ${item.subject_name}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (messages.isEmpty() && !isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("لا توجد استفسارات أو رسائل حالياً من الطلاب.", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(messages) { msg ->
                                val isFromMe = msg.sender_role == "teacher"
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isFromMe) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isFromMe) "أنت (المدرس)" else "طالب (معرف: ${msg.sender_id.take(8)})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isFromMe) Color(0xFF2563EB) else Color(0xFF0F172A)
                                            )
                                            if (!msg.subject_name.isNullOrBlank()) {
                                                Text(msg.subject_name, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(msg.message_text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        if (!isFromMe) {
                                            Spacer(Modifier.height(8.dp))
                                            Button(
                                                onClick = { replyingToMessage = msg },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("رد على الطالب", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateAssignmentDialog(
                packages = packages,
                onDismiss = { showCreateDialog = false },
                onPublish = { className, section, subjectName, title, desc, dueDate, isPrivate, studentRecord ->
                    viewModel.publishAssignment(className, section, subjectName, title, desc, dueDate, isPrivate, studentRecord) { success ->
                        if (success) {
                            Toast.makeText(context, "تم نشر الواجب بنجاح وإرساله للطلاب ✓", Toast.LENGTH_LONG).show()
                            showCreateDialog = false
                        } else {
                            Toast.makeText(context, "فشل نشر الواجب. تأكد من اتصال الإنترنت", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        replyingToMessage?.let { targetMsg ->
            var replyText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { replyingToMessage = null },
                title = { Text("الرد على الطالب") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("سؤال الطالب: \"${targetMsg.message_text}\"", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text("نص الرد أو التوجيه") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                viewModel.replyToStudent(
                                    receiverId = targetMsg.sender_id,
                                    subjectName = targetMsg.subject_name ?: "مادة دراسية",
                                    replyText = replyText
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم إرسال الرد بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        replyingToMessage = null
                                    }
                                }
                            }
                        }
                    ) {
                        Text("إرسال")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { replyingToMessage = null }) { Text("إلغاء") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentDialog(
    packages: List<ClassPackage>,
    onDismiss: () -> Unit,
    onPublish: (String, String, String, String, String, String, Boolean, String?) -> Unit
) {
    val context = LocalContext.current
    val hwPrefs = remember(context) { context.getSharedPreferences("homework_hub_drafts", android.content.Context.MODE_PRIVATE) }
    
    val savedTitle = remember { hwPrefs.getString("hub_draft_title", "") ?: "" }
    val savedDesc = remember { hwPrefs.getString("hub_draft_desc", "") ?: "" }
    val savedDue = remember { hwPrefs.getString("hub_draft_due", "غداً") ?: "غداً" }

    var selectedPackage by remember { mutableStateOf(packages.firstOrNull()) }
    var title by remember { mutableStateOf(savedTitle) }
    var description by remember { mutableStateOf(savedDesc) }
    var dueDate by remember { mutableStateOf(if (savedDue.isNotBlank()) savedDue else "غداً") }
    var isPrivateTutoring by remember { mutableStateOf(false) }
    var studentRecord by remember { mutableStateOf("") }

    val saveDraft: (String, String, String) -> Unit = { t, d, due ->
        hwPrefs.edit()
            .putString("hub_draft_title", t)
            .putString("hub_draft_desc", d)
            .putString("hub_draft_due", due)
            .apply()
    }

    val clearDraft: () -> Unit = {
        title = ""
        description = ""
        dueDate = "غداً"
        hwPrefs.edit()
            .remove("hub_draft_title")
            .remove("hub_draft_desc")
            .remove("hub_draft_due")
            .apply()
        Toast.makeText(context, "تم تصفير الواجب بنجاح 🧹", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = {
            saveDraft(title, description, dueDate)
            onDismiss()
        },
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إرسال واجب / درس يومي 📝", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.clickable { clearDraft() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "تصفير الواجب", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تصفير", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPrivateTutoring, onCheckedChange = { isPrivateTutoring = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPrivateTutoring) "قناة تقوية خصوصية (طالب محدد)" else "صف عام وشعبة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isPrivateTutoring && packages.isNotEmpty()) {
                    Text("اختر الصف والمادة:", fontSize = 11.sp, color = Color.Gray)
                    packages.forEach { pkg ->
                        val isSelected = selectedPackage == pkg
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPackage = pkg }
                        ) {
                            Text(
                                text = "${pkg.subject} - ${pkg.grade} (${pkg.section})",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                } else if (isPrivateTutoring) {
                    OutlinedTextField(
                        value = studentRecord,
                        onValueChange = { studentRecord = it },
                        label = { Text("رقم القيد أو معرف الطالب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        saveDraft(it, description, dueDate)
                    },
                    label = { Text("عنوان الواجب أو الدرس") },
                    placeholder = { Text("مثال: حل تمارين ص 34") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        saveDraft(title, it, dueDate)
                    },
                    label = { Text("تفاصيل وملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { 
                        dueDate = it
                        saveDraft(title, description, it)
                    },
                    label = { Text("موعد التسليم / الإنجاز") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        saveDraft(title, description, dueDate)
                        val pkg = selectedPackage
                        val cls = pkg?.grade ?: "الكل"
                        val sec = pkg?.section ?: "عام"
                        val subj = pkg?.subject ?: "عام"
                        onPublish(cls, sec, subj, title, description, dueDate, isPrivateTutoring, studentRecord)
                    }
                }
            ) {
                Text("نشر وإرسال")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                saveDraft(title, description, dueDate)
                onDismiss()
            }) { Text("إلغاء") }
        }
    )
}
