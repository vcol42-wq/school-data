package com.theprincipal.keygen.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theprincipal.keygen.data.LicenseStorage
import com.theprincipal.keygen.logic.LicenseGeneratorEngine
import com.theprincipal.keygen.model.IssuedLicense
import com.theprincipal.keygen.model.ProductType
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyGenApp() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storage = remember { LicenseStorage(context) }

    var selectedProduct by remember { mutableStateOf(ProductType.THE_PRINCIPAL_DESKTOP) }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }

    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("زين كاش (ZainCash)") }
    var notes by remember { mutableStateOf("") }

    var generatedLicense by remember { mutableStateOf<IssuedLicense?>(null) }
    var historyList by remember { mutableStateOf(storage.getAllLicenses()) }
    var searchQuery by remember { mutableStateOf("") }

    val paymentOptions = listOf(
        "زين كاش (ZainCash) 🇮🇶",
        "كي كارد / مصرف الرافدين",
        "حوالة نقدية مباشرة",
        "ترخيص رسمي معتمد"
    )

    fun refreshHistory() {
        historyList = storage.getAllLicenses()
    }

    fun buildShareMessage(lic: IssuedLicense): String {
        return """
مرحباً إدارة (${lic.clientName}) المحترمين،
تم بنجاح إصدار وتفعيل ترخيصكم الرسمي لمنظومة ${lic.productName} لمرة واحدة مدى الحياة 💎.

🔑 كود التفعيل الخاص بمدرستكم:
${lic.licenseKey}

💻 رابط التنزيل المباشر للمنظومة:
${selectedProduct.downloadUrl}

📢 قناة المنظومة الرسمية على واتساب:
https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J

✉️ البريد الإلكتروني المعتمد للدعم:
vcol42@gmail.com

نتمنى لكم دوام التوفيق والتميز الإداري!
        """.trimIndent()
    }

    fun openWhatsApp(lic: IssuedLicense) {
        try {
            val message = buildShareMessage(lic)
            val encoded = URLEncoder.encode(message, "UTF-8")
            val phone = lic.clientPhone.trim().replace(Regex("[^0-9]"), "")
            val url = if (phone.isNotEmpty()) {
                val formattedPhone = if (phone.startsWith("07")) "964" + phone.substring(1) else phone
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encoded"
            } else {
                "https://api.whatsapp.com/send?text=$encoded"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareGeneric(text: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "مشاركة ترخيص المنظومة")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح قائمة المشاركة", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F19)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF064E3B))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("👑", fontSize = 22.sp)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "The Principal Master KeyGen",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "لوحة إصدار التراخيص الحصرية للمالك",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "صلاحية المالك 🔒",
                                    color = Color(0xFF34D399),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Generator Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(1.dp, Color(0xFF1F2937))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚡ توليد ترخيص جديد",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )

                        // Product Selection Dropdown
                        Column {
                            Text("البرنامج أو التطبيق المستهدف:", color = Color(0xFF9CA3AF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isProductDropdownExpanded,
                                onExpandedChange = { isProductDropdownExpanded = !isProductDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedProduct.titleAr,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF374151),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF030712),
                                        unfocusedContainerColor = Color(0xFF030712)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isProductDropdownExpanded,
                                    onDismissRequest = { isProductDropdownExpanded = false }
                                ) {
                                    ProductType.values().forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text(prod.titleAr, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                            onClick = {
                                                selectedProduct = prod
                                                isProductDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Client Name Input
                        Column {
                            Text("اسم المدرسة المستفيدة أو المستلم:", color = Color(0xFF9CA3AF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                placeholder = { Text("مثال: ثانوية المتفوقين للبنين", color = Color(0xFF4B5563), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF030712),
                                    unfocusedContainerColor = Color(0xFF030712)
                                )
                            )
                        }

                        // Client Phone Input
                        Column {
                            Text("رقم الهاتف (لإرسال الكود فوراً):", color = Color(0xFF9CA3AF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = clientPhone,
                                onValueChange = { clientPhone = it },
                                placeholder = { Text("078XXXXXXXX أو 077XXXXXXXX", color = Color(0xFF4B5563), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF030712),
                                    unfocusedContainerColor = Color(0xFF030712)
                                )
                            )
                        }

                        // Payment Method
                        Column {
                            Text("طريقة الدفع والحوالة:", color = Color(0xFF9CA3AF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                paymentOptions.take(2).forEach { method ->
                                    val isSelected = selectedPayment.startsWith(method.take(6))
                                    Surface(
                                        onClick = { selectedPayment = method },
                                        color = if (isSelected) Color(0xFF059669).copy(alpha = 0.3f) else Color(0xFF1F2937),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else Color(0xFF374151)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = method,
                                            color = if (isSelected) Color(0xFF6EE7B7) else Color(0xFF9CA3AF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Notes Input
                        Column {
                            Text("ملاحظات إضافية (اختياري):", color = Color(0xFF9CA3AF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("المحافظة، اسم المدير، تفاصيل الحوالة...", color = Color(0xFF4B5563), fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF030712),
                                    unfocusedContainerColor = Color(0xFF030712)
                                )
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Generate Button
                        Button(
                            onClick = {
                                if (clientName.trim().isEmpty()) {
                                    Toast.makeText(context, "يرجى كتابة اسم المدرسة أو الزبون أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val newKey = LicenseGeneratorEngine.generateUnifiedLicense(
                                    productCode = selectedProduct.code,
                                    tier = selectedProduct.defaultTier
                                )
                                val newRecord = IssuedLicense(
                                    licenseKey = newKey,
                                    productCode = selectedProduct.code,
                                    productName = selectedProduct.titleAr,
                                    clientName = clientName.trim(),
                                    clientPhone = clientPhone.trim(),
                                    paymentMethod = selectedPayment,
                                    notes = notes.trim()
                                )
                                storage.saveLicense(newRecord)
                                generatedLicense = newRecord
                                refreshHistory()
                                Toast.makeText(context, "تم توليد وحفظ كود التفعيل الموحد بنجاح 💎", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("توليد كود التفعيل الموحد لمرة واحدة 💎", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Generated Result Card (if present)
            generatedLicense?.let { lic ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.3f)),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✓ تم توليد الكود بنجاح للمدرسة:", color = Color(0xFF6EE7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(lic.productCode, color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Text(
                                text = lic.clientName,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )

                            // Glowing Key Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF030712))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lic.licenseKey,
                                    color = Color(0xFFFDE047),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 2.sp
                                )
                            }

                            // Action: WhatsApp Direct
                            Button(
                                onClick = { openWhatsApp(lic) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF052E16), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("مشاركة الكود والرسالة عبر WhatsApp 💬", color = Color(0xFF052E16), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(lic.licenseKey))
                                        Toast.makeText(context, "تم نسخ الكود فقط للحافظة", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("نسخ الكود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val fullMsg = buildShareMessage(lic)
                                        clipboardManager.setText(AnnotatedString(fullMsg))
                                        Toast.makeText(context, "تم نسخ الرسالة الكاملة مع روابط التنزيل", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                                    border = BorderStroke(1.dp, Color(0xFF4B5563)),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("نسخ الرسالة كاملة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // History Header & Search
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 سجل التراخيص الصادرة (${historyList.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp
                    )

                    if (historyList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val csv = storage.exportAsCsv()
                                shareGeneric(csv)
                            }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تصدير CSV", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Box
            if (historyList.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم المدرسة، الكود، أو رقم الهاتف...", color = Color(0xFF4B5563), fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF1F2937),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF111827),
                            unfocusedContainerColor = Color(0xFF111827)
                        )
                    )
                }
            }

            // Filtered List of Issued Licenses
            val filteredList = if (searchQuery.trim().isEmpty()) {
                historyList
            } else {
                historyList.filter {
                    it.clientName.contains(searchQuery, ignoreCase = true) ||
                    it.licenseKey.contains(searchQuery, ignoreCase = true) ||
                    it.clientPhone.contains(searchQuery)
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (historyList.isEmpty()) "لم يتم إصدار أي تراخيص بعد." else "لا توجد نتائج مطابقة للبحث.",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.clientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(item.productCode, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Text(
                                text = item.licenseKey,
                                color = Color(0xFFFBBF24),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.paymentMethod} • ${item.formattedDate}",
                                    color = Color(0xFF6B7280),
                                    fontSize = 10.5.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { openWhatsApp(item) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "واتساب", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(item.licenseKey))
                                            Toast.makeText(context, "تم نسخ الكود", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            storage.deleteLicense(item.id)
                                            refreshHistory()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
