package io.github.bijojp.weekendvibes.data

import androidx.room.withTransaction
import io.github.bijojp.weekendvibes.domain.ActivityFields
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.AgendaRepository
import io.github.bijojp.weekendvibes.domain.Limits
import io.github.bijojp.weekendvibes.domain.cleanNotes
import io.github.bijojp.weekendvibes.domain.cleanText
import io.github.bijojp.weekendvibes.domain.isWeekendDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/** Applies each change in one transaction, after checking it against the domain rules. */
internal class RoomAgendaRepository(
    private val db: WeekendVibesDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : AgendaRepository {
    private val dao = db.dao()

    override val agenda: Flow<Agenda> =
        db.invalidationTracker.createFlow("activities", "extra_days")
            .conflate()
            .map { dao.snapshot().toAgenda() }

    override suspend fun addActivity(date: LocalDate, fields: ActivityFields): ActivityId = db.withTransaction {
        val time = now()
        val id = newId()
        markFree(date)
        dao.insertActivity(fields.toEntity(id, date, createdAt = time, updatedAt = time))
        ActivityId(id)
    }

    override suspend fun editActivity(id: ActivityId, date: LocalDate, fields: ActivityFields) = db.withTransaction {
        val existing = requireNotNull(dao.activity(id.value)) { "Activity not found" }
        markFree(date)
        dao.updateActivity(fields.toEntity(id.value, date, existing.createdAt, updatedAt = now()))
    }

    override suspend fun deleteActivity(id: ActivityId) {
        dao.deleteActivity(id.value)
    }

    override suspend fun setExtraDay(date: LocalDate, label: String) = db.withTransaction {
        require(!date.isWeekendDay) { "Saturdays and Sundays are always free" }
        val existing = dao.extraDay(date.toEpochDay())
        val time = now()
        dao.upsertExtraDay(ExtraDayEntity(date.toEpochDay(), cleanText(label, Limits.LABEL), existing?.createdAt ?: time, time))
    }

    override suspend fun removeExtraDay(date: LocalDate) = db.withTransaction {
        dao.deleteActivitiesOn(date.toEpochDay())
        dao.deleteExtraDay(date.toEpochDay())
    }

    /** A weekday gets an Extra day so its Activities have a Free day to sit on. */
    private suspend fun markFree(date: LocalDate) {
        if (date.isWeekendDay || dao.extraDay(date.toEpochDay()) != null) return
        val time = now()
        dao.upsertExtraDay(ExtraDayEntity(date.toEpochDay(), "", time, time))
    }

    private fun ActivityFields.toEntity(id: String, date: LocalDate, createdAt: Long, updatedAt: Long) = ActivityEntity(
        id = id,
        date = date.toEpochDay(),
        title = cleanText(title, Limits.TITLE).also { require(it.isNotEmpty()) { "A title is required" } },
        time = time?.toMinutes(),
        location = cleanText(location, Limits.LOCATION),
        link = link.trim().take(Limits.LINK),
        notes = cleanNotes(notes, Limits.NOTES),
        picture = picture,
        // A Reminder needs a time to count back from.
        reminderMinutes = reminder?.minutes?.takeIf { time != null },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
