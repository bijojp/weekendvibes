package io.github.bijojp.weekendvibes.domain.eventpage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class RankingTest {
    private val today = LocalDate.of(2026, 10, 21)
    private val multi = EventPageReader.read(fixture("bookmyshow_multi.html"), "https://in.bookmyshow.com/events/perfumery", ZoneId.of("Asia/Kolkata"))

    @Test
    fun `Home city venues come first, weekend dates first among them`() {
        val ranked = multi.options.rankedFor("Bengaluru", today)
        assertTrue(ranked.first().venue!!.startsWith("Smoor Lounge"))
        assertEquals(LocalDate.of(2026, 10, 25), ranked.first().date)
        val bengaluru = ranked.takeWhile { it.isIn("Bengaluru") }
        assertEquals(5, bengaluru.size)
        assertTrue(ranked.drop(5).none { it.isIn("Bengaluru") })
    }

    @Test
    fun `old city names match the new ones`() {
        assertEquals(multi.options.rankedFor("Bengaluru", today), multi.options.rankedFor("Bangalore", today))
        assertEquals(multi.options.rankedFor("Mumbai", today), multi.options.rankedFor("bombay", today))
    }

    @Test
    fun `after the Home city, weekend dates come before weekdays`() {
        val others = multi.options.rankedFor("Bengaluru", today).drop(5)
        val firstWeekday = others.indexOfFirst { it.date.dayOfWeek.value < 6 }
        assertTrue(others.drop(firstWeekday).all { it.date.dayOfWeek.value < 6 })
        assertTrue(firstWeekday > 0)
    }

    @Test
    fun `upcoming before past, then by date and time, duplicates removed`() {
        val past = EventOption(LocalDate.of(2026, 10, 17), LocalTime.of(10, 0), "Hall", null)
        val late = EventOption(LocalDate.of(2026, 10, 24), LocalTime.of(20, 0), "Hall", null)
        val early = EventOption(LocalDate.of(2026, 10, 24), LocalTime.of(9, 0), "Hall", null)
        val anytime = EventOption(LocalDate.of(2026, 10, 24), null, "Hall", null)
        assertEquals(listOf(early, late, anytime, past), listOf(past, late, anytime, early, late).rankedFor("Bengaluru", today))
    }

    @Test
    fun `unknown places count as in the Home city`() {
        assertTrue(EventOption(today, null, null, null).isIn("Bengaluru"))
        assertTrue(EventOption(today, null, "Koramangala Social: Bengaluru", null).isIn("bangalore"))
        assertFalse(EventOption(today, null, "BKC Dive: Mumbai", "Bandra Kurla Complex, Mumbai").isIn("Bengaluru"))
    }

    @Test
    fun `place is the venue, else the address`() {
        assertEquals("Hall", EventOption(today, null, "Hall", "1 Road").place)
        assertEquals("1 Road", EventOption(today, null, null, "1 Road").place)
        assertEquals(null, EventOption(today, null, null, null).place)
    }
}
