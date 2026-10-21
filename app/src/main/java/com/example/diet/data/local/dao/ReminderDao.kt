package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.diet.data.local.entity.ReminderSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_setting ORDER BY timeMinutesOfDay ASC")
    fun observeAll(): Flow<List<ReminderSetting>>

    @Insert
    suspend fun insert(setting: ReminderSetting): Long

    @Update
    suspend fun update(setting: ReminderSetting)

    @Delete
    suspend fun delete(setting: ReminderSetting)

    @Query("SELECT * FROM reminder_setting WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderSetting>

    @Query("DELETE FROM reminder_setting")
    suspend fun clear()

    @Query("SELECT * FROM reminder_setting")
    suspend fun getAll(): List<ReminderSetting>
}
