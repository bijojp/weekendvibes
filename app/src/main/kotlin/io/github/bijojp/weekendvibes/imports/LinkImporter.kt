package io.github.bijojp.weekendvibes.imports

import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import io.github.bijojp.weekendvibes.data.PictureStore
import io.github.bijojp.weekendvibes.domain.eventpage.EventPage
import io.github.bijojp.weekendvibes.domain.eventpage.EventPageReader
import io.github.bijojp.weekendvibes.domain.eventpage.PageCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** What Link import found: the page as read, and its Picture once stored. */
data class LinkResult(val page: EventPage, val picture: String?)

/**
 * Reads a shared event link (PLAN §3): plain fetch, then the WebView fallback for block pages and
 * pages without dates, then downloads the picture. Never throws; returns null when nothing was found.
 */
class LinkImporter(context: Context, private val pictures: PictureStore) {
    private val appContext = context.applicationContext

    /** The phone's own WebView user agent, so plain fetches say what they really are. */
    private var userAgent: String? = null

    suspend fun read(link: String, zone: ZoneId = ZoneId.systemDefault()): LinkResult? {
        val agent = userAgent ?: withContext(Dispatchers.Main) { WebSettings.getDefaultUserAgent(appContext) }.also { userAgent = it }
        val fetched = runCatching { PageFetcher.page(link, agent) }
            .onFailure { Log.i(TAG, "Fetch failed for $link", it) }
            .getOrNull()
        var page = fetched
            ?.takeUnless { PageCheck.looksBlocked(it.status, it.html) || it.status !in 200..299 }
            ?.let { parse(it, zone) }
        if (page == null || page.options.isEmpty()) {
            val rendered = runCatching { PageRenderer.render(appContext, fetched?.url ?: link) }
                .onFailure { Log.i(TAG, "Render failed for $link", it) }
                .getOrNull()
                ?.takeUnless { PageCheck.looksBlocked(200, it.html) }
                ?.let { parse(it, zone) }
            page = listOfNotNull(page, rendered).maxByOrNull { (if (it.options.isNotEmpty()) 2 else 0) + (if (it.title != null) 1 else 0) }
        }
        if (page == null || page.isEmpty) return null
        val picture = page.image?.let { image ->
            runCatching { pictures.import(PageFetcher.image(image, agent)) }
                .onFailure { Log.i(TAG, "Picture failed for $image", it) }
                .getOrNull()
        }
        return LinkResult(page, picture)
    }

    private suspend fun parse(fetched: Fetched, zone: ZoneId): EventPage = withContext(Dispatchers.Default) {
        EventPageReader.read(fetched.html, fetched.url, zone)
    }

    private companion object {
        const val TAG = "LinkImport"
    }
}
