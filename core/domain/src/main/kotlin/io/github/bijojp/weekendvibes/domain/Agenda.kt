package io.github.bijojp.weekendvibes.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** Everything stored, at one moment. Breaks and the Timeline are worked out from it (ADR 0003). */
class Agenda(val activities: List<Activity>, extraDays: List<ExtraDay>) {
    val extraDays: List<ExtraDay> = extraDays.sortedBy { it.date }

    private val extraByDate = extraDays.associateBy { it.date }
    private val activitiesByDate = activities.groupBy { it.date }.mapValues { (_, list) -> list.sortedWith(DayOrder) }
    private val activitiesById = activities.associateBy { it.id }

    fun activity(id: ActivityId): Activity? = activitiesById[id]

    fun extraDay(date: LocalDate): ExtraDay? = extraByDate[date]

    /** Timed ones first in time order, then Anytime ones. */
    fun activitiesOn(date: LocalDate): List<Activity> = activitiesByDate[date].orEmpty()

    /** A Saturday, a Sunday or an Extra day: the days Breaks are made of. */
    fun isFree(date: LocalDate): Boolean = date.isWeekendDay || date in extraByDate

    /**
     * The Break [date] is in: its run of Free days, or a weekday with plans on its own (so it can't
     * chain Weekends together). Null for a weekday with nothing on it.
     */
    fun breakContaining(date: LocalDate): Break? {
        if (!isFree(date)) return if (date in activitiesByDate) Break(date, date) else null
        var start = date
        while (isFree(start.minusDays(1))) start = start.minusDays(1)
        var end = date
        while (isFree(end.plusDays(1))) end = end.plusDays(1)
        return Break(start, end)
    }

    /** The Weekend (or Extra days) containing [today], or else the next one. Weekday plans don't count. */
    fun thisWeekend(today: LocalDate): Break {
        // Every week has a Saturday, so this ends within six days.
        val firstFree = generateSequence(today) { it.plusDays(1) }.first(::isFree)
        return requireNotNull(breakContaining(firstFree))
    }

    /** The earliest date anything is stored on. */
    val firstDate: LocalDate?
        get() = listOfNotNull(activitiesByDate.keys.minOrNull(), extraByDate.keys.minOrNull()).minOrNull()

    /** The latest date anything is stored on. */
    val lastDate: LocalDate?
        get() = listOfNotNull(activitiesByDate.keys.maxOrNull(), extraByDate.keys.maxOrNull()).maxOrNull()
}

/** Order of Activities within a day: timed by time, then Anytime; ties in the order they were added. */
val DayOrder: Comparator<Activity> =
    compareBy<Activity> { it.time == null }.thenBy { it.time }.thenBy { it.createdAt }

val LocalDate.isWeekendDay: Boolean
    get() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
