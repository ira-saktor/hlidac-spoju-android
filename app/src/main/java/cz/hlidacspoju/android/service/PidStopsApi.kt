package cz.hlidacspoju.android.service

import cz.hlidacspoju.android.model.PidStopsDocument
import retrofit2.http.GET

/** Retrofit definition for the public PID stop register. No API key required. */
interface PidStopsApi {
    @GET("stops/json/stops.json")
    suspend fun getStops(): PidStopsDocument

    companion object {
        const val BASE_URL = "https://data.pid.cz/"
    }
}
