package com.example.diet.data.repository

import com.example.diet.data.local.dao.ReminderDao
import com.example.diet.data.local.entity.ReminderSetting
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao
) {
    fun observeAll(): Flow<List<ReminderSetting>> = dao.observeAll()

    suspend fun getEnabled(): List<ReminderSetting> = dao.getEnabled()

    suspend fun addReminder(setting: ReminderSetting): Long = dao.insert(setting)

    suspend fun updateReminder(setting: ReminderSetting) = dao.update(setting)

    suspend fun deleteReminder(setting: ReminderSetting) = dao.delete(setting)

    /** 首次启动时写入一组默认提醒（早中晚各一次） */
    suspend fun seedDefaultsIfEmpty(existing: List<ReminderSetting>) {
        if (existing.isEmpty()) {
            listOf(480, 720, 960, 1200, 1500).forEach {
                dao.insert(ReminderSetting(timeMinutesOfDay = it, label = "喝水提醒"))
            }
        }
    }
}
