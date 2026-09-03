package cz.hlidacspoju.android.service

import cz.hlidacspoju.android.model.AppSettings
import cz.hlidacspoju.android.model.Departure
import cz.hlidacspoju.android.model.WatchedConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.max

/**
 * Periodically checks Golemio for delay info on all currently-active watched connections and
 * emits [DelayUpdate]s when a trip's delay is new or changed.
 */
class MonitoringService(
    private val golemioClient: GolemioClient,
    private val settingsProvider: suspend () -> AppSettings,
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() }
) {
    // Keyed by (connectionId, tripId) -> last known delay in minutes, so we only notify on change.
    private val lastKnownDelays = HashMap<Pair<String, String>, Int>()

    // Keyed by (connectionId, tripId) -> already notified that the bus departed the stop.
    private val departedNotified = HashSet<Pair<String, String>>()

    private val _delayChanged = MutableSharedFlow<DelayUpdate>(extraBufferCapacity = 16)
    val delayChanged: SharedFlow<DelayUpdate> = _delayChanged

    private val _departureOccurred = MutableSharedFlow<DepartureOccurredUpdate>(extraBufferCapacity = 16)
    val departureOccurred: SharedFlow<DepartureOccurredUpdate> = _departureOccurred

    private val _pollError = MutableSharedFlow<Throwable>(extraBufferCapacity = 16)
    val pollError: SharedFlow<Throwable> = _pollError

    private val _statusUpdated = MutableSharedFlow<ConnectionStatusUpdate>(extraBufferCapacity = 16)
    val statusUpdated: SharedFlow<ConnectionStatusUpdate> = _statusUpdated

    /** Runs the poll loop forever until the coroutine is cancelled. Intended to be launched from a service. */
    suspend fun runLoop() {
        while (true) {
            try {
                pollOnce()
            } catch (ce: CancellationException) {
                throw ce
            } catch (ex: Exception) {
                _pollError.emit(ex)
            }

            val intervalSeconds = max(30, settingsProvider().pollIntervalSeconds)
            delay(intervalSeconds * 1000L)
        }
    }

    /** Runs a single poll cycle across all active connections. Public for manual "check now" / testing. */
    suspend fun pollOnce() {
        val settings = settingsProvider()
        val now = nowProvider()

        for (connection in settings.watchedConnections) {
            if (!connection.isScheduledToday(now)) {
                _statusUpdated.emit(
                    ConnectionStatusUpdate(connection, 0, 0, 0, isScheduledToday = false)
                )
                continue
            }

            if (now.toLocalTime().isAfter(connection.timeTo)) {
                _statusUpdated.emit(
                    ConnectionStatusUpdate(
                        connection,
                        0,
                        0,
                        0,
                        isScheduledToday = true,
                        windowPassedToday = true,
                        nextScheduledDay = connection.getNextScheduledDay(now.dayOfWeek)
                    )
                )
                continue
            }

            // Fetch/show upcoming departures for the rest of today's window even before it
            // starts, so the UI reflects them ahead of time. Notifications are still gated by
            // the configured lead time inside pollConnection.
            pollConnection(connection, settings.notificationLeadTimeMinutes)
        }

        // Forget delay history for connections the user has since removed.
        val knownConnectionIds = settings.watchedConnections.map { it.id }.toHashSet()
        val orphanedKeys = lastKnownDelays.keys.filter { it.first !in knownConnectionIds }
        orphanedKeys.forEach { lastKnownDelays.remove(it) }
        departedNotified.removeAll { it.first !in knownConnectionIds }
    }

    private suspend fun pollConnection(connection: WatchedConnection, notificationLeadTimeMinutes: Int) {
        if (connection.gtfsStopIds.isEmpty()) return

        // minutesBefore keeps recently-departed trips in the response for a short while so we
        // can detect and notify that they've actually left the stop, instead of them silently
        // disappearing from the board the moment they depart.
        val departures = golemioClient.getDepartures(
            connection.gtfsStopIds,
            minutesBefore = DEPARTED_LOOKBACK_MINUTES,
            minutesAfter = MAX_LOOKAHEAD_MINUTES,
            limit = 100
        )

        val allMatching = departures.filter { matchesConnection(connection, it) }
        val matching = allMatching.filter { it.delay?.isAvailable == true && it.delay.minutes != null }

        val now = OffsetDateTime.now()

        val delayedCount = matching.count { it.delay!!.minutes!! > ON_TIME_THRESHOLD_MINUTES }
        val onTimeCount = matching.count { it.delay!!.minutes!! <= ON_TIME_THRESHOLD_MINUTES }
        val notDepartedCount = allMatching.size - matching.size

        val currentTripIds = matching.map { it.trip?.id ?: "" }.toHashSet()
        val staleKeys = lastKnownDelays.keys.filter { it.first == connection.id && it.second !in currentTripIds }
        staleKeys.forEach { lastKnownDelays.remove(it) }
        departedNotified.removeAll { it.first == connection.id && it.second !in currentTripIds }

        _statusUpdated.emit(
            ConnectionStatusUpdate(connection, delayedCount, onTimeCount, notDepartedCount, isScheduledToday = true)
        )

        for (departure in matching) {
            val delayMinutes = departure.delay!!.minutes!!
            val tripId = departure.trip?.id ?: ""
            val key = connection.id to tripId

            val scheduledTime = parseTimestamp(departure.departureTimestamp?.scheduled)
                ?: parseTimestamp(departure.departureTimestamp?.predicted)
                ?: now
            val expectedTime = parseTimestamp(departure.departureTimestamp?.predicted)
                ?: scheduledTime

            val lineName = departure.route?.shortName ?: connection.lineName
            val headsign = departure.trip?.headsign ?: connection.direction

            // Once the expected departure time has passed, treat the trip as departed: notify
            // once and stop emitting further delay updates for it.
            if (!now.isBefore(expectedTime)) {
                if (departedNotified.add(key)) {
                    _departureOccurred.emit(
                        DepartureOccurredUpdate(
                            connection = connection,
                            tripId = tripId,
                            expectedTime = expectedTime,
                            lineName = lineName,
                            headsign = headsign
                        )
                    )
                }
                continue
            }

            // Only notify once the departure is within the configured lead time; further-out
            // departures are still reflected in the status counts above but stay silent so the
            // user isn't alerted a day in advance.
            val minutesUntilDeparture = java.time.Duration.between(now, expectedTime).toMinutes()
            if (minutesUntilDeparture > notificationLeadTimeMinutes) continue

            if (lastKnownDelays[key] == delayMinutes) continue
            lastKnownDelays[key] = delayMinutes

            val otherDeparturesOnTime = matching
                .filter { (it.trip?.id ?: "") != tripId }
                .all { it.delay!!.minutes!! <= ON_TIME_THRESHOLD_MINUTES }

            _delayChanged.emit(
                DelayUpdate(
                    connection = connection,
                    tripId = tripId,
                    scheduledTime = scheduledTime,
                    expectedTime = expectedTime,
                    delayMinutes = delayMinutes,
                    lineName = lineName,
                    headsign = headsign,
                    isDelayed = delayMinutes > ON_TIME_THRESHOLD_MINUTES,
                    otherDeparturesOnTime = matching.size > 1 && otherDeparturesOnTime
                )
            )
        }
    }

    private fun matchesConnection(connection: WatchedConnection, departure: Departure): Boolean {
        val lineMatches = departure.route?.shortName?.equals(connection.lineName, ignoreCase = true) == true
        val directionMatches = connection.direction.isBlank() ||
            departure.trip?.headsign?.contains(connection.direction, ignoreCase = true) == true

        val scheduled = parseTimestamp(departure.departureTimestamp?.predicted)
            ?: parseTimestamp(departure.departureTimestamp?.scheduled)
        val timeMatches = scheduled != null &&
            isWithinWindow(scheduled.toLocalTime(), connection.timeFrom, connection.timeTo)

        return lineMatches && directionMatches && timeMatches
    }

    private fun isWithinWindow(time: LocalTime, from: LocalTime, to: LocalTime): Boolean =
        !time.isBefore(from) && !time.isAfter(to)

    private fun parseTimestamp(raw: String?): OffsetDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            OffsetDateTime.parse(raw)
        }.getOrElse {
            runCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).toOffsetDateTime() }.getOrNull()
        }
    }

    companion object {
        /** Delay (in minutes) at or below which a departure is considered "on time". */
        private const val ON_TIME_THRESHOLD_MINUTES = 1

        /** How far ahead (in minutes) to fetch upcoming departures, so the UI can show
         * connections in the monitored window up to 24h in advance. */
        private const val MAX_LOOKAHEAD_MINUTES = 24 * 60

        /** How far back (in minutes) to still fetch already-departed trips, so a "bus departed"
         * notification can be sent shortly after it leaves the stop. */
        private const val DEPARTED_LOOKBACK_MINUTES = 10
    }
}
