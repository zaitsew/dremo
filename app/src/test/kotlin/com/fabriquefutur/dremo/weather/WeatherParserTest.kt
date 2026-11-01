package com.fabriquefutur.dremo.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fixture(name: String): String =
    WeatherParserTest::class.java.classLoader!!.getResource("fixtures/$name")!!.readText()

class WeatherParserTest {
    @Test
    fun `parses the current conditions`() {
        val w = WeatherParser.parseForecast(fixture("forecast.json"))!!
        assertEquals(14.6, w.tempC, 0.0001)
        assertEquals(2, w.code)
        assertFalse(w.isDay)
    }

    @Test
    fun `takes today's high and low`() {
        val w = WeatherParser.parseForecast(fixture("forecast.json"))!!
        assertEquals(19.8, w.highC!!, 0.0001)
        assertEquals(11.2, w.lowC!!, 0.0001)
    }

    @Test
    fun `next hours start after the current time`() {
        val w = WeatherParser.parseForecast(fixture("forecast.json"))!!
        assertEquals(WeatherParser.NEXT_HOURS, w.hours.size)
        // The current time is 22:15, so the first hour shown is 23:00, then it wraps to the next day.
        assertEquals(listOf(23, 0, 1, 2, 3), w.hours.map { it.hour })
    }

    @Test
    fun `a forecast with only current data still parses`() {
        val w = WeatherParser.parseForecast(fixture("forecast_minimal.json"))!!
        assertEquals(-0.2, w.tempC, 0.0001)
        assertEquals(71, w.code)
        assertTrue(w.isDay)
        assertNull(w.highC)
        assertTrue(w.hours.isEmpty())
    }

    @Test
    fun `garbage is not a forecast`() {
        assertNull(WeatherParser.parseForecast("not json"))
        assertNull(WeatherParser.parseForecast("{}"))
        assertNull(WeatherParser.parseForecast(""))
    }

    @Test
    fun `parses city matches in order`() {
        val cities = WeatherParser.parseCities(fixture("geocoding.json"))
        assertEquals(4, cities.size)
        assertEquals("Valencia", cities[0].name)
        assertEquals(39.46975, cities[0].latitude, 0.00001)
        assertEquals("Valencia, Valencian Community, Spain", cities[0].label)
        assertEquals("Valencia, Carabobo, Venezuela", cities[1].label)
    }

    @Test
    fun `a region that repeats the name is dropped from the label`() {
        val madrid = WeatherParser.parseCities(fixture("geocoding.json"))[2]
        assertEquals("Madrid, Spain", madrid.label)
    }

    @Test
    fun `a city without region or country is just its name`() {
        assertEquals("Nowhere", WeatherParser.parseCities(fixture("geocoding.json"))[3].label)
    }

    @Test
    fun `no results is an empty list`() {
        assertTrue(WeatherParser.parseCities(fixture("geocoding_empty.json")).isEmpty())
        assertTrue(WeatherParser.parseCities("oops").isEmpty())
    }
}

class ConditionTest {
    @Test
    fun `maps WMO codes`() {
        assertEquals(Condition.CLEAR, Condition.fromCode(0))
        assertEquals(Condition.PARTLY_CLOUDY, Condition.fromCode(2))
        assertEquals(Condition.CLOUDY, Condition.fromCode(3))
        assertEquals(Condition.FOG, Condition.fromCode(45))
        assertEquals(Condition.DRIZZLE, Condition.fromCode(53))
        assertEquals(Condition.RAIN, Condition.fromCode(63))
        assertEquals(Condition.RAIN, Condition.fromCode(81))
        assertEquals(Condition.SNOW, Condition.fromCode(75))
        assertEquals(Condition.THUNDER, Condition.fromCode(95))
        assertEquals(Condition.CLOUDY, Condition.fromCode(1234))
    }
}

class TempUnitTest {
    @Test
    fun `formats whole degrees`() {
        assertEquals("19°", TempUnit.format(18.6, false))
        assertEquals("-3°", TempUnit.format(-2.6, false))
        assertEquals("0°", TempUnit.format(-0.2, false))
    }

    @Test
    fun `converts to Fahrenheit`() {
        assertEquals("32°", TempUnit.format(0.0, true))
        assertEquals("68°", TempUnit.format(20.0, true))
    }

    @Test
    fun `automatic unit follows the region`() {
        assertTrue(TempUnit.AUTO.isFahrenheit("US"))
        assertTrue(TempUnit.AUTO.isFahrenheit("us"))
        assertFalse(TempUnit.AUTO.isFahrenheit("ES"))
        assertFalse(TempUnit.AUTO.isFahrenheit(""))
        assertTrue(TempUnit.FAHRENHEIT.isFahrenheit("ES"))
        assertFalse(TempUnit.CELSIUS.isFahrenheit("US"))
    }

    @Test
    fun `unknown keys fall back to automatic`() {
        assertEquals(TempUnit.AUTO, TempUnit.fromKey("kelvin"))
        assertEquals(TempUnit.CELSIUS, TempUnit.fromKey("c"))
    }
}

class RefreshPolicyTest {
    private val min = 60_000L

    @Test
    fun `never fetched means fetch now`() {
        assertEquals(0L, RefreshPolicy.delayUntilNext(0L, 1_000_000L))
    }

    @Test
    fun `fresh cache waits out the rest of the interval`() {
        assertEquals(20 * min, RefreshPolicy.delayUntilNext(1_000L, 1_000L + 10 * min))
    }

    @Test
    fun `stale cache fetches now`() {
        assertEquals(0L, RefreshPolicy.delayUntilNext(1_000L, 1_000L + 30 * min))
        assertEquals(0L, RefreshPolicy.delayUntilNext(1_000L, 1_000L + 90 * min))
    }

    @Test
    fun `never waits less than a minute`() {
        assertEquals(RefreshPolicy.MIN_WAIT_MS, RefreshPolicy.delayUntilNext(1_000L, 1_000L + 30 * min - 5_000L))
    }

    @Test
    fun `a clock that went backwards counts as stale`() {
        assertEquals(0L, RefreshPolicy.delayUntilNext(10_000_000L, 5_000L))
    }
}

class WeatherMatchTest {
    private val city = City("Valencia", null, "Spain", 39.46975, -0.37739)
    private val weather = Weather(10.0, 0, true, null, null, emptyList(), 1L, 39.46975, -0.37739)

    @Test
    fun `matches the city it was fetched for`() {
        assertTrue(WeatherRepository.matches(weather, city))
    }

    @Test
    fun `does not match another city, a missing city or no weather`() {
        assertFalse(WeatherRepository.matches(weather, city.copy(latitude = 40.4, longitude = -3.7)))
        assertFalse(WeatherRepository.matches(weather, null))
        assertFalse(WeatherRepository.matches(null, city))
        assertNotNull(weather)
    }
}
