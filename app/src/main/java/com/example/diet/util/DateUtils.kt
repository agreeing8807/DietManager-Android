package com.example.diet.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** 默认每日开始时间：凌晨 4 点 */
    const val DEFAULT_DAY_START_HOUR = 4

    /** 按“每日开始时间”换算出的当前归属日期：4 点前算前一天 */
    fun effectiveDate(dayStartHour: Int = DEFAULT_DAY_START_HOUR): LocalDate {
        val now = LocalDateTime.now()
        return if (now.hour < dayStartHour) now.toLocalDate().minusDays(1) else now.toLocalDate()
    }

    /** 日期键 yyyy-MM-dd */
    fun today(dayStartHour: Int = DEFAULT_DAY_START_HOUR): String = dateOf(effectiveDate(dayStartHour))

    fun dateOf(localDate: LocalDate): String = localDate.format(dateFormatter)

    fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()

    fun now(): LocalDateTime = LocalDateTime.now()

    fun formatTime(ts: Long): String =
        DateTimeFormatter.ofPattern("HH:mm")
            .format(java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())

    fun formatDate(ts: Long): String =
        dateFormatter.format(java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate())

    /** 10/24 周五 */
    fun dayLabel(date: String): String {
        val d = parse(date) ?: return date
        return "${d.monthValue}/${d.dayOfMonth} ${weekdayLabel(d)}"
    }

    /** 2026-10-25 18:30 周日 */
    fun nowLabel(): String {
        val now = LocalDateTime.now()
        return "${dateOf(now.toLocalDate())} ${minuteOfDayToString(now.hour * 60 + now.minute)} " +
            weekdayLabel(now.toLocalDate())
    }

    /** 按小时划分用餐时段，供推荐时选择合适食物 */
    fun mealPeriod(hour: Int = LocalDateTime.now().hour): String = when (hour) {
        in 5..9 -> "早餐"
        in 10..14 -> "午餐"
        in 15..16 -> "下午加餐"
        in 17..20 -> "晚餐"
        else -> "夜宵"
    }

    private fun weekdayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }

    fun minuteOfDayToString(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
