package com.school.system.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onActivationComplete: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: androidx.navigation.NavController? = null
) {
    var teacherName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var schoolId by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var isIndependentMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Observe code scanned from QR scanner
    val scannedCode = navController?.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("scanned_code")
        ?.observeAsState()

    LaunchedEffect(scannedCode?.value) {
        scannedCode?.value?.let { code ->
            try {
                val gson = com.google.gson.Gson()
                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                val data: Map<String, String> = gson.fromJson(code, mapType)
                schoolId = data["schoolId"] ?: ""
                serverUrl = data["url"] ?: ""
                pairingCode = data["pairingCode"] ?: ""
                Toast.makeText(context, "تم قراءة باركود المدرسة بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                serverUrl = code
                Toast.makeText(context, "تم تعيين رابط الخادم يدوياً", Toast.LENGTH_SHORT).show()
            }
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_code")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8FAFC))
    ) {
        // Royal Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF065F46), Color(0xFF064E3B))
                    ),
                    shape = RoundedCornerShape(bottomStart = 64.dp, bottomEnd = 64.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFFFCD34D), // Golden
                    modifier = Modifier.size(70.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "سجل المدرس الذكي",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "بوابة رصد الدرجات والغيابات الفورية",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        // Selection Mode & Form
        Column(
            modifier = Modifier
                .padding(24.dp)
                .offset(y = (-40).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    Text(
                        "تفعيل طريقة العمل",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )

                    // Tabs selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isIndependentMode,
                            onClick = { isIndependentMode = false },
                            label = { Text("الربط السحابي", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF065F46),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = isIndependentMode,
                            onClick = { isIndependentMode = true },
                            label = { Text("سجل مستقل (أوفلاين)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF065F46),
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("اسم الأستاذ الكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF065F46)) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, null, tint = Color(0xFF065F46)) },
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (!isIndependentMode) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("عنوان خادم المدرسة (IP)") },
                            placeholder = { Text("مثال: 192.168.1.100:3000") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Link, null, tint = Color(0xFF065F46)) },
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = schoolId,
                                onValueChange = { schoolId = it.uppercase() },
                                label = { Text("معرف المدرسة ID") },
                                placeholder = { Text("SCH-XXXX") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = pairingCode,
                                onValueChange = { pairingCode = it },
                                label = { Text("رمز الاقتران") },
                                placeholder = { Text("999888") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToQrScanner,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF065F46))
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null)
                            Spacer(Modifier.width(8.dp))
                            Text("مسح باركود المدرسة QR")
                        }
                    }

                    Button(
                        onClick = {
                            if (teacherName.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة اسم الأستاذ أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true

                            if (isIndependentMode) {
                                // Activate local offline register
                                viewModel.activate(teacherName, email, "", "") {
                                    isLoading = false
                                    onActivationComplete()
                                }
                            } else {
                                // Request pairing with School Cloud Server
                                viewModel.viewModelScope.launch {
                                    try {
                                        var formattedUrl = serverUrl.trim()
                                        if (formattedUrl.isNotEmpty() && !formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                            formattedUrl = "http://$formattedUrl"
                                        }

                                        // Connect to Cloud
                                        val connected = viewModel.syncManager.connectToCloud(formattedUrl)
                                        if (!connected) {
                                            isLoading = false
                                            Toast.makeText(context, "تعذر الاتصال بالخادم. يرجى التحقق من العنوان أو الشبكة.", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }

                                        // Request Pairing
                                        val result = viewModel.syncManager.requestPairing(
                                            teacherName = teacherName,
                                            grade = "",
                                            section = "",
                                            subject = "",
                                            pairingCode = pairingCode
                                        )

                                        if (result.success) {
                                            viewModel.activate(teacherName, email, formattedUrl, schoolId) {
                                                isLoading = false
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                                onActivationComplete()
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(context, "حدث خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                        enabled = !isLoading && teacherName.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, null)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (isIndependentMode) "تفعيل العمل المستقل" else "تفعيل والربط السحابي",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Text(
                "العمل كجزء من منصة مدرسة يتيح التحديث الفوري للأسماء والدرجات مع إدارة المدرسة بسلاسة وأمان.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}
