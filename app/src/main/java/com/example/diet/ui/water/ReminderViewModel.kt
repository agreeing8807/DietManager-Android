package com.example.diet.ui.water

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diet.data.local.entity.ReminderSetting
import com.example.diet.data.repository.ReminderRepository
import com.example.diet.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val reminders: StateFlow<List<ReminderSetting>> = reminderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val current = reminderRepository.observeAll().first()
            reminderRepository.seedDefaultsIfEmpty(current)
            reschedule()
        }
    }

    fun addReminder(timeMinutesOfDay: Int) {
        viewModelScope.launch {
            val id = reminderRepository.addReminder(ReminderSetting(timeMinutesOfDay = timeMinutesOfDay))
            ReminderScheduler.schedule(context, ReminderSetting(id = id, timeMinutesOfDay = timeMinutesOfDay))
        }
    }

    fun toggle(setting: ReminderSetting) {
        viewModelScope.launch {
            val updated = setting.copy(enabled = !setting.enabled)
            reminderRepository.updateReminder(updated)
            if (updated.enabled) ReminderScheduler.schedule(context, updated)
            else ReminderScheduler.cancel(context, setting.id)
        }
    }

    fun delete(setting: ReminderSetting) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(setting)
            ReminderScheduler.cancel(context, setting.id)
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
        }
    }
}
