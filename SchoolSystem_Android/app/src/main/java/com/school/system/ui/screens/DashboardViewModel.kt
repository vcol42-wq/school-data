package com.school.system.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.system.data.SyncManager
import com.school.system.data.SyncRepository
import com.school.system.data.dao.ClassPackageDao
import com.school.system.data.dao.ConfigDao
import com.school.system.data.dao.StudentDao
import com.school.system.data.model.ClassPackage
import com.school.system.data.model.SchoolConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val packageDao: ClassPackageDao,
    val syncManager: SyncManager,
    private val configDao: ConfigDao,
    val syncRepository: SyncRepository,
    private val studentDao: StudentDao
) : ViewModel() {

    val packages = packageDao.getAllPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val config = configDao.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshAiStatus()
    }

    private fun refreshAiStatus() {
        viewModelScope.launch {
            val current: SchoolConfig? = configDao.getConfig().first()
            if (current != null && current.userEmail.isNotEmpty()) {
                // Background AI Activation based on email
                try {
                    val aiResponse = syncManager.queryAiAssistant("ping_activation")
                    if (aiResponse.success) {
                        configDao.saveConfig(current.copy(isAiActivated = true))
                    }
                } catch (e: Exception) {
                    // Fail silently
                }
            }
        }
    }

    fun addPackage(grade: String, section: String, subject: String, icon: String) {
        viewModelScope.launch {
            val standardized = syncRepository.standardizeSubjectName(subject)
            val success = syncManager.downloadSimpleRosterForClass(grade, section, standardized)
            if (!success) {
                packageDao.insertPackage(
                    ClassPackage(grade = grade, section = section, subject = standardized, iconName = icon)
                )
            }
            syncManager.propagateStudents()
        }
    }

    fun updatePackage(pkg: ClassPackage, newGrade: String, newSection: String, newSubject: String) {
        viewModelScope.launch {
            val oldSubject = pkg.subject
            val oldGrade = pkg.grade
            val oldSection = pkg.section
            val cleanGrade = newGrade.trim()
            val cleanSection = newSection.trim()
            val cleanSubject = syncRepository.standardizeSubjectName(newSubject.trim())

            val updated = pkg.copy(
                grade = cleanGrade,
                section = cleanSection,
                subject = cleanSubject
            )
            packageDao.updatePackage(updated)

            // Update linked students in Room database so grades/roster remain connected
            studentDao.updateSubjectForClass(
                grade = oldGrade,
                section = oldSection,
                oldSubject = oldSubject,
                newSubject = cleanSubject
            )
            syncManager.propagateStudents()
        }
    }

    fun deletePackage(pkg: ClassPackage) {
        viewModelScope.launch {
            packageDao.deletePackage(pkg)
        }
    }

    fun summonSectionDetailed(grade: String, section: String, subject: String, onComplete: (com.school.system.data.SummonResult) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.summonClassRosterDetailed(grade, section, subject)
            syncManager.propagateStudents()
            onComplete(result)
        }
    }

    suspend fun syncGradesOnly(): Boolean {
        return syncManager.syncGrades("", "", "")
    }

    suspend fun syncAllFromPrincipal(): Boolean {
        return syncManager.fetchDataFromPrincipal()
    }

    suspend fun queryAi(queryText: String) = syncManager.queryAiAssistant(queryText)

    suspend fun pairSchoolByCode(pairingCode: String, teacherName: String) = 
        syncManager.pairSchoolByCode(pairingCode, teacherName)

    suspend fun fetchStudentData(schoolId: String, name: String, grade: String, section: String) =
        syncRepository.fetchStudentDashboardData(schoolId, name, grade, section)
}
