package io.github.bijojp.weekendvibes.domain.eventpage

import org.jsoup.nodes.Document
import java.time.ZoneId

/** Reads one site whose pages keep their event data somewhere other than JSON-LD. One file per site. */
internal interface SiteAdapter {
    /** [host] is lower case, e.g. "urbanaut.app". */
    fun handles(host: String): Boolean

    /** Null when the page doesn't have what this adapter looks for. */
    fun read(doc: Document, zone: ZoneId): EventPage?
}
