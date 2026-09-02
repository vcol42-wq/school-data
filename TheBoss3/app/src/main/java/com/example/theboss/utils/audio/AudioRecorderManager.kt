package com.example.theboss.utils.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    // بدء التسجيل الصوتي
    fun startRecording(fileNamePrefix: String = "voice_note"): String? {
        val outputDir = File(context.filesDir, "audio_notes").apply {
            if (!exists()) mkdirs()
        }

        val file = File(outputDir, "${fileNamePrefix}_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return file.absolutePath
    }

    // إيقاف التسجيل واسترجاع المسار النهائي للملف
    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorder = null
            currentOutputFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // إلغاء وحذف التسجيل الحالي إذا تم التراجع
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
