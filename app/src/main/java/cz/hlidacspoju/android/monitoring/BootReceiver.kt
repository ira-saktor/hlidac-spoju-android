package cz.hlidacspoju.android.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restarts the monitoring foreground service after the device reboots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, MonitoringForegroundService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
            MonitoringWatchdogWorker.schedule(context)
        }
    }
}
