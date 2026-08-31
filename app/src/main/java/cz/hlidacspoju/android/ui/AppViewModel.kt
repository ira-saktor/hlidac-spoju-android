package cz.hlidacspoju.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.hlidacspoju.android.model.AppSettings
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

    fun addOrUpdateConnection(connection: WatchedConnection) = updateSettings { current ->
        val exists = current.watchedConnections.any { it.id == connection.id }
        val updated = if (exists) {
            current.watchedConnections.map { if (it.id == connection.id) connection else it }
        } else {
            current.watchedConnections + connection
        }
        current.copy(watchedConnections = updated)
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
