package io.github.bijojp.weekendvibes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

private fun at(month: Int, day: Int, hour: Int, minute: Int = 0): LocalDateTime = LocalDateTime.of(2026, month, day, hour, minute)

class RemindersTest {
    private val settings = Settings()

    @Test
    fun `the Digest is due at 6 PM on Friday for a plain Weekend`() {
        assertEquals(at(10, 23, 18), Reminders.next(agenda(), settings, after = at(10, 21, 9)))

        val due = Reminders.digestsDue(agenda(), settings, from = at(10, 23, 17, 55), to = at(10, 23, 18, 5))
        assertEquals(listOf(Break(oct(24), oct(25))), due.map { it.span })
    }

    @Test
    fun `a Friday off moves the Digest to Thursday`() {
        val agenda = agenda(extraDays = listOf(extra(oct(23))))
        assertEquals(at(10, 22, 18), Reminders.next(agenda, settings, after = at(10, 21, 9)))
    }

    @Test
    fun `a standalone Extra day gets its own Digest`() {
        val agenda = agenda(extraDays = listOf(extra(oct(28), "Diwali")))
        assertEquals(at(10, 27, 18), Reminders.next(agenda, settings, after = at(10, 26, 9)))
    }

    @Test
    fun `the Digest lists every Activity in the Break, and none when switched off`() {
        val agenda = agenda(activities = listOf(activity("Hike", oct(24), LocalTime.of(6, 0)), activity("Rest", oct(25))))
        val digest = Reminders.digestsDue(agenda, settings, from = at(10, 23, 17), to = at(10, 23, 19)).single()
        assertEquals(listOf("Hike", "Rest"), digest.activities.map { it.title })

        val off = settings.copy(digestEnabled = false)
        assertEquals(emptyList<Reminders.Digest>(), Reminders.digestsDue(agenda, off, from = at(10, 23, 17), to = at(10, 23, 19)))
        assertNull(Reminders.next(agenda, off, after = at(10, 21, 9)))
    }

    @Test
    fun `a Reminder is due its lead time before a timed Activity`() {
        val hike = activity("Hike", oct(24), LocalTime.of(6, 0)).copy(reminder = ReminderLead.Hour)
        val untimed = activity("Rest", oct(24)).copy(reminder = ReminderLead.Hour)
        val agenda = agenda(activities = listOf(hike, untimed))
        val off = settings.copy(digestEnabled = false)

        assertEquals(at(10, 24, 5), Reminders.next(agenda, off, after = at(10, 23, 20)))
        assertEquals(listOf("Hike"), Reminders.remindersDue(agenda, from = at(10, 24, 4, 55), to = at(10, 24, 5, 5)).map { it.title })
    }

    @Test
    fun `a day-before Reminder can come before the Digest`() {
        val gig = activity("Gig", oct(24), LocalTime.of(17, 0)).copy(reminder = ReminderLead.Day)
        assertEquals(at(10, 23, 17), Reminders.next(agenda(activities = listOf(gig)), settings, after = at(10, 23, 9)))
    }

    @Test
    fun `weekday plans get no Digest, only their own Reminders`() {
        val dentist = activity("Dentist", oct(21), LocalTime.of(10, 0)).copy(reminder = ReminderLead.Hour)
        val agenda = agenda(activities = listOf(dentist))
        assertEquals(at(10, 21, 9), Reminders.next(agenda, settings, after = at(10, 20, 9)))
        assertEquals(emptyList<Reminders.Digest>(), Reminders.digestsDue(agenda, settings, from = at(10, 20, 17), to = at(10, 20, 19)))
    }
}
