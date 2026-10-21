package io.github.bijojp.weekendvibes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BreakTest {
    @Test
    fun `a plain Weekend is Saturday and Sunday`() {
        val span = agenda().breakContaining(oct(25))
        assertEquals(Break(oct(24), oct(25)), span)
        assertTrue(span!!.isWeekend)
        assertFalse(span.isLongWeekend)
    }

    @Test
    fun `a weekday that isn't marked is not in any Break`() {
        assertNull(agenda().breakContaining(oct(21)))
    }

    @Test
    fun `a Friday or Monday Extra day joins the Weekend next to it`() {
        val friday = agenda(extraDays = listOf(extra(oct(23))))
        assertEquals(Break(oct(23), oct(25)), friday.breakContaining(oct(24)))
        assertTrue(friday.breakContaining(oct(23))!!.isLongWeekend)

        val monday = agenda(extraDays = listOf(extra(oct(26))))
        assertEquals(Break(oct(24), oct(26)), monday.breakContaining(oct(26)))
    }

    @Test
    fun `Thursday and Friday off make a four-day Weekend`() {
        val agenda = agenda(extraDays = listOf(extra(oct(22)), extra(oct(23))))
        assertEquals(Break(oct(22), oct(25)), agenda.breakContaining(oct(22)))
    }

    @Test
    fun `a Wednesday holiday stands alone`() {
        val span = agenda(extraDays = listOf(extra(oct(28), "Diwali"))).breakContaining(oct(28))
        assertEquals(Break(oct(28), oct(28)), span)
        assertFalse(span!!.isWeekend)
    }

    @Test
    fun `a whole week off joins two Weekends into one Break`() {
        val week = (26..30).map { extra(oct(it)) }
        assertEquals(Break(oct(24), nov(1)), agenda(extraDays = week).breakContaining(oct(28)))
    }

    @Test
    fun `a weekday with Activities counts as free even without an Extra day`() {
        val agenda = agenda(activities = listOf(activity("Dentist", oct(22))))
        assertEquals(Break(oct(22), oct(22)), agenda.breakContaining(oct(22)))
    }

    @Test
    fun `This weekend is the Break containing today, or the next one`() {
        assertEquals(Break(oct(24), oct(25)), agenda().thisWeekend(oct(21)))
        assertEquals(Break(oct(24), oct(25)), agenda().thisWeekend(oct(25)))
        assertEquals(Break(oct(31), nov(1)), agenda().thisWeekend(oct(26)))

        val holiday = agenda(extraDays = listOf(extra(oct(21))))
        assertEquals(Break(oct(21), oct(21)), holiday.thisWeekend(oct(21)))
    }

    @Test
    fun `within a day timed Activities come first in time order, then Anytime ones`() {
        val agenda = agenda(
            activities = listOf(
                activity("Laundry", oct(24), createdAt = 1),
                activity("Dinner", oct(24), LocalTime.of(19, 0)),
                activity("Rest", oct(24), createdAt = 2),
                activity("Hike", oct(24), LocalTime.of(6, 30)),
            ),
        )
        assertEquals(listOf("Hike", "Dinner", "Laundry", "Rest"), agenda.activitiesOn(oct(24)).map { it.title })
    }
}
