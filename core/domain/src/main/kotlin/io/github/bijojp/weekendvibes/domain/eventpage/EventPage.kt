package io.github.bijojp.weekendvibes.domain.eventpage

import io.github.bijojp.weekendvibes.domain.isWeekendDay
import java.time.LocalDate
import java.time.LocalTime

/** What could be read from one event page. Any part may be missing. */
data class EventPage(val title: String?, val image: String?, val options: List<EventOption>) {
    val isEmpty: Boolean get() = title == null && options.isEmpty()
}

/** One day (and maybe time and venue) the event happens on. */
data class EventOption(val date: LocalDate, val time: LocalTime?, val venue: String?, val address: String?) {
    /** What goes in an Activity's location: the venue, else the address. */
    val place: String? get() = venue ?: address

    /** Whether the venue or address is in [city], allowing old names (Bangalore = Bengaluru). Unknown places count as in. */
    fun isIn(city: String): Boolean {
        val text = listOfNotNull(venue, address).joinToString(" ").lowercase()
        if (text.isBlank() || city.isBlank()) return true
        return cityNames(city).any { it in text }
    }
}

/** Upcoming before past, then Home city first, then weekend dates first, then by date and time. Duplicates removed. */
fun List<EventOption>.rankedFor(homeCity: String, today: LocalDate): List<EventOption> =
    distinct().sortedWith(
        compareBy<EventOption> { it.date < today }
            .thenBy { !it.isIn(homeCity) }
            .thenBy { !it.date.isWeekendDay }
            .thenBy { it.date }
            .thenBy(nullsLast()) { it.time },
    )

/** Cities that are often still written by their old name. */
private val RENAMED_CITIES = listOf(
    setOf("bengaluru", "bangalore"),
    setOf("mumbai", "bombay"),
    setOf("chennai", "madras"),
    setOf("kolkata", "calcutta"),
    setOf("gurugram", "gurgaon"),
    setOf("mysuru", "mysore"),
    setOf("kochi", "cochin"),
    setOf("thiruvananthapuram", "trivandrum"),
    setOf("puducherry", "pondicherry"),
    setOf("pune", "poona"),
    setOf("vadodara", "baroda"),
)

private fun cityNames(city: String): Set<String> {
    val name = city.trim().lowercase()
    return RENAMED_CITIES.firstOrNull { name in it } ?: setOf(name)
}
