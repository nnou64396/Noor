package com.noor.islamic.data.remote.prayer

import com.google.gson.Gson
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.PrayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PrayerTimesMapperTest {

    private val mapper = PrayerTimesMapper()
    private val date = LocalDate.of(2026, 9, 24)
    private val point = GeographicPoint(36.75, 3.05)

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

    @Test
    fun `maps a valid response into the seven canonical prayers in order`() {
        val schedule = mapper.map(parse(validJson), date, point)

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
    fun `falls back to the requested point when meta coordinates are absent`() {
        val json = validJson.replace("\"latitude\": 36.75, \"longitude\": 3.05,", "")
        val mapped = mapper.map(parse(json), date, point)
        assertNotNull(mapped)
        assertEquals(point.latitude, mapped!!.latitude, 1e-9)
        assertEquals(point.longitude, mapped!!.longitude, 1e-9)
    }

    @Test
    fun `returns null when the isha timing is missing`() {
        val json = validJson.replace("\"Maghrib\": \"19:38\", \"Isha\": \"20:59\"", "\"Maghrib\": \"19:38\"")
        assertNull(mapper.map(parse(json), date, point))
    }

    @Test
    fun `returns null on an unparseable time`() {
        val json = validJson.replace("\"Fajr\": \"05:20\"", "\"Fajr\": \"zz:20\"")
        assertNull(mapper.map(parse(json), date, point))
    }

    @Test
    fun `returns null on an invalid timezone`() {
        val json = validJson.replace("\"Africa/Algiers\"", "\"Not/AZone\"")
        assertNull(mapper.map(parse(json), date, point))
    }

    @Test
    fun `returns null when the data payload is absent`() {
        assertNull(mapper.map(TimingsResponse(code = 200, status = "OK", data = null), date, point))
    }
}