package com.example.theboss.ui.dashboard

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.theboss.data.local.AssignmentEntity
import com.example.theboss.data.local.AttendanceEntity
import com.example.theboss.data.remote.DirectiveDto
import com.example.theboss.utils.NotificationHelper
import com.example.theboss.utils.alarm.StudyAlarmScheduler
import com.example.theboss.widget.StudentScheduleWidgetProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class StudentLessonSlot(
    val subject: String,
    val teacherName: String
)

data class PeriodHeaderData(
    val title: String,
    val startTime: String,
    val bgColor: Color,
    val borderColor: Color
)

data class AppThemePalette(
    val id: Int,
    val name: String,
    val topBarBg: Color,
    val screenBgBrush: Brush,
    val headerBannerBg: Color,
    val headerBannerText: Color,
    val cornerCellBgBrush: Brush,
    val dayCardBg: Color,
    val dayCardBorder: Color,
    val dayCardText: Color,
    val lessonCardBg: Brush,
    val lessonCardBorder: Color,
    val lessonSubjectText: Color,
    val lessonTeacherText: Color,
    val prepCardBg: Brush,
    val prepCardBorder: Color,
    val prepSubjectText: Color
)

data class DayStyle(
    val name: String,
    val bg: Color,
    val border: Color,
    val text: Color
)

fun shortenSubject(subj: String): String {
    if (subj.isBlank() || subj == "شاغر") return subj
    var s = subj.trim()
        .replace("اللغة العربية", "عربي")
        .replace("اللغة الانكليزية", "إنكليزي")
        .replace("اللغة الإنجليزية", "إنكليزي")
        .replace("التربية الإسلامية", "إسلامية")
        .replace("التربية الاسلامية", "إسلامية")
        .replace("العلوم العامة", "علوم")
        .replace("التربية الفنية", "فنية")
        .replace("التربية الرياضية", "رياضة")
        .replace("الاجتماعيات", "اجتماعيات")
        .replace("الرياضيات", "رياضيات")
        .replace("الفيزياء", "فيزياء")
        .replace("الكيمياء", "كيمياء")
        .replace("الأحياء", "أحياء")
        .replace("الحاسوب", "حاسوب")
        .replace("القرآن الكريم", "قرآن")

    if (s.startsWith("ال") && s.length > 3) {
        s = s.substring(2)
    }
    return s
}

@Composable
fun DiagonalHeaderCell(
    modifier: Modifier = Modifier,
    cornerBrush: Brush
) {
    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(topEnd = 10.dp))
            .clip(RoundedCornerShape(topEnd = 10.dp))
            .background(cornerBrush)
            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(topEnd = 10.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
        Text(
            text = "الحصه",
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 3.dp)
        )
        Text(
            text = "اليوم",
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 2.dp, start = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTimetableScreen(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val prefs = remember { context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    val alarmScheduler = remember { StudyAlarmScheduler(context) }

    val assignments by viewModel.assignments.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val directives by viewModel.directives.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var studentGrade by remember { mutableStateOf(prefs.getString("student_grade", "الصف الأول المتوسط") ?: "الصف الأول المتوسط") }
    var studentSection by remember { mutableStateOf(prefs.getString("student_section", "أ") ?: "أ") }
    var rawScheduleJson by remember { mutableStateOf(prefs.getString("synced_schedule", "{}") ?: "{}") }
    var customOverridesJson by remember { mutableStateOf(prefs.getString("custom_subject_overrides", "{}") ?: "{}") }
    var isRefreshingSchedule by remember { mutableStateOf(false) }

    var selectedSlotData by remember { mutableStateOf<Triple<String, Int, StudentLessonSlot>?>(null) }
    var editSubjectText by remember { mutableStateOf("") }
    var showEditSubjectDialog by remember { mutableStateOf(false) }

    // Dialog States
    var showNotebookSheet by remember { mutableStateOf(false) }
    var showTasksSheet by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPomodoroDialog by remember { mutableStateOf(false) }

    // Pomodoro Timer State
    var pomodoroSeconds by remember { mutableIntStateOf(1500) }
    var isPomodoroRunning by remember { mutableStateOf(false) }
    var isBreakMode by remember { mutableStateOf(false) }

    LaunchedEffect(isPomodoroRunning, pomodoroSeconds) {
        if (isPomodoroRunning && pomodoroSeconds > 0) {
            delay(1000L)
            pomodoroSeconds--
        } else if (isPomodoroRunning && pomodoroSeconds == 0) {
            isPomodoroRunning = false
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(context, if (isBreakMode) "انتهت الاستراحة! حان وقت التركيز 📚" else "انتهت فترة التركيز! خذ استراحة ☕", Toast.LENGTH_LONG).show()
        }
    }

    var currentThemeIndex by remember { mutableIntStateOf(prefs.getInt("app_theme_index", 0)) }

    val themesList = listOf(
        AppThemePalette(
            id = 0,
            name = "الداكن الفخم (Dark Slate) 🌙",
            topBarBg = Color(0xFF0F172A),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
            headerBannerBg = Color(0xFF1E293B),
            headerBannerText = Color(0xFF38BDF8),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF86EFAC), Color(0xFF4ADE80))),
            dayCardBg = Color.White,
            dayCardBorder = Color(0xFF0F172A),
            dayCardText = Color(0xFF0F172A),
            lessonCardBg = Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))),
            lessonCardBorder = Color(0xFF0F172A),
            lessonSubjectText = Color(0xFF0F172A),
            lessonTeacherText = Color(0xFF2563EB),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFF59E0B),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 1,
            name = "الذهبي الفاخر (Warm Amber Gold) 🌟",
            topBarBg = Color(0xFF271C10),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF1C130B), Color(0xFF332212))),
            headerBannerBg = Color(0xFF3D2A18),
            headerBannerText = Color(0xFFFBBF24),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
            dayCardBg = Color(0xFFFFFBEB),
            dayCardBorder = Color(0xFFD97706),
            dayCardText = Color(0xFF78350F),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))),
            lessonCardBorder = Color(0xFFD97706),
            lessonSubjectText = Color(0xFF451A03),
            lessonTeacherText = Color(0xFFB45309),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
            prepCardBorder = Color(0xFFB45309),
            prepSubjectText = Color(0xFF451A03)
        ),
        AppThemePalette(
            id = 2,
            name = "الأزرق الملكي (Royal Blue) 💙",
            topBarBg = Color(0xFF0A2540),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF0A192F), Color(0xFF1E3A8A))),
            headerBannerBg = Color(0xFF1E3A8A),
            headerBannerText = Color(0xFF38BDF8),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF93C5FD), Color(0xFF3B82F6))),
            dayCardBg = Color.White,
            dayCardBorder = Color(0xFF0A2540),
            dayCardText = Color(0xFF0A2540),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))),
            lessonCardBorder = Color(0xFF0284C7),
            lessonSubjectText = Color(0xFF0369A1),
            lessonTeacherText = Color(0xFF1D4ED8),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFD97706),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 3,
            name = "الفاتح الحديث (Light Minimal) ☀️",
            topBarBg = Color(0xFF1E293B),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))),
            headerBannerBg = Color(0xFFF1F5F9),
            headerBannerText = Color(0xFF0F172A),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF86EFAC), Color(0xFF4ADE80))),
            dayCardBg = Color.White,
            dayCardBorder = Color(0xFF334155),
            dayCardText = Color(0xFF0F172A),
            lessonCardBg = Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC))),
            lessonCardBorder = Color(0xFF334155),
            lessonSubjectText = Color(0xFF0F172A),
            lessonTeacherText = Color(0xFF2563EB),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFF59E0B),
            prepSubjectText = Color(0xFF78350F)
        )
    )

    val activeTheme = themesList.getOrElse(currentThemeIndex) { themesList[0] }

    val dayStylesMap = remember {
        mapOf(
            "الأحد" to DayStyle("الأحد", Color(0xFFD1FAE5), Color(0xFF059669), Color(0xFF065F46)),
            "الأثنين" to DayStyle("الأثنين", Color(0xFFE0F2FE), Color(0xFF0284C7), Color(0xFF0369A1)),
            "الثلاثاء" to DayStyle("الثلاثاء", Color(0xFFF3E8FF), Color(0xFF9333EA), Color(0xFF581C87)),
            "الأربعاء" to DayStyle("الأربعاء", Color(0xFFFFE4E6), Color(0xFFE11D48), Color(0xFF881337)),
            "الخميس" to DayStyle("الخميس", Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFF78350F))
        )
    }

    // Personal Notes State
    var personalNoteText by remember { mutableStateOf("") }
    var personalNotesList by remember {
        mutableStateOf(
            try {
                val json = prefs.getString("user_personal_notes", "[]") ?: "[]"
                gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    fun savePersonalNote() {
        if (personalNoteText.isBlank()) return
        val updated = personalNotesList.toMutableList()
        updated.add(0, personalNoteText.trim())
        personalNotesList = updated
        prefs.edit().putString("user_personal_notes", gson.toJson(updated)).apply()
        personalNoteText = ""
        Toast.makeText(context, "تم حفظ الملاحظة بنجاح ✍️", Toast.LENGTH_SHORT).show()
    }

    fun deletePersonalNote(index: Int) {
        val updated = personalNotesList.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            personalNotesList = updated
            prefs.edit().putString("user_personal_notes", gson.toJson(updated)).apply()
        }
    }

    fun saveCustomSubjectOverride(day: String, period: Int, newSubj: String) {
        if (newSubj.isBlank()) return
        try {
            val type = object : TypeToken<MutableMap<String, MutableMap<String, String>>>() {}.type
            val customMap: MutableMap<String, MutableMap<String, String>> = gson.fromJson(customOverridesJson, type) ?: mutableMapOf()
            val dayMap = customMap[day] ?: mutableMapOf()
            dayMap[period.toString()] = shortenSubject(newSubj)
            customMap[day] = dayMap
            val updatedJson = gson.toJson(customMap)
            customOverridesJson = updatedJson
            prefs.edit().putString("custom_subject_overrides", updatedJson).apply()
            StudentScheduleWidgetProvider.sendRefreshBroadcast(context)
            Toast.makeText(context, "تم حفظ اسم المادة الجديد بنجاح ✏️", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCustomSubjectOverride(day: String, period: Int): String? {
        return try {
            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            val customMap: Map<String, Map<String, String>> = gson.fromJson(customOverridesJson, type) ?: emptyMap()
            customMap[day]?.get(period.toString())
        } catch (e: Exception) {
            null
        }
    }

    fun standardizeGradeName(gradeStr: String?): String {
        if (gradeStr.isNullOrBlank()) return "الأول المتوسط"
        val clean = gradeStr.trim().replace("^(الصف|صف)\\s+".toRegex(), "").trim()
        val norm = clean.replace("[أإآ]".toRegex(), "ا").replace("ة", "ه").replace("ى", "ي").lowercase()

        var base = "الأول"
        if (norm.contains("سادس") || norm.contains("6") || norm.contains("٦")) base = "السادس"
        else if (norm.contains("خامس") || norm.contains("5") || norm.contains("٥")) base = "الخامس"
        else if (norm.contains("رابع") || norm.contains("4") || norm.contains("٤")) base = "الرابع"
        else if (norm.contains("ثالث") || norm.contains("3") || norm.contains("٣")) base = "الثالث"
        else if (norm.contains("ثاني") || norm.contains("2") || norm.contains("٢")) base = "الثاني"
        else if (norm.contains("اول") || norm.contains("1") || norm.contains("١")) base = "الأول"

        var branch = ""
        if (norm.contains("احيائ") || norm.contains("تطبيق") || norm.contains("علم")) branch = "العلمي"
        else if (norm.contains("ادب")) branch = "الأدبي"
        else if (norm.contains("صناع")) branch = "الصناعي"
        else if (norm.contains("تجار")) branch = "التجاري"
        else if (norm.contains("متوسط")) branch = "المتوسط"
        else if (norm.contains("اعداد") || norm.contains("ثانوي")) branch = "الإعدادي"
        else if (norm.contains("ابتدائ")) branch = "الابتدائي"

        if (branch.isEmpty()) {
            if (base == "الأول" || base == "الثاني" || base == "الثالث") {
                branch = "المتوسط"
            } else if (base == "الرابع" || base == "الخامس" || base == "السادس") {
                branch = "الإعدادي"
            }
        }

        return if (branch.isNotEmpty()) "$base $branch" else base
    }

    fun isGradeMatch(g1: String?, g2: String?): Boolean {
        if (g1.isNullOrBlank() || g2.isNullOrBlank()) return false
        val std1 = standardizeGradeName(g1)
        val std2 = standardizeGradeName(g2)
        if (std1 == std2) return true

        fun normStr(s: String) = s.replace("[أإآ]".toRegex(), "ا").replace("ة", "ه").replace("ى", "ي").lowercase()
        val n1 = normStr(g1)
        val n2 = normStr(g2)

        fun extractBase(norm: String): String {
            return when {
                norm.contains("سادس") || norm.contains("6") || norm.contains("٦") -> "سادس"
                norm.contains("خامس") || norm.contains("5") || norm.contains("٥") -> "خامس"
                norm.contains("رابع") || norm.contains("4") || norm.contains("٤") -> "رابع"
                norm.contains("ثالث") || norm.contains("3") || norm.contains("٣") -> "ثالث"
                norm.contains("ثاني") || norm.contains("2") || norm.contains("٢") -> "ثاني"
                norm.contains("اول") || norm.contains("1") || norm.contains("١") -> "اول"
                else -> norm
            }
        }

        val b1 = extractBase(n1)
        val b2 = extractBase(n2)
        if (b1 != b2) return false

        val hasBranch1 = n1.contains("متوسط") || n1.contains("اعداد") || n1.contains("ابتدائ") || n1.contains("ثانوي") || n1.contains("علم") || n1.contains("ادب")
        val hasBranch2 = n2.contains("متوسط") || n2.contains("اعداد") || n2.contains("ابتدائ") || n2.contains("ثانوي") || n2.contains("علم") || n2.contains("ادب")

        if (!hasBranch1 || !hasBranch2) return true
        return std1 == std2
    }

    fun standardizeSectionName(secStr: String?): String {
        if (secStr.isNullOrBlank()) return "أ"
        val clean = secStr.trim().replace("^(شعبة|الشعبة|ش)\\s*".toRegex(), "").trim()
        val lower = clean.lowercase()

        val letterOnly = clean
            .replace("(الصف|صف|الأول|الاول|الثاني|الثالث|الرابع|الخامس|السادس|المتوسط|الإعدادي|الاعدادي|الابتدائي|العلمي|الأدبي|الادبي)".toRegex(), "")
            .trim()
        val target = if (letterOnly.isNotBlank()) letterOnly else clean
        val targetLower = target.lowercase()

        if (target == "ا" || target == "أ" || target == "إ" || target == "آ" || targetLower == "a" || targetLower == "1" || targetLower == "١") return "أ"
        if (target == "ب" || targetLower == "b" || targetLower == "2" || targetLower == "٢") return "ب"
        if (target == "ج" || targetLower == "c" || targetLower == "3" || targetLower == "٣") return "ج"
        if (target == "د" || targetLower == "d" || targetLower == "4" || targetLower == "٤") return "د"
        if (target == "ه" || target == "هـ" || targetLower == "e" || targetLower == "5" || targetLower == "٥") return "هـ"
        if (target == "و" || targetLower == "f" || targetLower == "6" || targetLower == "٦") return "و"
        if (target == "ز" || targetLower == "z" || targetLower == "7" || targetLower == "٧") return "ز"
        if (target == "ح" || targetLower == "h" || targetLower == "8" || targetLower == "٨") return "ح"
        if (target == "ط" || targetLower == "9" || targetLower == "٩") return "ط"
        if (target == "خ") return "خ"
        return if (target.length == 1 && target[0].isLetter()) target else "أ"
    }

    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

    fun norm(str: String): String = str
        .replace("[أإآ]".toRegex(), "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
        .replace("^(الصف|صف)\\s*".toRegex(), "")
        .trim()

    fun normDay(day: String): String = day
        .replace("[أإآ]".toRegex(), "ا")
        .replace("ة", "ه")
        .replace("ى", "ي")
        .trim()

    val defaultLessonsMap = remember {
        mapOf(
            "الأحد" to mapOf(
                1 to StudentLessonSlot("إسلامية", "أ. أحمد العلي"),
                2 to StudentLessonSlot("رياضيات", "أ. محمد الموسوي"),
                3 to StudentLessonSlot("عربي", "أ. علي الحسين"),
                4 to StudentLessonSlot("علوم", "أ. حيدر عباس"),
                5 to StudentLessonSlot("إنكليزي", "أ. عمر الفاروق"),
                6 to StudentLessonSlot("اجتماعيات", "أ. جاسم محمد")
            ),
            "الإثنين" to mapOf(
                1 to StudentLessonSlot("عربي", "أ. علي الحسين"),
                2 to StudentLessonSlot("رياضيات", "أ. محمد الموسوي"),
                3 to StudentLessonSlot("علوم", "أ. حيدر عباس"),
                4 to StudentLessonSlot("فيزياء", "أ. أسامة كريم"),
                5 to StudentLessonSlot("رياضة", "أ. مصطفى الشمري"),
                6 to StudentLessonSlot("إنكليزي", "أ. عمر الفاروق")
            ),
            "الثلاثاء" to mapOf(
                1 to StudentLessonSlot("كيمياء", "أ. سامر صاحب"),
                2 to StudentLessonSlot("إنكليزي", "أ. عمر الفاروق"),
                3 to StudentLessonSlot("رياضيات", "أ. محمد الموسوي"),
                4 to StudentLessonSlot("إسلامية", "أ. أحمد العلي"),
                5 to StudentLessonSlot("حاسوب", "أ. ماهر فاضل"),
                6 to StudentLessonSlot("عربي", "أ. علي الحسين")
            ),
            "الأربعاء" to mapOf(
                1 to StudentLessonSlot("رياضيات", "أ. محمد الموسوي"),
                2 to StudentLessonSlot("عربي", "أ. علي الحسين"),
                3 to StudentLessonSlot("اجتماعيات", "أ. جاسم محمد"),
                4 to StudentLessonSlot("علوم", "أ. حيدر عباس"),
                5 to StudentLessonSlot("إنكليزي", "أ. عمر الفاروق"),
                6 to StudentLessonSlot("فنية", "أ. زينب الجبوري")
            ),
            "الخميس" to mapOf(
                1 to StudentLessonSlot("إنكليزي", "أ. عمر الفاروق"),
                2 to StudentLessonSlot("رياضيات", "أ. محمد الموسوي"),
                3 to StudentLessonSlot("إسلامية", "أ. أحمد العلي"),
                4 to StudentLessonSlot("عربي", "أ. علي الحسين"),
                5 to StudentLessonSlot("علوم", "أ. حيدر عباس"),
                6 to StudentLessonSlot("نشاط مدرسي", "إدارة المدرسة")
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
        StudentScheduleWidgetProvider.sendRefreshBroadcast(context)
        if (rawScheduleJson == "{}" || rawScheduleJson.length < 10) {
            isRefreshingSchedule = true
            viewModel.syncScheduleManual {
                isRefreshingSchedule = false
                rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                StudentScheduleWidgetProvider.sendRefreshBroadcast(context)
            }
        }
    }

    val periodHeaders = listOf(
        PeriodHeaderData("الأولى", "8:00 ص", Color(0xFFFEF08A), Color(0xFFCA8A04)),  // Yellow
        PeriodHeaderData("الثانية", "8:50 ص", Color(0xFFBAE6FD), Color(0xFF0284C7)),  // Light Blue
        PeriodHeaderData("الثالثة", "9:40 ص", Color(0xFFFEF08A), Color(0xFFCA8A04)),  // Yellow
        PeriodHeaderData("الرابعة", "10:30 ص", Color(0xFFFBCFE8), Color(0xFFDB2777)), // Pink
        PeriodHeaderData("الخامسة", "11:20 ص", Color(0xFFBAE6FD), Color(0xFF0284C7)), // Light Blue
        PeriodHeaderData("السادسة", "12:10 م", Color(0xFFFEF08A), Color(0xFFCA8A04))  // Yellow
    )

    val parsedTimetable: Map<String, Map<Int, StudentLessonSlot>> = remember(rawScheduleJson, customOverridesJson, studentGrade, studentSection) {
        val result = mutableMapOf<String, MutableMap<Int, StudentLessonSlot>>()
        daysList.forEach { result[it] = mutableMapOf() }

        var parsedAny = false
        try {
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            if (rootObj != null) {
                val stdS = standardizeSectionName(studentSection)

                // Pass 1: Strict Grade + Section match
                for (day in daysList) {
                    val dayData = rootObj.entries.find { normDay(it.key) == normDay(day) }?.value as? List<*>
                    if (dayData != null) {
                        for (row in dayData) {
                            if (row is Map<*, *>) {
                                val g = row["grade"]?.toString() ?: ""
                                val s = row["section"]?.toString() ?: ""
                                val rowS = standardizeSectionName(s)

                                val matchesGrade = isGradeMatch(g, studentGrade)
                                val matchesSection = (rowS == stdS || stdS == "الكل" || rowS.isEmpty())

                                if (matchesGrade && matchesSection) {
                                    val lessons = row["lessons"] as? Map<*, *>
                                    if (lessons != null) {
                                        for (i in 1..6) {
                                            val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                            val rawSubj = lessonObj?.get("subject")?.toString() ?: ""
                                            val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                            val isOff = lessonObj?.get("isOff") as? Boolean ?: false
                                            if (!isOff && (rawSubj.isNotEmpty() || teacher.isNotEmpty())) {
                                                val subj = shortenSubject(rawSubj)
                                                result[day]?.put(i, StudentLessonSlot(subj, teacher))
                                                parsedAny = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pass 2: Fallback to first available schedule row in school JSON if pass 1 found no matches
                if (!parsedAny) {
                    for (day in daysList) {
                        val dayData = rootObj.entries.find { normDay(it.key) == normDay(day) }?.value as? List<*>
                        if (dayData != null && dayData.isNotEmpty()) {
                            val firstRow = dayData.firstOrNull { it is Map<*, *> } as? Map<*, *>
                            val lessons = firstRow?.get("lessons") as? Map<*, *>
                            if (lessons != null) {
                                for (i in 1..6) {
                                    val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                    val rawSubj = lessonObj?.get("subject")?.toString() ?: ""
                                    val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                    val isOff = lessonObj?.get("isOff") as? Boolean ?: false
                                    if (!isOff && (rawSubj.isNotEmpty() || teacher.isNotEmpty())) {
                                        val subj = shortenSubject(rawSubj)
                                        result[day]?.put(i, StudentLessonSlot(subj, teacher))
                                        parsedAny = true
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

        val finalMap = if (!parsedAny) defaultLessonsMap else result

        // Apply custom subject overrides!
        val overriddenMap = mutableMapOf<String, MutableMap<Int, StudentLessonSlot>>()
        for (day in daysList) {
            val daySlots = finalMap[day]?.toMutableMap() ?: mutableMapOf()
            for (i in 1..6) {
                val customSubj = getCustomSubjectOverride(day, i)
                if (!customSubj.isNullOrBlank()) {
                    val existingTeacher = daySlots[i]?.teacherName ?: "المدرس"
                    daySlots[i] = StudentLessonSlot(customSubj, existingTeacher)
                }
            }
            overriddenMap[day] = daySlots
        }
        overriddenMap
    }

    fun getSlotsForDay(dayName: String): Map<Int, StudentLessonSlot> {
        val normTarget = normDay(dayName)
        val parsedKey = parsedTimetable.keys.firstOrNull { normDay(it) == normTarget }
        if (parsedKey != null) {
            val slots = parsedTimetable[parsedKey]
            if (!slots.isNullOrEmpty()) return slots
        }
        val defaultKey = defaultLessonsMap.keys.firstOrNull { normDay(it) == normTarget }
        return if (defaultKey != null) defaultLessonsMap[defaultKey] ?: emptyMap() else emptyMap()
    }

    fun findPrepForSubject(subj: String): AssignmentEntity? {
        if (subj.isBlank() || subj == "شاغر") return null
        val sNorm = norm(subj)
        return assignments.firstOrNull {
            val aNorm = norm(it.subjectName)
            aNorm.contains(sNorm) || sNorm.contains(aNorm)
        }
    }

    fun findDirectiveForSubject(subj: String): DirectiveDto? {
        if (subj.isBlank() || subj == "شاغر") return null
        val sNorm = norm(subj)
        return directives.firstOrNull {
            norm(it.content).contains(sNorm) || norm(it.title).contains(sNorm)
        }
    }

    fun getAttendanceForSubject(subj: String): List<AttendanceEntity> {
        if (subj.isBlank() || subj == "شاغر") return emptyList()
        val sNorm = norm(subj)
        return attendance.filter {
            val aNorm = norm(it.subject)
            aNorm.contains(sNorm) || sNorm.contains(aNorm)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (isLandscape) {
            // ----------------------------------------------------
            // LANDSCAPE MODE: VERTICAL ICON RAIL ON LEFT + FULL SCREEN SCHEDULE
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activeTheme.screenBgBrush)
            ) {
                // Left Vertical Rail (Around Notch / Left Camera cutout)
                Surface(
                    color = activeTheme.topBarBg,
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (onBack != null) {
                            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Spacer(Modifier.height(1.dp))
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 1: Themes
                            IconButton(onClick = { showThemeDialog = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Palette, contentDescription = "الثيمات", tint = Color(0xFFC084FC), modifier = Modifier.size(20.dp))
                            }
                            // 2: Pomodoro Timer
                            IconButton(onClick = { showPomodoroDialog = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = "بومودورو", tint = Color(0xFFF43F5E), modifier = Modifier.size(20.dp))
                            }
                            // 3: Tasks
                            IconButton(onClick = { showTasksSheet = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = "المهام", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                            // 4: Notebook
                            IconButton(onClick = { showNotebookSheet = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.EditNote, contentDescription = "دفتر الملاحظات", tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                            }
                            // 5: Reload
                            IconButton(
                                onClick = {
                                    isRefreshingSchedule = true
                                    viewModel.refreshData()
                                    viewModel.syncScheduleManual { success ->
                                        isRefreshingSchedule = false
                                        rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                                        StudentScheduleWidgetProvider.sendRefreshBroadcast(context)
                                        Toast.makeText(context, if (success) "تم التحديث ⚡" else "بيانات الجدول متاحة", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                if (isRefreshingSchedule || isRefreshing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                }
                            }
                            // 6: Settings
                            IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = "الضبط", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.height(1.dp))
                    }
                }

                // Schedule Content filling 100% of the remaining screen
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Micro Ribbon Banner
                    Surface(
                        color = activeTheme.headerBannerBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$studentGrade • شعبة ($studentSection)",
                            color = activeTheme.headerBannerText,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                        )
                    }

                    // Table Grid
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        val horizontalScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DiagonalHeaderCell(
                                    modifier = Modifier
                                        .width(66.dp)
                                        .height(38.dp),
                                    cornerBrush = activeTheme.cornerCellBgBrush
                                )

                                periodHeaders.forEach { header ->
                                    Box(
                                        modifier = Modifier
                                            .width(68.dp)
                                            .height(38.dp)
                                            .shadow(2.dp, RoundedCornerShape(8.dp))
                                            .background(header.bgColor, RoundedCornerShape(8.dp))
                                            .border(1.5.dp, header.borderColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = header.title,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = header.startTime,
                                                color = Color(0xFF334155),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.5.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(daysList) { dayName ->
                                    val daySlots = getSlotsForDay(dayName)
                                    val dayStyle = dayStylesMap[dayName] ?: DayStyle(dayName, activeTheme.dayCardBg, activeTheme.dayCardBorder, activeTheme.dayCardText)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(66.dp)
                                                .height(46.dp)
                                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                                .background(dayStyle.bg, RoundedCornerShape(8.dp))
                                                .border(1.5.dp, dayStyle.border, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayName,
                                                color = dayStyle.text,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.5.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        for (i in 1..6) {
                                            val slot = daySlots[i]
                                            val subj = slot?.subject ?: ""
                                            val prep = findPrepForSubject(subj)
                                            val directive = findDirectiveForSubject(subj)
                                            val hasPrep = prep != null || directive != null
                                            val isVacant = subj.isBlank() || subj == "شاغر"

                                            val cardBrush = if (hasPrep) activeTheme.prepCardBg else activeTheme.lessonCardBg
                                            val cardBorderColor = if (hasPrep) activeTheme.prepCardBorder else activeTheme.lessonCardBorder

                                            Surface(
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .height(46.dp)
                                                    .shadow(
                                                        elevation = if (hasPrep) 3.dp else 1.5.dp,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (slot != null && subj.isNotBlank()) {
                                                            selectedSlotData = Triple(dayName, i, slot)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    if (hasPrep) 1.8.dp else 1.dp,
                                                    cardBorderColor
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(cardBrush)
                                                        .padding(horizontal = 1.dp, vertical = 1.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        if (hasPrep) {
                                                            Surface(
                                                                color = Color(0xFFD97706),
                                                                shape = RoundedCornerShape(2.dp),
                                                                modifier = Modifier.padding(bottom = 1.dp)
                                                            ) {
                                                                Text(
                                                                    text = "تحضير 📝",
                                                                    color = Color.White,
                                                                    fontSize = 6.5.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text = if (!isVacant) subj else "",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 10.5.sp,
                                                            color = if (hasPrep) activeTheme.prepSubjectText else activeTheme.lessonSubjectText,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
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
        } else {
            // ----------------------------------------------------
            // PORTRAIT MODE: TOP APP BAR + FULL SCREEN SCHEDULE
            // ----------------------------------------------------
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                                }
                            }
                        },
                        actions = {
                            // 1: Themes
                            IconButton(onClick = { showThemeDialog = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "الثيمات", tint = Color(0xFFC084FC), modifier = Modifier.size(24.dp))
                            }
                            // 2: Pomodoro
                            IconButton(onClick = { showPomodoroDialog = true }) {
                                Icon(Icons.Default.Timer, contentDescription = "بومودورو", tint = Color(0xFFF43F5E), modifier = Modifier.size(24.dp))
                            }
                            // 3: Tasks
                            IconButton(onClick = { showTasksSheet = true }) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = "المهام", tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                            }
                            // 4: Notebook
                            IconButton(onClick = { showNotebookSheet = true }) {
                                Icon(Icons.Default.EditNote, contentDescription = "دفتر الملاحظات", tint = Color(0xFFFBBF24), modifier = Modifier.size(26.dp))
                            }
                            // 5: Reload
                            IconButton(
                                onClick = {
                                    isRefreshingSchedule = true
                                    viewModel.refreshData()
                                    viewModel.syncScheduleManual { success ->
                                        isRefreshingSchedule = false
                                        rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                                        StudentScheduleWidgetProvider.sendRefreshBroadcast(context)
                                        Toast.makeText(context, if (success) "تم تحديث البيانات ⚡" else "بيانات الجدول متاحة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                if (isRefreshingSchedule || isRefreshing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "إعادة التحميل", tint = Color(0xFF34D399), modifier = Modifier.size(24.dp))
                                }
                            }
                            // 6: Settings
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "الضبط", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = activeTheme.topBarBg)
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(activeTheme.screenBgBrush)
                ) {
                    Surface(
                        color = activeTheme.headerBannerBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$studentGrade • شعبة ($studentSection)",
                                color = activeTheme.headerBannerText,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        val horizontalScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DiagonalHeaderCell(
                                    modifier = Modifier
                                        .width(66.dp)
                                        .height(38.dp),
                                    cornerBrush = activeTheme.cornerCellBgBrush
                                )

                                periodHeaders.forEach { header ->
                                    Box(
                                        modifier = Modifier
                                            .width(68.dp)
                                            .height(38.dp)
                                            .shadow(2.dp, RoundedCornerShape(8.dp))
                                            .background(header.bgColor, RoundedCornerShape(8.dp))
                                            .border(1.5.dp, header.borderColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = header.title,
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = header.startTime,
                                                color = Color(0xFF334155),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.5.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(daysList) { dayName ->
                                    val daySlots = getSlotsForDay(dayName)
                                    val dayStyle = dayStylesMap[dayName] ?: DayStyle(dayName, activeTheme.dayCardBg, activeTheme.dayCardBorder, activeTheme.dayCardText)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(66.dp)
                                                .height(46.dp)
                                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                                .background(dayStyle.bg, RoundedCornerShape(8.dp))
                                                .border(1.5.dp, dayStyle.border, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayName,
                                                color = dayStyle.text,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.5.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        for (i in 1..6) {
                                            val slot = daySlots[i]
                                            val subj = slot?.subject ?: ""
                                            val prep = findPrepForSubject(subj)
                                            val directive = findDirectiveForSubject(subj)
                                            val hasPrep = prep != null || directive != null
                                            val isVacant = subj.isBlank() || subj == "شاغر"

                                            val cardBrush = if (hasPrep) activeTheme.prepCardBg else activeTheme.lessonCardBg
                                            val cardBorderColor = if (hasPrep) activeTheme.prepCardBorder else activeTheme.lessonCardBorder

                                            Surface(
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .height(46.dp)
                                                    .shadow(
                                                        elevation = if (hasPrep) 3.dp else 1.5.dp,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (slot != null && subj.isNotBlank()) {
                                                            selectedSlotData = Triple(dayName, i, slot)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    if (hasPrep) 1.8.dp else 1.dp,
                                                    cardBorderColor
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(cardBrush)
                                                        .padding(horizontal = 1.dp, vertical = 1.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        if (hasPrep) {
                                                            Surface(
                                                                color = Color(0xFFD97706),
                                                                shape = RoundedCornerShape(2.dp),
                                                                modifier = Modifier.padding(bottom = 1.dp)
                                                            ) {
                                                                Text(
                                                                    text = "تحضير 📝",
                                                                    color = Color.White,
                                                                    fontSize = 6.5.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text = if (!isVacant) subj else "",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 10.5.sp,
                                                            color = if (hasPrep) activeTheme.prepSubjectText else activeTheme.lessonSubjectText,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
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

        // ----------------------------------------------------
        // FULL SCREEN LESSON CARD DIALOG (WITH LESSON ALARM 🔔 & SUBJECT EDIT ✏️)
        // ----------------------------------------------------
        if (selectedSlotData != null) {
            val (day, periodNum, slot) = selectedSlotData!!
            val periodHeaderObj = periodHeaders.getOrNull(periodNum - 1)
            val periodName = periodHeaderObj?.title ?: "الحصة $periodNum"
            val startTimeStr = periodHeaderObj?.startTime ?: "8:00 ص"
            val prep = findPrepForSubject(slot.subject)
            val directive = findDirectiveForSubject(slot.subject)
            val subjectAbsences = getAttendanceForSubject(slot.subject)

            val alarmKey = "alarm_period_${day}_${periodNum}"
            var isAlarmEnabled by remember { mutableStateOf(prefs.getBoolean(alarmKey, false)) }

            Dialog(
                onDismissRequest = { selectedSlotData = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = true
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Class, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = slot.subject,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$day • $periodName ($startTimeStr) | المدرس: ${slot.teacherName.ifEmpty { "غير محدد" }}",
                                            fontSize = 13.sp,
                                            color = Color(0xFFE0F2FE),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedSlotData = null },
                                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Alarm Scheduler Toggle Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAlarmEnabled) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isAlarmEnabled) Color(0xFF166534) else Color(0xFFF59E0B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isAlarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = if (isAlarmEnabled) Color(0xFF166534) else Color(0xFFD97706),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isAlarmEnabled) "منبه الحصة مفعل 🔔" else "تفعيل منبه لـ $startTimeStr 🔔",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = if (isAlarmEnabled) Color(0xFF166534) else Color(0xFF78350F)
                                            )
                                            Text(
                                                text = "ينبهك التطبيق تلقائياً عند وقت بدء الحصة",
                                                fontSize = 12.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isAlarmEnabled,
                                        onCheckedChange = { enabled ->
                                            isAlarmEnabled = enabled
                                            prefs.edit().putBoolean(alarmKey, enabled).apply()
                                            if (enabled) {
                                                alarmScheduler.scheduleExactAlarm(
                                                    alarmId = periodNum * 100 + day.hashCode(),
                                                    triggerAtMillis = System.currentTimeMillis() + 60000L,
                                                    title = "موعد بدء الحصة: ${slot.subject} 🔔",
                                                    message = "بدأت الحصة $periodName ($startTimeStr) لمادة ${slot.subject}"
                                                )
                                                Toast.makeText(context, "تم تفعيل المنبه لحصة ${slot.subject} في $startTimeStr 🔔", Toast.LENGTH_SHORT).show()
                                            } else {
                                                alarmScheduler.cancelAlarm(periodNum * 100 + day.hashCode())
                                                Toast.makeText(context, "تم إلغاء منبه الحصة 🔕", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }

                            // Section 1: Lesson Details & Quick Subject Editor
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("تفاصيل الدرس والمعلومات الأساسية 📌", fontWeight = FontWeight.Black, fontSize = 15.5.sp, color = Color(0xFF0F172A))
                                        }

                                        // Edit Subject Name Button
                                        IconButton(onClick = {
                                            editSubjectText = slot.subject
                                            showEditSubjectDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل اسم المادة", tint = Color(0xFF0284C7))
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المادة:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(slot.subject, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("المدرس المسؤول:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(slot.teacherName.ifEmpty { "غير محدد" }, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("اليوم ووقت الحصة:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("$day - $periodName ($startTimeStr)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("الصف والشعبة:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("$studentGrade ($studentSection)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    }
                                }
                            }

                            // Section 2: Homework & Preparation
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (prep != null) Color(0xFFFFFBEB) else Color.White
                                ),
                                border = BorderStroke(
                                    if (prep != null) 1.5.dp else 1.dp,
                                    if (prep != null) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("التحضير والواجب المطلوب 📝", fontWeight = FontWeight.Black, fontSize = 15.5.sp, color = Color(0xFF78350F))
                                        }

                                        if (prep != null) {
                                            Surface(
                                                color = if (prep.isCompleted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (prep.isCompleted) "مكتمل ✓" else "قيد الإنجاز ⏳",
                                                    color = if (prep.isCompleted) Color(0xFF166534) else Color(0xFF991B1B),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFFDE68A))

                                    if (prep != null) {
                                        Text(prep.title, fontWeight = FontWeight.Black, fontSize = 15.5.sp, color = Color(0xFF451A03))
                                        if (prep.description.isNotBlank()) {
                                            Text(prep.description, fontSize = 13.5.sp, color = Color(0xFF78350F), lineHeight = 20.sp)
                                        }
                                        val formattedDueDate = if (prep.dueDate > 0) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(prep.dueDate)) else "قريباً"
                                        Text("موعد التسليم: $formattedDueDate", fontSize = 12.5.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)

                                        Button(
                                            onClick = { viewModel.toggleAssignment(prep) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (prep.isCompleted) Color(0xFF64748B) else Color(0xFF059669)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (prep.isCompleted) "إلغاء وضع علامة مكتمل" else "تأكيد إنجاز التحضير ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    } else {
                                        Text(
                                            text = "لا يوجد تحضير أو واجب مسجل لهذه الحصة حالياً.",
                                            color = Color(0xFF64748B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Section 3: Attendance & Absence Record
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = if (subjectAbsences.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF059669),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("سجل الحضور والغيابات لهذه المادة 📊", fontWeight = FontWeight.Black, fontSize = 15.5.sp, color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    if (subjectAbsences.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            subjectAbsences.forEach { record ->
                                                Surface(
                                                    color = Color(0xFFFEE2E2),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("الحالة: ${record.status}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF991B1B))
                                                            if (!record.note.isNullOrBlank()) {
                                                                Text(record.note, fontSize = 11.5.sp, color = Color(0xFF7F1D1D))
                                                            }
                                                        }
                                                        Text(record.dateString.ifEmpty { "سابقاً" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFFF0FDF4),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("سجل ممتاز! لا توجد غيابات مسجلة لهذه المادة ✓", color = Color(0xFF166534), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (directive != null) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("توجيهات وتعليمات المدرسة 📢", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF1E40AF))
                                        }
                                        Text(directive.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF1E3A8A))
                                        Text(directive.content, fontSize = 12.5.sp, color = Color(0xFF172554), lineHeight = 18.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { selectedSlotData = null },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("إغلاق البطاقة", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // EDIT SUBJECT NAME DIALOG (تعديل اسم المادة لتطابق الجدول الحقيقي 100%)
        // ----------------------------------------------------
        if (showEditSubjectDialog && selectedSlotData != null) {
            val (day, periodNum, slot) = selectedSlotData!!

            AlertDialog(
                onDismissRequest = { showEditSubjectDialog = false },
                title = { Text("مواءمة اسم المادة ✏️", fontWeight = FontWeight.Black, fontSize = 16.5.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اكتب اسم المادة الحقيقي لهذه الحصة ($day - الحصة $periodNum):", fontSize = 12.5.sp, color = Color(0xFF334155))
                        OutlinedTextField(
                            value = editSubjectText,
                            onValueChange = { editSubjectText = it },
                            placeholder = { Text("مثال: رياضيات، كيمياء، إنكليزي...", fontSize = 12.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editSubjectText.isNotBlank()) {
                                saveCustomSubjectOverride(day, periodNum, editSubjectText)
                                val updatedSlot = slot.copy(subject = shortenSubject(editSubjectText))
                                selectedSlotData = Triple(day, periodNum, updatedSlot)
                                showEditSubjectDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("حفظ الحصة ⚡", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showEditSubjectDialog = false }) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ----------------------------------------------------
        // POMODORO FOCUS TIMER DIALOG
        // ----------------------------------------------------
        if (showPomodoroDialog) {
            Dialog(
                onDismissRequest = { showPomodoroDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("منبه بومودورو للتركيز ⏱️", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                            }
                            IconButton(onClick = { showPomodoroDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                            CircularProgressIndicator(
                                progress = { (pomodoroSeconds.toFloat() / if (isBreakMode) 300f else 1500f) },
                                modifier = Modifier.fillMaxSize(),
                                color = if (isBreakMode) Color(0xFF10B981) else Color(0xFFF43F5E),
                                strokeWidth = 12.dp,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val mins = pomodoroSeconds / 60
                                val secs = pomodoroSeconds % 60
                                val timeFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

                                Text(
                                    text = timeFormatted,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 42.sp
                                )
                                Text(
                                    text = if (isBreakMode) "فترة استراحة ☕" else "فترة تركيز ومذاكرة 📚",
                                    color = if (isBreakMode) Color(0xFF34D399) else Color(0xFFFB7185),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isPomodoroRunning = !isPomodoroRunning },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPomodoroRunning) Color(0xFFE11D48) else Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPomodoroRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isPomodoroRunning) "إيقاف مؤقت" else "بدء التركيز الان ⏱️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isPomodoroRunning = false
                                        pomodoroSeconds = if (isBreakMode) 300 else 1500
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إعادة ضبط", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isPomodoroRunning = false
                                        isBreakMode = !isBreakMode
                                        pomodoroSeconds = if (isBreakMode) 300 else 1500
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isBreakMode) "نمط المذاكرة (25 د)" else "نمط الاستراحة (5 د)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showPomodoroDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق المنبه", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // PERSONAL NOTEBOOK DIALOG
        // ----------------------------------------------------
        if (showNotebookSheet) {
            Dialog(
                onDismissRequest = { showNotebookSheet = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("دفتر الملاحظات الشخصية 📖", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                            }
                            IconButton(onClick = { showNotebookSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        OutlinedTextField(
                            value = personalNoteText,
                            onValueChange = { personalNoteText = it },
                            placeholder = { Text("اكتب ملاحظتك الشخصية هنا وتذكر حفظها...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Button(
                            onClick = { savePersonalNote() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = personalNoteText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("حفظ الملاحظة الشخصية ✍️", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }

                        Text("الملاحظات المحفوظة سابقا: 📌", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))

                        if (personalNotesList.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(personalNotesList) { index, noteStr ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(noteStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                            IconButton(onClick = { deletePersonalNote(index) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد ملاحظات شخصية محفوظة بعد.", fontSize = 13.sp, color = Color.Gray)
                            }
                        }

                        Button(
                            onClick = { showNotebookSheet = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق دفتر الملاحظات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // TASKS DIALOG
        // ----------------------------------------------------
        if (showTasksSheet) {
            Dialog(
                onDismissRequest = { showTasksSheet = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("قائمة المهام والتحضيرات 📋", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                            }
                            IconButton(onClick = { showTasksSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        val allTasks = assignments

                        if (allTasks.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(allTasks) { assign ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (assign.isCompleted) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                                        ),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (assign.isCompleted) Color(0xFF166534) else Color(0xFFF59E0B)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(assign.subjectName, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (assign.isCompleted) Color(0xFF166534) else Color(0xFFB45309))
                                                Text(assign.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                                                if (assign.description.isNotBlank()) {
                                                    Text(assign.description, fontSize = 12.sp, color = Color(0xFF475569))
                                                }
                                            }

                                            Checkbox(
                                                checked = assign.isCompleted,
                                                onCheckedChange = { viewModel.toggleAssignment(assign) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد مهام دراسية مسجلة حالياً.", fontSize = 13.sp, color = Color.Gray)
                            }
                        }

                        Button(
                            onClick = { showTasksSheet = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق قائمة المهام", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // THEME SELECTOR DIALOG
        // ----------------------------------------------------
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("اختر ثيم ومظهر الجدول 🎨", fontWeight = FontWeight.Black, fontSize = 17.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        themesList.forEachIndexed { index, themeObj ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentThemeIndex = index
                                        prefs.edit().putInt("app_theme_index", index).apply()
                                        showThemeDialog = false
                                        Toast.makeText(context, "تم تطبيق الثيم بنجاح 🎨", Toast.LENGTH_SHORT).show()
                                    },
                                color = if (currentThemeIndex == index) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = themeObj.name,
                                    color = if (currentThemeIndex == index) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(onClick = { showThemeDialog = false }) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
