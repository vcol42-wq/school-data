package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.DailyColumnSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyColumnDao {
    @Query("SELECT * FROM daily_column_settings WHERE id = :id")
    fun getSettings(id: String): Flow<DailyColumnSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: DailyColumnSetting)
}
