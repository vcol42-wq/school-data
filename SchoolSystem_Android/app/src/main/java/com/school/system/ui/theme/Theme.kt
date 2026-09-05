package com.school.system.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * الثيمات الـ 5 المعتمدة لتطبيق سجل المدرس الذكي - هادئة، متناسقة، متدرجة وغير صارخة
 */
enum class AppThemeType(
    val titleArabic: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val tableHeaderBg: Color,
    val tableHeaderTextColor: Color,
    val tableCellBg: Color,
    val tableAltCellBg: Color,
    val tableBorderColor: Color,
    val ribbonGradient: List<Color>,
    val isDark: Boolean = false
) {
    // 1. الوضع الكريمي الدافئ (Cream / Warm Vanilla Ivory)
    CREAM(
        titleArabic = "الوضع الكريمي الدافئ",
        primaryColor = Color(0xFF786248),
        secondaryColor = Color(0xFFA89078),
        backgroundColor = Color(0xFFFDFBF7),
        surfaceColor = Color(0xFFFFFFFF),
        textPrimaryColor = Color(0xFF382F24),
        textSecondaryColor = Color(0xFF786A58),
        tableHeaderBg = Color(0xFFF5EFEB),
        tableHeaderTextColor = Color(0xFF382F24),
        tableCellBg = Color(0xFFFFFFFF),
        tableAltCellBg = Color(0xFFFBF8F4),
        tableBorderColor = Color(0xFFE8DFD5),
        ribbonGradient = listOf(Color(0xFF786248), Color(0xFF8C7355)),
        isDark = false
    ),

    // 2. الوضع العنابي الملكي (Royal Burgundy / Rich Wine)
    BURGUNDY(
        titleArabic = "الوضع العنابي الملكي",
        primaryColor = Color(0xFF881337),
        secondaryColor = Color(0xFF9F1239),
        backgroundColor = Color(0xFFFFF1F2),
        surfaceColor = Color(0xFFFFFFFF),
        textPrimaryColor = Color(0xFF4C0519),
        textSecondaryColor = Color(0xFF9F1239),
        tableHeaderBg = Color(0xFFFFE4E6),
        tableHeaderTextColor = Color(0xFF4C0519),
        tableCellBg = Color(0xFFFFFFFF),
        tableAltCellBg = Color(0xFFFFF1F2),
        tableBorderColor = Color(0xFFFECDD3),
        ribbonGradient = listOf(Color(0xFF4C0519), Color(0xFF881337)),
        isDark = false
    ),

    // 3. الوضع السماوي الهادئ (Serene Sky / Coastal Blue)
    SKY(
        titleArabic = "الوضع السماوي",
        primaryColor = Color(0xFF2B6CB0),
        secondaryColor = Color(0xFF4299E1),
        backgroundColor = Color(0xFFF4F8FA),
        surfaceColor = Color(0xFFFFFFFF),
        textPrimaryColor = Color(0xFF1A365D),
        textSecondaryColor = Color(0xFF4A5568),
        tableHeaderBg = Color(0xFFE8F1F5),
        tableHeaderTextColor = Color(0xFF1A365D),
        tableCellBg = Color(0xFFFFFFFF),
        tableAltCellBg = Color(0xFFF4F8FA),
        tableBorderColor = Color(0xFFD3E4ED),
        ribbonGradient = listOf(Color(0xFF2B6CB0), Color(0xFF3182CE)),
        isDark = false
    ),

    // 4. الوضع الزمردي الهادئ (Sage / Forest Moss)
    EMERALD(
        titleArabic = "الوضع الزمردي",
        primaryColor = Color(0xFF2F6F52),
        secondaryColor = Color(0xFF4A8F6E),
        backgroundColor = Color(0xFFF4F7F5),
        surfaceColor = Color(0xFFFFFFFF),
        textPrimaryColor = Color(0xFF143525),
        textSecondaryColor = Color(0xFF2F6F52),
        tableHeaderBg = Color(0xFFE7EFEA),
        tableHeaderTextColor = Color(0xFF143525),
        tableCellBg = Color(0xFFFFFFFF),
        tableAltCellBg = Color(0xFFF4F7F5),
        tableBorderColor = Color(0xFFD1E2D7),
        ribbonGradient = listOf(Color(0xFF245740), Color(0xFF2F6F52)),
        isDark = false
    ),

    // 5. الوضع الليلي عالي التباين (Night / High-Contrast Dark)
    NIGHT(
        titleArabic = "الوضع الليلي عالي التباين",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF60A5FA),
        backgroundColor = Color(0xFF0F172A),
        surfaceColor = Color(0xFF1E293B),
        textPrimaryColor = Color(0xFFF8FAFC),
        textSecondaryColor = Color(0xFFCBD5E1),
        tableHeaderBg = Color(0xFF1E293B),
        tableHeaderTextColor = Color(0xFF93C5FD),
        tableCellBg = Color(0xFF1E293B),
        tableAltCellBg = Color(0xFF0F172A),
        tableBorderColor = Color(0xFF475569),
        ribbonGradient = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
        isDark = true
    ),

    // 6. الوضع القمري الهادئ (Lunar / Deep Midnight Moonlight)
    LUNAR(
        titleArabic = "الوضع القمري الهادئ",
        primaryColor = Color(0xFF818CF8),
        secondaryColor = Color(0xFFA5B4FC),
        backgroundColor = Color(0xFF090D16),
        surfaceColor = Color(0xFF131B2E),
        textPrimaryColor = Color(0xFFF1F5F9),
        textSecondaryColor = Color(0xFF94A3B8),
        tableHeaderBg = Color(0xFF131B2E),
        tableHeaderTextColor = Color(0xFFA5B4FC),
        tableCellBg = Color(0xFF131B2E),
        tableAltCellBg = Color(0xFF0E1524),
        tableBorderColor = Color(0xFF334155),
        ribbonGradient = listOf(Color(0xFF090D16), Color(0xFF1E1B4B)),
        isDark = true
    )
}

object ThemeManager {
    private const val PREFS_NAME = "diyala_theme_prefs"
    private const val KEY_THEME = "selected_theme_key"

    private val _currentTheme = MutableStateFlow(AppThemeType.CREAM)
    val currentTheme = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_THEME, AppThemeType.CREAM.name) ?: AppThemeType.CREAM.name
        val theme = try {
            when (savedName) {
                "DARK", "NIGHT" -> AppThemeType.NIGHT
                "MOONLIGHT", "LUNAR" -> AppThemeType.LUNAR
                "ROSE", "LIGHT_PINK" -> AppThemeType.CREAM
                "BURGUNDY", "CRIMSON" -> AppThemeType.BURGUNDY
                else -> AppThemeType.valueOf(savedName)
            }
        } catch (e: Exception) {
            AppThemeType.CREAM
        }
        _currentTheme.value = theme
    }

    fun setTheme(context: Context, theme: AppThemeType) {
        _currentTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }
}

val LocalAppTheme = staticCompositionLocalOf { AppThemeType.CREAM }

@Composable
fun SchoolSystemTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ThemeManager.init(context)
    }

    val activeTheme by ThemeManager.currentTheme.collectAsState()

    val colorScheme = if (activeTheme.isDark) {
        darkColorScheme(
            primary = activeTheme.primaryColor,
            onPrimary = Color(0xFF0F172A),
            secondary = activeTheme.secondaryColor,
            onSecondary = Color(0xFF0F172A),
            background = activeTheme.backgroundColor,
            onBackground = activeTheme.textPrimaryColor,
            surface = activeTheme.surfaceColor,
            onSurface = activeTheme.textPrimaryColor,
            surfaceVariant = activeTheme.tableAltCellBg,
            onSurfaceVariant = activeTheme.textSecondaryColor,
            outline = activeTheme.tableBorderColor
        )
    } else {
        lightColorScheme(
            primary = activeTheme.primaryColor,
            onPrimary = Color.White,
            secondary = activeTheme.secondaryColor,
            onSecondary = Color.White,
            background = activeTheme.backgroundColor,
            onBackground = activeTheme.textPrimaryColor,
            surface = activeTheme.surfaceColor,
            onSurface = activeTheme.textPrimaryColor,
            surfaceVariant = activeTheme.tableAltCellBg,
            onSurfaceVariant = activeTheme.textSecondaryColor,
            outline = activeTheme.tableBorderColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) break
                ctx = ctx.baseContext
            }
            val activity = ctx as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = activeTheme.primaryColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !activeTheme.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides activeTheme,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * نافذة اختيار الثيم المنبثقة لاختيار الثيم من بين الـ 5 ثيمات الهادئة
 */
@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by ThemeManager.currentTheme.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = currentTheme.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("تخصيص ثيم ومظهر التطبيق 🎨", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اختر مظهراً متناسقاً وهادئاً يناسب ذوقك أثناء العمل اليومي:",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(AppThemeType.values()) { theme ->
                        val isSelected = theme == currentTheme
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.backgroundColor,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) theme.primaryColor else Color(0xFFCBD5E1)
                            ),
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemeManager.setTheme(context, theme)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                Brush.linearGradient(listOf(theme.primaryColor, theme.secondaryColor)),
                                                CircleShape
                                            )
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = theme.titleArabic,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (theme.isDark) Color.White else Color(0xFF1E293B)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = theme.primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor)
            ) {
                Text("تم ✓", fontWeight = FontWeight.Bold)
            }
        }
    )
}
