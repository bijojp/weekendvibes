package io.github.bijojp.weekendvibes.ui.editor

import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.ActivityFields
import io.github.bijojp.weekendvibes.domain.ReminderLead
import io.github.bijojp.weekendvibes.ui.EditorRoute
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** The editor's working copy of an Activity. */
internal data class Draft(
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val location: String,
    val link: String,
    val notes: String,
    val picture: String?,
    val reminder: ReminderLead?,
) {
    val canSave: Boolean get() = title.isNotBlank()

    fun fields(): ActivityFields = ActivityFields(title, time, location, link, notes, picture, reminder)

    companion object {
        fun from(activity: Activity?, route: EditorRoute, today: LocalDate): Draft {
            if (activity != null) {
                return Draft(
                    activity.title, activity.date, activity.time, activity.location,
                    activity.link, activity.notes, activity.picture, activity.reminder,
                )
            }
            // From the + button, a new Activity starts on the coming Saturday.
            val date = route.date?.let(LocalDate::parse) ?: today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            return Draft("", date, null, "", "", "", null, null)
        }
    }
}
