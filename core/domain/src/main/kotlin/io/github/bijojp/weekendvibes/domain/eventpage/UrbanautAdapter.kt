package io.github.bijojp.weekendvibes.domain.eventpage

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.jsoup.nodes.Document
import java.time.ZoneId

/** Urbanaut has no JSON-LD; its Angular page state (`script#ng-state`) holds the spot's details. */
internal object UrbanautAdapter : SiteAdapter {
    override fun handles(host: String) = host == "urbanaut.app" || host.endsWith(".urbanaut.app")

    override fun read(doc: Document, zone: ZoneId): EventPage? {
        val state = doc.getElementById("ng-state")?.data()?.let(::parseJsonOrNull) as? JsonObject ?: return null
        val spot = state.entries.firstOrNull { it.key.startsWith("spot_detail") }?.value as? JsonObject ?: return null
        // A series has its next session apart from its first one.
        val seconds = spot.long("upcoming_session_timestamp") ?: spot.long("start_timestamp")
        val option = seconds?.let { EventDates.fromEpochSeconds(it, zone) }
            ?.let { EventOption(it.date, it.time, spot.text("venue"), spot.text("address")) }
        val image = (spot["medias"] as? JsonArray)?.firstNotNullOfOrNull { (it as? JsonObject)?.text("path_url") }
        return EventPage(spot.text("name"), image, listOfNotNull(option))
    }
}
