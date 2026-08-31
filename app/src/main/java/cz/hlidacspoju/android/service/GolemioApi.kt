package cz.hlidacspoju.android.service

import cz.hlidacspoju.android.model.DepartureBoardResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/** Retrofit definition for the Golemio Output Gateway PID Departure Boards endpoint. */
interface GolemioApi {
    @GET("v2/pid/departureboards")
    suspend fun getDepartureBoards(
        @Header("X-Access-Token") apiKey: String,
        @Query("ids") gtfsStopIds: List<String>,
        @Query("minutesBefore") minutesBefore: Int = 0,
        @Query("minutesAfter") minutesAfter: Int = 120,
        @Query("limit") limit: Int = 20,
        @Query("order") order: String = "real"
    ): DepartureBoardResponse

    companion object {
        const val BASE_URL = "https://api.golemio.cz/"
    }
}
