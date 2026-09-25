package com.noor.islamic.presentation.common

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Presentation helpers for localizing parsed times without leaking API formats. */
object NoorFormatters {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

    fun prayerTime(time: LocalTime): String = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        time.hour,
        time.minute,
    )

    /** Countdown as `HH:MM:SS`. */
    fun countdown(duration: Duration): String {
        val totalSeconds = duration.coerceAtLeast(Duration.ZERO).seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun date(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        dateFormatter.withLocale(locale).format(date)
}