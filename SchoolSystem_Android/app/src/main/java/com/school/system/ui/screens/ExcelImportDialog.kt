package com.school.system.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.school.system.utils.excel.IraqiSchoolExcelParser
import com.school.system.utils.excel.ParsedSchoolClass
import com.school.system.utils.excel.XlsxStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ImportStep {
    SELECT_FILE,
    READING_FILE,
    PREVIEW,
    UPLOADING,
    SUCCESS,
    ERROR
}

@Composable
fun ExcelImportDialog(
    onDismiss: () -> Unit,
    onUploadClasses: (
        classes: List<ParsedSchoolClass>,
        onProgress: (Float, String) -> Unit,
        onResult: (Boolean, String, Pair<Int, Int>?) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(ImportStep.SELECT_FILE) }
    var selectedFileName by remember { mutableStateOf("") }
    var parsedClasses by remember { mutableStateOf<List<ParsedSchoolClass>>(emptyList()) }
    var totalStudentsCount by remember { mutableIntStateOf(0) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var progressMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var uploadSummary by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentStep = ImportStep.READING_FILE
            progressMessage = "جاري قراءة واستخراج أوراق العمل..."
            selectedFileName = uri.lastPathSegment ?: "ملف إكسل"

            scope.launch {
                try {
                    val rawSheets = withContext(Dispatchers.IO) {
                        XlsxStreamReader.readWorkbook(context, uri)
                    }

                    if (rawSheets.isEmpty()) {
                        errorMessage = "تعذر قراءة ملف الإكسل. يرجى التأكد من أن الملف بصيغة .xlsx صالحة."
                        currentStep = ImportStep.ERROR
                        return@launch
                    }

                    progressMessage = "جاري تطبيق خوارزميات الفلترة المدرسية الذكية..."
                    val classes = withContext(Dispatchers.Default) {
                        IraqiSchoolExcelParser.parseWorkbook(rawSheets)
                    }

                    if (classes.isEmpty() || classes.all { it.students.isEmpty() }) {
                        errorMessage = "لم يتم العثور على أي بيانات طلاب أو فصول مطابقة داخل الملف."
                        currentStep = ImportStep.ERROR
                        return@launch
                    }

                    parsedClasses = classes
                    totalStudentsCount = classes.sumOf { it.students.size }
                    currentStep = ImportStep.PREVIEW
                } catch (e: Exception) {
                    errorMessage = "حدث خطأ أثناء معالجة الملف: ${e.localizedMessage ?: e.message}"
                    currentStep = ImportStep.ERROR
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (currentStep != ImportStep.UPLOADING) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "الاستيراد الذكي لقوائم الطلاب 📊",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "محرك الإكسل السحابي للمدير (نظام التربية العراقي)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (currentStep != ImportStep.UPLOADING) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Dynamic Body according to current step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        ImportStep.SELECT_FILE -> {
                            SelectFileView(
                                onPickFile = {
                                    filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                },
                                onPickAllFiles = {
                                    filePickerLauncher.launch("*/*")
                                }
                            )
                        }
                        ImportStep.READING_FILE -> {
                            LoadingProgressView(message = progressMessage)
                        }
                        ImportStep.PREVIEW -> {
                            PreviewClassesView(
                                classes = parsedClasses,
                                totalStudents = totalStudentsCount,
                                fileName = selectedFileName,
                                onReSelect = {
                                    currentStep = ImportStep.SELECT_FILE
                                },
                                onConfirmUpload = {
                                    currentStep = ImportStep.UPLOADING
                                    uploadProgress = 0.05f
                                    progressMessage = "بدء رفع الفصول والطلاب إلى السحابة..."
                                    onUploadClasses(
                                        parsedClasses,
                                        { prog, msg ->
                                            uploadProgress = prog
                                            progressMessage = msg
                                        },
                                        { success, resultMsg, counts ->
                                            if (success) {
                                                uploadSummary = counts
                                                currentStep = ImportStep.SUCCESS
                                            } else {
                                                errorMessage = resultMsg
                                                currentStep = ImportStep.ERROR
                                            }
                                        }
                                    )
                                }
                            )
                        }
                        ImportStep.UPLOADING -> {
                            UploadingProgressView(
                                progress = uploadProgress,
                                message = progressMessage
                            )
                        }
                        ImportStep.SUCCESS -> {
                            SuccessView(
                                counts = uploadSummary,
                                onClose = onDismiss
                            )
                        }
                        ImportStep.ERROR -> {
                            ErrorView(
                                message = errorMessage,
                                onRetry = {
                                    currentStep = ImportStep.SELECT_FILE
                                },
                                onClose = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectFileView(
    onPickFile: () -> Unit,
    onPickAllFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onPickFile() },
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "اضغط هنا لاختيار ملف الإكسل (.xlsx)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF065F46)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "يتم التحليل بدون استهلاك الذاكرة وبسرعة فائقة",
                        fontSize = 11.sp,
                        color = Color(0xFF047857)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مميزات المحرك العراقي الذكي 🇮🇶:",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.height(8.dp))
                    FeaturePoint("كشف تلقائي للصف والشعبة من اسم الورقة أو عنوان الجدول.")
                    FeaturePoint("توحيد المسميات الوزارية (الأول المتوسط، الرابع العلمي، إلخ).")
                    FeaturePoint("استخراج وتجزئة الأسماء الرباعية والألقاب بدقة عالية.")
                    FeaturePoint("فلترة هوامش المجموع وترويسات الوزارة والإحصائيات غير المرغوبة.")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("تصفح ملفات الإكسل 📁", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onPickAllFiles,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("كافة الملفات", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeaturePoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("•", color = Color(0xFF059669), fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.5.sp, color = Color(0xFF334155))
    }
}

@Composable
private fun LoadingProgressView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFF059669),
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PreviewClassesView(
    classes: List<ParsedSchoolClass>,
    totalStudents: Int,
    fileName: String,
    onReSelect: () -> Unit,
    onConfirmUpload: () -> Unit
) {
    var expandedIndex by remember { mutableIntStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("إجمالي الشعب", fontSize = 11.sp, color = Color(0xFF1D4ED8))
                    Text(
                        "${classes.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E40AF)
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("إجمالي الطلاب", fontSize = 11.sp, color = Color(0xFF15803D))
                    Text(
                        "$totalStudents",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF166534)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "معاينة الفصول المكتشفة في ($fileName):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(classes.indices.toList()) { index ->
                val item = classes[index]
                val isExpanded = expandedIndex == index

                Surface(
                    color = if (isExpanded) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) Color(0xFF3B82F6) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            expandedIndex = if (isExpanded) -1 else index
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF2563EB).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🏫", fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${item.grade} - شعبة ${item.section}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "الورقة: ${item.sheetName}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${item.students.size} طالب",
                                        color = Color(0xFF166534),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Expanded Student Preview
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "عينة من الطلاب المسجلين:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(4.dp))
                                item.students.take(8).forEachIndexed { sIdx, s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${sIdx + 1}. ${s.fullName}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (s.recordNumber.isNotBlank()) {
                                            Text(
                                                text = "رقم: ${s.recordNumber}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                                if (item.students.size > 8) {
                                    Text(
                                        text = "... و ${item.students.size - 8} طلاب آخرين",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReSelect,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.8f)
            ) {
                Text("ملف آخر", fontSize = 12.sp)
            }

            Button(
                onClick = onConfirmUpload,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("رفع للسحابة 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun UploadingProgressView(
    progress: Float,
    message: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Color(0xFF10B981).copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "جاري رفع القوائم وتحديث قاعدة البيانات...",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF059669),
            trackColor = Color(0xFFD1FAE5)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF059669)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = message,
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SuccessView(
    counts: Pair<Int, Int>?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Color(0xFFD1FAE5),
            shape = CircleShape,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "اكتمل الاستيراد والمزامنة بنجاح! 🎉",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = Color(0xFF065F46)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "تم رفع ${counts?.first ?: 0} صف/شعبة و ${counts?.second ?: 0} طالب وتحديث السجلات المحلية فوراً.",
            fontSize = 12.sp,
            color = Color(0xFF047857),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("تم والعودة للوحة القيادة ✓", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Color(0xFFFEE2E2),
            shape = CircleShape,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "تعذر إتمام الاستيراد",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF991B1B)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = message,
            fontSize = 11.5.sp,
            color = Color(0xFFB91C1C),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onClose,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("محاولة ثانية 🔄", fontWeight = FontWeight.Bold)
            }
        }
    }
}
