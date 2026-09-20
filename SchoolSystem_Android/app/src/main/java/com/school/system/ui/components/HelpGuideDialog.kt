package com.school.system.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HelpGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                // Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1E1B4B), Color(0xFF2563EB))
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
                                    text = "دليل الاستخدام والتعليمات 📖",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "إرشادات استخدام سجل المدرس الذكي",
                                    color = Color(0xFFC7D2FE),
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
                    // Section 1: Online vs Offline
                    HelpGuideCard(
                        icon = Icons.Default.CloudSync,
                        iconTint = Color(0xFF2563EB),
                        title = "1. العمل المتصل (أونلاين) والوضع المستقل (أوفلاين)",
                        badge = "حرية الاتصال",
                        badgeColor = Color(0xFFEFF6FF),
                        badgeTextColor = Color(0xFF1D4ED8)
                    ) {
                        Text(
                            text = "• الوضع المتصل (Online ☁️): يمكنك ربط التطبيق بسحابة المدرسة لتنزيل قوائم الطلاب، ومزامنة الدرجات مع برنامج الإدارة، واستيراد جدول الحصص بضغطة زر واحدة.\n" +
                                    "• الوضع غير المتصل (Offline 👤): يعمل التطبيق بكامل وظائفه محلياً داخل جهازك دون الحاجة لإنترنت؛ حيث يمكنك إنشاء وتعديل الشعب وإدخال الدرجات والغيابات والطباعة، وتُحفظ البيانات في قاعدة بيانات هاتفك بأمان تام.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 2: Daily Register is the ONLY source of truth
                    HelpGuideCard(
                        icon = Icons.Default.Stars,
                        iconTint = Color(0xFFD97706),
                        title = "2. السجل الوحيد المعتمد في التعديل (سجل اليومي)",
                        badge = "قاعدة أساسية ⭐",
                        badgeColor = Color(0xFFFEF3C7),
                        badgeTextColor = Color(0xFF92400E)
                    ) {
                        Text(
                            text = "• (سجل اليومي) هو السجل الرئيسي والوحيد الذي تُدخل وتُعدل فيه الدرجات والتقييمات الشهرية واليومية.\n" +
                                    "• السجلات الأخرى (سجل المدرس التفصيلي، سجل الإدارة والامتحانات) هي سجلات احتساب تلقائي تُستخرج وتُحسب معدلاتها وسعياتها تلقائياً وتبعاً لما تدخله في سجل اليومي، مما يوفر وقتك ويمنع أخطاء الحساب اليدوي.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Section 3: Subject Types
                    HelpGuideCard(
                        icon = Icons.Default.Category,
                        iconTint = Color(0xFF059669),
                        title = "3. أنواع المواد والتقييمات",
                        badge = "معايير وزارية",
                        badgeColor = Color(0xFFECFDF5),
                        badgeTextColor = Color(0xFF047857)
                    ) {
                        Text(
                            text = "• المواد التحريرية + الشفهية: (مثل العربي، الإنكليزي، الإسلامية) يدعم النظام تقسيم درجات الشفهي والتحريري والنشاط.\n" +
                                    "• المواد التحريرية: (مثل الرياضيات، العلوم، الفيزياء، الكيمياء، الأحياء، الاجتماعيات) تعتمد الدرجات التحريرية والامتحانات.\n" +
                                    "• مواد التقويم والأنشطة الخاصة: (مثل التربية الفنية، الرياضة، النشيد) يتم احتسابها بنظام التقويم التقديري المستمر وفق التعليمات التربوية.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 4: Print & Share
                    HelpGuideCard(
                        icon = Icons.Default.Print,
                        iconTint = Color(0xFF7C3AED),
                        title = "4. الطباعة والمشاركة الرسمية (A4 & PDF)",
                        badge = "تقارير معتمدة",
                        badgeColor = Color(0xFFF5F3FF),
                        badgeTextColor = Color(0xFF6D28D9)
                    ) {
                        Text(
                            text = "• يمكنك طباعة أي سجل (سجل اليومي، سجل المدرس، سجل الإدارة، سجل الغيابات) مباشرة عبر الطابعات المتصلة بتنسيق ورقي A4 رسمي.\n" +
                                    "• كما يمكنك تصدير ومشاركة السجل كملف PDF عالي الجودة أو ملف رقمي لمشاركته مع إدارة المدرسة أو اللجنة الامتحانية عبر تطبيقات التواصل.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 5: Pairing with School
                    HelpGuideCard(
                        icon = Icons.Default.QrCodeScanner,
                        iconTint = Color(0xFF0284C7),
                        title = "5. طريقة الربط والاقتران مع المدرسة",
                        badge = "خطوات سهلة",
                        badgeColor = Color(0xFFF0F9FF),
                        badgeTextColor = Color(0xFF0369A1)
                    ) {
                        Text(
                            text = "• الطريقة الأولى (رمز QR 📷): من شاشة برنامج المدير المكتبي، اضغط على إظهار باركود المعلم، ثم اختر (مسح رمز QR) في هاتفك ليتم الربط الآمن والتنزيل الفوري للشعب.\n" +
                                    "• الطريقة الثانية (رمز الاقتران 🔢): أدخل رمز المدرسة الموحد (مثل 112233) واسم الأستاذ من شاشة الإعدادات أو واجهة الترحيب.\n" +
                                    "• لإلغاء الاقتران: يمكنك في أي وقت إلغاء الاقتران من شاشة الإعدادات للعودة للوضع المحلي المستقل ومسح بيانات الربط.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 19.sp
                        )
                    }

                    // Section 6: Official Support & Contact Channels
                    HelpGuideCard(
                        icon = Icons.Default.HeadsetMic,
                        iconTint = Color(0xFF059669),
                        title = "6. الدعم الفني وقناة الواتساب والبريد المعتمد",
                        badge = "تواصل مباشر 💬",
                        badgeColor = Color(0xFFECFDF5),
                        badgeTextColor = Color(0xFF047857)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "يسعدنا دائماً استقبال استفساراتكم ومقترحاتكم أو طلب تفعيل النسخة المكتبية للحاسوب عبر قنواتنا الرسمية المعتمدة:",
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 18.sp
                            )
                            
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) { }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("الانضمام لقناة المنظومة على واتساب 📢", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:vcol42@gmail.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "استفسار بخصوص منظومة ThePrincipal")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) { }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2563EB))
                                Spacer(Modifier.width(8.dp))
                                Text("البريد الإلكتروني: vcol42@gmail.com ✉️", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                        }
                    }
                }

                // Footer Action
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("فهمت ذلك ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpGuideCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = iconTint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
            Spacer(Modifier.height(8.dp))

            content()
        }
    }
}
