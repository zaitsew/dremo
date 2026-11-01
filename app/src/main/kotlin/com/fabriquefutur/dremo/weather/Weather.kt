package com.fabriquefutur.dremo.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/** A place chosen from the Open-Meteo geocoder. Only its name and coordinates are kept. */
@Serializable
data class City(
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
) {
    /** "Valencia, Valencian Community, Spain": the region is dropped when it only repeats the name. */
    val label: String
        get() = listOfNotNull(name, region?.takeIf { it != name }, country).joinToString(", ")
}

@Serializable
data class HourPoint(val hour: Int, val tempC: Double, val code: Int)

/**
 * One forecast, in Celsius whatever the display unit is, so the cache does not
 * depend on the setting. [fetchedAtMs] and the coordinates are filled in by the
 * repository, not by the parser.
 */
@Serializable
data class Weather(
    val tempC: Double,
    val code: Int,
    val isDay: Boolean,
    val highC: Double?,
    val lowC: Double?,
    val hours: List<HourPoint>,
    val fetchedAtMs: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

enum class Condition { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER;

    companion object {
        /** WMO weather interpretation codes, as Open-Meteo returns them. */
        fun fromCode(code: Int): Condition = when (code) {
            0, 1 -> CLEAR
            2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            in 51..57 -> DRIZZLE
            in 61..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> THUNDER
            else -> CLOUDY
        }
    }
}

enum class TempUnit(val key: String) {
    AUTO("auto"),
    CELSIUS("c"),
    FAHRENHEIT("f");

    /** The unit to show: Fahrenheit for the few countries that use it, Celsius elsewhere. */
    fun isFahrenheit(country: String): Boolean = when (this) {
        AUTO -> country.uppercase(Locale.ROOT) in FAHRENHEIT_COUNTRIES
        CELSIUS -> false
        FAHRENHEIT -> true
    }

    companion object {
        private val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")

        fun fromKey(key: String?): TempUnit = entries.firstOrNull { it.key == key } ?: AUTO

        /** "18°" or "64°", rounded to a whole degree. */
        fun format(tempC: Double, fahrenheit: Boolean): String {
            val value = if (fahrenheit) tempC * 9.0 / 5.0 + 32.0 else tempC
            val rounded = Math.round(value).toInt()
            // Avoid "-0°".
            return "${if (rounded == 0) 0 else rounded}°"
        }
    }
}

/** Parsing for the two Open-Meteo responses. Pure: it takes text and returns values. */
object WeatherParser {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Serializable
    private data class ForecastDto(
        val current: CurrentDto,
        val hourly: HourlyDto? = null,
        val daily: DailyDto? = null,
    )

    @Serializable
    private data class CurrentDto(
        val time: String,
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("weather_code") val weatherCode: Int = 0,
        @SerialName("is_day") val isDay: Int = 1,
    )

    @Serializable
    private data class HourlyDto(
        val time: List<String> = emptyList(),
        @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
        @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    )

    @Serializable
    private data class DailyDto(
        val time: List<String> = emptyList(),
        @SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
        @SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
    )

    @Serializable
    private data class GeocodingDto(val results: List<GeoResultDto> = emptyList())

    @Serializable
    private data class GeoResultDto(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val country: String? = null,
        val admin1: String? = null,
    )

    /** The hours shown after the current time. */
    const val NEXT_HOURS = 5

    /** Returns null for anything that is not a usable forecast. Never throws. */
    fun parseForecast(text: String): Weather? = try {
        val dto = json.decodeFromString<ForecastDto>(text)
        val now = dto.current.time
        val date = now.substringBefore('T')
        val dayIndex = dto.daily?.time?.indexOf(date)?.takeIf { it >= 0 } ?: 0
        val hourly = dto.hourly
        val hours = if (hourly == null) {
            emptyList()
        } else {
            // ISO local times compare correctly as text.
            val first = hourly.time.indexOfFirst { it > now }
            if (first < 0) {
                emptyList()
            } else {
                (first until minOf(first + NEXT_HOURS, hourly.time.size)).mapNotNull { i ->
                    val temp = hourly.temperature.getOrNull(i) ?: return@mapNotNull null
                    val hour = hourly.time[i].substringAfter('T').substringBefore(':').toIntOrNull()
                        ?: return@mapNotNull null
                    HourPoint(hour, temp, hourly.weatherCode.getOrNull(i) ?: 0)
                }
            }
        }
        Weather(
            tempC = dto.current.temperature,
            code = dto.current.weatherCode,
            isDay = dto.current.isDay != 0,
            highC = dto.daily?.max?.getOrNull(dayIndex),
            lowC = dto.daily?.min?.getOrNull(dayIndex),
            hours = hours,
        )
    } catch (_: Exception) {
        null
    }

    /** City matches in the geocoder's order. Empty for no match and for unreadable text. */
    fun parseCities(text: String): List<City> = try {
        json.decodeFromString<GeocodingDto>(text).results.map {
            City(it.name, it.admin1, it.country, it.latitude, it.longitude)
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** When to fetch next: the cache is good for [intervalMs]; never sooner than [MIN_WAIT_MS]. */
object RefreshPolicy {
    const val INTERVAL_MS = 30 * 60 * 1000L
    const val MIN_WAIT_MS = 60 * 1000L

    /** Milliseconds to wait before the next fetch; 0 means fetch now. */
    fun delayUntilNext(lastFetchMs: Long, nowMs: Long, intervalMs: Long = INTERVAL_MS): Long {
        if (lastFetchMs <= 0L) return 0L
        val age = nowMs - lastFetchMs
        // A clock that moved backwards makes the cache look brand new; treat that as stale.
        if (age < 0) return 0L
        return if (age >= intervalMs) 0L else maxOf(intervalMs - age, MIN_WAIT_MS)
    }
}
