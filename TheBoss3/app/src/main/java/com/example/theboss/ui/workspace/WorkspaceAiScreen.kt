package com.example.theboss.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceAiScreen(
    viewModel: StudentAiViewModel = hiltViewModel()
) {
    val isAiConfigured by viewModel.isAiConfigured.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()

    var inputPhrase by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // إذا لم يكن مفتاح Gemini مُعَداً — عرض شاشة التنبيه
    if (!isAiConfigured) {
        AiNotConfiguredCard()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // AI Header card with Gradient-like feel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column {
                    Text(
                        "المعلم الدراسي الذكي",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "اسأل عن أي درس، لخص، أو اطلب خطة مذاكرة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        // Input control row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (recordingState == RecordingState.RECORDING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "جاري تسجيل صوتك الآن...",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { viewModel.stopRecordingAndSend() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2E7D32))
                        ) {
                            Text("إرسال", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { viewModel.cancelRecording() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button
                IconButton(
                    onClick = {
                        if (recordingState == RecordingState.RECORDING) {
                            viewModel.stopRecordingAndSend()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (recordingState == RecordingState.RECORDING)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "تسجيل الصوت",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text field
                OutlinedTextField(
                    value = inputPhrase,
                    onValueChange = { inputPhrase = it },
                    placeholder = { Text("كيف أساعدك في المذاكرة اليوم؟") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    maxLines = 3
                )

                // Send Button
                FloatingActionButton(
                    onClick = {
                        if (inputPhrase.isNotBlank()) {
                            viewModel.sendMessage(inputPhrase)
                            inputPhrase = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(54.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال"
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (message.isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp),
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun AiNotConfiguredCard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("AI", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "المساعد الذكي غير مُفعَّل حالياً",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "لتفعيل المساعد الدراسي الذكي، يرجى ربط التطبيق مع مدرستك عبر رمز المدرسة أو QR Code. سيتم تفعيل الذكاء الاصطناعي تلقائياً بعد الربط.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}
