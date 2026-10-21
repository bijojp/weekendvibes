package io.github.bijojp.weekendvibes.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Where Activities and Extra days are stored. Implementations apply each change atomically. */
interface AgendaRepository {
    /** The latest snapshot, re-emitted after every change. */
    val agenda: Flow<Agenda>

    /** Putting an Activity on a weekday that isn't free marks it as an Extra day. */
    suspend fun addActivity(date: LocalDate, fields: ActivityFields): ActivityId

    /** Moving it to a weekday that isn't free marks that day as an Extra day. */
    suspend fun editActivity(id: ActivityId, date: LocalDate, fields: ActivityFields)

    suspend fun deleteActivity(id: ActivityId)

    /** Marks a weekday as an Extra day, or changes its label. Saturdays and Sundays are refused. */
    suspend fun setExtraDay(date: LocalDate, label: String)

    /** Unmarks the Extra day and deletes its Activities. */
    suspend fun removeExtraDay(date: LocalDate)
}
