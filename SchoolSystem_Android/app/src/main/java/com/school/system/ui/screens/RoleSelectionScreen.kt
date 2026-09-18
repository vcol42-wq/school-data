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
import androidx.compose.material.icons.filled.School
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
    var pendingRoleToConfirm by remember { mutableStateOf<AppRole?>(null) }

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
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "اختر بوابتك المخصصة للدخول الفوري",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Compact Role Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Teacher Card
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 30 }
                ) {
                    CompactRoleCard(
                        title = "بوابة الأستاذ / المعلم",
                        badge = "👨‍🏫 كادر المدرسة والمشرفين",
                        subtitle = "سجل الدرجات التفاعلي، جدول الحصص، والغيابات",
                        tags = listOf("سجل الدرجات", "الجدول", "الغيابات", "الإشراف"),
                        primaryColor = Color(0xFF4F46E5),
                        secondaryColor = Color(0xFF6366F1),
                        accentBorder = Color(0xFF818CF8),
                        onClick = {
                            pendingRoleToConfirm = AppRole.TEACHER
                        }
                    )
                }

                // Student Card
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { 40 }
                ) {
                    CompactRoleCard(
                        title = "بوابة الطالب / ولي الأمر",
                        badge = "🎓 الطلبة وأولياء الأمور",
                        subtitle = "الجدول الأسبوعي المباشر، الواجبات، والنتائج",
                        tags = listOf("جدول الدروس", "الواجبات", "الحضور", "التقوية"),
                        primaryColor = Color(0xFF059669),
                        secondaryColor = Color(0xFF10B981),
                        accentBorder = Color(0xFF34D399),
                        onClick = {
                            pendingRoleToConfirm = AppRole.STUDENT
                        }
                    )
                }

                // Principal Card
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { 50 }
                ) {
                    CompactRoleCard(
                        title = "بوابة المدير / الإدارة العامة",
                        badge = "👑 الإدارة المدرسية والمشرفين",
                        subtitle = "لوحة القيادة المباشرة، بث التوجيهات، والرقابة الشاملة",
                        tags = listOf("لوحة القيادة", "بث التوجيهات", "متابعة الحصص", "كافة الصفوف"),
                        primaryColor = Color(0xFFD97706),
                        secondaryColor = Color(0xFFF59E0B),
                        accentBorder = Color(0xFFFBBF24),
                        onClick = {
                            pendingRoleToConfirm = AppRole.PRINCIPAL
                        }
                    )
                }
            }

            // 3. Compact Bottom Guarantee Note - Fully visible & scrollable
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
                            text = "عزل تام: يتم تذكر اختيارك، ويمكنك دائماً تغيير البوابة من شاشة الإعدادات.",
                            fontSize = 10.5.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Role Selection Confirmation Dialog (تأكيد الاختيار لمنع الخطأ)
    pendingRoleToConfirm?.let { role ->
        val isTeacher = role == AppRole.TEACHER
        val isStudent = role == AppRole.STUDENT
        val isPrincipal = role == AppRole.PRINCIPAL
        AlertDialog(
            onDismissRequest = { pendingRoleToConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isTeacher -> Icons.Default.School
                            isStudent -> Icons.Default.School
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = when {
                            isTeacher -> Color(0xFF6366F1)
                            isStudent -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            isTeacher -> "تأكيد الدخول كـ (أستاذ) 👨‍🏫"
                            isStudent -> "تأكيد الدخول كـ (طالب) 🎓"
                            else -> "تأكيد الدخول كـ (مدير / إدارة المدرسة) 👑"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                Text(
                    text = when {
                        isTeacher ->
                            "أنت على وشك الدخول إلى بوابة الأستاذ والمشرف (سجل الدرجات والجداول).\n\nسيتم تثبيت هذا الخيار لفتح واجهتك مباشرة في كل مرة، كما يمكنك دائماً تغيير البوابة أو إعادة الاختيار من شاشة الإعدادات في أي وقت.\n\nهل تود المتابعة؟"
                        isStudent ->
                            "أنت على وشك الدخول إلى بوابة الطالب وولي الأمر (الجدول والواجبات).\n\nسيتم تثبيت هذا الخيار لفتح واجهتك مباشرة في كل مرة، كما يمكنك دائماً تغيير البوابة أو إعادة الاختيار من شاشة الإعدادات في أي وقت.\n\nهل تود المتابعة؟"
                        else ->
                            "أنت على وشك الدخول إلى بوابة المدير والإدارة المدرسية (لوحة القيادة، بث التوجيهات، ومتابعة كافة الصفوف والأساتذة).\n\nسيتم تثبيت هذا الخيار لفتح واجهتك مباشرة في كل مرة، كما يمكنك دائماً تغيير البوابة من شاشة الإعدادات.\n\nهل تود المتابعة؟"
                    },
                    fontSize = 12.5.sp,
                    color = Color(0xFF334155),
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        RoleManager.setSelectedRole(context, role)
                        when (role) {
                            AppRole.TEACHER -> onSelectTeacher()
                            AppRole.STUDENT -> onSelectStudent()
                            AppRole.PRINCIPAL -> onSelectPrincipal()
                        }
                        pendingRoleToConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isTeacher -> Color(0xFF4F46E5)
                            isStudent -> Color(0xFF059669)
                            else -> Color(0xFFD97706)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، تأكيد وتثبيت", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoleToConfirm = null }) {
                    Text("تراجع", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CompactRoleCard(
    title: String,
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
        border = BorderStroke(1.5.dp, accentBorder.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Row: Badge & Enter Button Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = primaryColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentBorder.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBorder,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "دخول",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row (mini pill chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                tags.take(4).forEach { tag ->
                    Surface(
                        color = Color(0xFF334155).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
