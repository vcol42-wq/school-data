package com.example.theboss.utils.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var player: MediaPlayer? = null
    var isPlaying: Boolean = false
        private set

    // تشغيل ملف صوتي من مساره المحلي
    fun playAudio(filePath: String, onComplete: () -> Unit) {
        stopAudio() // إيقاف أي صوت سابق قيد التشغيل

        val file = File(filePath)
        if (!file.exists()) return

        player = MediaPlayer().apply {
            try {
                setDataSource(context, file.toUri())
                prepare()
                start()
                this@AudioPlayerManager.isPlaying = true
                setOnCompletionListener {
                    this@AudioPlayerManager.isPlaying = false
                    releasePlayer()
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                releasePlayer()
            }
        }
    }

    // إيقاف مؤقت أو كلي
    fun stopAudio() {
        try {
            if (player?.isPlaying == true) {
                player?.stop()
            }
            releasePlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releasePlayer() {
        player?.reset()
        player?.release()
        player = null
        isPlaying = false
    }
}
