package com.example.diet.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** 日期键 yyyy-MM-dd */
    fun today(): String = LocalDate.now().format(dateFormatter)

    fun dateOf(localDate: LocalDate): String = localDate.format(dateFormatter)

    fun now(): LocalDateTime = LocalDateTime.now()

    fun formatTime(ts: Long): String =
        DateTimeFormatter.ofPattern("HH:mm")
            .format(java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())

    fun formatDate(ts: Long): String =
        dateFormatter.format(java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate())

    fun minuteOfDayToString(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
