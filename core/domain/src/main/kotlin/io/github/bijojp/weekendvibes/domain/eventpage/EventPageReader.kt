package io.github.bijojp.weekendvibes.domain.eventpage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.time.ZoneId

/**
 * Reads an event page's title, picture and dates (PLAN §3): schema.org JSON-LD first, then a
 * site adapter for pages without it, then preview tags for whatever is still missing.
 */
object EventPageReader {
    private val adapters: List<SiteAdapter> = listOf(UrbanautAdapter)

    fun read(html: String, url: String, zone: ZoneId): EventPage {
        val doc = Jsoup.parse(html, url)
        val fromJsonLd = JsonLd.read(doc, zone)
        val fromSite = if (fromJsonLd.options.isEmpty()) adapterFor(url)?.read(doc, zone) else null
        return EventPage(
            title = fromJsonLd.title ?: fromSite?.title ?: PreviewTags.title(doc),
            image = (fromJsonLd.image ?: fromSite?.image ?: PreviewTags.image(doc))?.let { resolve(url, it) },
            options = fromJsonLd.options.ifEmpty { fromSite?.options.orEmpty() }.ifEmpty { PreviewTags.options(doc, zone) },
        )
    }

    private fun adapterFor(url: String): SiteAdapter? {
        val host = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return null
        return adapters.firstOrNull { it.handles(host) }
    }

    /** An absolute http(s) address for [ref], which may be relative to the page. */
    private fun resolve(pageUrl: String, ref: String): String? {
        val resolved = runCatching { URI(pageUrl).resolve(ref.trim()).toString() }.getOrDefault(ref.trim())
        return resolved.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }
}

/** schema.org Event objects in `application/ld+json` scripts. */
private object JsonLd {
    fun read(doc: Document, zone: ZoneId): EventPage {
        val events = mutableListOf<JsonObject>()
        doc.getElementsByTag("script")
            .filter { it.attr("type").trim().equals("application/ld+json", ignoreCase = true) }
            .mapNotNull { parseJsonOrNull(it.data()) }
            .forEach { collectEvents(it, events) }
        val options = events.mapNotNull { option(it, zone) }
        val first = events.firstOrNull { it.text("name") != null }
        return EventPage(
            title = first?.let { titleOf(it) },
            image = events.firstNotNullOfOrNull { image(it["image"]) },
            options = options,
        )
    }

    private fun collectEvents(element: JsonElement, into: MutableList<JsonObject>) {
        when (element) {
            is JsonArray -> element.forEach { collectEvents(it, into) }
            is JsonObject -> {
                if (element.types.any { it.endsWith("Event") }) into += element
                element["@graph"]?.let { collectEvents(it, into) }
                element["subEvent"]?.let { collectEvents(it, into) }
            }
            else -> Unit
        }
    }

    private fun option(event: JsonObject, zone: ZoneId): EventOption? {
        val start = event.text("startDate")?.let { EventDates.parse(it, zone) } ?: return null
        val (venue, address) = place(event["location"])
        return EventOption(start.date, start.time, venue, address)
    }

    /** The event's name, without a trailing " | City" that only repeats where it is. */
    private fun titleOf(event: JsonObject): String? {
        val name = event.text("name") ?: return null
        val cut = name.lastIndexOf(" | ")
        if (cut <= 0) return name
        val suffix = name.substring(cut + 3).trim().lowercase()
        val (venue, address) = place(event["location"])
        val where = listOfNotNull(venue, address).joinToString(" ").lowercase()
        return if (suffix.isNotEmpty() && suffix in where) name.substring(0, cut).trim() else name
    }

    /** Venue and address of a schema.org location. */
    private fun place(location: JsonElement?): Pair<String?, String?> = when (location) {
        is JsonArray -> place(location.firstOrNull())
        is JsonPrimitive -> location.text() to null
        is JsonObject -> if ("VirtualLocation" in location.types) "Online" to null else location.text("name") to address(location["address"])
        else -> null to null
    }

    private fun address(address: JsonElement?): String? = when (address) {
        is JsonArray -> address(address.firstOrNull())
        is JsonPrimitive -> address.text()
        is JsonObject -> listOfNotNull(address.text("streetAddress"), address.text("addressLocality"), address.text("addressRegion"))
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
        else -> null
    }

    private fun image(image: JsonElement?): String? = when (image) {
        is JsonArray -> image.firstNotNullOfOrNull { image(it) }
        is JsonPrimitive -> image.text()
        is JsonObject -> image(image["url"]) ?: image(image["contentUrl"])
        else -> null
    }
}

/** OpenGraph, Twitter card and microdata tags, for pages without event data. */
private object PreviewTags {
    fun title(doc: Document): String? =
        firstContent(doc, "meta[property=og:title]", "meta[name=twitter:title]", "meta[property=twitter:title]")
            ?: doc.title().trim().takeIf { it.isNotEmpty() }

    fun image(doc: Document): String? =
        firstContent(doc, "meta[property=og:image]", "meta[property=og:image:secure_url]", "meta[name=twitter:image]", "meta[property=twitter:image]")

    fun options(doc: Document, zone: ZoneId): List<EventOption> {
        val start = firstContent(doc, "meta[property=event:start_time]")
            ?: doc.select("[itemprop=startDate]").firstNotNullOfOrNull { element ->
                listOf(element.attr("content"), element.attr("datetime")).firstOrNull { it.isNotBlank() }
            }
        val day = start?.let { EventDates.parse(it, zone) } ?: return emptyList()
        return listOf(EventOption(day.date, day.time, null, null))
    }

    private fun firstContent(doc: Document, vararg selectors: String): String? =
        selectors.asSequence()
            .flatMap { doc.select(it).asSequence() }
            .map { it.attr("content").trim() }
            .firstOrNull { it.isNotEmpty() }
}
