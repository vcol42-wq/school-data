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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
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
            ScannedQrDetails(
                badgeTitle = if (tName.isNotBlank()) "👨‍🏫 بطاقة المعلم السحابية" else "🏫 باركود ربط المدرسة السحابي",
                teacherName = tName,
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
    } else if (trimmed.startsWith("SUPERVISOR:", ignoreCase = true)) {
        val parts = trimmed.split(":")
        val code = parts.getOrNull(1)?.trim() ?: ""
        val sId = parts.getOrNull(2)?.trim() ?: ""
        return ScannedQrDetails(
            badgeTitle = "👁️ بطاقة المشرف العام الرقابي",
            teacherName = "المشرف العام / المدير",
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مسح رمز التوثيق (QR)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
                            .padding(bottom = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
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

                            // Details Container
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
                                            "الاسم: ${details.teacherName}", 
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
                                    if (details.code.isNotBlank()) {
                                        Text(
                                            "رمز الدخول (PIN): ${details.code}", 
                                            fontSize = 12.sp, 
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    if (details.schoolId.isNotBlank()) {
                                        Text(
                                            "معرف المدرسة: ${details.schoolId}", 
                                            fontSize = 11.sp, 
                                            color = Color(0xFF64748B)
                                        )
                                    }
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
                Text("يرجى منح إذن الكاميرا لمسح الرمز")
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
