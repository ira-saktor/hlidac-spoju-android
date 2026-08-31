package cz.hlidacspoju.android.monitoring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import cz.hlidacspoju.android.MainActivity
import cz.hlidacspoju.android.R
import cz.hlidacspoju.android.service.DelayUpdate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Builds and posts user-facing delay notifications and the persistent foreground-service notification. */
object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "monitoring_service"
    const val DELAY_CHANNEL_ID = "delay_updates"
    const val SERVICE_NOTIFICATION_ID = 1

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Sledování spojů",
                NotificationManager.IMPORTANCE_MIN
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                DELAY_CHANNEL_ID,
                "Zpoždění spojů",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    fun buildServiceNotification(context: Context): android.app.Notification {
        val openAppIntent = android.app.PendingIntent.getActivity(
            context, 0,
            android.content.Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Hlídač spojů")
            .setContentText("Sledování zpoždění je aktivní")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private val departureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun postDelayNotification(context: Context, update: DelayUpdate) {
        val scheduledTime = update.departureTime
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(departureTimeFormatter)

        val text = if (update.isDelayed) {
            "Odjezd ${update.headsign} v $scheduledTime má zpoždění ${update.delayMinutes} min." +
                if (update.otherDeparturesOnTime) " Ostatní spoje této linky jedou na čas." else ""
        } else {
            "Odjezd ${update.headsign} v $scheduledTime jede na čas."
        }

        val notification = NotificationCompat.Builder(context, DELAY_CHANNEL_ID)
            .setContentTitle("Linka ${update.lineName} – ${update.connection.stopName}")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(update.connection.id.hashCode(), notification)
    }
}
