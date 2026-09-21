package com.theprincipal.keygen.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.theprincipal.keygen.model.IssuedLicense

class LicenseStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("the_principal_keygen_vault", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val keyListKey = "saved_issued_licenses_v1"

    fun getAllLicenses(): List<IssuedLicense> {
        val json = prefs.getString(keyListKey, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<IssuedLicense>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLicense(license: IssuedLicense) {
        val current = getAllLicenses().toMutableList()
        current.add(0, license) // Add at beginning
        val json = gson.toJson(current)
        prefs.edit().putString(keyListKey, json).apply()
    }

    fun deleteLicense(id: String) {
        val current = getAllLicenses().filter { it.id != id }
        val json = gson.toJson(current)
        prefs.edit().putString(keyListKey, json).apply()
    }

    fun clearAll() {
        prefs.edit().remove(keyListKey).apply()
    }

    fun exportAsCsv(): String {
        val list = getAllLicenses()
        val sb = StringBuilder()
        sb.append("المدرسة/المستفيد,البرنامج,كود التفعيل,الهاتف,طريقة الدفع,التاريخ,ملاحظات\n")
        for (item in list) {
            sb.append("\"${item.clientName}\",")
            sb.append("\"${item.productName}\",")
            sb.append("\"${item.licenseKey}\",")
            sb.append("\"${item.clientPhone}\",")
            sb.append("\"${item.paymentMethod}\",")
            sb.append("\"${item.formattedDate}\",")
            sb.append("\"${item.notes}\"\n")
        }
        return sb.toString()
    }
}
