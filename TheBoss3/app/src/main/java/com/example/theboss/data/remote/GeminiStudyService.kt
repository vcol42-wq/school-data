package com.example.theboss.data.remote

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiStudyService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
        return prefs.getString("gemini_api_key", null) ?: "AIzaSyA92sLsbAsl9HXjEQb2fsADFeOTsvi6aGQ"
    }

    private fun getModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = getApiKey()
        )
    }

    suspend fun explainTopic(subject: String, topic: String): String = withContext(Dispatchers.IO) {
        val prompt = "بسط واشرح الموضوع التالي في مادة $subject: $topic. اجعل الشرح ممتعاً وموجزاً وواضحاً لطالب في المدرسة العراقية، مع أمثلة ونقاط رئيسية للتفوق."
        try {
            val response = getModel().generateContent(prompt)
            response.text?.takeIf { it.isNotBlank() } ?: generateLocalExplanation(subject, topic)
        } catch (e: Exception) {
            // Regional block or network fallback
            generateLocalExplanation(subject, topic)
        }
    }

    suspend fun generateQuiz(subject: String, topic: String): String = withContext(Dispatchers.IO) {
        val prompt = "قم بتوليد 3 أسئلة اختيار من متعدد مع الإجابات الصحيحة والتفسير لموضوع $topic في مادة $subject. اجعل التنسيق بسيطاً وواضحاً."
        try {
            val response = getModel().generateContent(prompt)
            response.text?.takeIf { it.isNotBlank() } ?: generateLocalQuiz(subject, topic)
        } catch (e: Exception) {
            generateLocalQuiz(subject, topic)
        }
    }

    private fun generateLocalExplanation(subject: String, topic: String): String {
        return """
            📘 **شرح الدرس وموجز المذاكرة الذكي:**
            • **المادة:** $subject
            • **الموضوع:** $topic
            
            🌟 **أهم المفاهيم الأساسية:**
            1. فهم التعريف والقواعد المحورية للدرس وتدوين الملاحظات بخط اليد.
            2. التركيز على حل التمارين والمسائل النموذجية الواردة في نهاية الفصل.
            3. الربط بين الأمثلة النظرية والتطبيقات العملية لترسيخ المعلومة في الذاكرة.
            
            💡 **نصيحة المساعد الدراسي:** قم بتخصيص 25 دقيقة تركيز (تقنية بومودورو) لحل 3 تمارين تطبيقية حول هذا الموضوع لضمان الدرجة الكاملة!
        """.trimIndent()
    }

    private fun generateLocalQuiz(subject: String, topic: String): String {
        return """
            📝 **اختبار سريع في مادة $subject ($topic):**
            
            **السؤال الأول:** ما هي النقطة المركزية في درس $topic؟
            [أ] فهم القواعد والتطبيق العملي ✅
            [ب] الحفظ المجرد دون تركيز
            [ج] تأجيل المراجعة للامتحان النهائي
            
            **السؤال الثاني:** كيف تضمن التفوق في هذا الموضوع؟
            [أ] حل الواجبات اليومية ومراجعة ملخص الأستاذ ✅
            [ب] إهمال التمارين العامة
            
            🎯 **النتيجة:** أحسنت! الاستمرار بالممارسة والمراجعة اليومية هو سر التفوق المدرسي.
        """.trimIndent()
    }
}
