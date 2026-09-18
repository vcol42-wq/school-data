package com.school.system.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

data class ScannedQrDetails(
    val badgeTitle: String,
    val teacherName: String = "",
    val code: String = "",
    val schoolId: String = "",
    val schoolName: String = ""
)

fun parseScannedDetails(raw: String): ScannedQrDetails {
    val trimmed = raw.trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = Gson().fromJson(trimmed, mapType)
            val tName = data["teacherName"]?.toString() ?: data["teacher_name"]?.toString() ?: data["name"]?.toString() ?: ""
            val sId = data["schoolId"]?.toString() ?: data["school_id"]?.toString() ?: ""
            val pCode = data["pairingCode"]?.toString() ?: data["pairing_code"]?.toString() ?: ""
            val sName = data["schoolName"]?.toString() ?: data["school_name"]?.toString() ?: ""
            val role = data["role"]?.toString() ?: ""
            val isPrincipal = role.equals("principal", ignoreCase = true) || role.equals("supervisor", ignoreCase = true)
            val isStudent = role.equals("student", ignoreCase = true)
            val badgeTitle = when {
                isPrincipal -> "👑 بطاقة المدير السحابية (الإشراف العام)"
                isStudent -> "🎓 بطاقة الطالب السحابية (الجدول والنتائج)"
                tName.isNotBlank() -> "👨‍🏫 بطاقة المعلم السحابية"
                else -> "🏫 باركود ربط المدرسة السحابي"
            }
            ScannedQrDetails(
                badgeTitle = badgeTitle,
                teacherName = if (isPrincipal) "المدير / الإشراف العام" else if (isStudent) "طالب / ولي أمر" else tName,
                code = pCode,
                schoolId = sId,
                schoolName = sName
            )
        } catch (e: Exception) {
            ScannedQrDetails(badgeTitle = "🏫 باركود المدرسة", code = trimmed)
        }
    } else if (trimmed.startsWith("TEACHER:", ignoreCase = true)) {
        val parts = trimmed.split(":")
        var pin = ""
        var sId = ""
        var tName = ""
        if (parts.size >= 4) {
            pin = parts[1].trim()
            sId = parts[2].trim()
            tName = parts[3].trim()
        } else if (parts.size == 3) {
            pin = parts[1].trim()
            if (parts[2].startsWith("SCH-", ignoreCase = true)) {
                sId = parts[2].trim()
            } else {
                tName = parts[2].trim()
            }
        } else if (parts.size == 2) {
            pin = parts[1].trim()
        }
        return ScannedQrDetails(
            badgeTitle = "👨‍🏫 بطاقة المعلم (كود الدخول الموحد)",
            teacherName = tName,
            code = pin,
            schoolId = sId
        )
    } else if (trimmed.startsWith("SUPERVISOR:", ignoreCase = true) || trimmed.startsWith("PRINCIPAL:", ignoreCase = true)) {
        val parts = trimmed.split(":")
        val code = parts.getOrNull(1)?.trim() ?: ""
        val sId = parts.getOrNull(2)?.trim() ?: ""
        return ScannedQrDetails(
            badgeTitle = "👑 بطاقة المدير / الإشراف العام",
            teacherName = "مدير المدرسة / المشرف العام",
            code = code,
            schoolId = sId
        )
    } else if (trimmed.startsWith("OTP:", ignoreCase = true)) {
        val parts = trimmed.split(":")
        return ScannedQrDetails(
            badgeTitle = "🔑 رمز التحقق السريع (OTP)",
            code = parts.getOrNull(1)?.trim() ?: "",
            teacherName = parts.getOrNull(2)?.trim() ?: ""
        )
    } else if (trimmed.startsWith("SUP-", ignoreCase = true)) {
        return ScannedQrDetails(
            badgeTitle = "👁️ كود المشرف العام",
            code = trimmed
        )
    } else {
        return ScannedQrDetails(
            badgeTitle = "🔑 رمز التحقق والربط المباشر",
            code = trimmed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onCodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var capturedRawCode by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var showManualCodeDialog by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }

    if (showManualCodeDialog) {
        AlertDialog(
            onDismissRequest = { showManualCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF2563EB))
                    Spacer(Modifier.width(8.dp))
                    Text("الربط برمز المدرسة 🔑", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "أدخل رمز الاقتران الخاص بمدرستك (المكون عادة من 6 أرقام مثل: 112233، أو معرّف المدرسة SCH-...)",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        label = { Text("رمز أو كود المدرسة") },
                        placeholder = { Text("مثال: 112233") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = manualCodeInput.trim()
                        if (clean.isNotEmpty()) {
                            showManualCodeDialog = false
                            onCodeScanned(clean)
                        } else {
                            Toast.makeText(context, "يرجى إدخال كود المدرسة أولاً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ربط وتحقق الآن ✓", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showManualCodeDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مسح رمز التوثيق (QR)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    TextButton(onClick = { showManualCodeDialog = true }) {
                        Text("إدخال رمز 🔢", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (hasCameraPermission) {
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
                CameraPreviewWithAnalysis(
                    isPaused = capturedRawCode != null,
                    onCodeScanned = { raw ->
                        if (capturedRawCode == null) {
                            capturedRawCode = raw
                        }
                    }
                )

                QrOverlay()

                if (capturedRawCode == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "وجه الكاميرا نحو بطاقة المعلم أو باركود الإدارة",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Button(
                            onClick = { showManualCodeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("أو إدخال رمز المدرسة يدوياً (الكود) 🔢", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    // Interactive Confirmation Bottom Card
                    val details = remember(capturedRawCode) { parseScannedDetails(capturedRawCode!!) }

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFDCFCE7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, 
                                        contentDescription = null, 
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "تم التقاط الرمز بنجاح! 🎯", 
                                        fontWeight = FontWeight.Black, 
                                        fontSize = 15.sp, 
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        details.badgeTitle, 
                                        fontSize = 12.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }

                            // Do not echo the QR payload here: it contains the cloud key,
                            // tenant id and pairing secret. The payload is only handed to
                            // the pairing flow after explicit confirmation.
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp), 
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (details.teacherName.isNotBlank()) {
                                        Text(
                                            "الحساب: ${details.teacherName}",
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 13.sp, 
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                    if (details.schoolName.isNotBlank()) {
                                        Text(
                                            "المدرسة: ${details.schoolName}", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 12.sp, 
                                            color = Color(0xFF334155)
                                        )
                                    }
                                    Text(
                                        "بيانات اتصال آمنة جاهزة للإقران",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        "لن يتم عرض رمز الربط أو معرف المدرسة على الشاشة",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Confirm Button
                            Button(
                                onClick = { onCodeScanned(capturedRawCode!!) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("تأكيد الاتصال والربط الآن ✅", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }

                            // Rescan Button
                            OutlinedButton(
                                onClick = { capturedRawCode = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("إعادة المسح 🔄", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text("يرجى منح إذن الكاميرا لمسح الرمز أو إدخال الكود يدوياً", color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Button(
                        onClick = { showManualCodeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إدخال رمز المدرسة يدوياً 🔢", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraPreviewWithAnalysis(
    isPaused: Boolean,
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !isPaused) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (rawValue != null) {
                                        onCodeScanned(rawValue)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScanner", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun QrOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxSize = 250.dp.toPx()
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2
        drawRect(color = Color.Black.copy(alpha = 0.7f), size = Size(width, top))
        drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, top + boxSize), size = Size(width, height - (top + boxSize)))
        drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, top), size = Size(left, boxSize))
        drawRect(color = Color.Black.copy(alpha = 0.7f), topLeft = Offset(left + boxSize, top), size = Size(width - (left + boxSize), boxSize))
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
