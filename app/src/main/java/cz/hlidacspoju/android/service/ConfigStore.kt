package cz.hlidacspoju.android.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cz.hlidacspoju.android.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Loads and saves [AppSettings] as JSON via DataStore Preferences. */
class ConfigStore(private val dataStore: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val settingsKey = stringPreferencesKey("app_settings_json")

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        prefs[settingsKey]?.let { raw ->
            runCatching { json.decodeFromString(AppSettings.serializer(), raw) }.getOrNull()
        } ?: AppSettings()
    }

    suspend fun load(): AppSettings = settingsFlow.first()

    suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[settingsKey] = json.encodeToString(AppSettings.serializer(), settings)
        }
    }
}
