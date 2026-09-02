package com.example.theboss.data.models.ai

import com.google.gson.annotations.SerializedName

// الكائن الرئيسي المرجع من الذكاء الاصطناعي
data class AiAssistantResponse(
    @SerializedName("reply_message") val replyMessage: String, // الرد الودي الذي يظهر للطالب
    @SerializedName("actions") val actions: List<AiAction> = emptyList()
)

data class AiAction(
    @SerializedName("type") val type: String, // "CREATE_TASK" أو "SET_ALARM" أو "CREATE_NOTE"
    @SerializedName("task_data") val taskData: TaskActionData? = null,
    @SerializedName("alarm_data") val alarmData: AlarmActionData? = null,
    @SerializedName("note_data") val noteData: NoteActionData? = null
)

data class TaskActionData(
    @SerializedName("title") val title: String,
    @SerializedName("subject") val subject: String?,
    @SerializedName("due_in_hours") val dueInHours: Int? = 24,
    @SerializedName("priority") val priority: Int = 2 // 1: منخفض، 2: متوسط، 3: عالي
)

data class AlarmActionData(
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("delay_minutes") val delayMinutes: Int // بعد كم دقيقة ينطلق المنبه
)

data class NoteActionData(
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String
)
