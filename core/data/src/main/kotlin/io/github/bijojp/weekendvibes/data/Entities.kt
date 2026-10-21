package io.github.bijojp.weekendvibes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.ReminderLead
import java.time.LocalDate
import java.time.LocalTime

// Dates are stored as epoch days and times of day as minutes after midnight.

@Entity(tableName = "activities", indices = [Index("date")])
data class ActivityEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val title: String,
    val time: Int?,
    val location: String,
    val link: String,
    val notes: String,
    /** File name under the app's pictures folder. */
    val picture: String?,
    /** A [ReminderLead]'s minutes; null when off. */
    val reminderMinutes: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "extra_days")
data class ExtraDayEntity(
    @PrimaryKey val date: Long,
    val label: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Every table at one consistent moment. */
data class Snapshot(
    val activities: List<ActivityEntity>,
    val extraDays: List<ExtraDayEntity>,
)

internal fun Snapshot.toAgenda(): Agenda = Agenda(
    activities = activities.map { it.toDomain() },
    extraDays = extraDays.map { it.toDomain() },
)

internal fun ActivityEntity.toDomain() = Activity(
    id = ActivityId(id),
    date = LocalDate.ofEpochDay(date),
    title = title,
    time = time?.let { LocalTime.of(it / 60, it % 60) },
    location = location,
    link = link,
    notes = notes,
    picture = picture,
    reminder = reminderMinutes?.let { minutes -> ReminderLead.entries.firstOrNull { it.minutes == minutes } },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ExtraDayEntity.toDomain() = ExtraDay(LocalDate.ofEpochDay(date), label, createdAt, updatedAt)

internal fun Activity.toEntity() = ActivityEntity(
    id = id.value,
    date = date.toEpochDay(),
    title = title,
    time = time?.toMinutes(),
    location = location,
    link = link,
    notes = notes,
    picture = picture,
    reminderMinutes = reminder?.minutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ExtraDay.toEntity() = ExtraDayEntity(date.toEpochDay(), label, createdAt, updatedAt)

internal fun LocalTime.toMinutes(): Int = hour * 60 + minute
