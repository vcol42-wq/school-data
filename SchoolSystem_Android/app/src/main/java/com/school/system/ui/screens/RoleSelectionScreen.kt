package com.school.system.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.system.R
import com.school.system.utils.AppRole
import com.school.system.utils.RoleManager

@Composable
fun RoleSelectionScreen(
    onSelectTeacher: () -> Unit,
    onSelectStudent: () -> Unit,
    onSelectPrincipal: () -> Unit = {}
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1120),
                        Color(0xFF0F172A),
                        Color(0xFF090D1A)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Compact Header
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -30 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .shadow(6.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.sxs),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "المنظومة المدرسية الذكية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "اختر بوابتك المخصصة للدخول المباشر",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Role Cards in the exact order requested: Student first, then Teacher, then Principal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: STUDENT (طالب) - First as requested
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 30 }
                ) {
                    CompactRoleCard(
                        roleWord = "طالب",
                        ctaText = "أنت طالب؟ اضغط هنا 👈",
                        badge = "🎓 الطلبة وأولياء الأمور",
                        subtitle = "الجدول الأسبوعي المباشر، الواجبات اليومية، والنتائج",
                        tags = listOf("جدول الحصص", "الواجبات", "الحضور", "التقوية"),
                        primaryColor = Color(0xFF059669),
                        secondaryColor = Color(0xFF10B981),
                        accentBorder = Color(0xFF34D399),
                        onClick = {
                            RoleManager.setSelectedRole(context, AppRole.STUDENT)
                            onSelectStudent()
                        }
                    )
                }

                // Card 2: TEACHER (أستاذ)
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 40 }
                ) {
                    CompactRoleCard(
                        roleWord = "أستاذ",
                        ctaText = "أنت أستاذ؟ اضغط هنا 👈",
                        badge = "👨‍🏫 كادر المدرسة والمعلمين",
                        subtitle = "سجل الدرجات التفاعلي، جدول الحصص، والغيابات",
                        tags = listOf("سجل الدرجات", "جدول الحصص", "الغيابات", "الإشراف"),
                        primaryColor = Color(0xFF4F46E5),
                        secondaryColor = Color(0xFF6366F1),
                        accentBorder = Color(0xFF818CF8),
                        onClick = {
                            RoleManager.setSelectedRole(context, AppRole.TEACHER)
                            onSelectTeacher()
                        }
                    )
                }

                // Card 3: PRINCIPAL (مدير)
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { 50 }
                ) {
                    CompactRoleCard(
                        roleWord = "مدير",
                        ctaText = "أنت مدير؟ اضغط هنا 👈",
                        badge = "👑 الإدارة المدرسية والمشرفين",
                        subtitle = "لوحة القيادة المباشرة، بث التوجيهات، والرقابة الشاملة",
                        tags = listOf("لوحة القيادة", "بث التوجيهات", "متابعة الحصص", "كافة الصفوف"),
                        primaryColor = Color(0xFFD97706),
                        secondaryColor = Color(0xFFF59E0B),
                        accentBorder = Color(0xFFFBBF24),
                        onClick = {
                            RoleManager.setSelectedRole(context, AppRole.PRINCIPAL)
                            onSelectPrincipal()
                        }
                    )
                }
            }

            // 3. Compact Bottom Guarantee Note
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(900))
            ) {
                Surface(
                    color = Color(0x1A38BDF8),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x3338BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔒 القفل بعد الربط: يمكنك الرجوع وتغيير البوابة قبل إدخال البيانات، وبمجرد الربط الفعلي بالمدرسة يتم قفل الجهاز نهائياً.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactRoleCard(
    roleWord: String,
    ctaText: String,
    badge: String,
    subtitle: String,
    tags: List<String>,
    primaryColor: Color,
    secondaryColor: Color,
    accentBorder: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        border = BorderStroke(1.8.dp, accentBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.28f),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header Row: Badge & Big Role Word
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = primaryColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBorder,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = roleWord,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tags row (mini pill chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.take(4).forEach { tag ->
                    Surface(
                        color = Color(0xFF334155).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VERY PROMINENT CTA BUTTON (أنت طالب؟ اضغط هنا / أنت أستاذ؟ اضغط هنا / أنت مدير؟ اضغط هنا)
            Surface(
                onClick = onClick,
                color = primaryColor,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ctaText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
