package com.example.theboss.ui.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.system.utils.RoleManager

@Composable
fun OnboardingScreen(
    onNavigateToJoin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToRoleSelection: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Top Bar Switch Role Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(
                    onClick = {
                        RoleManager.clearSelectedRole(context)
                        onNavigateToRoleSelection()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تبديل الصفة ⇄", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "بوابة الطالب الذكية",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "متابعة الدرجات والغيابات وجدول الحصص والتحضيرات المدرسية",
                    color = Color(0xFFC7D2FE),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Action Options Card
        Card(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Primary Action: Instant Direct Connect
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("onboarding_shown", true)
                            .putBoolean("independent_mode", false)
                            .putString("school_id", "SCH-VCOL-6072")
                            .putString("school_code", "112233")
                            .putString("school_name", "م.كعب بن مالك المسائية للبنين")
                            .putString("supabase_url", "https://pexehlvkpdhmpukjydwd.supabase.co")
                            .putString("supabase_key", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY")
                            .apply()
                        onNavigateToJoin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("دخول واتصال فوري بالمدرسة ⚡", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }

                // 2. Secondary Action: Scan QR Code or Join Request
                OutlinedButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("onboarding_shown", true).apply()
                        onNavigateToJoin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4338CA)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF6366F1))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("مسح باركود المدرسة أو إدخال كود 📷", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 3. Standalone Mode
                TextButton(
                    onClick = {
                        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("onboarding_shown", true)
                            .putBoolean("independent_mode", true)
                            .apply()
                        onNavigateToDashboard()
                    }
                ) {
                    Text("المتابعة كطالب مستقل بدون ربط مدرسي 👤", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
