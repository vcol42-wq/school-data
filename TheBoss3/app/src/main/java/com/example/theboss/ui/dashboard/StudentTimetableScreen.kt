package com.example.theboss.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

data class StudentLessonSlot(
    val subject: String,
    val teacherName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTimetableScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE) }
    
    var studentGrade by remember { mutableStateOf(prefs.getString("student_grade", "الصف الأول") ?: "الصف الأول") }
    var studentSection by remember { mutableStateOf(prefs.getString("student_section", "أ") ?: "أ") }
    var rawScheduleJson by remember { mutableStateOf(prefs.getString("synced_schedule", "{}") ?: "{}") }
    var isRefreshingSchedule by remember { mutableStateOf(false) }

    fun norm(str: String): String = str
        .replace("[أإآ]".toRegex(), "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
        .replace("^(الصف|صف)\\s*".toRegex(), "")
        .trim()

    LaunchedEffect(Unit) {
        if (rawScheduleJson == "{}" || rawScheduleJson.length < 10) {
            isRefreshingSchedule = true
            viewModel.syncScheduleManual {
                isRefreshingSchedule = false
                rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            }
        }
    }

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val lessonColumns = listOf("الدرس 1", "الدرس 2", "الدرس 3", "الدرس 4", "الدرس 5", "الدرس 6", "الدرس 7")

    val gson = remember { Gson() }

    // Parse Schedule Map into: Day -> (LessonIndex -> StudentLessonSlot)
    val parsedTimetable: Map<String, Map<Int, StudentLessonSlot>> = remember(rawScheduleJson, studentGrade, studentSection) {
        val result = mutableMapOf<String, MutableMap<Int, StudentLessonSlot>>()
        daysList.forEach { result[it] = mutableMapOf() }

        try {
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            if (rootObj != null) {
                val stdG = norm(studentGrade)
                val stdS = norm(studentSection)

                for (day in daysList) {
                    val dayData = rootObj[day]
                    if (dayData is List<*>) {
                        for (row in dayData) {
                            if (row is Map<*, *>) {
                                val g = row["grade"]?.toString() ?: ""
                                val s = row["section"]?.toString() ?: ""
                                val rowG = norm(g)
                                val rowS = norm(s)
                                
                                val matchesGrade = rowG.contains(stdG) || stdG.contains(rowG) || rowG.isEmpty()
                                val matchesSection = rowS.contains(stdS) || stdS.contains(rowS) || rowS.isEmpty()
                                
                                if (matchesGrade && matchesSection) {
                                    val lessons = row["lessons"] as? Map<*, *>
                                    if (lessons != null) {
                                        for (i in 1..7) {
                                            val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                            val subj = lessonObj?.get("subject")?.toString() ?: ""
                                            val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                            val isOff = lessonObj?.get("isOff") as? Boolean ?: false
                                            if (!isOff && (subj.isNotEmpty() || teacher.isNotEmpty())) {
                                                result[day]?.put(i, StudentLessonSlot(subj, teacher))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "جدول دروس الشعبة والتوقيتات 📅",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$studentGrade - شعبة ($studentSection)",
                            fontSize = 12.sp,
                            color = Color(0xFFBAE6FD),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshingSchedule = true
                            viewModel.syncScheduleManual { success ->
                                isRefreshingSchedule = false
                                rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                                if (success) {
                                    Toast.makeText(context, "تم تحديث الجدول بنجاح من السحابة! ⚡", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "تم قراءة الجدول المحلي المتاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        if (isRefreshingSchedule) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A) // Sleek Dark Slate
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            
            // Student Grade & Section Quick Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Class, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("توزيع الحصص المدرسية المعتمد", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("الأيام في الجانب والدروس أمامه", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "7 حصص يومياً",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Timetable Grid: Days on the side, Lessons across horizontally
            val horizontalScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Header Row (Day Label + Lesson 1..7)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اليوم 📆",
                            modifier = Modifier.width(90.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        lessonColumns.forEach { colTitle ->
                            Text(
                                text = colTitle,
                                modifier = Modifier.width(110.dp),
                                color = Color(0xFFFBBF24), // Gold
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Days Rows
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(daysList) { dayName ->
                            val daySlots = parsedTimetable[dayName] ?: emptyMap()
                            
                            Row(
                                modifier = Modifier
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0))
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Day Name on the Side
                                Surface(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .padding(horizontal = 6.dp),
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = dayName,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // 7 Lesson Cells
                                for (i in 1..7) {
                                    val slot = daySlots[i]
                                    Surface(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .padding(horizontal = 4.dp),
                                        color = if (slot != null && slot.subject.isNotBlank()) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (slot != null && slot.subject.isNotBlank()) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (slot != null && slot.subject.isNotBlank()) slot.subject else "شاغر",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = if (slot != null && slot.subject.isNotBlank()) Color(0xFF065F46) else Color.LightGray,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                            if (slot != null && slot.teacherName.isNotBlank()) {
                                                Text(
                                                    text = slot.teacherName,
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
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
    }
}
