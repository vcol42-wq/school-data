package com.example.theboss.ui.workspace

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.ai.AiActionExecutor
import com.example.theboss.data.models.ai.AiAssistantResponse
import com.example.theboss.data.remote.GeminiAssistantService
import com.example.theboss.utils.audio.AudioRecorderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RecordingState {
    IDLE, RECORDING
}

@HiltViewModel
class StudentAiViewModel @Inject constructor(
    private val aiService: GeminiAssistantService,
    private val actionExecutor: AiActionExecutor,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    // التحقق من أن مفتاح Gemini مُعَد ومتوفر
    private val _isAiConfigured = MutableStateFlow(aiService.isConfigured())
    val isAiConfigured: StateFlow<Boolean> = _isAiConfigured

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("مرحباً بك! أنا معلمك ومساعدك الدراسي الذكي. كيف يمكنني مساعدتك اليوم؟ يمكنك أن تطلب مني تنظيم جدولك الدراسي، أو ضبط منبهات للمذاكرة، أو تدوين ملخصات للمواد.", false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val recorderManager = AudioRecorderManager(context)
    private var currentRecordingPath: String? = null

    // 1. إرسال نص للذكاء الاصطناعي
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // إضافة رسالة الطالب للدرشة
        val userMsg = ChatMessage(text, isUser = true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            aiService.processStudentInput(text)
                .onSuccess { response ->
                    actionExecutor.executeActions(response)
                    _messages.value = _messages.value + ChatMessage(response.replyMessage, isUser = false)
                }
                .onFailure { error ->
                    _messages.value = _messages.value + ChatMessage("عذراً، حدث خطأ أثناء معالجة الطلب: ${error.localizedMessage}", isUser = false)
                }
            _isLoading.value = false
        }
    }

    // 2. إدارة تسجيل الصوت
    fun startRecording() {
        viewModelScope.launch {
            val path = recorderManager.startRecording("ai_voice_prompt")
            if (path != null) {
                currentRecordingPath = path
                _recordingState.value = RecordingState.RECORDING
            }
        }
    }

    fun stopRecordingAndSend() {
        viewModelScope.launch {
            _recordingState.value = RecordingState.IDLE
            val path = recorderManager.stopRecording()
            if (path != null && currentRecordingPath == path) {
                // إضافة رسالة صوتية للمحادثة
                _messages.value = _messages.value + ChatMessage("[رسالة صوتية مسموعة]", isUser = true)

                _isLoading.value = true
                aiService.processStudentAudioInput(path)
                    .onSuccess { response ->
                        actionExecutor.executeActions(response)
                        _messages.value = _messages.value + ChatMessage(response.replyMessage, isUser = false)
                    }
                    .onFailure { error ->
                        _messages.value = _messages.value + ChatMessage("عذراً، لم أتمكن من معالجة الصوت: ${error.localizedMessage}", isUser = false)
                    }
                _isLoading.value = false
            }
        }
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
        _recordingState.value = RecordingState.IDLE
        currentRecordingPath = null
    }
}
