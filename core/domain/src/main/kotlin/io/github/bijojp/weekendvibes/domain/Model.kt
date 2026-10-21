package io.github.bijojp.weekendvibes.domain

import java.time.LocalDate
import java.time.LocalTime

// What the user plans and remembers. Vocabulary is defined in GLOSSARY.md; keep these names in step with it.

@JvmInline
value class ActivityId(val value: String)

/** Something on one Free day. */
data class Activity(
    val id: ActivityId,
    val date: LocalDate,
    val title: String,
    /** Null for an Anytime Activity. */
    val time: LocalTime?,
    val location: String,
    val link: String,
    val notes: String,
    /** File name of its Picture in the app's storage. */
    val picture: String?,
    /** Only ever set on a timed Activity. */
    val reminder: ReminderLead?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val fields: ActivityFields get() = ActivityFields(title, time, location, link, notes, picture, reminder)
}

/** How long before a timed Activity its Reminder goes off. */
enum class ReminderLead(val minutes: Long) {
    HalfHour(30),
    Hour(60),
    ThreeHours(180),
    Day(1440),
}

/** A weekday the user marked as free. [label] may be empty. */
data class ExtraDay(
    val date: LocalDate,
    val label: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** What the user fills in for an Activity, apart from its date. */
data class ActivityFields(
    val title: String,
    val time: LocalTime?,
    val location: String = "",
    val link: String = "",
    val notes: String = "",
    val picture: String? = null,
    val reminder: ReminderLead? = null,
)

object Limits {
    const val TITLE = 100
    const val LOCATION = 120
    const val LINK = 2000
    const val NOTES = 4000
    const val LABEL = 40
}

/** Trims and collapses inner whitespace; the result may be empty. */
fun cleanText(text: String, maxLength: Int): String =
    text.trim().replace(Regex("\\s+"), " ").take(maxLength)

/** Like [cleanText] but keeps line breaks, for notes. */
fun cleanNotes(text: String, maxLength: Int): String =
    text.lines().joinToString("\n") { it.trimEnd() }.trim().take(maxLength)
