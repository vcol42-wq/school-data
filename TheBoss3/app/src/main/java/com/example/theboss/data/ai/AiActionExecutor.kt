package com.example.theboss.data.ai

import com.example.theboss.data.local.WorkspaceDao
import com.example.theboss.data.local.NoteEntity
import com.example.theboss.data.local.TaskEntity
import com.example.theboss.data.models.ai.AiAssistantResponse
import com.example.theboss.utils.alarm.StudyAlarmScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AiActionExecutor @Inject constructor(
    private val workspaceDao: WorkspaceDao,
    private val alarmScheduler: StudyAlarmScheduler
) {

    suspend fun executeActions(aiResponse: AiAssistantResponse) {
        aiResponse.actions.forEach { action ->
            when (action.type) {
                "CREATE_TASK" -> action.taskData?.let { data ->
                    val dueTimestamp = System.currentTimeMillis() + ((data.dueInHours ?: 24) * 3600 * 1000L)
                    val task = TaskEntity(
                        title = data.title,
                        dueDate = dueTimestamp,
                        priority = data.priority,
                        subjectTag = data.subject,
                        isCompleted = false
                    )
                    workspaceDao.insertTask(task)
                }

                "SET_ALARM" -> action.alarmData?.let { data ->
                    val alarmId = Random.nextInt(1000, 99999)
                    val triggerTime = System.currentTimeMillis() + (data.delayMinutes * 60 * 1000L)
                    alarmScheduler.scheduleExactAlarm(
                        alarmId = alarmId,
                        triggerAtMillis = triggerTime,
                        title = data.title,
                        message = data.message
                    )
                }

                "CREATE_NOTE" -> action.noteData?.let { data ->
                    val note = NoteEntity(
                        title = data.title,
                        content = data.content,
                        audioPath = null,
                        audioDurationSeconds = 0
                    )
                    workspaceDao.insertNote(note)
                }
            }
        }
    }
}
