package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    const val DEFAULT_TIMEZONE_ID = "Asia/Dhaka"

    fun resolveTimeZone(useDeviceTimezone: Boolean, customTimezoneId: String): TimeZone {
        return if (useDeviceTimezone) {
            TimeZone.getDefault()
        } else {
            TimeZone.getTimeZone(customTimezoneId.ifBlank { DEFAULT_TIMEZONE_ID })
        }
    }

    fun calendarAt(epochMillis: Long, timeZone: TimeZone): Calendar {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
        }
    }

    fun epochDayFromMillis(epochMillis: Long, timeZone: TimeZone): Long {
        val cal = calendarAt(epochMillis, timeZone)
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / 86_400_000L
    }

    fun millisFromEpochDay(epochDay: Long, hourOfDay: Int, minute: Int, timeZone: TimeZone): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * 86_400_000L
        }
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                hourOfDay,
                minute,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfDayMillis(epochDay: Long, timeZone: TimeZone): Long =
        millisFromEpochDay(epochDay, 0, 0, timeZone)

    fun endOfDayMillis(epochDay: Long, timeZone: TimeZone): Long =
        startOfDayMillis(epochDay + 1L, timeZone) - 1L

    fun todayEpochDay(timeZone: TimeZone): Long =
        epochDayFromMillis(System.currentTimeMillis(), timeZone)

    fun yearMonthDayFromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * 86_400_000L
        }
        return Triple(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH), // 0-indexed
            utcCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun epochDayFromYmd(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / 86_400_000L
    }

    fun weekDaysAround(anchorEpochDay: Long): List<Long> {
        val (y, m, d) = yearMonthDayFromEpochDay(anchorEpochDay)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y, m, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        return (0..6).map { offset ->
            val day = cal.timeInMillis / 86_400_000L
            cal.add(Calendar.DAY_OF_MONTH, 1)
            day
        }
    }

    /**
     * Generates 42 cells (6 weeks x 7 days) starting on Sunday for a given year & 0-based month.
     */
    fun monthGridDays(year: Int, monthZeroBased: Int): List<Long> {
        val firstDay = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) // Sunday = 1
        firstDay.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.SUNDAY))
        return (0 until 42).map {
            val epochDay = firstDay.timeInMillis / 86_400_000L
            firstDay.add(Calendar.DAY_OF_MONTH, 1)
            epochDay
        }
    }

    private val formatterCache = object : ThreadLocal<HashMap<String, SimpleDateFormat>>() {
        override fun initialValue(): HashMap<String, SimpleDateFormat> = HashMap(16)
    }

    private fun getFormatter(pattern: String, timeZone: TimeZone): SimpleDateFormat {
        val locale = Locale.getDefault()
        val key = "$pattern|${timeZone.id}|${locale.toLanguageTag()}"
        val map = formatterCache.get()!!
        return map.getOrPut(key) {
            SimpleDateFormat(pattern, locale).apply {
                this.timeZone = timeZone
            }
        }
    }

    fun formatTime(epochMillis: Long, use24Hour: Boolean, timeZone: TimeZone): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return getFormatter(pattern, timeZone).format(Date(epochMillis))
    }

    fun formatLiveClock(epochMillis: Long, use24Hour: Boolean, timeZone: TimeZone): String {
        val pattern = if (use24Hour) "HH:mm:ss" else "h:mm:ss a"
        return getFormatter(pattern, timeZone).format(Date(epochMillis))
    }

    fun formatDateShort(epochMillis: Long, timeZone: TimeZone): String {
        return getFormatter("EEE, MMM d", timeZone).format(Date(epochMillis))
    }

    fun formatDateFull(epochMillis: Long, timeZone: TimeZone): String {
        return getFormatter("EEEE, MMMM d, yyyy", timeZone).format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long, use24Hour: Boolean, timeZone: TimeZone): String {
        val pattern = if (use24Hour) "MMM d, yyyy · HH:mm" else "MMM d, yyyy · h:mm a"
        return getFormatter(pattern, timeZone).format(Date(epochMillis))
    }

    fun formatEpochDay(epochDay: Long, pattern: String = "EEE, MMM d, yyyy"): String {
        return getFormatter(pattern, TimeZone.getTimeZone("UTC")).format(Date(epochDay * 86_400_000L))
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, 1, 0, 0, 0)
        }
        return getFormatter("MMMM yyyy", TimeZone.getTimeZone("UTC")).format(Date(cal.timeInMillis))
    }

    fun addDays(epochMillis: Long, days: Int, timeZone: TimeZone): Long {
        return calendarAt(epochMillis, timeZone).apply {
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    fun addMonths(epochMillis: Long, months: Int, timeZone: TimeZone): Long {
        return calendarAt(epochMillis, timeZone).apply {
            add(Calendar.MONTH, months)
        }.timeInMillis
    }

    fun addYears(epochMillis: Long, years: Int, timeZone: TimeZone): Long {
        return calendarAt(epochMillis, timeZone).apply {
            add(Calendar.YEAR, years)
        }.timeInMillis
    }

    fun greetingPeriod(epochMillis: Long, timeZone: TimeZone): Int {
        val hour = calendarAt(epochMillis, timeZone).get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..11 -> 0 // Morning
            hour in 12..16 -> 1 // Afternoon
            else -> 2 // Evening
        }
    }

    val popularTimeZones: List<String> = listOf(
        "Asia/Dhaka",
        "Asia/Kolkata",
        "Asia/Dubai",
        "Asia/Singapore",
        "Asia/Tokyo",
        "Europe/London",
        "Europe/Paris",
        "America/New_York",
        "America/Chicago",
        "America/Los_Angeles",
        "Australia/Sydney",
        "UTC"
    )
}
