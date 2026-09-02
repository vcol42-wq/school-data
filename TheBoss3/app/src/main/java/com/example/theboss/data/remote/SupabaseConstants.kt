package com.example.theboss.data.remote

/**
 * إعدادات الاتصال بـ Supabase.
 * القيم الفعلية تُقرأ ديناميكياً من SharedPreferences (عبر DynamicUrlInterceptor)
 * ولا تُستخدم هذه الثوابت مباشرة في الإنتاج.
 *
 * يتم تعبئة SharedPreferences عند:
 * 1. مسح QR Code الخاص بالمدرسة
 * 2. إدخال بيانات المدرسة يدوياً في شاشة الربط
 */
object SupabaseConstants {
    // هذه قيم placeholder فقط — القيم الفعلية تُقرأ من SharedPreferences
    const val PLACEHOLDER_BASE_URL = "https://YOUR_PROJECT.supabase.co/"
}
