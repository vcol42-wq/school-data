package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.ClassPackage
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassPackageDao {
    @Query("SELECT * FROM class_packages")
    fun getAllPackages(): Flow<List<ClassPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: ClassPackage)

    @Delete
    suspend fun deletePackage(pkg: ClassPackage)

    @Query("SELECT * FROM class_packages")
    suspend fun getAllPackagesList(): List<ClassPackage>

    @Query("DELETE FROM class_packages")
    suspend fun clearAll()
}
