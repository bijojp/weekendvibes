package io.github.bijojp.weekendvibes.domain

import java.time.LocalDate

/** A run of consecutive Free days: one card in the Timeline. */
data class Break(val start: LocalDate, val end: LocalDate) {
    init {
        require(start <= end) { "A Break ends on or after its start" }
    }

    val days: List<LocalDate> get() = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()

    /** Has a Saturday or Sunday in it; otherwise it's Extra days on their own. */
    val isWeekend: Boolean get() = days.any { it.isWeekendDay }

    /** A Weekend with Extra days in it. */
    val isLongWeekend: Boolean get() = isWeekend && days.any { !it.isWeekendDay }

    operator fun contains(date: LocalDate): Boolean = date in start..end
}

/** One card as the Timeline shows it. */
data class BreakCard(
    val span: Break,
    val days: List<FreeDay>,
    val isPast: Boolean,
    val isThisWeekend: Boolean,
) {
    val isEmpty: Boolean get() = days.all { it.activities.isEmpty() }
}

/** One day in a card. [extraDay] is set when the user marked it. */
data class FreeDay(val date: LocalDate, val extraDay: ExtraDay?, val activities: List<Activity>)

object Timeline {
    /**
     * Past Breaks that have Activities, then This weekend and every Break after it, up to [until].
     * A Break that runs past [until] is included whole.
     */
    fun build(agenda: Agenda, today: LocalDate, until: LocalDate): List<BreakCard> {
        val thisWeekend = agenda.thisWeekend(today)
        val from = minOf(agenda.firstDate ?: thisWeekend.start, thisWeekend.start)
        val cards = mutableListOf<BreakCard>()
        var date = from
        while (date <= until) {
            val span = agenda.breakContaining(date)
            if (span == null) {
                date = date.plusDays(1)
                continue
            }
            val card = BreakCard(
                span = span,
                days = span.days.map { FreeDay(it, agenda.extraDay(it), agenda.activitiesOn(it)) },
                isPast = span.end < today,
                isThisWeekend = span == thisWeekend,
            )
            if (!card.isPast || !card.isEmpty) cards += card
            date = span.end.plusDays(1)
        }
        return cards
    }
}
