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

    /** A date with Activities always counts, so nothing can drop out of the Timeline. */
    fun isFree(date: LocalDate): Boolean = date.isWeekendDay || date in extraByDate || date in activitiesByDate

    /** The Break [date] is in, or null if it isn't a Free day. */
    fun breakContaining(date: LocalDate): Break? {
        if (!isFree(date)) return null
        var start = date
        while (isFree(start.minusDays(1))) start = start.minusDays(1)
        var end = date
        while (isFree(end.plusDays(1))) end = end.plusDays(1)
        return Break(start, end)
    }

    /** The Break containing [today], or else the next one. */
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
