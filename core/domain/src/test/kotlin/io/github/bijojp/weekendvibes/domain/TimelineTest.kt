package io.github.bijojp.weekendvibes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    private fun starts(cards: List<BreakCard>) = cards.map { it.span.start }

    @Test
    fun `every coming Weekend is shown, empty or not`() {
        val cards = Timeline.build(agenda(), today = oct(21), until = nov(8))
        assertEquals(listOf(oct(24), oct(31), nov(7)), starts(cards))
        assertTrue(cards.all { it.isEmpty && !it.isPast })
    }

    @Test
    fun `past Breaks are shown only when they have Activities`() {
        val agenda = agenda(activities = listOf(activity("Trek", oct(10))))
        val cards = Timeline.build(agenda, today = oct(21), until = oct(25))
        // 3–4 Oct comes before the first Activity; 17–18 Oct is past and empty.
        assertEquals(listOf(oct(10), oct(24)), starts(cards))
        assertTrue(cards.first().isPast)
    }

    @Test
    fun `the Weekend in progress is This weekend, not past`() {
        val agenda = agenda(activities = listOf(activity("Hike", oct(24))))
        val cards = Timeline.build(agenda, today = oct(25), until = oct(25))
        assertEquals(listOf(oct(24)), starts(cards))
        assertTrue(cards.single().isThisWeekend)
        assertTrue(!cards.single().isPast)
    }

    @Test
    fun `Extra days show in their own card or inside a Long weekend, even when empty`() {
        val agenda = agenda(extraDays = listOf(extra(oct(23)), extra(oct(28), "Diwali")))
        val cards = Timeline.build(agenda, today = oct(21), until = nov(1))
        assertEquals(listOf(oct(23), oct(28), oct(31)), starts(cards))
        assertEquals("Diwali", cards[1].days.single().extraDay?.label)
        assertEquals(3, cards[0].days.size)
    }

    @Test
    fun `a Break that runs past the end is included whole`() {
        val agenda = agenda(extraDays = listOf(extra(oct(26))))
        val cards = Timeline.build(agenda, today = oct(21), until = oct(24))
        assertEquals(Break(oct(24), oct(26)), cards.single().span)
    }

    @Test
    fun `only one card is This weekend`() {
        val cards = Timeline.build(agenda(), today = oct(21), until = nov(15))
        assertEquals(listOf(oct(24)), cards.filter { it.isThisWeekend }.map { it.span.start })
    }
}
