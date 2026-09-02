package com.principal.system.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import com.principal.system.data.local.BroadcastEntity
import com.principal.system.data.repository.PrincipalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val repository: PrincipalRepository
) : ViewModel() {

    val broadcasts: StateFlow<List<BroadcastEntity>> = repository.broadcasts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSending = mutableStateOf(false)
    val isSending: State<Boolean> = _isSending

    fun sendBroadcast(
        title: String,
        message: String,
        targetAudience: String,
        priority: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (title.isBlank() || message.isBlank()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            val res = repository.dispatchBroadcast(title, message, targetAudience, priority)
            _isSending.value = false
            onComplete(res.isSuccess)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastAnnouncementsScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val broadcasts by viewModel.broadcasts.collectAsState()
    val isSending by viewModel.isSending
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("all") } // "all", "teachers", "students"
    var isUrgent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مركز التعميمات الإدارية 📢", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            // 1. Dispatch Form Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("إرسال تعميم جديد للمدرسة", fontWeight = FontWeight.Black, fontSize = 15.sp)

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان التعميم الإداري") },
                            placeholder = { Text("مثال: موعد اختبارات الشهر الثاني") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("نص التعميم والتعليمات") },
                            placeholder = { Text("اكتب تفاصيل التوجيه أو القرار هنا...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Target Audience Selector
                        Text("الفئة المستهدفة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = targetAudience == "all",
                                onClick = { targetAudience = "all" },
                                label = { Text("الجميع", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = targetAudience == "teachers",
                                onClick = { targetAudience = "teachers" },
                                label = { Text("المعلمون فقط", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = targetAudience == "students",
                                onClick = { targetAudience = "students" },
                                label = { Text("الطلاب وأولياء الأمور", fontSize = 11.sp) }
                            )
                        }

                        // Urgent Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تصنيف التعميم كـ (عاجل وهام 🔥)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Switch(checked = isUrgent, onCheckedChange = { isUrgent = it })
                        }

                        Button(
                            onClick = {
                                viewModel.sendBroadcast(
                                    title = title,
                                    message = message,
                                    targetAudience = targetAudience,
                                    priority = if (isUrgent) "عاجل" else "عادي"
                                ) { success ->
                                    if (success) {
                                        title = ""
                                        message = ""
                                        Toast.makeText(context, "تم إرسال التعميم بنجاح لجميع الأجهزة ✓", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "فشل الإرسال. تأكد من اتصال الإنترنت والربط.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = title.isNotBlank() && message.isNotBlank() && !isSending,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("نشر وإرسال التعميم فورياً", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Broadcast History
            item {
                Text("سجل التعميمات المرسلة مؤخراً", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (broadcasts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم إرسال تعميمات سابقة بعد", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                items(broadcasts, key = { it.id }) { bcast ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (bcast.priority == "عاجل") Color(0xFFDC2626) else Color(0xFF312E81),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = bcast.priority,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(bcast.createdAt, fontSize = 10.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(bcast.title, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(bcast.message, fontSize = 12.sp, color = Color(0xFF334155))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "المستهدفون: ${when (bcast.targetAudience) { "teachers" -> "المعلمون فقط"; "students" -> "الطلاب وأولياء الأمور"; else -> "جميع منسوبي المدرسة" }}",
                                fontSize = 10.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
