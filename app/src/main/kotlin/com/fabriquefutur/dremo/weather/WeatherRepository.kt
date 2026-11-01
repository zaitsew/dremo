package com.fabriquefutur.dremo.weather

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val Context.weatherStore by preferencesDataStore(name = "weather")

/**
 * Open-Meteo forecast and geocoding, no API key, no location permission. The
 * only thing sent is the chosen city's coordinates (or a search text). The last
 * forecast is cached on the device so the clock has a weather line at once.
 */
class WeatherRepository(context: Context) {
    private val store = context.applicationContext.weatherStore
    private val cacheKey = stringPreferencesKey("cache")
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _weather = MutableStateFlow<Weather?>(null)

    /** The latest forecast, from the cache or the network. It may belong to a city that is no longer chosen: check [matches]. */
    val weather: StateFlow<Weather?> = _weather

    /** Loads the cached forecast once, at start. */
    suspend fun loadCache() {
        val text = store.data.first()[cacheKey] ?: return
        val cached = try {
            json.decodeFromString<Weather>(text)
        } catch (_: Exception) {
            null
        }
        if (cached != null && _weather.value == null) _weather.value = cached
    }

    /** Fetches a fresh forecast for [city]. Returns true on success; on failure the old value stays. */
    suspend fun refresh(city: City, nowMs: Long = System.currentTimeMillis()): Boolean = try {
        val url = FORECAST_URL.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", city.latitude.toString())
            .addQueryParameter("longitude", city.longitude.toString())
            .addQueryParameter("current", "temperature_2m,weather_code,is_day")
            .addQueryParameter("hourly", "temperature_2m,weather_code")
            .addQueryParameter("daily", "temperature_2m_max,temperature_2m_min")
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "2")
            .build()
        val body = get(url.toString())
        val parsed = body?.let(WeatherParser::parseForecast)
        if (parsed == null) {
            false
        } else {
            val result = parsed.copy(fetchedAtMs = nowMs, latitude = city.latitude, longitude = city.longitude)
            _weather.value = result
            store.edit { it[cacheKey] = json.encodeToString(result) }
            true
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    /** Searches cities by name. Returns null when the network failed, an empty list for no match. */
    suspend fun searchCities(query: String, language: String): List<City>? {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        return try {
            val url = GEOCODING_URL.toHttpUrl().newBuilder()
                .addQueryParameter("name", trimmed)
                .addQueryParameter("count", "8")
                .addQueryParameter("language", language)
                .addQueryParameter("format", "json")
                .build()
            get(url.toString())?.let(WeatherParser::parseCities)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Dremo/0.1 (+https://github.com/zaitsew/dremo)")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }

    companion object {
        const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"

        /** True when [weather] was fetched for [city]. Coordinates are compared to three decimals. */
        fun matches(weather: Weather?, city: City?): Boolean =
            weather != null && city != null &&
                Math.abs(weather.latitude - city.latitude) < 0.001 &&
                Math.abs(weather.longitude - city.longitude) < 0.001
    }
}
