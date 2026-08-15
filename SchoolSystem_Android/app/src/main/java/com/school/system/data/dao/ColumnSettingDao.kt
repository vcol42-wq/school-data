package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.ColumnSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnSettingDao {
    @Query("SELECT * FROM column_settings WHERE grade = :grade AND section = :section AND subject = :subject")
    fun getSettings(grade: String, section: String, subject: String): Flow<List<ColumnSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: ColumnSetting)

    @Delete
    suspend fun deleteSetting(setting: ColumnSetting)
    
    @Update
    suspend fun updateSetting(setting: ColumnSetting)
}
