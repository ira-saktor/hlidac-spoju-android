package cz.hlidacspoju.android.service

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "hlidac_spoju_settings")

/** Simple manual dependency container shared by the Activity and the background service. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val configStore: ConfigStore by lazy { ConfigStore(appContext.dataStore) }

    val pidStopsService: PidStopsService by lazy {
        PidStopsService(NetworkModule.pidStopsApi, File(appContext.filesDir, "pid_stops_cache.json"))
    }

    val golemioClient: GolemioClient by lazy {
        GolemioClient(NetworkModule.golemioApi) { latestApiKey }
    }

    val monitoringService: MonitoringService by lazy {
        MonitoringService(golemioClient, settingsProvider = { configStore.load() })
    }

    /** Updated by the caller whenever settings are loaded/changed, so [golemioClient] always has
     * a fresh key without needing a suspend call from a non-suspend context. */
    @Volatile
    var latestApiKey: String = ""

    /** Whether logging (Logcat + HTTP) is currently enabled, kept in sync with settings. */
    @Volatile
    var loggingEnabled: Boolean = false
        set(value) {
            field = value
            NetworkModule.loggingEnabled = value
        }

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun getInstance(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
