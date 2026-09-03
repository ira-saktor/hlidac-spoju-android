package cz.hlidacspoju.android.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Periodic watchdog that restarts [MonitoringForegroundService] if it isn't running.
 *
 * This is a safety net on top of `START_STICKY` and `onTaskRemoved`: some OEMs aggressively
 * kill background services and never redeliver the sticky restart. WorkManager's periodic jobs
 * are scheduled via the OS job scheduler, which survives app kills and (if the app has the
 * `RECEIVE_BOOT_COMPLETED` permission, already granted) reschedules itself after reboot.
 */
class MonitoringWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!MonitoringForegroundService.isRunning) {
            val serviceIntent = Intent(applicationContext, MonitoringForegroundService::class.java)
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "monitoring_watchdog"

        /** Schedules the periodic watchdog, replacing any existing schedule. Safe to call
         * repeatedly (e.g. on every app/service start). WorkManager enforces a 15 minute
         * minimum interval for periodic work. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonitoringWatchdogWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
