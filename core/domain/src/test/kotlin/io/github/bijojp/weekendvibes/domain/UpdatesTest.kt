package io.github.bijojp.weekendvibes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test
    fun `reads the version and page from GitHub's latest release`() {
        // Trimmed from https://api.github.com/repos/bijojp/weekendvibes/releases/latest.
        val json = """
            {"html_url": "https://github.com/bijojp/weekendvibes/releases/tag/v0.1.0", "id": 1,
             "tag_name": "v0.1.0", "name": "Weekend Vibes 0.1.0", "draft": false, "prerelease": false,
             "assets": [{"name": "weekendvibes-0.1.0.apk", "size": 2868382}]}
        """
        assertEquals(Release("0.1.0", "https://github.com/bijojp/weekendvibes/releases/tag/v0.1.0"), Updates.parseLatest(json))
    }

    @Test
    fun `anything that isn't a release is ignored`() {
        assertNull(Updates.parseLatest("""{"message": "Not Found", "status": "404"}"""))
        assertNull(Updates.parseLatest("""{"tag_name": "v0.2.0", "html_url": "http://example.com/plain"}"""))
        assertNull(Updates.parseLatest("<html>rate limited</html>"))
    }

    @Test
    fun `later versions are newer, numbers compared as numbers`() {
        assertTrue(Updates.isNewer("0.2.0", "0.1.0"))
        assertTrue(Updates.isNewer("0.10.0", "0.9.3"))
        assertTrue(Updates.isNewer("1.0", "0.9.9"))
        assertTrue(Updates.isNewer("v0.1.1", "0.1.0"))
    }

    @Test
    fun `the same or an older version is not newer`() {
        assertFalse(Updates.isNewer("0.1.0", "0.1.0"))
        assertFalse(Updates.isNewer("0.1", "0.1.0"))
        assertFalse(Updates.isNewer("0.1.0", "0.2.0"))
    }

    @Test
    fun `build suffixes don't count`() {
        assertFalse(Updates.isNewer("0.1.0", "0.1.0-debug"))
        assertTrue(Updates.isNewer("0.2.0", "0.1.0-debug"))
    }

    @Test
    fun `each new version is alerted once`() {
        assertTrue(Updates.needsAlert("0.3.0", lastAlerted = null))
        assertTrue(Updates.needsAlert("0.4.0", lastAlerted = "0.3.0"))
        assertFalse(Updates.needsAlert("0.3.0", lastAlerted = "0.3.0"))
        assertFalse(Updates.needsAlert("0.3.0", lastAlerted = "0.4.0"))
    }

    @Test
    fun `an unreadable version is never newer`() {
        assertFalse(Updates.isNewer("latest", "0.1.0"))
        assertFalse(Updates.isNewer("", "0.1.0"))
    }
}
