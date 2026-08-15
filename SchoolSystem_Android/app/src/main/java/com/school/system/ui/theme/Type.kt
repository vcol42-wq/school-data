package com.school.system.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.school.system.R

// Local Font Families
val Amiri = FontFamily(Font(R.font.amiri))
val Naskh = FontFamily(
    Font(R.font.naskh_regular, FontWeight.Normal),
    Font(R.font.naskh_bold, FontWeight.Bold)
)
val ArabType = FontFamily(Font(R.font.arabtype))
val ArabSq = FontFamily(Font(R.font.arabsq))
val ArabSqTp = FontFamily(Font(R.font.arabsqtp))
val Kitab = FontFamily(Font(R.font.kitab_regular))

// Default typography using Naskh as the primary font for Iraqi school context
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Naskh,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Naskh,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Naskh,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ArabSq,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    )
)
