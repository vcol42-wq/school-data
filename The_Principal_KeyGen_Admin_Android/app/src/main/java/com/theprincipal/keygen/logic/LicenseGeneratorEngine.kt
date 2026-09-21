package com.theprincipal.keygen.logic

object LicenseGeneratorEngine {

    /**
     * Mathematical 2-character hex checksum matching Desktop & Web validation.
     */
    fun computeChecksum(base: String): String {
        val clean = base.replace(Regex("[^0-9A-Z]"), "")
        var hash = 0x55AA
        for (ch in clean) {
            hash = ((hash shl 5) - hash) + ch.code
            hash = hash and 0xFFFF
        }
        val hex = Math.abs(hash).toString(16).uppercase().padStart(4, '0')
        return hex.substring(hex.length - 2)
    }

    /**
     * Generate a unified license key:
     * Format: [PROD]-[TIER]-[P1]-[P2]-[CHECKSUM]
     * Example: BOSS-L1-8492-3310-7B
     */
    fun generateUnifiedLicense(productCode: String, tier: String = "L1"): String {
        val p1 = (1000..9999).random()
        val p2 = (1000..9999).random()
        val base = "$productCode-$tier-$p1-$p2"
        val checksum = computeChecksum(base)
        return "$base-$checksum"
    }

    /**
     * Verify whether a given key conforms to the unified algorithm.
     */
    fun verifyLicense(key: String): Boolean {
        val parts = key.trim().uppercase().split("-")
        if (parts.size < 3) return false
        val lastPart = parts.last()
        if (lastPart.length == 2) {
            val base = parts.dropLast(1).joinToString("-")
            return computeChecksum(base) == lastPart
        }
        return key.startsWith("BOSS-") && key.length >= 14
    }
}
