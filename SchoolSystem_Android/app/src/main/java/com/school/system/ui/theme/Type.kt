package com.school.system.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.school.system.R

// Amiri Font Family unified across the ecosystem
val Amiri = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold),
    Font(R.font.amiri, FontWeight.Medium)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 14.sp
    )
)
