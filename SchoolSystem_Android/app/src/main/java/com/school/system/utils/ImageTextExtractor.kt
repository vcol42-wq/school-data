package com.school.system.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ImageTextExtractor {

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText.text)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) {
                continuation.resume("")
            }
        }
    }

    fun parseStudentNamesFromRawText(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        val lines = rawText.split("\n", "\r")
        val namesList = mutableListOf<String>()

        for (line in lines) {
            var cleanLine = line.trim()
            if (cleanLine.isBlank()) continue

            // Remove leading row numbers like "1-", "1.", "1)", "١-", "١.", "١)"
            cleanLine = cleanLine.replace(Regex("^[0-9١-٩]+\\s*[-.)]\\s*"), "")
            
            // Filter out table headers or non-name metadata
            if (cleanLine.contains("الاسم") || cleanLine.contains("اسم الطالب") || cleanLine.contains("الملاحظات") || cleanLine.contains("التسلسل") || cleanLine.contains("المدرسة")) {
                continue
            }

            cleanLine = cleanLine.trim()
            if (cleanLine.length >= 3) {
                namesList.add(cleanLine)
            }
        }

        return namesList
    }
}
