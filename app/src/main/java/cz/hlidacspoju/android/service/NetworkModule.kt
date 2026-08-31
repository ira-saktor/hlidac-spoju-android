package cz.hlidacspoju.android.service

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Builds shared OkHttp/Retrofit instances used by [GolemioApi] and [PidStopsApi]. */
object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    /** Toggled from [AppContainer] whenever settings load/change, so HTTP logging respects the
     * user's logging preference (must be off for Play Store releases handling sensitive data). */
    var loggingEnabled: Boolean = false
        set(value) {
            field = value
            loggingInterceptor.level =
                if (value) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val golemioApi: GolemioApi by lazy {
        Retrofit.Builder()
            .baseUrl(GolemioApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GolemioApi::class.java)
    }

    val pidStopsApi: PidStopsApi by lazy {
        Retrofit.Builder()
            .baseUrl(PidStopsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PidStopsApi::class.java)
    }
}
