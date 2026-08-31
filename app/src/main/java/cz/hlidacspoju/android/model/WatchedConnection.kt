package cz.hlidacspoju.android.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** A connection the user wants to be notified about (line + origin stop + direction + time window). */
@Serializable
data class WatchedConnection(
    val id: String = UUID.randomUUID().toString(),
    /** Display name, e.g. "191 Rozýnova → Anděl". */
    val name: String = "",
    /** Line short name, e.g. "191". */
    val lineName: String = "",
    /** Name of the origin stop group, e.g. "Rozýnova". */
    val stopName: String = "",
    /** GTFS stop ids to query (all platforms of the stop group that serve the line). */
    val gtfsStopIds: List<String> = emptyList(),
    /** Expected headsign/direction, e.g. "Anděl". */
    val direction: String = "",
    /** Days of week this connection should be monitored, stored as ISO day-of-week names. */
    val days: Set<@Serializable(with = DayOfWeekSerializer::class) DayOfWeek> = emptySet(),
    /** Start of the monitoring time window (local time). */
    @Serializable(with = LocalTimeSerializer::class)
    val timeFrom: LocalTime = LocalTime.of(8, 0),
    /** End of the monitoring time window (local time). */
    @Serializable(with = LocalTimeSerializer::class)
    val timeTo: LocalTime = LocalTime.of(9, 0),
    val isEnabled: Boolean = true
) {
    /** Whether this connection is enabled and its day-of-week schedule includes today. */
    fun isScheduledToday(localNow: LocalDateTime): Boolean =
        isEnabled && days.contains(localNow.dayOfWeek)

    fun isActiveNow(localNow: LocalDateTime): Boolean {
        if (!isScheduledToday(localNow)) return false
        val time = localNow.toLocalTime()
        return !time.isBefore(timeFrom) && !time.isAfter(timeTo)
    }

    /**
     * Finds the next day (strictly after [today], wrapping within a week) on which this
     * connection is scheduled to run, or null if it's disabled or has no monitored days at all.
     */
    fun getNextScheduledDay(today: DayOfWeek): DayOfWeek? {
        if (!isEnabled || days.isEmpty()) return null

        for (offset in 1..7) {
            val candidate = DayOfWeek.of(((today.value - 1 + offset) % 7) + 1)
            if (days.contains(candidate)) return candidate
        }
        return null
    }
}
