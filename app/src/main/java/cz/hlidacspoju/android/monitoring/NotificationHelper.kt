package cz.hlidacspoju.android.monitoring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import cz.hlidacspoju.android.MainActivity
import cz.hlidacspoju.android.R
import cz.hlidacspoju.android.model.AppLanguage
import cz.hlidacspoju.android.service.DelayUpdate
import cz.hlidacspoju.android.service.DepartureOccurredUpdate
import cz.hlidacspoju.android.ui.Strings
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Builds and posts user-facing delay notifications and the persistent foreground-service notification. */
object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "monitoring_service"
    const val DELAY_CHANNEL_ID = "delay_updates"
    const val SERVICE_NOTIFICATION_ID = 1

    fun ensureChannels(context: Context, language: AppLanguage = AppLanguage.CZECH) {
        val strings = Strings(language)
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                strings("notification_channel_service_name"),
                NotificationManager.IMPORTANCE_MIN
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                DELAY_CHANNEL_ID,
                strings("notification_channel_delay_name"),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    fun buildServiceNotification(context: Context, language: AppLanguage = AppLanguage.CZECH): android.app.Notification {
        val strings = Strings(language)
        val openAppIntent = android.app.PendingIntent.getActivity(
            context, 0,
            android.content.Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(strings("notification_service_title"))
            .setContentText(strings("notification_service_text"))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private val departureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun postDelayNotification(context: Context, update: DelayUpdate, language: AppLanguage = AppLanguage.CZECH) {
        val strings = Strings(language)
        val scheduledTime = update.scheduledTime
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(departureTimeFormatter)
        val expectedTime = update.expectedTime
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(departureTimeFormatter)

        val text = if (update.isDelayed) {
            strings.get(
                "notification_delayed_text",
                update.headsign,
                scheduledTime,
                update.delayMinutes,
                expectedTime
            ) + if (update.otherDeparturesOnTime) strings("notification_other_on_time_suffix") else ""
        } else {
            strings.get("notification_on_time_text", update.headsign, scheduledTime)
        }

        val notification = NotificationCompat.Builder(context, DELAY_CHANNEL_ID)
            .setContentTitle(strings.get("notification_line_title", update.lineName, update.connection.stopName))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        // Use tripId (not just connection id) so multiple departures of the same connection
        // each get their own notification instead of overwriting one another.
        manager.notify((update.connection.id + update.tripId).hashCode(), notification)
    }

    fun postDepartureOccurredNotification(
        context: Context,
        update: DepartureOccurredUpdate,
        language: AppLanguage = AppLanguage.CZECH
    ) {
        val strings = Strings(language)
        val expectedTime = update.expectedTime
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(departureTimeFormatter)

        val notification = NotificationCompat.Builder(context, DELAY_CHANNEL_ID)
            .setContentTitle(strings.get("notification_line_title", update.lineName, update.connection.stopName))
            .setContentText(strings.get("notification_departed_text", update.headsign, expectedTime))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify((update.connection.id + update.tripId + "_departed").hashCode(), notification)
    }
}
