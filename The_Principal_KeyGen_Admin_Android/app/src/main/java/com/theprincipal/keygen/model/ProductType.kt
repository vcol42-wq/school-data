package com.theprincipal.keygen.model

enum class ProductType(
    val code: String,
    val titleAr: String,
    val defaultTier: String,
    val downloadUrl: String
) {
    THE_PRINCIPAL_DESKTOP(
        code = "BOSS",
        titleAr = "ترخيص برنامج الحاسوب The Principal v6.3 (سوق مايكروسوفت)",
        defaultTier = "L1",
        downloadUrl = "https://apps.microsoft.com/detail/9P0SWQHDT4H5"
    ),
    PRINCIPAL_COMPOSITE_CODE(
        code = "ADM",
        titleAr = "كود بوابة المدير والإشراف المركب (ADM-XXXX-XX)",
        defaultTier = "SUP",
        downloadUrl = "https://apps.microsoft.com/detail/9P0SWQHDT4H5"
    ),
    TEACHER_PAIRING_CODE(
        code = "TCH",
        titleAr = "كود ربط بوابة الأستاذ والمعلم (TCH-XXXX)",
        defaultTier = "EDU",
        downloadUrl = "https://apps.microsoft.com/detail/9P0SWQHDT4H5"
    ),
    STUDENT_PAIRING_CODE(
        code = "STU",
        titleAr = "كود ربط بوابة الطالب وولي الأمر (4 أرقام)",
        defaultTier = "STD",
        downloadUrl = "https://apps.microsoft.com/detail/9P0SWQHDT4H5"
    ),
    SMART_ACCOUNTS(
        code = "ACCT",
        titleAr = "منظومة الحسابات والمالية المدرسية",
        defaultTier = "L1",
        downloadUrl = "https://apps.microsoft.com/detail/9P0SWQHDT4H5"
    ),
    SMART_ATTENDANCE(
        code = "ATND",
        titleAr = "منظومة البصمة والدوام الذكي",
        defaultTier = "L1",
        downloadUrl = "https://apps.microsoft.com/detail/9P0SWQHDT4H5"
    )
}
