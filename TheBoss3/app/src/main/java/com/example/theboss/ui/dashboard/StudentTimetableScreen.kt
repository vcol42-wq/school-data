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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
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
    val startTime: String
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
    val periodCardBg: Color,
    val periodCardBorder: Color,
    val periodTitleText: Color,
    val periodTimeText: Color,
    val gridFloorBg: Color,
    val gridFloorBorder: Color,
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
        .replace("التربية الفنية", "فنية")
        .replace("التربية الرياضية", "نشاط بدني")
        .replace("النشاط البدني", "نشاط بدني")
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
    var showTimingDialog by remember { mutableStateOf(false) }

    // Timing States
    var schoolStartHour by remember { mutableStateOf(prefs.getString("school_start_hour", "08:00") ?: "08:00") }
    var lessonDuration by remember { mutableIntStateOf(prefs.getInt("lesson_duration_minutes", 45)) }
    var breakDuration by remember { mutableIntStateOf(prefs.getInt("break_duration_minutes", 10)) }

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
            name = "الداكن الفخم (Dark Slate & Luminous Cards) 🌙",
            topBarBg = Color(0xFF070B14),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF030712), Color(0xFF0B1120))),
            headerBannerBg = Color(0xFF0B1726),
            headerBannerText = Color(0xFF38BDF8),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
            dayCardBg = Color(0xFFE0F2FE),
            dayCardBorder = Color(0xFF0284C7),
            dayCardText = Color(0xFF0369A1),
            periodCardBg = Color(0xFFF0F9FF),
            periodCardBorder = Color(0xFF38BDF8),
            periodTitleText = Color(0xFF0284C7),
            periodTimeText = Color(0xFF0369A1),
            gridFloorBg = Color(0xFF070E1A),
            gridFloorBorder = Color(0xFF1E293B),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE0F2FE))),
            lessonCardBorder = Color(0xFF0284C7).copy(alpha = 0.65f),
            lessonSubjectText = Color(0xFF0F172A),
            lessonTeacherText = Color(0xFF0284C7),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFF59E0B),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 1,
            name = "العنابي الملكي (Royal Burgundy & Rose Cards) 🍷",
            topBarBg = Color(0xFF1B040A),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF1B040A), Color(0xFF3B0B18))),
            headerBannerBg = Color(0xFF310613),
            headerBannerText = Color(0xFFFDA4AF),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFF9F1239))),
            dayCardBg = Color(0xFFFFE4E6),
            dayCardBorder = Color(0xFFE11D48),
            dayCardText = Color(0xFF881337),
            periodCardBg = Color(0xFFFFF1F2),
            periodCardBorder = Color(0xFFFB7185),
            periodTitleText = Color(0xFF9F1239),
            periodTimeText = Color(0xFFBE123C),
            gridFloorBg = Color(0xFF22040C),
            gridFloorBorder = Color(0xFF5B0D23),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFE4E6))),
            lessonCardBorder = Color(0xFFE11D48).copy(alpha = 0.65f),
            lessonSubjectText = Color(0xFF4C0E1E),
            lessonTeacherText = Color(0xFF9F1239),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFF59E0B),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 2,
            name = "الأزرق الملكي (Royal Sapphire & Ice Cards) 💙",
            topBarBg = Color(0xFF020B17),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF020B17), Color(0xFF091C36))),
            headerBannerBg = Color(0xFF0B203E),
            headerBannerText = Color(0xFF93C5FD),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
            dayCardBg = Color(0xFFDBEAFE),
            dayCardBorder = Color(0xFF2563EB),
            dayCardText = Color(0xFF1E3A8A),
            periodCardBg = Color(0xFFEFF6FF),
            periodCardBorder = Color(0xFF60A5FA),
            periodTitleText = Color(0xFF1D4ED8),
            periodTimeText = Color(0xFF1E40AF),
            gridFloorBg = Color(0xFF030D1E),
            gridFloorBorder = Color(0xFF0A2540),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFDBEAFE))),
            lessonCardBorder = Color(0xFF2563EB).copy(alpha = 0.65f),
            lessonSubjectText = Color(0xFF0A2540),
            lessonTeacherText = Color(0xFF1D4ED8),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFF59E0B),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 3,
            name = "الفاتح العصري عالي التباين (Modern High-Contrast Light) ☀️",
            topBarBg = Color(0xFF0F172A),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))),
            headerBannerBg = Color(0xFFFFFFFF),
            headerBannerText = Color(0xFF0F172A),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
            dayCardBg = Color(0xFF0F172A),
            dayCardBorder = Color(0xFF0284C7),
            dayCardText = Color(0xFFFFFFFF),
            periodCardBg = Color(0xFFE2E8F0),
            periodCardBorder = Color(0xFF0284C7),
            periodTitleText = Color(0xFF0F172A),
            periodTimeText = Color(0xFF0284C7),
            gridFloorBg = Color(0xFFCBD5E1),
            gridFloorBorder = Color(0xFF64748B),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))),
            lessonCardBorder = Color(0xFF0284C7).copy(alpha = 0.6f),
            lessonSubjectText = Color(0xFF0F172A),
            lessonTeacherText = Color(0xFF0284C7),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFD97706),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 4,
            name = "الزمردي النقي (Emerald Mint & Jade Cards) 🌿",
            topBarBg = Color(0xFF02100B),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF02100B), Color(0xFF07261A))),
            headerBannerBg = Color(0xFF0A2B1D),
            headerBannerText = Color(0xFF6EE7B7),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
            dayCardBg = Color(0xFFD1FAE5),
            dayCardBorder = Color(0xFF059669),
            dayCardText = Color(0xFF065F46),
            periodCardBg = Color(0xFFECFDF5),
            periodCardBorder = Color(0xFF34D399),
            periodTitleText = Color(0xFF047857),
            periodTimeText = Color(0xFF065F46),
            gridFloorBg = Color(0xFF02140D),
            gridFloorBorder = Color(0xFF0E3827),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD1FAE5))),
            lessonCardBorder = Color(0xFF059669).copy(alpha = 0.65f),
            lessonSubjectText = Color(0xFF052E16),
            lessonTeacherText = Color(0xFF047857),
            prepCardBg = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))),
            prepCardBorder = Color(0xFFF59E0B),
            prepSubjectText = Color(0xFF78350F)
        ),
        AppThemePalette(
            id = 5,
            name = "البنفسجي الإمبراطوري (Amethyst & Lavender Cards) 🔮",
            topBarBg = Color(0xFF0D0317),
            screenBgBrush = Brush.verticalGradient(listOf(Color(0xFF0D0317), Color(0xFF1E0B33))),
            headerBannerBg = Color(0xFF240C3D),
            headerBannerText = Color(0xFFC4B5FD),
            cornerCellBgBrush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
            dayCardBg = Color(0xFFEDE9FE),
            dayCardBorder = Color(0xFF7C3AED),
            dayCardText = Color(0xFF581C87),
            periodCardBg = Color(0xFFF5F3FF),
            periodCardBorder = Color(0xFFA78BFA),
            periodTitleText = Color(0xFF6D28D9),
            periodTimeText = Color(0xFF581C87),
            gridFloorBg = Color(0xFF10031C),
            gridFloorBorder = Color(0xFF301152),
            lessonCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDE9FE))),
            lessonCardBorder = Color(0xFF7C3AED).copy(alpha = 0.65f),
            lessonSubjectText = Color(0xFF2E1065),
            lessonTeacherText = Color(0xFF6D28D9),
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
        if (rawScheduleJson == "{}" || rawScheduleJson.length < 10) {
            isRefreshingSchedule = true
            viewModel.syncScheduleManual {
                isRefreshingSchedule = false
                rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            }
        }
    }

    val periodHeaders = remember(rawScheduleJson, schoolStartHour, lessonDuration, breakDuration) {
        val timingObj = try {
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            rootObj?.get("_timing") as? Map<*, *>
        } catch (e: Exception) { null }

        val startHourStr = if (schoolStartHour.isNotBlank()) schoolStartHour else (timingObj?.get("schoolStartHour")?.toString() ?: prefs.getString("school_start_hour", "08:00") ?: "08:00")
        val lessonDur = if (lessonDuration > 0) lessonDuration else ((timingObj?.get("lessonDurationMinutes") as? Number)?.toInt() ?: prefs.getInt("lesson_duration_minutes", 45))
        val breakDur = if (breakDuration >= 0) breakDuration else ((timingObj?.get("breakDurationMinutes") as? Number)?.toInt() ?: prefs.getInt("break_duration_minutes", 10))

        val names = listOf("الأولى", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة")
        val bgColors = listOf(
            Color(0xFFFEF08A), Color(0xFFBAE6FD), Color(0xFFFEF08A),
            Color(0xFFFBCFE8), Color(0xFFBAE6FD), Color(0xFFFEF08A)
        )
        val textColors = listOf(
            Color(0xFFCA8A04), Color(0xFF0284C7), Color(0xFFCA8A04),
            Color(0xFFDB2777), Color(0xFF0284C7), Color(0xFFCA8A04)
        )

        val parts = startHourStr.split(":").mapNotNull { it.toIntOrNull() }
        val startH = if (parts.isNotEmpty()) parts[0] else 8
        val startM = if (parts.size > 1) parts[1] else 0

        names.mapIndexed { idx, name ->
            val lessonNum = idx + 1
            var currentTotalMinutes = startH * 60 + startM
            for (i in 1 until lessonNum) {
                currentTotalMinutes += lessonDur + breakDur
            }
            var h = currentTotalMinutes / 60
            val m = currentTotalMinutes % 60
            val period = if (h in 12..23) "م" else "ص"
            h %= 12
            if (h == 0) h = 12
            val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d %s", h, m, period)

            PeriodHeaderData(name, timeFormatted)
        }
    }

    val parsedTimetable: Map<String, Map<Int, StudentLessonSlot>> = remember(rawScheduleJson, customOverridesJson, studentGrade, studentSection) {
        val result = mutableMapOf<String, MutableMap<Int, StudentLessonSlot>>()
        daysList.forEach { result[it] = mutableMapOf() }

        fun extractLesson(lessons: Any?, period: Int): StudentLessonSlot? {
            if (lessons == null) return null
            var map: Map<*, *>? = null
            if (lessons is Map<*, *>) {
                map = (lessons["lesson$period"] ?: lessons["$period"] ?: lessons[period.toString()]) as? Map<*, *>
            } else if (lessons is List<*>) {
                if (period - 1 in lessons.indices) {
                    map = lessons[period - 1] as? Map<*, *>
                }
            }
            if (map != null) {
                val rawSubj = (map["subject"] ?: map["subjectName"] ?: map["name"])?.toString() ?: ""
                val teacher = (map["teacherName"] ?: map["teacher"] ?: map["teacher_name"])?.toString() ?: ""
                val isOff = map["isOff"] == true || map["isOff"]?.toString() == "true"
                if (!isOff && (rawSubj.isNotBlank() || teacher.isNotBlank())) {
                    return StudentLessonSlot(shortenSubject(rawSubj), teacher)
                }
            }
            return null
        }

        var parsedAny = false
        try {
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            if (rootObj != null) {
                val actualMap = (rootObj["schedule_map"] as? Map<String, Any>) ?: rootObj
                val stdS = standardizeSectionName(studentSection)

                // Pass 1: Strict Grade + Section match
                for (day in daysList) {
                    val dayData = actualMap.entries.find { normDay(it.key) == normDay(day) }?.value as? List<*>
                    if (dayData != null) {
                        for (row in dayData) {
                            if (row is Map<*, *>) {
                                val g = row["grade"]?.toString() ?: ""
                                val s = row["section"]?.toString() ?: ""
                                val rowS = standardizeSectionName(s)

                                val matchesGrade = isGradeMatch(g, studentGrade)
                                val matchesSection = (rowS == stdS || stdS == "الكل" || rowS.isEmpty())

                                if (matchesGrade && matchesSection) {
                                    val lessons = row["lessons"]
                                    for (i in 1..6) {
                                        val slot = extractLesson(lessons, i)
                                        if (slot != null) {
                                            result[day]?.put(i, slot)
                                            parsedAny = true
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
                        val dayData = actualMap.entries.find { normDay(it.key) == normDay(day) }?.value as? List<*>
                        if (dayData != null && dayData.isNotEmpty()) {
                            val firstRow = dayData.firstOrNull { it is Map<*, *> } as? Map<*, *>
                            val lessons = firstRow?.get("lessons")
                            for (i in 1..6) {
                                val slot = extractLesson(lessons, i)
                                if (slot != null) {
                                    result[day]?.put(i, slot)
                                    parsedAny = true
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

    fun matchSubjectName(s1: String?, s2: String?): Boolean {
        if (s1.isNullOrBlank() || s2.isNullOrBlank()) return false
        val std1 = shortenSubject(s1).replace("[أإآ]".toRegex(), "ا").replace("[ةه]".toRegex(), "ه").replace("[ىي]".toRegex(), "ي").lowercase()
        val std2 = shortenSubject(s2).replace("[أإآ]".toRegex(), "ا").replace("[ةه]".toRegex(), "ه").replace("[ىي]".toRegex(), "ي").lowercase()
        return std1.contains(std2) || std2.contains(std1)
    }

    fun findPrepForSubject(subj: String): AssignmentEntity? {
        if (subj.isBlank() || subj == "شاغر") return null
        return assignments.firstOrNull {
            matchSubjectName(it.subjectName, subj) || matchSubjectName(it.subjectId, subj)
        }
    }

    fun isNextUpcomingSlotForSubject(targetDay: String, targetPeriod: Int, subj: String): Boolean {
        if (subj.isBlank() || subj == "شاغر") return false

        data class SlotPos(val dayIdx: Int, val periodNum: Int, val dayName: String)
        val matchingSlots = mutableListOf<SlotPos>()

        daysList.forEachIndexed { dIdx, dName ->
            val slots = getSlotsForDay(dName)
            slots.forEach { (pNum, slot) ->
                if (matchSubjectName(slot.subject, subj)) {
                    matchingSlots.add(SlotPos(dIdx, pNum, dName))
                }
            }
        }

        if (matchingSlots.isEmpty()) return false
        if (matchingSlots.size == 1) {
            val single = matchingSlots[0]
            return single.dayName == targetDay && single.periodNum == targetPeriod
        }

        val cal = Calendar.getInstance()
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        val currentDayIdx = when (calDay) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            else -> 0
        }

        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentPeriodEstimate = when {
            currentHour < 8 -> 1
            currentHour == 8 -> 1
            currentHour == 9 -> 2
            currentHour == 10 -> 3
            currentHour == 11 -> 4
            currentHour == 12 -> 5
            currentHour == 13 -> 6
            else -> 7
        }

        val currentLinearPos = currentDayIdx * 10 + currentPeriodEstimate

        val upcomingSlot = matchingSlots
            .filter { (it.dayIdx * 10 + it.periodNum) >= currentLinearPos }
            .minByOrNull { it.dayIdx * 10 + it.periodNum }
            ?: matchingSlots.minByOrNull { it.dayIdx * 10 + it.periodNum }

        return upcomingSlot?.dayName == targetDay && upcomingSlot?.periodNum == targetPeriod
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
                            // 2: Timing
                            IconButton(onClick = { showTimingDialog = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.AccessTime, contentDescription = "توقيت الدوام", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                            // 3: Pomodoro Timer
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
                    // Table Grid Surface Container (أرضية الجدول ثلاثية الأبعاد)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = activeTheme.gridFloorBg,
                        border = BorderStroke(1.5.dp, activeTheme.gridFloorBorder),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
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
                                            .background(activeTheme.periodCardBg, RoundedCornerShape(8.dp))
                                            .border(1.5.dp, activeTheme.periodCardBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = header.title,
                                                color = activeTheme.periodTitleText,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = header.startTime,
                                                color = activeTheme.periodTimeText,
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

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(66.dp)
                                                .height(46.dp)
                                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                                .background(activeTheme.dayCardBg, RoundedCornerShape(8.dp))
                                                .border(1.5.dp, activeTheme.dayCardBorder, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayName,
                                                color = activeTheme.dayCardText,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.5.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        for (i in 1..6) {
                                            val slot = daySlots[i]
                                            val subj = slot?.subject ?: ""
                                            val prep = findPrepForSubject(subj)
                                            val isNextUpcoming = prep != null && prep.isCompleted == false && isNextUpcomingSlotForSubject(dayName, i, subj)
                                            val hasPrep = isNextUpcoming
                                            val isVacant = subj.isBlank() || subj == "شاغر"

                                            val cardBrush = when {
                                                hasPrep -> activeTheme.prepCardBg
                                                isVacant -> Brush.verticalGradient(listOf(activeTheme.gridFloorBg.copy(alpha = 0.5f), activeTheme.gridFloorBg.copy(alpha = 0.8f)))
                                                else -> activeTheme.lessonCardBg
                                            }
                                            val cardBorderColor = when {
                                                hasPrep -> activeTheme.prepCardBorder
                                                isVacant -> activeTheme.gridFloorBorder.copy(alpha = 0.35f)
                                                else -> activeTheme.lessonCardBorder
                                            }
                                            val subjectTextColor = if (hasPrep) activeTheme.prepSubjectText else activeTheme.lessonSubjectText

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
                                                            Text(
                                                                text = if (subj.isNotBlank()) "📝 تحضير $subj" else "📝 تحضير",
                                                                color = activeTheme.prepSubjectText,
                                                                fontSize = 7.5.sp,
                                                                fontWeight = FontWeight.Black,
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1,
                                                                lineHeight = 9.sp,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        Text(
                                                            text = if (!isVacant) subj else "",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 11.sp,
                                                            color = subjectTextColor,
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
                            // 2: Timing
                            IconButton(onClick = { showTimingDialog = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "توقيت الدوام", tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                            }
                            // 3: Pomodoro
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
                        .verticalScroll(rememberScrollState())
                ) {
                    // Table Grid Surface Container (أرضية الجدول ثلاثية الأبعاد)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = activeTheme.gridFloorBg,
                        border = BorderStroke(1.5.dp, activeTheme.gridFloorBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        ) {
                        val horizontalScrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                            .background(activeTheme.periodCardBg, RoundedCornerShape(8.dp))
                                            .border(1.5.dp, activeTheme.periodCardBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = header.title,
                                                color = activeTheme.periodTitleText,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = header.startTime,
                                                color = activeTheme.periodTimeText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.5.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            daysList.forEach { dayName ->
                                val daySlots = getSlotsForDay(dayName)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(66.dp)
                                            .height(46.dp)
                                            .shadow(2.dp, RoundedCornerShape(8.dp))
                                            .background(activeTheme.dayCardBg, RoundedCornerShape(8.dp))
                                            .border(1.5.dp, activeTheme.dayCardBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayName,
                                            color = activeTheme.dayCardText,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    for (i in 1..6) {
                                        val slot = daySlots[i]
                                        val subj = slot?.subject ?: ""
                                        val prep = findPrepForSubject(subj)
                                        val isNextUpcoming = prep != null && prep.isCompleted == false && isNextUpcomingSlotForSubject(dayName, i, subj)
                                        val hasPrep = isNextUpcoming
                                        val isVacant = subj.isBlank() || subj == "شاغر"

                                        val cardBrush = when {
                                            hasPrep -> activeTheme.prepCardBg
                                            isVacant -> Brush.verticalGradient(listOf(activeTheme.gridFloorBg.copy(alpha = 0.5f), activeTheme.gridFloorBg.copy(alpha = 0.8f)))
                                            else -> activeTheme.lessonCardBg
                                        }
                                        val cardBorderColor = when {
                                            hasPrep -> activeTheme.prepCardBorder
                                            isVacant -> activeTheme.gridFloorBorder.copy(alpha = 0.35f)
                                            else -> activeTheme.lessonCardBorder
                                        }
                                        val subjectTextColor = if (hasPrep) activeTheme.prepSubjectText else activeTheme.lessonSubjectText

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
                                                        Text(
                                                            text = if (subj.isNotBlank()) "📝 تحضير $subj" else "📝 تحضير",
                                                            color = activeTheme.prepSubjectText,
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            lineHeight = 9.sp,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Text(
                                                        text = if (!isVacant) subj else "",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp,
                                                        color = subjectTextColor,
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

                Spacer(Modifier.height(10.dp))

                // ----------------------------------------------------
                // DIRECTIVES & INSTRUCTIONS EXPANDABLE 3D BAR UNDER TIMETABLE
                // ----------------------------------------------------
                StudentDirectivesCardUnderTimetable(
                    directives = directives,
                    isRefreshing = isRefreshingSchedule || isRefreshing,
                    onRefresh = {
                        isRefreshingSchedule = true
                        viewModel.refreshData()
                        viewModel.syncScheduleManual {
                            isRefreshingSchedule = false
                            Toast.makeText(context, "تم تحديث التوجيهات والجدول ⚡", Toast.LENGTH_SHORT).show()
                        }
                    },
                    activeTheme = activeTheme,
                    studentGrade = studentGrade,
                    studentSection = studentSection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(24.dp))
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
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        val formattedDueDate = when {
                                            prep.dueDateString.isNotBlank() -> prep.dueDateString
                                            prep.dueDate > 0 -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(prep.dueDate))
                                            else -> "قريباً"
                                        }
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
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("منبه بومودورو للتركيز ⏱️", fontWeight = FontWeight.Black, fontSize = 17.5.sp, color = Color.White)
                            }
                            IconButton(onClick = { showPomodoroDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                            CircularProgressIndicator(
                                progress = { (pomodoroSeconds.toFloat() / if (isBreakMode) 300f else 1500f) },
                                modifier = Modifier.fillMaxSize(),
                                color = if (isBreakMode) Color(0xFF10B981) else Color(0xFFF43F5E),
                                strokeWidth = 10.dp,
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
                                    fontSize = 40.sp
                                )
                                Text(
                                    text = if (isBreakMode) "فترة استراحة ☕" else "فترة تركيز ومذاكرة 📚",
                                    color = if (isBreakMode) Color(0xFF34D399) else Color(0xFFFB7185),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPomodoroRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isPomodoroRunning) "إيقاف مؤقت" else "بدء التركيز الآن ⏱️",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
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
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إعادة ضبط 🔄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, textAlign = TextAlign.Center, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isPomodoroRunning = false
                                        isBreakMode = !isBreakMode
                                        pomodoroSeconds = if (isBreakMode) 300 else 1500
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isBreakMode) "نمط المذاكرة (25 د)" else "نمط الاستراحة (5 د)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, textAlign = TextAlign.Center, maxLines = 1)
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
        // SCHOOL TIMING CONFIGURATION DIALOG (تعديل توقيت بدء الدوام للحصة الأولى)
        // ----------------------------------------------------
        if (showTimingDialog) {
            var selectedHourText by remember { mutableStateOf(schoolStartHour) }
            var selectedLessonMinutes by remember { mutableIntStateOf(lessonDuration) }

            AlertDialog(
                onDismissRequest = { showTimingDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("توقيت بدء الدوام المدرسي ⏰", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "حدد توقيت بدء الحصة الأولى ومعدل مدة الحصة ليتوافق جدول الدروس مع الدوام الفعلي لمدرستك 100%:",
                            fontSize = 12.5.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )

                        Text("اختيار توقيت بدء الحصة الأولى (الصباحي / المسائي):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "08:00" to "08:00 ص (صباحي)",
                                "08:30" to "08:30 ص (صباحي)",
                                "12:00" to "12:00 م (مسائي)",
                                "12:30" to "12:30 م (مسائي)",
                                "13:00" to "01:00 م (ظهري)"
                            )
                            presets.forEach { (timeVal, label) ->
                                FilterChip(
                                    selected = (selectedHourText == timeVal),
                                    onClick = { selectedHourText = timeVal },
                                    label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = selectedHourText,
                            onValueChange = { selectedHourText = it },
                            label = { Text("توقيت مخصص (HH:mm)") },
                            placeholder = { Text("08:00 أو 12:30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text("مدة الحصة الواحدة:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(40, 45, 50).forEach { mins ->
                                FilterChip(
                                    selected = (selectedLessonMinutes == mins),
                                    onClick = { selectedLessonMinutes = mins },
                                    label = { Text("$mins دقيقة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanHour = selectedHourText.trim()
                            if (cleanHour.isNotBlank()) {
                                schoolStartHour = cleanHour
                                lessonDuration = selectedLessonMinutes
                                prefs.edit()
                                    .putString("school_start_hour", cleanHour)
                                    .putInt("lesson_duration_minutes", selectedLessonMinutes)
                                    .apply()
                                showTimingDialog = false
                                Toast.makeText(context, "تم حفظ توقيت الدوام المخصص ⚡", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("تأكيد التوقيت ⏰", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showTimingDialog = false }) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                }
            )
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
}

@Composable
fun StudentDirectivesCardUnderTimetable(
    directives: List<DirectiveDto>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    activeTheme: AppThemePalette,
    studentGrade: String = "",
    studentSection: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    // Auto-expand automatically when directives arrive or update
    val directivesFingerprint = remember(directives) {
        directives.map { it.id }.sorted().joinToString(",")
    }
    LaunchedEffect(directivesFingerprint) {
        if (directives.isNotEmpty()) {
            isExpanded = true
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = activeTheme.headerBannerBg,
        border = BorderStroke(1.5.dp, activeTheme.dayCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Sleek 3D Expandable Ribbon/Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(3.dp, CircleShape)
                            .clip(CircleShape)
                            .background(activeTheme.cornerCellBgBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "توجيهات الإدارة المدرسية 📢",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = activeTheme.headerBannerText
                            )

                            if (directives.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = activeTheme.dayCardBorder.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, activeTheme.dayCardBorder)
                                ) {
                                    Text(
                                        text = "${directives.size}",
                                        color = activeTheme.headerBannerText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        if (studentGrade.isNotBlank()) {
                            Text(
                                text = "$studentGrade • شعبة ($studentSection)",
                                fontSize = 11.sp,
                                color = activeTheme.periodTimeText
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = activeTheme.headerBannerText,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث التوجيهات",
                                tint = activeTheme.headerBannerText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "طي" else "توسيع",
                        tint = activeTheme.headerBannerText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expanded Directives List
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = activeTheme.dayCardBorder.copy(alpha = 0.3f))

                    if (directives.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = activeTheme.periodCardBg,
                            border = BorderStroke(1.dp, activeTheme.periodCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "لا توجد توجيهات جديدة حالياً",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = activeTheme.periodTitleText
                                    )
                                    Text(
                                        text = "ستصلك هنا تعليمات وتعاميم الإدارة المدرسية المعتمدة فور نشرها 🌟",
                                        fontSize = 11.sp,
                                        color = activeTheme.periodTimeText
                                    )
                                }
                            }
                        }
                    } else {
                        val sortedDirectives = remember(directives) {
                            directives.sortedByDescending { it.createdAt ?: it.id.toString() }
                        }

                        sortedDirectives.forEachIndexed { idx: Int, directive: DirectiveDto ->
                            DirectiveItemView(
                                directive = directive,
                                isPrimary = (idx == 0),
                                activeTheme = activeTheme,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString("${directive.title}\n${directive.content}"))
                                    Toast.makeText(context, "تم نسخ التوجيه إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectiveItemView(
    directive: DirectiveDto,
    isPrimary: Boolean,
    activeTheme: AppThemePalette,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = activeTheme.periodCardBg,
        border = BorderStroke(1.dp, if (isPrimary) activeTheme.dayCardBorder else activeTheme.periodCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = activeTheme.dayCardBorder.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, activeTheme.dayCardBorder)
                    ) {
                        Text(
                            text = if (isPrimary) "⭐ أحدث تعميم" else "تعميم مدرسي",
                            color = activeTheme.headerBannerText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = directive.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = activeTheme.periodTitleText
                    )
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ",
                        tint = activeTheme.periodTimeText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = directive.content,
                fontSize = 12.sp,
                color = activeTheme.periodTimeText,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DirectiveItemView(
    directive: DirectiveDto,
    isPrimary: Boolean,
    onCopy: () -> Unit
) {
    val bgColor = if (isPrimary) Color(0xFF1E293B) else Color(0xFF162032)
    val borderColor = if (isPrimary) Color(0xFF0284C7).copy(alpha = 0.6f) else Color(0xFF334155)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isPrimary) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF475569).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, if (isPrimary) Color(0xFF38BDF8) else Color(0xFF64748B))
                    ) {
                        Text(
                            text = if (isPrimary) "⭐ أحدث تعميم" else "تعميم مدرسي",
                            color = if (isPrimary) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, Color(0xFF34D399).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = when (directive.targetRole) {
                                "students", "student" -> "موجه للطلبة 👨‍🎓"
                                "teachers", "teacher" -> "موجه للكادر 👨‍🏫"
                                else -> "موجه للجميع 📢"
                            },
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ النص",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = directive.title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = directive.content,
                fontSize = 12.5.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 19.sp
            )

            if (!directive.createdAt.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = directive.createdAt.take(16).replace("T", " "),
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
