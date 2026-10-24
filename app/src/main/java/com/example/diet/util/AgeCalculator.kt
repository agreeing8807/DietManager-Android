package com.example.diet.util

import java.util.Calendar

object AgeCalculator {
    /** 由生日（epoch millis）计算周岁 */
    fun ageFromBirthday(birthdayMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
        val b = Calendar.getInstance().apply { timeInMillis = birthdayMillis }
        val n = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var age = n.get(Calendar.YEAR) - b.get(Calendar.YEAR)
        val hadBirthdayThisYear = n.get(Calendar.MONTH) > b.get(Calendar.MONTH) ||
            (n.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                n.get(Calendar.DAY_OF_MONTH) >= b.get(Calendar.DAY_OF_MONTH))
        if (!hadBirthdayThisYear) age--
        return age.coerceAtLeast(0)
    }
}
