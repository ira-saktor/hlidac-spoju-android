package cz.hlidacspoju.android.service

import cz.hlidacspoju.android.model.WatchedConnection
import java.time.DayOfWeek
import java.time.OffsetDateTime

/**
 * Raised after each poll cycle for a connection, summarizing the state of its departures in the
 * current time window (delayed / on time / not yet departed from the terminus).
 */
data class ConnectionStatusUpdate(
    val connection: WatchedConnection,
    val delayedCount: Int,
    val onTimeCount: Int,
    val notDepartedCount: Int,
    /** Whether the connection's day-of-week schedule includes today (and it's enabled). When
     * false, no departure data was fetched and the counts above are all zero. */
    val isScheduledToday: Boolean,
    /** True when [isScheduledToday] is true but the current time is already past the
     * connection's monitoring window end for today (so no more departures will be checked). */
    val windowPassedToday: Boolean = false,
    /** When [windowPassedToday] is true, the next day of week (if any) on which this connection
     * is scheduled to run again. */
    val nextScheduledDay: DayOfWeek? = null
)

/** Raised when a monitored trip's delay is newly known or has changed since the last check. */
data class DelayUpdate(
    val connection: WatchedConnection,
    val tripId: String,
    /** The scheduled (timetabled) departure time, unaffected by delay. */
    val scheduledTime: OffsetDateTime,
    /** The predicted/expected departure time (scheduled + delay). */
    val expectedTime: OffsetDateTime,
    val delayMinutes: Int,
    val lineName: String,
    val headsign: String,
    val isDelayed: Boolean,
    /** True if all other departures of this connection matched this poll cycle were on time. */
    val otherDeparturesOnTime: Boolean = false
)

/** Raised soon after a monitored trip is detected to have departed its origin stop. */
data class DepartureOccurredUpdate(
    val connection: WatchedConnection,
    val tripId: String,
    val expectedTime: OffsetDateTime,
    val lineName: String,
    val headsign: String
)
