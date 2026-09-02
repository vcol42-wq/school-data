package com.principal.system.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.principal.system.R

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold),
    Font(R.font.amiri, FontWeight.Medium)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 14.sp
    )
)
