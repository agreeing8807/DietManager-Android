package com.example.diet.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.diet.data.local.entity.ReminderSetting
import java.util.Calendar
import java.util.TimeZone

/**
 * 可选功能：把喝水提醒写入系统自带日历。
 *
 * 每个启用的提醒时间对应一条「每日重复 + 带闹钟」的日历事件，
 * 由系统日历负责到点提醒，应用被清理后依然有效。
 * 所有写入的事件 id 都会记录在本地，关闭或重新同步时只删除自己写的那几条，
 * 不会影响用户的其他日程。
 */
class CalendarReminderSync(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 按当前提醒设置重建日历事件；未开启或无权限时直接返回。
     * 返回写入的事件条数。
     */
    fun sync(settings: List<ReminderSetting>): Result<Int> {
        if (!isEnabled()) return Result.success(0)
        if (!hasPermission()) return Result.failure(SecurityException("未授予日历权限"))
        val calendarId = findWritableCalendarId()
            ?: return Result.failure(IllegalStateException("没有可写入的系统日历，请先在日历应用里添加账号"))

        clearEvents()
        val enabled = settings.filter { it.enabled }
        if (enabled.isEmpty()) return Result.success(0)

        // 每条提醒分配一句标语，循环使用用户自定义的列表；删空时统一用兜底文案
        val texts = WaterReminderTexts.current(context)
            .ifEmpty { listOf(WaterReminderTexts.FALLBACK_TEXT) }
        val inserted = enabled.mapIndexedNotNull { index, setting ->
            insertDailyEvent(calendarId, setting, texts[index % texts.size])
        }
        prefs.edit().putString(KEY_EVENT_IDS, inserted.joinToString(",")).apply()
        if (inserted.isEmpty()) {
            return Result.failure(IllegalStateException("日历拒绝写入，请检查日历账号权限"))
        }
        return Result.success(inserted.size)
    }

    /** 移除应用写入的全部日历事件 */
    fun clear(): Int {
        if (!hasPermission()) return 0
        val count = savedEventIds().size
        clearEvents()
        return count
    }

    private fun savedEventIds(): List<Long> =
        prefs.getString(KEY_EVENT_IDS, "").orEmpty()
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }

    private fun clearEvents() {
        val resolver = context.contentResolver
        savedEventIds().forEach { id ->
            runCatching {
                resolver.delete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                    null,
                    null
                )
            }
        }
        prefs.edit().putString(KEY_EVENT_IDS, "").apply()
    }

    /** 优先选择主日历；只接受「可修改」及以上权限的日历 */
    @SuppressLint("Range")
    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val order = "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection, selection, args, order
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        }.getOrNull()
    }

    private fun insertDailyEvent(calendarId: Long, setting: ReminderSetting, title: String): Long? {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, setting.timeMinutesOfDay / 60)
            set(Calendar.MINUTE, setting.timeMinutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MINUTE, 10) }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title.ifBlank { "喝水提醒" })
            put(CalendarContract.Events.DESCRIPTION, DESCRIPTION)
            put(CalendarContract.Events.DTSTART, start.timeInMillis)
            put(CalendarContract.Events.DTEND, end.timeInMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            // 每天重复：一条事件覆盖所有天，不必逐日插入
            put(CalendarContract.Events.RRULE, "FREQ=DAILY")
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return runCatching {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: return null
            val eventId = ContentUris.parseId(uri)
            // 事件开始时刻触发提醒
            val reminder = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 0)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
            eventId
        }.getOrNull()
    }

    companion object {
        private const val PREFS = "calendar_reminder_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_EVENT_IDS = "event_ids"
        private const val DESCRIPTION = "由「饮食管理」写入，可在应用内的喝水提醒页关闭"
    }
}
