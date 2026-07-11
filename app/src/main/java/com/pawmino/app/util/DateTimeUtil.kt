package com.pawmino.app.util

import com.pawmino.app.model.WeekDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Central date/time helpers. All storage uses fixed patterns and the local device clock.
 * Parsing never throws to the caller — invalid input returns null / a safe fallback.
 */
object DateTimeUtil {

    val STORAGE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val STORAGE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun todayIso(): String = LocalDate.now().format(STORAGE_DATE)

    /** ISO-8601 timestamp for createdAt / updatedAt / completedAt fields. */
    fun nowTimestamp(): String = Instant.now().toString()

    fun nowTimeIso(): String = LocalTime.now().withSecond(0).withNano(0).format(STORAGE_TIME)

    fun parseDateOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value, STORAGE_DATE)
        } catch (e: Exception) {
            null
        }
    }

    fun parseTimeOrNull(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalTime.parse(value, STORAGE_TIME)
        } catch (e: Exception) {
            null
        }
    }

    fun isValidDate(value: String?): Boolean = parseDateOrNull(value) != null

    fun isValidTime(value: String?): Boolean = parseTimeOrNull(value) != null

    fun weekDayOf(date: LocalDate): WeekDay = WeekDay.fromIso(date.dayOfWeek.value)

    fun weekDayOf(dateIso: String): WeekDay? = parseDateOrNull(dateIso)?.let { weekDayOf(it) }

    fun formatDate(dateIso: String): LocalDate? = parseDateOrNull(dateIso)

    /** Friendly medium date, e.g. "Mon, Jul 10 2026". Falls back to raw value if unparsable. */
    fun displayDate(dateIso: String): String {
        val d = parseDateOrNull(dateIso) ?: return dateIso.ifBlank { "—" }
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val month = d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$dow, $month ${d.dayOfMonth} ${d.year}"
    }

    fun displayDateShort(dateIso: String): String {
        val d = parseDateOrNull(dateIso) ?: return dateIso.ifBlank { "—" }
        val month = d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        return "$month ${d.dayOfMonth}"
    }

    /**
     * Display a stored "HH:mm" time honoring the user preference.
     * @param prefer24 resolved boolean (system default is passed in by the caller).
     */
    fun displayTime(timeIso: String, prefer24: Boolean): String {
        val t = parseTimeOrNull(timeIso) ?: return timeIso
        val pattern = if (prefer24) "HH:mm" else "h:mm a"
        return t.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun monthTitle(yearMonth: YearMonth): String {
        val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$month ${yearMonth.year}"
    }

    fun daysBetween(start: LocalDate, end: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(start, end)

    fun isToday(dateIso: String): Boolean = dateIso == todayIso()
}
