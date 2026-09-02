package com.principal.system.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.principal.system.data.repository.PrincipalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: PrincipalRepository
) : ViewModel() {

    fun pairWithQr(qrContent: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.pairWithQr(qrContent)
            if (res.isSuccess) {
                repository.syncAllExecutiveMetrics()
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun pairDirectDefault(onResult: (Boolean) -> Unit) {
        repository.pairManual(
            schoolId = "SCH-VCOL-6072",
            schoolName = "م.كعب بن مالك المسائية للبنين",
            schoolCode = "112233",
            url = "https://pexehlvkpdhmpukjydwd.supabase.co",
            apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY"
        )
        viewModelScope.launch {
            repository.syncAllExecutiveMetrics()
            onResult(true)
        }
    }

    fun pairManualSimple(schoolCode: String, onResult: (Boolean) -> Unit) {
        repository.pairManual(
            schoolId = if (schoolCode.startsWith("SCH-")) schoolCode else "SCH-VCOL-6072",
            schoolName = "م.كعب بن مالك المسائية للبنين",
            schoolCode = schoolCode,
            url = "https://pexehlvkpdhmpukjydwd.supabase.co",
            apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY"
        )
        viewModelScope.launch {
            repository.syncAllExecutiveMetrics()
            onResult(true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalOnboardingScreen(
    onPairedSuccess: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isCameraActive by remember { mutableStateOf(false) }
    var simpleSchoolCode by remember { mutableStateOf("112233") }
    var isLoading by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> 
            hasCameraPermission = granted
            if (granted) isCameraActive = true
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF0F172A))
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Surface(
                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لوحة قيادة المدير التنفيذي",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "متابعة الحضور المباشر ورادار نشاط المعلمين",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Action Options Card
        Column(
            modifier = Modifier
                .padding(20.dp)
                .offset(y = (-20).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // 1. Primary Big Action: Instant 1-Click Connect
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.pairDirectDefault { success ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "تم الاتصال المباشر بمدرستك بنجاح ⚡", Toast.LENGTH_SHORT).show()
                                    onPairedSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("دخول واتصال فوري بمدرستي ⚡", fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // 2. Secondary Big Action: Scan QR Code
                    OutlinedButton(
                        onClick = {
                            if (hasCameraPermission) {
                                isCameraActive = !isCameraActive
                            } else {
                                launcher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF0284C7)),
                        enabled = !isLoading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isCameraActive) "إغلاق الكاميرا ✕" else "مسح باركود شاشة الكمبيوتر 📷", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Camera Scanner View if active
                    if (isCameraActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PrincipalQrScannerView(
                                onCodeScanned = { qr ->
                                    isCameraActive = false
                                    isLoading = true
                                    viewModel.pairWithQr(qr) { success, error ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, "تم ربط المدرسة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                            onPairedSuccess()
                                        } else {
                                            Toast.makeText(context, "خطأ في قراءة الرمز: ${error ?: "غير صالح"}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                    // 3. Simple 6-Digit Code Entry
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("أو إدخال رمز الاقتران البسيط للمدرسة:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = simpleSchoolCode,
                                onValueChange = { simpleSchoolCode = it },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    isLoading = true
                                    viewModel.pairManualSimple(simpleSchoolCode) { success ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, "تم الربط بالرمز بنجاح ✓", Toast.LENGTH_SHORT).show()
                                            onPairedSuccess()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("ربط 🔗", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrincipalQrScannerView(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isScanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !isScanned) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank() && !isScanned) {
                                        isScanned = true
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

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
