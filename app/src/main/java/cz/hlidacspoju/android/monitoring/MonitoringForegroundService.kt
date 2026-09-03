package cz.hlidacspoju.android.monitoring

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import cz.hlidacspoju.android.model.AppLanguage
import cz.hlidacspoju.android.service.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Keeps [cz.hlidacspoju.android.service.MonitoringService] running while the app is backgrounded. */
class MonitoringForegroundService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)
    private lateinit var container: AppContainer

    @Volatile
    private var currentLanguage: AppLanguage = AppLanguage.CZECH

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        container = AppContainer.getInstance(applicationContext)
        MonitoringWatchdogWorker.schedule(applicationContext)
        NotificationHelper.ensureChannels(applicationContext, currentLanguage)
        scope.launch {
            runCatching { container.configStore.load() }.getOrNull()?.let {
                container.loggingEnabled = it.loggingEnabled
                currentLanguage = it.language
                NotificationHelper.ensureChannels(applicationContext, currentLanguage)
            }
        }
        scope.launch {
            container.configStore.settingsFlow.collect { currentLanguage = it.language }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildServiceNotification(applicationContext, currentLanguage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        scope.launch {
            container.monitoringService.delayChanged.collect { update ->
                val id = NotificationHelper.postDelayNotification(applicationContext, update, currentLanguage)
                scope.launch {
                    NotificationHelper.scheduleAutoDismiss(applicationContext, id, update.expectedTime.plusMinutes(AUTO_DISMISS_MINUTES))
                }
            }
        }
        scope.launch {
            container.monitoringService.departureOccurred.collect { update ->
                val id = NotificationHelper.postDepartureOccurredNotification(applicationContext, update, currentLanguage)
                scope.launch {
                    NotificationHelper.scheduleAutoDismiss(applicationContext, id, update.expectedTime.plusMinutes(AUTO_DISMISS_MINUTES))
                }
            }
        }
        scope.launch {
            container.monitoringService.pollError.collect { error ->
                if (container.loggingEnabled) Log.w(TAG, "Poll error", error)
            }
        }
        scope.launch {
            container.monitoringService.runLoop()
        }

        return START_STICKY
    }

    /** Some OEMs kill the whole task (including services) when the user swipes the app away
     * from Recents. Restarting the service here ensures monitoring keeps running. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, MonitoringForegroundService::class.java)
        ContextCompat.startForegroundService(applicationContext, restartIntent)
    }

    override fun onDestroy() {
        isRunning = false
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MonitoringFgService"

        /** Old delay/departure notifications auto-dismiss this many minutes after the (delay-adjusted)
         * departure time, so the tray doesn't fill up with stale trips. */
        private const val AUTO_DISMISS_MINUTES = 10L

        /** Tracked so the [cz.hlidacspoju.android.monitoring.MonitoringWatchdogWorker] can tell
         * whether the service needs to be restarted. */
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
