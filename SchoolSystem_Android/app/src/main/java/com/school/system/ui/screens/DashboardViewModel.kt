package com.school.system.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.system.data.SyncManager
import com.school.system.data.dao.ClassPackageDao
import com.school.system.data.model.ClassPackage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val packageDao: ClassPackageDao,
    private val syncManager: SyncManager
) : ViewModel() {

    val packages = packageDao.getAllPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPackage(grade: String, section: String, subject: String, icon: String) {
        viewModelScope.launch {
            // Try to download student roster for this class/section
            val success = syncManager.downloadSimpleRosterForClass(grade, section, subject)
            if (!success) {
                // If the download fails (e.g. offline), fallback to creating the empty class package locally
                packageDao.insertPackage(
                    ClassPackage(grade = grade, section = section, subject = subject, iconName = icon)
                )
            }
            syncManager.propagateStudents()
        }
    }

    fun deletePackage(pkg: ClassPackage) {
        viewModelScope.launch {
            packageDao.deletePackage(pkg)
        }
    }

    suspend fun syncAllFromPrincipal(): Boolean {
        return syncManager.fetchDataFromPrincipal()
    }

    suspend fun queryAi(queryText: String) = syncManager.queryAiAssistant(queryText)
}
