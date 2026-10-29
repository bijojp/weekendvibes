package io.github.bijojp.weekendvibes.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.bijojp.weekendvibes.domain.Settings
import io.github.bijojp.weekendvibes.domain.SettingsRepository
import io.github.bijojp.weekendvibes.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

private val DIGEST_ENABLED = booleanPreferencesKey("digest_enabled")
private val HOME_CITY = stringPreferencesKey("home_city")
private val THEME = stringPreferencesKey("theme")
private val UPDATE_ALERTS = booleanPreferencesKey("update_alerts")

internal class DataStoreSettingsRepository(context: Context) : SettingsRepository {
    private val store = context.settingsStore

    override val settings: Flow<Settings> = store.data.map { it.toSettings() }

    override suspend fun update(change: (Settings) -> Settings) {
        store.edit { prefs ->
            val settings = change(prefs.toSettings())
            prefs[DIGEST_ENABLED] = settings.digestEnabled
            prefs[HOME_CITY] = settings.homeCity
            prefs[THEME] = settings.theme.name
            prefs[UPDATE_ALERTS] = settings.updateAlerts
        }
    }
}

private fun Preferences.toSettings(): Settings {
    val defaults = Settings()
    return Settings(
        digestEnabled = this[DIGEST_ENABLED] ?: defaults.digestEnabled,
        homeCity = this[HOME_CITY] ?: defaults.homeCity,
        theme = this[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: defaults.theme,
        updateAlerts = this[UPDATE_ALERTS] ?: defaults.updateAlerts,
    )
}
