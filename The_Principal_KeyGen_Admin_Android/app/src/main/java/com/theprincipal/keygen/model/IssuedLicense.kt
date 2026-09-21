package com.theprincipal.keygen.model

data class IssuedLicense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val licenseKey: String,
    val productCode: String,
    val productName: String,
    val clientName: String,
    val clientPhone: String,
    val paymentMethod: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
)
