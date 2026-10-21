package io.github.bijojp.weekendvibes.domain

import io.github.bijojp.weekendvibes.domain.eventpage.parseJsonOrNull
import io.github.bijojp.weekendvibes.domain.eventpage.text
import kotlinx.serialization.json.JsonObject

/** A published version of the app and the web page it can be downloaded from. */
data class Release(val version: String, val page: String)

/** The rules behind Check for updates (ADR 0002): reading GitHub's answer and comparing versions. */
object Updates {
    /** The release in GitHub's `releases/latest` JSON, or null when it doesn't describe one. */
    fun parseLatest(json: String): Release? {
        val release = parseJsonOrNull(json) as? JsonObject ?: return null
        val version = release.text("tag_name")?.removePrefix("v") ?: return null
        val page = release.text("html_url")?.takeIf { it.startsWith("https://") } ?: return null
        return Release(version, page)
    }

    /** Whether [candidate] is a later version than [current]. Only the dotted numbers count, so "0.2.0" beats "0.1.0-debug". */
    fun isNewer(candidate: String, current: String): Boolean {
        val new = numbers(candidate) ?: return false
        val old = numbers(current) ?: return false
        for (i in 0 until maxOf(new.size, old.size)) {
            val order = new.getOrElse(i) { 0 }.compareTo(old.getOrElse(i) { 0 })
            if (order != 0) return order > 0
        }
        return false
    }

    private fun numbers(version: String): List<Int>? {
        val parts = version.trim().removePrefix("v").substringBefore('-').substringBefore('+').split('.')
        return parts.map { it.toIntOrNull() ?: return null }
    }
}
