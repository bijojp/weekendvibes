package io.github.bijojp.weekendvibes.domain

import java.time.LocalDate
import java.time.LocalTime

// October 2026: Wed 21 is "today" in most tests; Sat 24 and Sun 25 are the next Weekend.

fun oct(day: Int): LocalDate = LocalDate.of(2026, 10, day)

fun nov(day: Int): LocalDate = LocalDate.of(2026, 11, day)

fun activity(
    title: String,
    date: LocalDate,
    time: LocalTime? = null,
    createdAt: Long = 0,
): Activity = Activity(
    id = ActivityId(title),
    date = date,
    title = title,
    time = time,
    location = "",
    link = "",
    notes = "",
    picture = null,
    reminder = null,
    createdAt = createdAt,
    updatedAt = createdAt,
)

fun extra(date: LocalDate, label: String = ""): ExtraDay = ExtraDay(date, label, 0, 0)

fun agenda(activities: List<Activity> = emptyList(), extraDays: List<ExtraDay> = emptyList()) = Agenda(activities, extraDays)
