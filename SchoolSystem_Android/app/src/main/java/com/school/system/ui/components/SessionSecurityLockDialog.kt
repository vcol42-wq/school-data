package com.school.system.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.school.system.data.SecurityLockState

@Composable
fun SessionSecurityLockDialog(
    lockedState: SecurityLockState.Locked,
    onVerifyNewCode: (String) -> Unit,
    onSwitchOffline: () -> Unit,
    onScanQr: () -> Unit
) {
    var newCodeInput by remember { mutableStateOf("") }
    var showConfirmOffline by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Non-dismissible: cannot be dismissed by back press or outside click */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning / Lock Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color(0xFFFEF2F2),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل أمني",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Title
                Text(
                    text = "تعليق الجلسة: حماية بيانات المدرسة",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Reason message
                Text(
                    text = lockedState.reason,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                if (!showConfirmOffline) {
                    // Option 1: Enter New Code
                    Text(
                        text = "الخيار الأول: إدخال رمز المدرسة أو الباركود الجديد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    OutlinedTextField(
                        value = newCodeInput,
                        onValueChange = { newCodeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رمز المدرسة / المعلم الجديد") },
                        placeholder = { Text("مثال: 112233 أو كود الباركود") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newCodeInput.isNotBlank()) {
                                    onVerifyNewCode(newCodeInput.trim())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            enabled = newCodeInput.isNotBlank()
                        ) {
                            Text("تأكيد الرمز الجديد ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = onScanQr,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 2: Switch to Offline Mode
                    OutlinedButton(
                        onClick = { showConfirmOffline = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Text(
                            text = "الخيار الثاني: التحول إلى أوفلاين (مسح الشعب) ⚠️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Confirmation View for Wiping Data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                                Text(
                                    text = "تأكيد مسح الشعب والتحول لأوفلاين",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF9F1239)
                                )
                            }
                            Text(
                                text = "سيتم مسح كافة الصفوف والشعب وسجلات الطلاب المرتبطة بهذه المدرسة فوراً لحماية الخصوصية، وتصفير الجداول والودجت والعودة إلى تطبيق محلي نظيف.",
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF881337)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSwitchOffline,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("تأكيد المسح والتحول لأوفلاين", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { showConfirmOffline = false },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("تراجع")
                        }
                    }
                }
            }
        }
    }
}
