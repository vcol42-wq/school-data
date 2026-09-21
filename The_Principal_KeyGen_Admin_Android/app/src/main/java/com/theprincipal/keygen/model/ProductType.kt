package com.theprincipal.keygen.model

enum class ProductType(
    val code: String,
    val titleAr: String,
    val defaultTier: String,
    val downloadUrl: String
) {
    THE_PRINCIPAL_DESKTOP(
        code = "BOSS",
        titleAr = "The Principal Desktop v6.0 (الحاسوب)",
        defaultTier = "L1",
        downloadUrl = "https://github.com/vcol42-wq/school-data/releases/download/v6.0/The_Principal_Setup_v6.0.exe"
    ),
    SMART_ACCOUNTS(
        code = "ACCT",
        titleAr = "منظومة الحسابات والمالية (مستقبلي)",
        defaultTier = "L1",
        downloadUrl = "https://github.com/vcol42-wq/school-data/releases/download/v6.0/Smart_Accounts_Setup.exe"
    ),
    SMART_ATTENDANCE(
        code = "ATND",
        titleAr = "منظومة البصمة والدوام الذكي (مستقبلي)",
        defaultTier = "L1",
        downloadUrl = "https://github.com/vcol42-wq/school-data/releases/download/v6.0/Smart_Attendance_Setup.exe"
    ),
    CUSTOM_APP(
        code = "GEN",
        titleAr = "تطبيق مخصص عام",
        defaultTier = "L1",
        downloadUrl = "https://whatsapp.com/channel/0029Vb9C7bs0QeaggKCbuI0J"
    )
}
