package dz.techvision.noor.data.remote.prayer

import com.google.gson.Gson
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.PrayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PrayerTimesMapperTest {

    private val mapper = PrayerTimesMapper()
    private val date = LocalDate.of(2026, 9, 24)

    private val validJson = """
        {
          "code": 200,
          "status": "OK",
          "data": {
            "timings": {
              "Fajr": "05:20", "Sunrise": "06:16", "Dhuhr": "12:44",
              "Asr": "16:30", "Sunset": "19:38", "Maghrib": "19:38", "Isha": "20:59"
            },
            "date": {
              "hijri": { "readable": "12 Rabi' al-awwal 1448" },
              "gregorian": { "date": "24-09-2026" }
            },
            "meta": {
              "latitude": 36.75, "longitude": 3.05, "timezone": "Africa/Algiers"
            }
          }
        }
    """.trimIndent()

    private fun parse(json: String): TimingsResponse = Gson().fromJson(json, TimingsResponse::class.java)

    private companion object {
        /** The `meta` fragment that echoes the requested coordinates back. */
        const val COORDINATE_ECHO = "\"latitude\": 36.75, \"longitude\": 3.05,"
    }

    @Test
    fun `maps a valid response into the seven canonical prayers in order`() {
        val schedule = mapper.map(parse(validJson), date, CalculationMethod.MUSLIM_WORLD_LEAGUE)

        assertEquals(date, schedule?.date)
        assertEquals("12 Rabi' al-awwal 1448", schedule?.hijriReadable)
        assertEquals("Africa/Algiers", schedule?.timezone)
        assertEquals(true, schedule?.isComplete)

        val names = schedule!!.prayers.map { it.prayer }
        assertEquals(
            listOf(
                PrayerName.FAJR, PrayerName.SUNRISE, PrayerName.DHUHR, PrayerName.ASR,
                PrayerName.SUNSET, PrayerName.MAGHRIB, PrayerName.ISHA,
            ),
            names,
        )

        val fajr = schedule.prayers.first { it.prayer == PrayerName.FAJR }
        assertEquals(5, fajr.time.hour)
        assertEquals(20, fajr.time.minute)
    }

    @Test
    fun `the echoed response coordinates are accepted but not required`() {
        // The AlAdhan response echoes the requested coordinates back in `meta`.
        // The mapper must tolerate them, and the domain type has nowhere to put
        // them, so they are dropped here rather than persisted downstream.
        val withCoordinates = mapper.map(parse(validJson), date, CalculationMethod.MUSLIM_WORLD_LEAGUE)
        val withoutCoordinates = mapper.map(parse(validJson.replace(COORDINATE_ECHO, "")), date, CalculationMethod.MUSLIM_WORLD_LEAGUE)

        assertNotNull(withCoordinates)
        assertNotNull(withoutCoordinates)
        assertEquals(withCoordinates!!.prayers, withoutCoordinates!!.prayers)
        assertEquals(withCoordinates.timezone, withoutCoordinates.timezone)
    }

    @Test
    fun `a response without meta coordinates still maps`() {
        val mapped = mapper.map(parse(validJson.replace(COORDINATE_ECHO, "")), date, CalculationMethod.MUSLIM_WORLD_LEAGUE)
        assertNotNull(mapped)
        assertEquals(7, mapped!!.prayers.size)
    }

    @Test
    fun `the requested calculation method is carried into the domain schedule`() {
        // The mapper must never silently switch the user's calculation method
        // to a hard-coded default.
        for (method in CalculationMethod.entries) {
            val schedule = mapper.map(parse(validJson), date, method)
            assertEquals(method, schedule?.method)
        }
    }

    @Test
    fun `returns null when the isha timing is missing`() {
        val json = validJson.replace("\"Maghrib\": \"19:38\", \"Isha\": \"20:59\"", "\"Maghrib\": \"19:38\"")
        assertNull(mapper.map(parse(json), date, CalculationMethod.MUSLIM_WORLD_LEAGUE))
    }

    @Test
    fun `returns null on an unparseable time`() {
        val json = validJson.replace("\"Fajr\": \"05:20\"", "\"Fajr\": \"zz:20\"")
        assertNull(mapper.map(parse(json), date, CalculationMethod.MUSLIM_WORLD_LEAGUE))
    }

    @Test
    fun `returns null on an invalid timezone`() {
        val json = validJson.replace("\"Africa/Algiers\"", "\"Not/AZone\"")
        assertNull(mapper.map(parse(json), date, CalculationMethod.MUSLIM_WORLD_LEAGUE))
    }

    @Test
    fun `returns null when the data payload is absent`() {
        assertNull(mapper.map(TimingsResponse(code = 200, status = "OK", data = null), date, CalculationMethod.MUSLIM_WORLD_LEAGUE))
    }
}
