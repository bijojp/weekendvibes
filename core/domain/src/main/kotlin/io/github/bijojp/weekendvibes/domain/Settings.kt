package io.github.bijojp.weekendvibes.domain

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { System, Light, Dark }

data class Settings(
    val digestEnabled: Boolean = true,
    /** Venues here are offered first when an event page lists many. */
    val homeCity: String = "Bengaluru",
    val theme: ThemeMode = ThemeMode.System,
    /** Whether the app checks GitHub once a day and sends an Update alert for a newer release. */
    val updateAlerts: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun update(change: (Settings) -> Settings)
}
