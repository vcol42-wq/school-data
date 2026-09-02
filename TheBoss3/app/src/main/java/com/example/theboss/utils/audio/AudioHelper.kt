package com.example.theboss.utils.audio

import android.media.MediaMetadataRetriever

object AudioHelper {
    fun getDurationInSeconds(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLong() ?: 0L
            (timeInMillis / 1000).toInt()
        } catch (e: Exception) {
            0
        } finally {
            retriever.release()
        }
    }
}
