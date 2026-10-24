package com.example.diet.data.repository

import android.content.Context
import com.example.diet.data.local.dao.ReminderDao
import com.example.diet.data.local.entity.ReminderSetting
import com.example.diet.util.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao,
    @ApplicationContext private val context: Context
) {
    fun observeAll(): Flow<List<ReminderSetting>> = dao.observeAll()

    suspend fun getEnabled(): List<ReminderSetting> = dao.getEnabled()

    suspend fun addReminder(setting: ReminderSetting): Long = dao.insert(setting)

    suspend fun updateReminder(setting: ReminderSetting) = dao.update(setting)

    suspend fun deleteReminder(setting: ReminderSetting) = dao.delete(setting)

    /** 首次启动时写入一组默认提醒（早中晚各一次） */
    suspend fun seedDefaultsIfEmpty(existing: List<ReminderSetting>) {
        if (existing.isEmpty()) {
            listOf(480, 720, 960, 1200, 1320).forEach {
                dao.insert(ReminderSetting(timeMinutesOfDay = it, label = "喝水提醒"))
            }
        }
    }

    /** 应用启动时调用：确保默认提醒已写入并排程所有启用项（避免“从未打开提醒页就不提醒”） */
    suspend fun ensureScheduled() {
        val current = observeAll().first()
        seedDefaultsIfEmpty(current)
        ReminderScheduler.scheduleAll(context, getEnabled())
    }
}
