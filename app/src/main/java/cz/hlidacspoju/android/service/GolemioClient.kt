package cz.hlidacspoju.android.service

import cz.hlidacspoju.android.model.Departure

/** Client for the Golemio Output Gateway PID Departure Boards endpoint. */
class GolemioClient(
    private val api: GolemioApi,
    private val apiKeyProvider: () -> String
) {
    /** Fetches upcoming departures for the given GTFS stop ids. */
    suspend fun getDepartures(
        gtfsStopIds: List<String>,
        minutesBefore: Int = 0,
        minutesAfter: Int = 120,
        limit: Int = 20
    ): List<Departure> {
        val apiKey = apiKeyProvider()
        check(apiKey.isNotBlank()) { "Golemio API key is not configured." }

        val response = api.getDepartureBoards(
            apiKey = apiKey,
            gtfsStopIds = gtfsStopIds,
            minutesBefore = minutesBefore,
            minutesAfter = minutesAfter,
            limit = limit
        )
        return response.departures
    }
}
