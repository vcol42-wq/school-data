package com.example.theboss.ui.tutoring

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.theboss.data.local.AppDao
import com.example.theboss.data.local.DirectMessageEntity
import com.example.theboss.data.local.SubjectEntity
import com.example.theboss.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DirectTutoringViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val dao: AppDao
) : ViewModel() {

    val subjects: StateFlow<List<SubjectEntity>> = dao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<DirectMessageEntity>> = dao.getAllDirectMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    init {
        syncMessages()
    }

    fun syncMessages() {
        viewModelScope.launch {
            repository.syncDirectMessages()
        }
    }

    fun sendMessage(
        teacherId: String,
        subjectName: String,
        messageText: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            val result = repository.sendDirectMessage(teacherId, subjectName, messageText)
            _isSending.value = false
            onComplete(result.isSuccess)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectTutoringScreen(
    onBack: () -> Unit,
    viewModel: DirectTutoringViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()

    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(subjects) {
        if (selectedSubject == null && subjects.isNotEmpty()) {
            selectedSubject = subjects.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قناة الأسئلة والتقوية المباشرة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncMessages() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث الرسائل")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Subject / Teacher Selection Chips
            if (subjects.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subjects) { subj ->
                            val isSelected = selectedSubject?.id == subj.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubject = subj },
                                label = { Text("${subj.name} (${subj.teacherName})", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val filteredMessages = messages.filter {
                    selectedSubject == null || it.subjectName == selectedSubject?.name || it.receiverId == selectedSubject?.teacherName
                }

                if (filteredMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "لا توجد رسائل سابقة مع الأستاذ.",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                "اطرح سؤالك أو استفسارك حول الواجب أو الدرس مباشرة هنا",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredMessages) { msg ->
                            val isFromStudent = msg.senderRole == "student"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isFromStudent) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isFromStudent) Color(0xFF059669) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isFromStudent) 16.dp else 2.dp,
                                        bottomEnd = if (isFromStudent) 2.dp else 16.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 280.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isFromStudent) "أنت" else "الأستاذ (${msg.subjectName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isFromStudent) Color.White.copy(alpha = 0.8f) else Color(0xFF2563EB)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = msg.messageText,
                                            fontSize = 13.sp,
                                            color = if (isFromStudent) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("اكتب سؤالك لمعلم المادة...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val subj = selectedSubject
                            val teacherId = subj?.teacherName ?: "مدرس المادة"
                            val subjectName = subj?.name ?: "عام"
                            viewModel.sendMessage(teacherId, subjectName, messageText) { success ->
                                if (success) {
                                    messageText = ""
                                    Toast.makeText(context, "تم إرسال السؤال للأستاذ ✓", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "فشل الإرسال. تأكد من اتصال الإنترنت", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = messageText.isNotBlank() && !isSending,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF059669))
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
