package com.example.theboss.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun StudentHelpGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with Emerald & Cyan Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF065F46), Color(0xFF0284C7))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "دليل الطالب وولي الأمر 📖",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "إرشادات استخدام منصة الطالب والجدول المدرسي",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: Timetable & Periods
                    StudentHelpCard(
                        icon = Icons.Default.CalendarMonth,
                        iconTint = Color(0xFF059669),
                        title = "1. الجدول المدرسي ومتابعة الحصص اليومية",
                        badge = "الجدول الذكي 📅",
                        badgeColor = Color(0xFFECFDF5),
                        badgeTextColor = Color(0xFF047857)
                    ) {
                        Text(
                            text = "• متابعة الحصص الأسبوعية: يمكنك تصفح جدول جميع أيام الأسبوع (من الأحد إلى الخميس) بنقرة واحدة، ومعرفة اسم المادة، رقم الحصة، والأستاذ المكلف لكل درس.\n" +
                                    "• تتبع الحصة الحالية: يبرز التطبيق الحصة الجارية باللون الأخضر مع عداد تنازلي للوقت المتبقي حتى نهاية الدرس.\n" +
                                    "• تنبيهات الشواغر والاستراحة: يوضح التطبيق فترات الاستراحة والشواغر بدقة لمساعدتك في تنظيم وقتك داخل المدرسة.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 2: Homework & Tasks
                    StudentHelpCard(
                        icon = Icons.Default.AssignmentTurnedIn,
                        iconTint = Color(0xFF0284C7),
                        title = "2. الواجبات والمهام المدرسية اليومية",
                        badge = "تنظيم المذاكرة 📝",
                        badgeColor = Color(0xFFF0F9FF),
                        badgeTextColor = Color(0xFF0369A1)
                    ) {
                        Text(
                            text = "• تسجيل ومتابعة الواجبات: يمكنك فتح قائمة المهام المدرسية وتسجيل الواجبات المطلوبة لكل مادة دراسية.\n" +
                                    "• علامة الإنجاز: اضغط على الدائرة بجانب أي مهمة لإكمالها فور إنجازها، مما يمنحك شعوراً بالإنجاز ورؤية واضحة للمهام المتبقية.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 3: Pomodoro & Study Notes
                    StudentHelpCard(
                        icon = Icons.Default.Timer,
                        iconTint = Color(0xFFE11D48),
                        title = "3. مؤقت بومودورو للتركيز ودفتر الملاحظات",
                        badge = "مذاكرة ذكية ⏱️",
                        badgeColor = Color(0xFFFFF1F2),
                        badgeTextColor = Color(0xFFBE123C)
                    ) {
                        Text(
                            text = "• مؤقت بومودورو الذكي (Pomodoro): اضغط على أيقونة الساعة بالأعلى لتفعيل جلسات التركيز الدراسي (25 دقيقة مذاكرة مركزة تليها 5 دقائق استراحة) لزيادة الاستيعاب والتركيز.\n" +
                                    "• دفتر الملاحظات السريع: أيقونة الدفتر تتيح لك تدوين الملاحظات والقوانين والملخصات الدراسية وحفظها محلياً للرجوع إليها في أي وقت.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 4: Cloud Sync & Linking
                    StudentHelpCard(
                        icon = Icons.Default.CloudSync,
                        iconTint = Color(0xFF7C3AED),
                        title = "4. الربط السحابي ومزامنة بيانات المدرسة",
                        badge = "تزامن فوري ⚡",
                        badgeColor = Color(0xFFF5F3FF),
                        badgeTextColor = Color(0xFF6D28D9)
                    ) {
                        Text(
                            text = "• الاقتران السريع: يتم ربط التطبيق مع مدرستك بمسح رمز الباركود (QR) لمرة واحدة فقط عند فتح التطبيق.\n" +
                                    "• الحفظ التلقائي للرمز: يتم تخزين رمز الربط بأمان على هاتفك، بحيث يمكنك إعادة الاتصال المباشر بنقرة واحدة من شاشة الضبط دون الحاجة لمسح الباركود ثانية.\n" +
                                    "• التحديث الفوري للجدول: عند قيام إدارة المدرسة أو المدرس بأي تعديل في الأنصبة أو الحصص، يمكنك الضغط على زر التحديث 🔄 لمزامنة أحدث نسخة فوراً.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 5: Offline Mode & Privacy
                    StudentHelpCard(
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFF0D9488),
                        title = "5. العمل بدون إنترنت وأمان البيانات",
                        badge = "خصوصية تامة 🔒",
                        badgeColor = Color(0xFFF0FDFA),
                        badgeTextColor = Color(0xFF0F766E)
                    ) {
                        Text(
                            text = "• يعمل أوفلاين بالكامل: بمجرد اقتران جدولك، يعمل التطبيق بكافة شاشاته دون الحاجة لأي اتصال بالإنترنت.\n" +
                                    "• خالٍ تماماً من الإعلانات: لا يحتوي التطبيق على أي إعلانات أو برمجيات تتبع تجارية.\n" +
                                    "• تشفير وخصوصية: جميع بيانات الطالب والجدول مشفرة ومخصصة لمدرستك وشعبتك فقط وفق أعلى معايير أمان البيانات المدرسية.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 6: Themes & Customization
                    StudentHelpCard(
                        icon = Icons.Default.Palette,
                        iconTint = Color(0xFFF59E0B),
                        title = "6. المظهر والثيمات وتوقيت الدوام",
                        badge = "تخصيص كامل 🎨",
                        badgeColor = Color(0xFFFFFBEB),
                        badgeTextColor = Color(0xFFB45309)
                    ) {
                        Text(
                            text = "• ثيمات متعددة: يمكنك التبديل بين الثيمات اللونية (الليلي، الكوني، الأخضر الزمردي، الأزرق الهادئ) عبر أيقونة لوحة الألوان بالأعلى.\n" +
                                    "• تعديل توقيت الدوام: تتيح لك أيقونة الساعة ضبط أوقات الحصص والاستراحة لتتوافق تماماً مع توقيت دوام مدرستك (صباحي / مسائي).",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // Footer Bar
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("فهمت، حسناً ✓", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentHelpCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeTextColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

            content()
        }
    }
}
