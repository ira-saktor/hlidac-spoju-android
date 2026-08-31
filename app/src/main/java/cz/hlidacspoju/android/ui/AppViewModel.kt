package cz.hlidacspoju.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.hlidacspoju.android.model.AppSettings
import cz.hlidacspoju.android.model.PidStopGroup
import cz.hlidacspoju.android.model.PidStopsDocument
import cz.hlidacspoju.android.model.WatchedConnection
import cz.hlidacspoju.android.service.AppContainer
import cz.hlidacspoju.android.service.ConnectionStatusUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds [AppSettings] state for the Compose UI and mediates changes back to [AppContainer]. */
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings

    // Latest status per connection id, updated by the background monitoring service's flow.
    private val _statusByConnectionId = MutableStateFlow<Map<String, ConnectionStatusUpdate>>(emptyMap())
    val statusByConnectionId: StateFlow<Map<String, ConnectionStatusUpdate>> = _statusByConnectionId

    init {
        viewModelScope.launch {
            container.configStore.settingsFlow.collect { loaded ->
                _settings.value = loaded
                container.latestApiKey = loaded.golemioApiKey
                container.loggingEnabled = loaded.loggingEnabled
            }
        }
        viewModelScope.launch {
            container.monitoringService.statusUpdated.collect { update ->
                _statusByConnectionId.update { it + (update.connection.id to update) }
            }
        }
    }

    fun completeOnboarding(apiKey: String) = updateSettings {
        it.copy(golemioApiKey = apiKey, onboardingCompleted = true)
    }

    fun addOrUpdateConnection(connection: WatchedConnection) {
        viewModelScope.launch {
            val current = _settings.value
            val exists = current.watchedConnections.any { it.id == connection.id }
            val updatedConnections = if (exists) {
                current.watchedConnections.map { if (it.id == connection.id) connection else it }
            } else {
                current.watchedConnections + connection
            }
            val updated = current.copy(watchedConnections = updatedConnections)
            _settings.value = updated
            container.latestApiKey = updated.golemioApiKey
            container.loggingEnabled = updated.loggingEnabled
            container.configStore.save(updated)

            // Immediately check status for the new/edited connection so the UI doesn't show
            // "no connection watching is set for today" until the next scheduled poll.
            runCatching { container.monitoringService.pollOnce() }
        }
    }

    fun removeConnection(connectionId: String) = updateSettings { current ->
        current.copy(watchedConnections = current.watchedConnections.filterNot { it.id == connectionId })
    }

    fun setConnectionEnabled(connectionId: String, enabled: Boolean) = updateSettings { current ->
        current.copy(
            watchedConnections = current.watchedConnections.map {
                if (it.id == connectionId) it.copy(isEnabled = enabled) else it
            }
        )
    }

    fun updateApiKey(apiKey: String) = updateSettings { it.copy(golemioApiKey = apiKey) }

    fun updatePollIntervalSeconds(seconds: Int) = updateSettings { it.copy(pollIntervalSeconds = seconds) }

    fun updateLanguage(language: cz.hlidacspoju.android.model.AppLanguage) = updateSettings { it.copy(language = language) }

    fun updateTheme(theme: cz.hlidacspoju.android.model.AppTheme) = updateSettings { it.copy(theme = theme) }

    fun updateLoggingEnabled(enabled: Boolean) = updateSettings { it.copy(loggingEnabled = enabled) }

    fun updateNotificationLeadTimeMinutes(minutes: Int) =
        updateSettings { it.copy(notificationLeadTimeMinutes = minutes) }

    /** Loads (from cache or network) the PID stop register used to power stop/line autocomplete. */
    suspend fun loadPidStops(): PidStopsDocument = container.pidStopsService.getCached()

    /** Diacritics-insensitive substring search over stop group names. */
    fun searchStopGroups(doc: PidStopsDocument, query: String): List<PidStopGroup> =
        container.pidStopsService.searchStopGroups(doc, query)

    fun checkNow() {
        viewModelScope.launch {
            runCatching { container.monitoringService.pollOnce() }
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = transform(_settings.value)
            _settings.value = updated
            container.latestApiKey = updated.golemioApiKey
            container.loggingEnabled = updated.loggingEnabled
            container.configStore.save(updated)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(container) as T
            }
    }
}
