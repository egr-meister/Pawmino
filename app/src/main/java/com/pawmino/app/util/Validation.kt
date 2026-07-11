package com.pawmino.app.util

import com.pawmino.app.model.ScheduleType
import com.pawmino.app.model.WeekDay

/**
 * Friendly, non-throwing validation. Each function returns null when valid or a short,
 * user-facing message when not. Nothing here is medical or diagnostic.
 */
object Validation {

    const val NOTE_TITLE_MAX = 80
    const val NOTE_BODY_MAX = 1000

    fun petNameError(name: String): String? =
        if (name.isBlank()) "Please enter a pet name." else null

    /** Optional dates: blank is allowed; if present they must parse. */
    fun optionalDateError(value: String): String? =
        if (value.isBlank() || DateTimeUtil.isValidDate(value)) null
        else "Use the date format yyyy-MM-dd."

    fun taskTitleError(title: String): String? =
        if (title.isBlank()) "Please enter a task title." else null

    fun timeError(value: String): String? =
        if (value.isBlank() || DateTimeUtil.isValidTime(value)) null
        else "Use the time format HH:mm (24-hour)."

    /**
     * Validate a schedule configuration per schedule type. Returns null when valid.
     */
    fun scheduleError(
        scheduleType: ScheduleType,
        daysOfWeek: List<WeekDay>,
        specificDate: String,
        repeatIntervalDays: Int?
    ): String? = when (scheduleType) {
        ScheduleType.Daily -> null
        ScheduleType.Unscheduled -> null
        ScheduleType.SelectedDays ->
            if (daysOfWeek.isEmpty()) "Select at least one weekday." else null
        ScheduleType.OneTime ->
            if (!DateTimeUtil.isValidDate(specificDate)) "Choose a valid date for the one-time task." else null
        ScheduleType.EveryNumberOfDays ->
            if (repeatIntervalDays == null || repeatIntervalDays < 1) "Repeat interval must be at least 1 day." else null
    }

    fun noteTitleError(title: String): String? =
        if (title.length > NOTE_TITLE_MAX) "Title is limited to $NOTE_TITLE_MAX characters." else null

    fun noteBodyError(text: String): String? =
        if (text.length > NOTE_BODY_MAX) "Note is limited to $NOTE_BODY_MAX characters." else null

    fun shoppingTitleError(title: String): String? =
        if (title.isBlank()) "Please enter an item name." else null

    /** Parse a free-text duration into minutes; blank is allowed (null), invalid returns null. */
    fun parseOptionalInt(value: String): Int? =
        value.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    fun parseOptionalDouble(value: String): Double? =
        value.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}
