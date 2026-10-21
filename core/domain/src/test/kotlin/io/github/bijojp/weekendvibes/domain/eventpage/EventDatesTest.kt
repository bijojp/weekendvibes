package io.github.bijojp.weekendvibes.domain.eventpage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EventDatesTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    private fun parse(text: String) = EventDates.parse(text, ist)

    private fun at(day: Int, hour: Int? = null, minute: Int = 0) =
        DayAndTime(LocalDate.of(2026, 10, day), hour?.let { LocalTime.of(it, minute) })

    @Test
    fun `a bare date has no time`() {
        assertEquals(at(24), parse("2026-10-24"))
    }

    @Test
    fun `offsets with and without a colon are converted to the phone's zone`() {
        assertEquals(at(24, 17), parse("2026-10-24T17:00:00+05:30"))
        assertEquals(at(24, 17), parse("2026-10-24T17:00:00+0530"))
        assertEquals(at(24, 21, 30), parse("2026-10-24T17:00:00+01:00"))
    }

    @Test
    fun `UTC times and fractional seconds are converted`() {
        assertEquals(at(24, 19), parse("2026-10-24T13:30:00.000Z"))
        assertEquals(at(24, 9, 30), parse("2026-10-24T09:30:00.000+05:30"))
        assertEquals(at(24, 19), parse("2026-10-24T13:30Z"))
    }

    @Test
    fun `midnight UTC means a date with no time`() {
        assertEquals(at(25), parse("2026-10-25T00:00Z"))
        assertEquals(at(25), parse("2026-10-25T00:00:00.000Z"))
    }

    @Test
    fun `a time without an offset is local, with a T or a space`() {
        assertEquals(at(25, 7, 30), parse("2026-10-25T07:30:00"))
        assertEquals(at(25, 7, 30), parse("2026-10-25 07:30:00"))
        assertEquals(at(25, 7, 30), parse("2026-10-25T07:30"))
    }

    @Test
    fun `local midnight means no time`() {
        assertEquals(at(25), parse("2026-10-25T00:00:00"))
        assertEquals(at(25), parse("2026-10-25T00:00:00+05:30"))
    }

    @Test
    fun `seconds are dropped`() {
        assertEquals(at(25, 11, 56), parse("2026-10-25T11:56:42+05:30"))
    }

    @Test
    fun `anything else is not a date`() {
        assertNull(parse("Sunday, Oct 25"))
        assertNull(parse(""))
        assertNull(parse("2026-13-40"))
    }
}
