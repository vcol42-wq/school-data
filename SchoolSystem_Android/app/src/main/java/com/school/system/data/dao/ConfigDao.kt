package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.SchoolConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config WHERE id = 1")
    fun getConfig(): Flow<SchoolConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SchoolConfig)
}
