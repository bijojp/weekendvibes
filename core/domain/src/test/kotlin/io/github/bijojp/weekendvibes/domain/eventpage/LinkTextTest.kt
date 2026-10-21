package io.github.bijojp.weekendvibes.domain.eventpage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkTextTest {
    @Test
    fun `the first link in shared text, without trailing punctuation`() {
        assertEquals("https://dis.tr/abc123", SharedText.firstUrl("Check out X on District! https://dis.tr/abc123."))
        assertEquals("https://a.example/1", SharedText.firstUrl("https://a.example/1 and http://b.example/2"))
        assertEquals("http://b.example/x?y=1", SharedText.firstUrl("(see http://b.example/x?y=1)"))
        assertNull(SharedText.firstUrl("Let's go to the concert on Saturday"))
    }

    @Test
    fun `block pages are recognised whatever the status`() {
        val blocked = fixture("bookmyshow_blocked.html")
        assertTrue(PageCheck.looksBlocked(403, blocked))
        assertTrue(PageCheck.looksBlocked(200, blocked))
        assertTrue(PageCheck.looksBlocked(429, "<html></html>"))
        assertTrue(PageCheck.looksBlocked(200, "<html><head><title>Just a moment...</title></head></html>"))
        assertTrue(PageCheck.looksBlocked(200, """<iframe src="https://geo.captcha-delivery.com/captcha/?initialCid=x"></iframe>"""))
    }

    @Test
    fun `normal event pages are not blocked`() {
        for (name in listOf("district.html", "bookmyshow.html", "urbanaut.html", "luma.html")) {
            assertFalse(name, PageCheck.looksBlocked(200, fixture(name)))
        }
        // Cloudflare's script path appears on ordinary pages too.
        assertFalse(PageCheck.looksBlocked(200, """<script src="/cdn-cgi/challenge-platform/scripts/jsd/main.js"></script>"""))
    }

    @Test
    fun `event data is spotted without parsing`() {
        assertTrue(PageCheck.hasEventData(fixture("district.html")))
        assertTrue(PageCheck.hasEventData(fixture("bookmyshow.html")))
        assertTrue(PageCheck.hasEventData(fixture("urbanaut.html")))
        assertFalse(PageCheck.hasEventData(fixture("bookmyshow_blocked.html")))
        assertFalse(PageCheck.hasEventData("""<script type="application/ld+json">{"@type":"Organization"}</script>"""))
    }
}
