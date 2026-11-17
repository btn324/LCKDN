package com.lckdn.app

import java.time.LocalDate
import java.time.Month

/**
 * Hungarian school-day calendar, used by the "allow Shorts/Reels during school hours" exception:
 * weekends, public holidays and school breaks are NOT school days, so the exception does not apply
 * then and Shorts/Reels stay blocked as usual.
 *
 * Public holidays (including the Easter-based ones) are computed exactly. School-break windows are
 * recurring approximations of the Hungarian academic calendar — close, but a given year's official
 * dates can shift by a few days.
 */
object Holidays {

    fun isSchoolDay(date: LocalDate): Boolean {
        val dow = date.dayOfWeek.value // 1 = Mon .. 7 = Sun
        if (dow >= 6) return false                 // weekend
        if (isPublicHoliday(date)) return false
        if (isSchoolBreak(date)) return false
        return true
    }

    fun isPublicHoliday(date: LocalDate): Boolean {
        val m = date.month
        val d = date.dayOfMonth
        // Fixed Hungarian public holidays.
        val fixed = when (m) {
            Month.JANUARY -> d == 1
            Month.MARCH -> d == 15
            Month.MAY -> d == 1
            Month.AUGUST -> d == 20
            Month.OCTOBER -> d == 23
            Month.NOVEMBER -> d == 1
            Month.DECEMBER -> d == 25 || d == 26
            else -> false
        }
        if (fixed) return true
        // Movable, Easter-based holidays.
        val easter = easter(date.year)
        return date == easter.minusDays(2) ||   // Good Friday (Nagypéntek)
            date == easter.plusDays(1) ||        // Easter Monday (Húsvéthétfő)
            date == easter.plusDays(50)          // Pentecost Monday (Pünkösdhétfő)
    }

    /** Approximate recurring windows of the Hungarian school year's breaks. */
    private fun isSchoolBreak(date: LocalDate): Boolean {
        val m = date.month
        val d = date.dayOfMonth

        // Summer break: mid-June to the end of August (school starts around September 1).
        if (m == Month.JULY || m == Month.AUGUST) return true
        if (m == Month.JUNE && d >= 16) return true

        // Winter break: late December into the first days of January.
        if (m == Month.DECEMBER && d >= 21) return true
        if (m == Month.JANUARY && d <= 3) return true

        // Autumn break: the week around October 23.
        if (m == Month.OCTOBER && d in 25..31) return true
        if (m == Month.NOVEMBER && d == 2) return true

        // Spring break: the days around Easter.
        val easter = easter(date.year)
        val springStart = easter.minusDays(4)
        val springEnd = easter.plusDays(1)
        if (!date.isBefore(springStart) && !date.isAfter(springEnd)) return true

        return false
    }

    /** Computus (Anonymous Gregorian algorithm): the date of Easter Sunday for [year]. */
    private fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val dd = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - dd - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val mm = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * mm + 114) / 31
        val day = ((h + l - 7 * mm + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
