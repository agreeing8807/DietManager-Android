package com.example.diet.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.diet.receiver.ReminderBroadcastReceiver
import com.example.diet.data.local.entity.ReminderSetting
import java.util.Calendar

object ReminderScheduler {
    const val ACTION = "com.example.diet.REMINDER"
    private const val REQ_BASE = 10000

    private fun pendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION
            putExtra("id", id)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, (REQ_BASE + id).toInt(), intent, flags)
    }

    private fun nextTrigger(minutesOfDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun schedule(context: Context, setting: ReminderSetting) {
        if (!setting.enabled) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) return
        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTrigger(setting.timeMinutesOfDay),
            pendingIntent(context, setting.id)
        )
    }

    fun cancel(context: Context, id: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(pendingIntent(context, id))
    }

    fun scheduleAll(context: Context, settings: List<ReminderSetting>) {
        settings.filter { it.enabled }.forEach { schedule(context, it) }
    }

    fun cancelAll(context: Context, settings: List<ReminderSetting>) {
        settings.forEach { cancel(context, it.id) }
    }
}
