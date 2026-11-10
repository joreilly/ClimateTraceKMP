package dev.johnoreilly.climatetrace.data

import dev.johnoreilly.climatetrace.remote.parseIssPosition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IssPositionTest {
    private fun parse(json: String) = parseIssPosition(Json.parseToJsonElement(json).jsonObject)

    @Test
    fun flatProxyResponseIsParsed() {
        val position = parse("""{"latitude":46.674088743121,"longitude":112.54963736534}""")
        assertEquals(46.674088743121, position.latitude)
        assertEquals(112.54963736534, position.longitude)
    }

    @Test
    fun openNotifyResponseWithStringCoordinatesIsParsed() {
        val position = parse(
            """{"iss_position": {"longitude": "-176.0657", "latitude": "-20.1270"}, "timestamp": 1794330243, "message": "success"}"""
        )
        assertEquals(-20.127, position.latitude)
        assertEquals(-176.0657, position.longitude)
        assertEquals(1794330243, position.timestamp)
    }

    @Test
    fun responseWithoutCoordinatesIsAnError() {
        assertFailsWith<NoSuchElementException> { parse("""{"message": "rate limited"}""") }
    }
}
