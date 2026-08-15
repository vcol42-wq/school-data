package com.school.system.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

enum class AppThemeType {
    VIBRANT, CLASSIC, DIYALA, EMERALD, DARK, BURGUNDY
}

private val VibrantColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF7C3AED),
    background = Color(0xFFF8FAFC)
)

private val ClassicColorScheme = lightColorScheme(
    primary = Color(0xFF334155),
    secondary = Color(0xFF64748B),
    background = Color(0xFFF1F5F9)
)

private val DiyalaColorScheme = lightColorScheme(
    primary = Color(0xFF065F46),
    secondary = Color(0xFF059669),
    background = Color(0xFFECFDF5)
)

private val EmeraldColorScheme = lightColorScheme(
    primary = Color(0xFF10B981),
    secondary = Color(0xFF059669),
    background = Color(0xFFF0FDF4)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF0F172A)
)

private val BurgundyColorScheme = lightColorScheme(
    primary = Color(0xFF9F1239),
    secondary = Color(0xFFBE123C),
    background = Color(0xFFFFF1F2)
)

@Composable
fun SchoolSystemTheme(
    themeType: AppThemeType = AppThemeType.VIBRANT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        AppThemeType.VIBRANT -> VibrantColorScheme
        AppThemeType.CLASSIC -> ClassicColorScheme
        AppThemeType.DIYALA -> DiyalaColorScheme
        AppThemeType.EMERALD -> EmeraldColorScheme
        AppThemeType.DARK -> DarkColorScheme
        AppThemeType.BURGUNDY -> BurgundyColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            val activity = context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            content = content
        )
    }
}
