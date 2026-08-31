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
    val stopsLastUpdatedUtcMillis: Long? = null,
    /** Whether the app is allowed to write logs (Logcat + HTTP request/response logging).
     * Should be turned off for Play Store releases handling sensitive data. */
    val loggingEnabled: Boolean = false,
    /** How many minutes before a monitored departure a delay notification may be sent.
     * Departures further away than this are still shown in the UI but don't trigger a notification. */
    val notificationLeadTimeMinutes: Int = 30
)
