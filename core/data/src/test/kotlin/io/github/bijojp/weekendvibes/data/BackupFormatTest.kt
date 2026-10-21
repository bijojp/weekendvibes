package io.github.bijojp.weekendvibes.data

import io.github.bijojp.weekendvibes.data.backup.BackupFormat
import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.ReminderLead
import io.github.bijojp.weekendvibes.domain.Settings
import io.github.bijojp.weekendvibes.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BackupFormatTest {
    private val hike = Activity(
        id = ActivityId("a1"),
        date = LocalDate.of(2026, 10, 24),
        title = "Hike at Nandi Hills",
        time = LocalTime.of(5, 30),
        location = "Nandi Hills",
        link = "https://example.com/hike",
        notes = "Leave by 4.\\nCarry water.",
        picture = "p1.jpg",
        reminder = ReminderLead.Day,
        createdAt = 10,
        updatedAt = 20,
    )
    private val rest = hike.copy(id = ActivityId("a2"), title = "Rest", time = null, location = "", link = "", notes = "", picture = null, reminder = null)
    private val diwali = ExtraDay(LocalDate.of(2026, 11, 9), "Diwali", 1, 2)
    private val settings = Settings(digestEnabled = false, homeCity = "Mumbai", theme = ThemeMode.Dark)

    @Test
    fun `a backup restores exactly what was saved`() {
        val contents = BackupFormat.Contents(Agenda(listOf(hike, rest), listOf(diwali)), settings)
        val decoded = BackupFormat.decode(BackupFormat.encode(contents, "0.1.0", 99))

        assertEquals(listOf(hike, rest), decoded.agenda.activities)
        assertEquals(listOf(diwali), decoded.agenda.extraDays)
        assertEquals(settings, decoded.settings)
    }

    @Test
    fun `anything that isn't a backup is refused before restoring`() {
        assertThrows(IllegalArgumentException::class.java) { BackupFormat.decode("{\"hello\": 1}") }
        assertThrows(IllegalArgumentException::class.java) { BackupFormat.decode("not json") }
        assertThrows(IllegalArgumentException::class.java) {
            BackupFormat.decode("{\"format\": 99, \"appVersion\": \"9\", \"createdAt\": 0, \"settings\": {}}")
        }
    }
}
