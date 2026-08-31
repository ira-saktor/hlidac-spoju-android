package cz.hlidacspoju.android.model

import kotlinx.serialization.Serializable

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class AppLanguage { CZECH, ENGLISH, RUSSIAN, GERMAN, FRENCH }

/** Persisted application settings, stored as JSON via DataStore. */
@Serializable
data class AppSettings(
    val golemioApiKey: String = "",
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.CZECH,
    /** How often (in seconds) to poll Golemio for delay updates. */
    val pollIntervalSeconds: Int = 120,
    /** How often (in days) to refresh the cached PID stops list. */
    val stopsRefreshIntervalDays: Int = 1,
    /** Whether the user has already gone through (or explicitly skipped) the onboarding wizard. */
    val onboardingCompleted: Boolean = false,
    val watchedConnections: List<WatchedConnection> = emptyList(),
    /** Epoch millis of the last successful stops refresh, or null if never refreshed. */
    val stopsLastUpdatedUtcMillis: Long? = null
)
