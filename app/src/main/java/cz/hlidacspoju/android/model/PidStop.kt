package cz.hlidacspoju.android.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Root document downloaded from https://data.pid.cz/stops/json/stops.json */
@Serializable
data class PidStopsDocument(
    @SerialName("stopGroups")
    val stopGroups: List<PidStopGroup> = emptyList()
)

/** A named stop (e.g. "Anděl"), grouping one or more physical stop platforms. */
@Serializable
data class PidStopGroup(
    @SerialName("name")
    val name: String = "",
    @SerialName("node")
    val node: Int = 0,
    @SerialName("municipality")
    val municipality: String? = null,
    @SerialName("stops")
    val stops: List<PidStop> = emptyList()
)

/** A single physical stop/platform within a stop group. */
@Serializable
data class PidStop(
    @SerialName("id")
    val id: String = "",
    @SerialName("platform")
    val platform: String? = null,
    @SerialName("gtfsIds")
    val gtfsIds: List<String> = emptyList(),
    @SerialName("lines")
    val lines: List<PidStopLine> = emptyList()
)

/** A transit line serving a stop, with its direction (destination headsign). */
@Serializable
data class PidStopLine(
    @SerialName("id")
    val id: Int = 0,
    @SerialName("name")
    val name: String = "",
    @SerialName("type")
    val type: String = "",
    @SerialName("direction")
    val direction: String? = null,
    @SerialName("direction2")
    val direction2: String? = null,
    @SerialName("isNight")
    val isNight: Boolean = false
)
