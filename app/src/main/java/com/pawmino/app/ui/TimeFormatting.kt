package com.pawmino.app.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pawmino.app.model.TimeFormatPreference

/** Resolve whether 24-hour formatting applies, honoring the user preference + system default. */
fun prefer24Hour(pref: TimeFormatPreference, systemIs24: Boolean): Boolean = when (pref) {
    TimeFormatPreference.TwelveHour -> false
    TimeFormatPreference.TwentyFourHour -> true
    TimeFormatPreference.SystemDefault -> systemIs24
}

@Composable
fun rememberPrefer24(pref: TimeFormatPreference): Boolean {
    val systemIs24 = DateFormat.is24HourFormat(LocalContext.current)
    return prefer24Hour(pref, systemIs24)
}
