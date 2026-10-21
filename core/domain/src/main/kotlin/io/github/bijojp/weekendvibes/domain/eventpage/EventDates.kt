package io.github.bijojp.weekendvibes.domain.eventpage

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** A day, and the time on it if the page gave one. */
internal data class DayAndTime(val date: LocalDate, val time: LocalTime?)

/** Turns the date texts event pages use into a day and time in the phone's zone. */
internal object EventDates {
    private val DATE_ONLY = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val SPACE_BEFORE_TIME = Regex("""^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2})""")
    private val OFFSET_WITHOUT_COLON = Regex("""([+-]\d{2})(\d{2})$""")

    /** Null when [text] isn't a date this understands. */
    fun parse(text: String, zone: ZoneId): DayAndTime? {
        val trimmed = text.trim()
        if (DATE_ONLY.matches(trimmed)) return runCatching { DayAndTime(LocalDate.parse(trimmed), null) }.getOrNull()
        val iso = trimmed
            .replace(SPACE_BEFORE_TIME, "$1T$2")
            .replace(OFFSET_WITHOUT_COLON, "$1:$2")
        runCatching { OffsetDateTime.parse(iso) }.getOrNull()?.let { withOffset ->
            // Midnight UTC is how several sites write "this day, no time given".
            if (withOffset.offset == ZoneOffset.UTC && withOffset.toLocalTime() == LocalTime.MIDNIGHT) {
                return DayAndTime(withOffset.toLocalDate(), null)
            }
            return withOffset.atZoneSameInstant(zone).toLocalDateTime().toDayAndTime()
        }
        return runCatching { LocalDateTime.parse(iso) }.getOrNull()?.toDayAndTime()
    }

    /** A moment given as seconds since 1970, as a day and time in [zone]. */
    fun fromEpochSeconds(seconds: Long, zone: ZoneId): DayAndTime =
        Instant.ofEpochSecond(seconds).atZone(zone).toLocalDateTime().toDayAndTime()

    /** Midnight means the page gave only a day. */
    private fun LocalDateTime.toDayAndTime() =
        DayAndTime(toLocalDate(), toLocalTime().truncatedTo(ChronoUnit.MINUTES).takeUnless { it == LocalTime.MIDNIGHT })
}
