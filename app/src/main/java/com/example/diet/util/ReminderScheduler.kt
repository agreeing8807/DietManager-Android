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

    private const val PREFS = "reminder_prefs"
    private const val KEY_EXACT = "use_exact_alarm"

    /** 用户是否选择使用“精确闹钟”（更准时，但 Android 12+ 需授权） */
    fun isExactAlarmPreferred(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_EXACT, false)

    fun setExactAlarmPreferred(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_EXACT, enabled).apply()
    }

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
        val triggerAt = nextTrigger(setting.timeMinutesOfDay)
        val pi = pendingIntent(context, setting.id)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        if (isExactAlarmPreferred(context) && exactAllowed) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // 用户关闭精确闹钟，或未授予权限时，使用非精确闹钟：无需权限，Doze 下仍可触发（误差几分钟）
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
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
