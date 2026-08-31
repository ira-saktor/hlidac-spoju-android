package cz.hlidacspoju.android.monitoring

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
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
        container = AppContainer.getInstance(applicationContext)
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
                NotificationHelper.postDelayNotification(applicationContext, update, currentLanguage)
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

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MonitoringFgService"
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
