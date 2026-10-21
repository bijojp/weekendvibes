package io.github.bijojp.weekendvibes.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.bijojp.weekendvibes.R
import io.github.bijojp.weekendvibes.domain.Break
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// Dates and times as people read them, following the phone's language and 12/24-hour setting.

fun formatTime(context: Context, time: LocalTime): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Composable
fun timeText(time: LocalTime): String = formatTime(LocalContext.current, time)

/** "SAT" */
fun weekdayCaps(date: LocalDate): String = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault())

/** "Sat 24 Oct", or with the year when it isn't this year. */
fun mediumDay(date: LocalDate, today: LocalDate): String {
    val skeleton = if (date.year == today.year) "EEEdMMM" else "EEEdMMMyyyy"
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)))
}

/** "Saturday 24 October" */
fun longDay(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMMyyyy")))

/**
 * "Oct 24 – 25", "Oct 31 – Nov 1" (or "24 – 25 Oct" where the day comes first), or "Wed 28 Oct" for one day.
 * Breaks outside this year spell out both dates with the year.
 */
fun breakRange(span: Break, today: LocalDate): String {
    if (span.start == span.end) return mediumDay(span.start, today)
    if (span.start.year != today.year || span.end.year != today.year) {
        val full = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMMMd"))
        return "${span.start.format(full)} – ${span.end.format(full)}"
    }
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd")
    val dayMonth = DateTimeFormatter.ofPattern(pattern)
    if (span.start.month != span.end.month) return "${span.start.format(dayMonth)} – ${span.end.format(dayMonth)}"
    val day = DateTimeFormatter.ofPattern("d")
    val dayFirst = pattern.indexOf('d') < pattern.indexOf('M')
    return if (dayFirst) {
        "${span.start.format(day)} – ${span.end.format(dayMonth)}"
    } else {
        "${span.start.format(dayMonth)} – ${span.end.format(day)}"
    }
}

/** "Now", "Tomorrow", "In 5 days", "In 3 weeks", "Last weekend", "2 weeks ago", "3 months ago". */
@Composable
fun relativeText(span: Break, today: LocalDate): String {
    if (today in span) return stringResource(R.string.rel_now)
    if (span.start > today) {
        val days = ChronoUnit.DAYS.between(today, span.start).toInt()
        return when {
            days == 1 -> stringResource(R.string.rel_tomorrow)
            days < 14 -> pluralStringResource(R.plurals.rel_in_days, days, days)
            else -> pluralStringResource(R.plurals.rel_in_weeks, days / 7, days / 7)
        }
    }
    val days = ChronoUnit.DAYS.between(span.end, today).toInt()
    return when {
        days < 7 && span.isWeekend -> stringResource(R.string.rel_last_weekend)
        days < 7 -> pluralStringResource(R.plurals.rel_days_ago, days, days)
        days < 60 -> pluralStringResource(R.plurals.rel_weeks_ago, days / 7, days / 7)
        else -> pluralStringResource(R.plurals.rel_months_ago, days / 30, days / 30)
    }
}

/** "November", or "November 2027" outside this year. */
fun monthName(date: LocalDate, today: LocalDate): String {
    val skeleton = if (date.year == today.year) "MMMM" else "MMMMyyyy"
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)))
}
