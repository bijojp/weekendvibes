package io.github.bijojp.weekendvibes.domain.eventpage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Trimmed copies of real Bangalore event pages, fetched on 2026-10-21.

private val IST: ZoneId = ZoneId.of("Asia/Kolkata")

fun fixture(name: String): String = EventPageReaderTest::class.java.getResource("/eventpages/$name")!!.readText()

private fun read(name: String, url: String = "https://example.com/event"): EventPage = EventPageReader.read(fixture(name), url, IST)

class EventPageReaderTest {
    @Test
    fun `District gives title without the city suffix, time converted from UTC, venue and image`() {
        val page = read("district.html", "https://www.district.in/events/papon-live-in-concert-bengaluru-2026-buy-tickets")
        assertEquals("Papon Live In Concert", page.title)
        val option = page.options.single()
        assertEquals(LocalDate.of(2026, 11, 14), option.date)
        assertEquals(LocalTime.of(19, 0), option.time)
        assertEquals("District Arena @ Terraform", option.venue)
        assertEquals("District Arena @ Terraform", option.place)
        assertTrue(option.address!!.contains("Yelahanka"))
        assertEquals(
            "https://cdn.district.in/assets/events/publisher/event_cover_image_horizontal/01M3KTP1SE27E7JFNT4P579QZG.jpg",
            page.image,
        )
    }

    @Test
    fun `BookMyShow gives a trimmed title, the local time and the venue`() {
        val page = read("bookmyshow.html", "https://in.bookmyshow.com/events/cubing-workshop/ET00000000")
        assertEquals("Cubing Workshop: Master the Pyraminx", page.title)
        val option = page.options.single()
        assertEquals(LocalDate.of(2026, 10, 25), option.date)
        assertEquals(LocalTime.of(11, 30), option.time)
        assertTrue(option.venue!!.contains("Brain Studio"))
        assertTrue(page.image!!.startsWith("https://assets-in.bmscdn.com/"))
    }

    @Test
    fun `a BookMyShow page with many venues gives one option each`() {
        val page = read("bookmyshow_multi.html", "https://in.bookmyshow.com/events/perfumery/ET00000001")
        assertEquals("Perfumery", page.title)
        assertEquals(13, page.options.size)
    }

    @Test
    fun `HighApe`() {
        val page = read("highape.html", "https://highape.com/bangalore/events/live-singing")
        assertEquals("Live Singing & Kid Activities", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 25), LocalTime.of(12, 30)), page.options.single().at)
        assertEquals("153 Biere Street", page.options.single().venue)
        assertTrue(page.image!!.endsWith(".jpg"))
    }

    @Test
    fun `allevents`() {
        val page = read("allevents.html", "https://allevents.in/bangalore/clay-date/1")
        assertEquals("Clay Date - Bangalore", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 24), LocalTime.of(17, 0)), page.options.single().at)
        assertEquals("Koramangala Social: Bengaluru", page.options.single().venue)
        assertNotNull(page.image)
    }

    @Test
    fun `Meetup`() {
        val page = read("meetup.html", "https://www.meetup.com/python/events/1")
        assertEquals("Python Meetup X Functional Programming India", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 24), LocalTime.of(10, 30)), page.options.single().at)
        assertEquals("EPAM Systems Bangalore", page.options.single().venue)
        assertEquals("https://secure-content.meetupstatic.com/images/classic-events/536374399/676x676.jpg", page.image)
    }

    @Test
    fun `Luma, with fractional seconds`() {
        val page = read("luma.html", "https://luma.com/blrdroid-133")
        assertEquals("BlrDroid + Blrkotlin October 2026 Meetup #133", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 24), LocalTime.of(9, 30)), page.options.single().at)
        assertEquals("Gojek (Goproducts Engineering India LLP)", page.options.single().venue)
        assertTrue(page.image!!.startsWith("https://images.lumacdn.com/"))
    }

    @Test
    fun `Eventbrite`() {
        val page = read("eventbrite.html", "https://www.eventbrite.co.uk/e/meet-the-rca-1")
        assertEquals("Meet the Royal College of Art in Bengaluru", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 24), LocalTime.of(17, 0)), page.options.single().at)
        assertEquals("Welcomhotel by ITC Hotels, Bengaluru", page.options.single().venue)
        assertTrue(page.image!!.startsWith("https://img.evbuc.com/"))
    }

    @Test
    fun `blr today copies the source page's event data`() {
        val page = read("blrtoday.html", "https://blr.today/events/silaa-wednesday-kannada-jamming/")
        assertEquals("Silaa Wednesday Kannada Jamming HSR Layout", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 21), LocalTime.of(18, 0)), page.options.single().at)
        assertEquals("Silaa The Garden Cafe, HSR Layout: Bengaluru", page.options.single().venue)
        assertTrue(page.image!!.startsWith("https://assets-in.bmscdn.com/"))
    }

    @Test
    fun `Urbanaut is read from its own page state`() {
        val page = read("urbanaut.html", "https://urbanaut.app/spot/the-army-town-and-its-tales")
        assertEquals("The Army Town and Its Tales", page.title)
        val option = page.options.single()
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 25), LocalTime.of(7, 30)), option.at)
        assertEquals("Koshy's", option.venue)
        assertTrue(option.address!!.contains("St Mark's Rd"))
        assertEquals(
            "https://d10y46cwh6y6x1.cloudfront.net/images/A67A9B45E2B04C0C8DB53B2EF0B7F669_converted_webp.webp",
            page.image,
        )
    }

    @Test
    fun `an Urbanaut series uses its upcoming session, not its first`() {
        val page = read("urbanaut_series.html", "https://urbanaut.app/spot/open-practice-amateur-calisthenics-lab")
        assertEquals("Open Practice: Amateur Calisthenics Lab", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 25), LocalTime.of(7, 0)), page.options.single().at)
    }

    @Test
    fun `a block page has no event in it`() {
        val page = read("bookmyshow_blocked.html", "https://in.bookmyshow.com/events/cubing-workshop/ET00000000")
        assertTrue(page.options.isEmpty())
        assertEquals(null, page.image)
    }

    @Test
    fun `without JSON-LD, preview tags give the title, image and start time`() {
        val html = """
            <html><head>
            <title>Fallback title</title>
            <meta property="og:title" content="">
            <meta name="twitter:title" content="Open Mic Night">
            <meta property="og:image" content="/img/poster.jpg">
            <meta property="event:start_time" content="2026-10-31T20:00:00+05:30">
            </head><body></body></html>
        """.trimIndent()
        val page = EventPageReader.read(html, "https://venue.example/events/open-mic", IST)
        assertEquals("Open Mic Night", page.title)
        assertEquals("https://venue.example/img/poster.jpg", page.image)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 31), LocalTime.of(20, 0)), page.options.single().at)
    }

    @Test
    fun `microdata start date is used when there is nothing else`() {
        val html = """
            <html><head><title>Pottery Saturday</title></head><body>
            <div itemscope itemtype="https://schema.org/Event">
              <time itemprop="startDate" datetime="2026-10-24">Sat 24 Oct</time>
            </div></body></html>
        """.trimIndent()
        val page = EventPageReader.read(html, "https://studio.example/pottery", IST)
        assertEquals("Pottery Saturday", page.title)
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 24), null), page.options.single().at)
    }

    @Test
    fun `JSON-LD inside a graph, typed as an array, online, with an entity in the name`() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[
              {"@type":"WebPage","name":"Ignore me"},
              {"@type":["Thing","MusicEvent"],"name":"Rock &amp; Roll Live","startDate":"2026-10-25T18:30:00+0530",
               "location":{"@type":"VirtualLocation","url":"https://stream.example"},
               "image":{"@type":"ImageObject","url":"https://img.example/rock.png"}}
            ]}
            </script>
            <script type="application/ld+json">{ not json </script>
            </head><body></body></html>
        """.trimIndent()
        val page = EventPageReader.read(html, "https://gigs.example/rock", IST)
        assertEquals("Rock & Roll Live", page.title)
        assertEquals("https://img.example/rock.png", page.image)
        val option = page.options.single()
        assertEquals(EventOptionAt(LocalDate.of(2026, 10, 25), LocalTime.of(18, 30)), option.at)
        assertEquals("Online", option.venue)
    }

    @Test
    fun `sub-events each become an option`() {
        val html = """
            <html><head><script type="application/ld+json">
            {"@type":"Festival","name":"Weekend Fest","subEvent":[
              {"@type":"Event","name":"Day 1","startDate":"2026-10-24T16:00:00+05:30","location":"Palace Grounds"},
              {"@type":"Event","name":"Day 2","startDate":"2026-10-25T16:00:00+05:30","location":"Palace Grounds"}
            ]}
            </script></head><body></body></html>
        """.trimIndent()
        val page = EventPageReader.read(html, "https://fest.example", IST)
        assertEquals("Day 1", page.title)
        assertEquals(listOf(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25)), page.options.map { it.date })
        assertEquals("Palace Grounds", page.options.first().venue)
    }

    @Test
    fun `an empty page is empty`() {
        val page = EventPageReader.read("<html><head></head><body></body></html>", "https://nothing.example", IST)
        assertTrue(page.isEmpty)
    }
}

/** Just the day and time, for compact assertions. */
data class EventOptionAt(val date: LocalDate, val time: LocalTime?)

val EventOption.at: EventOptionAt get() = EventOptionAt(date, time)
