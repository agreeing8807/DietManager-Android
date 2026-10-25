package com.example.diet.ui.water

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.ReminderSetting
import com.example.diet.data.repository.ReminderRepository
import com.example.diet.util.CalendarReminderSync
import com.example.diet.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val reminders: StateFlow<List<ReminderSetting>> = reminderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val calendarSync = CalendarReminderSync(context)

    /** 是否开启「写入系统日历」，由用户自主开关 */
    private val _calendarEnabled = MutableStateFlow(calendarSync.isEnabled())
    val calendarEnabled: StateFlow<Boolean> = _calendarEnabled.asStateFlow()

    /** 日历同步结果提示（null 表示无提示） */
    private val _calendarMessage = MutableStateFlow<String?>(null)
    val calendarMessage: StateFlow<String?> = _calendarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val current = reminderRepository.observeAll().first()
            reminderRepository.seedDefaultsIfEmpty(current)
            reschedule()
            when {
                // 上次开启过，但权限被用户在系统设置里撤销：回到关闭状态并提示
                calendarSync.isEnabled() && !calendarSync.hasPermission() -> onCalendarPermissionDenied()
                calendarSync.isEnabled() -> syncCalendar()
            }
        }
    }

    fun addReminder(timeMinutesOfDay: Int) {
        viewModelScope.launch {
            val id = reminderRepository.addReminder(ReminderSetting(timeMinutesOfDay = timeMinutesOfDay))
            ReminderScheduler.schedule(context, ReminderSetting(id = id, timeMinutesOfDay = timeMinutesOfDay))
            syncCalendarIfEnabled()
        }
    }

    fun toggle(setting: ReminderSetting) {
        viewModelScope.launch {
            val updated = setting.copy(enabled = !setting.enabled)
            reminderRepository.updateReminder(updated)
            if (updated.enabled) ReminderScheduler.schedule(context, updated)
            else ReminderScheduler.cancel(context, setting.id)
            syncCalendarIfEnabled()
        }
    }

    fun delete(setting: ReminderSetting) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(setting)
            ReminderScheduler.cancel(context, setting.id)
            syncCalendarIfEnabled()
        }
    }

    fun reschedule() {
        viewModelScope.launch {
            val enabled = reminderRepository.getEnabled()
            ReminderScheduler.scheduleAll(context, enabled)
        }
    }

    fun toggleAll(enable: Boolean) {
        viewModelScope.launch {
            reminders.value.forEach { setting ->
                if (setting.enabled != enable) {
                    val updated = setting.copy(enabled = enable)
                    reminderRepository.updateReminder(updated)
                    if (enable) ReminderScheduler.schedule(context, updated)
                    else ReminderScheduler.cancel(context, setting.id)
                }
            }
            syncCalendarIfEnabled()
        }
    }

    // ---------------- 系统日历同步（可选） ----------------

    /**
     * 开启或关闭日历同步。关闭时立即删除应用写入的日历事件，
     * 只清理自己写入的那几条，不影响用户的其他日程。
     */
    fun setCalendarSync(enabled: Boolean) {
        // 权限可能在系统设置里被撤销，开启前再确认一次，避免开关与实际状态不一致
        if (enabled && !calendarSync.hasPermission()) {
            onCalendarPermissionDenied()
            return
        }
        calendarSync.setEnabled(enabled)
        _calendarEnabled.value = enabled
        viewModelScope.launch {
            if (enabled) {
                syncCalendar()
            } else {
                val removed = withContext(Dispatchers.IO) { calendarSync.clear() }
                _calendarMessage.value = if (removed > 0) {
                    "已移除日历中的 $removed 条喝水提醒"
                } else {
                    "已关闭日历同步"
                }
            }
        }
    }

    fun clearCalendarMessage() {
        _calendarMessage.value = null
    }

    fun onCalendarPermissionDenied() {
        _calendarEnabled.value = false
        calendarSync.setEnabled(false)
        _calendarMessage.value = "未授予日历权限，已保持关闭"
    }

    /** 提醒时间变化后，若已开启同步则重建日历事件 */
    private suspend fun syncCalendarIfEnabled() {
        if (calendarSync.isEnabled()) syncCalendar()
    }

    private suspend fun syncCalendar() {
        val settings = reminderRepository.observeAll().first()
        val result = withContext(Dispatchers.IO) { calendarSync.sync(settings) }
        _calendarMessage.value = result.fold(
            onSuccess = { count ->
                if (count > 0) "已写入 $count 条日历提醒，系统日历会按时通知" else "没有启用的提醒时间"
            },
            onFailure = { "日历同步失败：${it.message}" }
        )
    }
}
