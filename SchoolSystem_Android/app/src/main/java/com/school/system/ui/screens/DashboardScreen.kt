package com.school.system.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.system.data.model.ClassPackage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToGrades: (grade: String, section: String, subject: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val packages by viewModel.packages.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("سجل درجات المدرس الذكي", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(
                            Icons.Default.QuestionAnswer,
                            contentDescription = "المساعد الذكي AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val success = viewModel.syncAllFromPrincipal()
                            if (success) {
                                Toast.makeText(context, "تم استقبال جميع الصفوف والطلاب من المدير بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "فشلت المزامنة. تأكد من الربط في الإعدادات", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync all")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة صف")
            }
        }
    ) { padding ->
        if (packages.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(100.dp).alpha(0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ابدأ بإضافة صفوفك الدراسية الآن", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(packages) { pkg ->
                    PackageCard(
                        pkg = pkg,
                        onClick = { onNavigateToGrades(pkg.grade, pkg.section, pkg.subject) },
                        onDelete = { viewModel.deletePackage(pkg) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddPackageDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { grade, section, subject, _ ->
                    viewModel.addPackage(grade, section, subject, "")
                    showAddDialog = false
                }
            )
        }

        if (showAiDialog) {
            AiAssistantDialog(
                onDismiss = { showAiDialog = false },
                viewModel = viewModel,
                onSearchStudent = { query ->
                    Toast.makeText(context, "بحث ذكي عن الطالب: $query", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
fun PackageCard(pkg: ClassPackage, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val gradeNumber = when (pkg.grade) {
                "الأول" -> "1"
                "الثاني" -> "2"
                "الثالث" -> "3"
                "الرابع" -> "4"
                "الخامس" -> "5"
                "السادس" -> "6"
                else -> pkg.grade.take(1)
            }
            val badgeText = "$gradeNumber${pkg.section}"
            
            Surface(
                modifier = Modifier.align(Alignment.TopStart),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = pkg.subject,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    maxLines = 1
                )
                Text(
                    text = pkg.grade,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPackageDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var grade by remember { mutableStateOf("الأول") }
    var section by remember { mutableStateOf("أ") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة صف جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("المادة الدراسية") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownSelector(label = "الصف", options = listOf("الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس"), selected = grade, onSelect = { grade = it }, modifier = Modifier.weight(1f))
                    DropdownSelector(label = "الشعبة", options = listOf("أ", "ب", "ج", "د"), selected = section, onSelect = { section = it }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (subject.isNotBlank()) onConfirm(grade, section, subject, "") }) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantDialog(
    onDismiss: () -> Unit,
    viewModel: DashboardViewModel,
    onSearchStudent: (String) -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<Pair<String, Boolean>>(
        Pair("مرحباً بك! أنا مساعد الذكاء الاصطناعي لسجل المدرس. كيف يمكنني مساعدتك اليوم؟ يمكنك سؤالي عن إحصائيات الطلاب، أو البحث، أو المزامنة.", false)
    )) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("المساعد والبحث الذكي (AI)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chat history container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatHistory) { message ->
                            val isUser = message.second
                            val text = message.first
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 12.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 220.dp)
                                ) {
                                    Text(
                                        text = text,
                                        modifier = Modifier.padding(10.dp),
                                        fontSize = 12.sp,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        if (isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }

                // Chat Input field
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("مثال: ابحث عن الطالب علي أحمد، أو كم عدد الطلاب؟") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    maxLines = 2,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (queryText.isNotBlank() && !isLoading) {
                                    val userQuery = queryText
                                    chatHistory = chatHistory + Pair(userQuery, true)
                                    queryText = ""
                                    isLoading = true
                                    coroutineScope.launch {
                                        val aiResponse = viewModel.queryAi(userQuery)
                                        isLoading = false
                                        val reply = aiResponse.responseText ?: aiResponse.error ?: "عذرًا، لم أتمكن من معالجة الطلب."
                                        chatHistory = chatHistory + Pair(reply, false)
                                        
                                        if (aiResponse.action == "SEARCH_STUDENT" && !aiResponse.searchQuery.isNullOrEmpty()) {
                                            onSearchStudent(aiResponse.searchQuery)
                                        }
                                    }
                                }
                            },
                            enabled = queryText.isNotBlank() && !isLoading
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (queryText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
