package io.github.bijojp.weekendvibes.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** When Digests and Reminders are due. Worked out here from the Agenda and Settings; the app only delivers them. */
object Reminders {
    /** The Digest goes out at this time the evening before a Break starts. */
    val DIGEST_TIME: LocalTime = LocalTime.of(18, 0)

    /** How far ahead to look for the next one. Any change looks again. */
    private const val LOOKAHEAD_DAYS = 9L

    /** A Digest for the Break starting on [span]'s first day, listing everything planned in it. */
    data class Digest(val span: Break, val activities: List<Activity>)

    /** Digests whose time fell in ([from], [to]]. */
    fun digestsDue(agenda: Agenda, settings: Settings, from: LocalDateTime, to: LocalDateTime): List<Digest> {
        if (!settings.digestEnabled) return emptyList()
        return breakStarts(agenda, from.toLocalDate().plusDays(1), to.toLocalDate().plusDays(1))
            .filter { start -> start.minusDays(1).atTime(DIGEST_TIME).isIn(from, to) }
            .map { start -> digest(agenda, start) }
    }

    /** Timed Activities with a Reminder whose reminder time fell in ([from], [to]]. */
    fun remindersDue(agenda: Agenda, from: LocalDateTime, to: LocalDateTime): List<Activity> =
        agenda.activities.filter { activity -> activity.remindAt()?.isIn(from, to) == true }

    /** The first moment after [after] when a Digest or Reminder is due, or null if nothing is coming up. */
    fun next(agenda: Agenda, settings: Settings, after: LocalDateTime): LocalDateTime? {
        val horizon = after.plusDays(LOOKAHEAD_DAYS)
        val digests = if (settings.digestEnabled) {
            breakStarts(agenda, after.toLocalDate(), horizon.toLocalDate()).map { it.minusDays(1).atTime(DIGEST_TIME) }
        } else {
            emptyList()
        }
        val reminders = agenda.activities.mapNotNull { it.remindAt() }
        return (digests + reminders).filter { it > after && it <= horizon }.minOrNull()
    }

    private fun digest(agenda: Agenda, start: LocalDate): Digest {
        val span = requireNotNull(agenda.breakContaining(start))
        return Digest(span, span.days.flatMap(agenda::activitiesOn))
    }

    /** Days from [from] to [to] on which a Break starts. */
    private fun breakStarts(agenda: Agenda, from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { it <= to }
            .filter { agenda.isFree(it) && !agenda.isFree(it.minusDays(1)) }
            .toList()

    private fun Activity.remindAt(): LocalDateTime? {
        val lead = reminder ?: return null
        val at = time ?: return null
        return date.atTime(at).minusMinutes(lead.minutes)
    }

    private fun LocalDateTime.isIn(from: LocalDateTime, to: LocalDateTime) = this > from && this <= to
}
