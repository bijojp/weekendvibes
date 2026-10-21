package io.github.bijojp.weekendvibes.domain.eventpage

/** Pulls the link out of what another app shares, which is often a sentence with a link in it. */
object SharedText {
    private val URL = Regex("""https?://[^\s<>"'`]+""", RegexOption.IGNORE_CASE)
    private const val TRAILING = ".,;:!?)]}>'\""

    /** The first http(s) link in shared text, without trailing punctuation. */
    fun firstUrl(text: String): String? = URL.find(text)?.value?.trimEnd { it in TRAILING }?.takeIf { it.length > "https://".length }
}

/** Quick looks at fetched HTML, before or instead of reading it properly. */
object PageCheck {
    private val BLOCKED_STATUSES = setOf(401, 403, 429, 503)

    // Only text that appears on block pages: Cloudflare's script paths also appear on ordinary pages.
    private val BLOCK_MARKERS = listOf(
        "attention required! | cloudflare",
        "you have been blocked",
        "<title>just a moment...</title>",
        "_cf_chl_opt",
        "cf-browser-verification",
        "captcha-delivery.com",
    )

    private val EVENT_TYPE = Regex(""""@type"\s*:\s*(\[\s*(?:"[^"]*"\s*,\s*)*)?"[A-Za-z]*Event"""")

    /** A bot wall or block page (403/429/503, Cloudflare "Attention Required"/"you have been blocked"/"Just a moment", DataDome) rather than the event. */
    fun looksBlocked(status: Int, html: String): Boolean {
        if (status in BLOCKED_STATUSES) return true
        val lower = html.lowercase()
        return BLOCK_MARKERS.any { it in lower }
    }

    /** Cheap text check (no parsing) that the HTML already has event data: JSON-LD with an @type ending in Event, or Urbanaut's ng-state. Used to poll a WebView. */
    fun hasEventData(html: String): Boolean =
        (html.contains("application/ld+json", ignoreCase = true) && EVENT_TYPE.containsMatchIn(html)) ||
            (html.contains("id=\"ng-state\"") && html.contains("spot_detail"))
}
