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

    /** Groups notifications belonging to the same line + departure stop so the system tray can
     * bundle them together instead of listing every trip separately. */
    private fun groupKeyFor(lineName: String, stopName: String) = "line_${lineName}_stop_$stopName"

    private fun postGroupSummary(
        context: Context,
        manager: NotificationManager,
        groupKey: String,
        title: String
    ) {
        val summary = NotificationCompat.Builder(context, DELAY_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        manager.notify(groupKey.hashCode(), summary)
    }

    /** Posts a delay/on-time notification and returns its notification ID so the caller can
     * schedule an automatic dismissal once the trip is no longer relevant. */
    fun postDelayNotification(context: Context, update: DelayUpdate, language: AppLanguage = AppLanguage.CZECH): Int {
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

        val groupTitle = strings.get("notification_line_title", update.lineName, update.connection.stopName)
        val groupKey = groupKeyFor(update.lineName, update.connection.stopName)

        val notification = NotificationCompat.Builder(context, DELAY_CHANNEL_ID)
            .setContentTitle(groupTitle)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(groupKey)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        // Use tripId (not just connection id) so multiple departures of the same connection
        // each get their own notification instead of overwriting one another.
        val id = (update.connection.id + update.tripId).hashCode()
        manager.notify(id, notification)
        postGroupSummary(context, manager, groupKey, groupTitle)
        return id
    }

    /** Posts a "departed" notification and returns its notification ID so the caller can
     * schedule an automatic dismissal once the trip is no longer relevant. */
    fun postDepartureOccurredNotification(
        context: Context,
        update: DepartureOccurredUpdate,
        language: AppLanguage = AppLanguage.CZECH
    ): Int {
        val strings = Strings(language)
        val expectedTime = update.expectedTime
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(departureTimeFormatter)

        val groupTitle = strings.get("notification_line_title", update.lineName, update.connection.stopName)
        val groupKey = groupKeyFor(update.lineName, update.connection.stopName)

        val notification = NotificationCompat.Builder(context, DELAY_CHANNEL_ID)
            .setContentTitle(groupTitle)
            .setContentText(strings.get("notification_departed_text", update.headsign, expectedTime))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(groupKey)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        val id = (update.connection.id + update.tripId + "_departed").hashCode()
        manager.notify(id, notification)
        postGroupSummary(context, manager, groupKey, groupTitle)
        return id
    }

    /** Cancels [notificationId] once [dismissAt] has passed. Intended to be launched from a
     * long-lived coroutine scope (e.g. the monitoring foreground service) so old delay/departure
     * notifications don't linger indefinitely in the tray. */
    suspend fun scheduleAutoDismiss(
        context: Context,
        notificationId: Int,
        dismissAt: java.time.OffsetDateTime
    ) {
        val delayMs = java.time.Duration.between(java.time.OffsetDateTime.now(), dismissAt).toMillis()
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        context.getSystemService(NotificationManager::class.java).cancel(notificationId)
    }
}
