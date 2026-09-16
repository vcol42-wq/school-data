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

            // Handle Excel / CSV / Word Tab or Comma separated rows
            val cells = cleanLine.split("\t", ",", ";", "|")
            if (cells.size > 1) {
                val nameCell = cells.map { it.trim() }.firstOrNull { cell ->
                    val cleanCell = cell.replace(Regex("^[0-9١-٩]+\\s*[-.)]\\s*"), "").trim()
                    cleanCell.isNotBlank() && 
                    !cleanCell.all { ch -> ch.isDigit() || ch == '.' || ch == '-' || ch == '/' } &&
                    !cleanCell.contains("الاسم") && !cleanCell.contains("التسلسل") && 
                    !cleanCell.contains("الملاحظات") && !cleanCell.contains("الصف") &&
                    cleanCell.split("\\s+".toRegex()).size >= 2
                }
                if (nameCell != null) {
                    cleanLine = nameCell
                }
            }

            // Remove leading row numbers like "1-", "1.", "1)", "١-", "١.", "١)"
            cleanLine = cleanLine.replace(Regex("^[0-9١-٩]+\\s*[-.)]\\s*"), "").trim()

            // Filter out table headers or non-name metadata
            if (cleanLine.contains("الاسم") || cleanLine.contains("اسم الطالب") || 
                cleanLine.contains("الملاحظات") || cleanLine.contains("التسلسل") || 
                cleanLine.contains("المدرسة") || cleanLine.contains("الصف") || cleanLine.contains("الشعبة")) {
                continue
            }

            cleanLine = cleanLine.replace("\\s+".toRegex(), " ")

            if (cleanLine.length >= 3 && cleanLine.any { it.isLetter() }) {
                namesList.add(cleanLine)
            }
        }

        return namesList.distinct()
    }
}
