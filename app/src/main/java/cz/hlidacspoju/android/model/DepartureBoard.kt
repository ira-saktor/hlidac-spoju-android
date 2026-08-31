package cz.hlidacspoju.android.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of GET https://api.golemio.cz/v2/pid/departureboards */
@Serializable
data class DepartureBoardResponse(
    @SerialName("departures")
    val departures: List<Departure> = emptyList()
)

@Serializable
data class Departure(
    @SerialName("stop")
    val stop: DepartureStop? = null,
    @SerialName("route")
    val route: DepartureRoute? = null,
    @SerialName("trip")
    val trip: DepartureTrip? = null,
    @SerialName("departure_timestamp")
    val departureTimestamp: DepartureStopTime? = null,
    @SerialName("delay")
    val delay: DepartureDelay? = null
)

@Serializable
data class DepartureStop(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("platform_code")
    val platformCode: String? = null
)

@Serializable
data class DepartureRoute(
    @SerialName("short_name")
    val shortName: String = "",
    @SerialName("type")
    val type: Int = 0
)

@Serializable
data class DepartureTrip(
    @SerialName("id")
    val id: String = "",
    @SerialName("headsign")
    val headsign: String? = null
)

@Serializable
data class DepartureStopTime(
    /** Scheduled or predicted departure time, ISO-8601. */
    @SerialName("predicted")
    val predicted: String? = null,
    @SerialName("scheduled")
    val scheduled: String? = null
)

@Serializable
data class DepartureDelay(
    @SerialName("is_available")
    val isAvailable: Boolean = false,
    @SerialName("minutes")
    val minutes: Int? = null,
    @SerialName("seconds")
    val seconds: Int? = null
)
