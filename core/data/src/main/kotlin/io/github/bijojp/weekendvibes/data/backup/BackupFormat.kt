package io.github.bijojp.weekendvibes.data.backup

import io.github.bijojp.weekendvibes.domain.Activity
import io.github.bijojp.weekendvibes.domain.ActivityId
import io.github.bijojp.weekendvibes.domain.Agenda
import io.github.bijojp.weekendvibes.domain.ExtraDay
import io.github.bijojp.weekendvibes.domain.ReminderLead
import io.github.bijojp.weekendvibes.domain.Settings
import io.github.bijojp.weekendvibes.domain.ThemeMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/**
 * `data.json` inside the backup zip (ADR 0004): everything except the Picture files themselves,
 * readable by people as well as by Restore. Dates are ISO (2026-10-24) and times HH:mm.
 * Bump [CURRENT_FORMAT] on any incompatible change and keep reading the older versions.
 */
object BackupFormat {
    const val CURRENT_FORMAT = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /** Everything a backup restores, apart from the Picture files. */
    data class Contents(val agenda: Agenda, val settings: Settings)

    fun encode(contents: Contents, appVersion: String, createdAt: Long): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                format = CURRENT_FORMAT,
                appVersion = appVersion,
                createdAt = createdAt,
                settings = contents.settings.run { SettingsJson(digestEnabled, homeCity, theme.name, updateAlerts) },
                extraDays = contents.agenda.extraDays.map { ExtraDayJson(it.date.toString(), it.label, it.createdAt, it.updatedAt) },
                activities = contents.agenda.activities.map { it.toJson() },
            ),
        )

    /** Fails with [IllegalArgumentException] for anything that isn't a Weekend Vibes backup this version can read. */
    fun decode(text: String): Contents {
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("Not a Weekend Vibes backup", it) }
        require(file.format in 1..CURRENT_FORMAT) { "Backup format ${file.format} is newer than this app" }
        return runCatching {
            val defaults = Settings()
            Contents(
                agenda = Agenda(
                    activities = file.activities.map { it.toDomain() },
                    extraDays = file.extraDays.map { ExtraDay(LocalDate.parse(it.date), it.label, it.createdAt, it.updatedAt) },
                ),
                settings = Settings(
                    digestEnabled = file.settings.digestEnabled,
                    homeCity = file.settings.homeCity.ifBlank { defaults.homeCity },
                    theme = ThemeMode.entries.firstOrNull { it.name == file.settings.theme } ?: defaults.theme,
                    updateAlerts = file.settings.updateAlerts,
                ),
            )
        }.getOrElse { throw IllegalArgumentException("Damaged backup", it) }
    }
}

@Serializable
private data class BackupFile(
    val format: Int,
    val appVersion: String,
    val createdAt: Long,
    val settings: SettingsJson,
    val extraDays: List<ExtraDayJson> = emptyList(),
    val activities: List<ActivityJson> = emptyList(),
)

@Serializable
private data class SettingsJson(
    val digestEnabled: Boolean = true,
    val homeCity: String = "",
    val theme: String = "",
    // Backups from before 0.3.0 don't have it, so they restore with Update alerts on.
    val updateAlerts: Boolean = true,
)

@Serializable
private data class ExtraDayJson(val date: String, val label: String = "", val createdAt: Long = 0, val updatedAt: Long = 0)

@Serializable
private data class ActivityJson(
    val id: String,
    val date: String,
    val title: String,
    val time: String? = null,
    val location: String = "",
    val link: String = "",
    val notes: String = "",
    /** File name inside the zip's `pictures/` folder. */
    val picture: String? = null,
    val reminderMinutes: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

private fun Activity.toJson() = ActivityJson(
    id = id.value,
    date = date.toString(),
    title = title,
    time = time?.toString(),
    location = location,
    link = link,
    notes = notes,
    picture = picture,
    reminderMinutes = reminder?.minutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ActivityJson.toDomain() = Activity(
    id = ActivityId(id),
    date = LocalDate.parse(date),
    title = title,
    time = time?.let(LocalTime::parse),
    location = location,
    link = link,
    notes = notes,
    picture = picture,
    reminder = reminderMinutes?.let { minutes -> ReminderLead.entries.firstOrNull { it.minutes == minutes } },
    createdAt = createdAt,
    updatedAt = updatedAt,
)
