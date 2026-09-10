package dev.liamchu.glance

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md "What a watcher can be set to" — durations add up, and what is refused. */
class SettingsTest {

    private fun watcher(
        checkDays: Int = 0,
        checkHours: Int = 4,
        checkMinutes: Int = 0,
        expiryMinutes: Int = 10,
        expirySeconds: Int = 0,
    ) = Watcher(
        id = 7,
        name = "Kotlin tips",
        url = "http://example.com/glance",
        credential = "s3cret",
        checkDays = checkDays,
        checkHours = checkHours,
        checkMinutes = checkMinutes,
        expiryMinutes = expiryMinutes,
        expirySeconds = expirySeconds,
        maxLength = 120,
        lastRunFailed = false,
    )

    @Test
    fun `the interval parts add up`() {
        assertEquals(240L, watcher(checkHours = 4).intervalMinutes)
        assertEquals(30L, watcher(checkHours = 0, checkMinutes = 30).intervalMinutes)
        assertEquals(1560L, watcher(checkDays = 1, checkHours = 2, checkMinutes = 0).intervalMinutes)
        assertEquals(1591L, watcher(checkDays = 1, checkHours = 2, checkMinutes = 31).intervalMinutes)
    }

    @Test
    fun `sixty minutes and one hour are the same interval`() {
        assertEquals(
            watcher(checkHours = 1, checkMinutes = 0).intervalMinutes,
            watcher(checkHours = 0, checkMinutes = 60).intervalMinutes,
        )
    }

    @Test
    fun `the expiry parts add up`() {
        assertEquals(600_000L, watcher(expiryMinutes = 10, expirySeconds = 0).expiryMillis)
        assertEquals(30_000L, watcher(expiryMinutes = 0, expirySeconds = 30).expiryMillis)
        assertEquals(90_000L, watcher(expiryMinutes = 1, expirySeconds = 30).expiryMillis)
        assertEquals(90L, watcher(expiryMinutes = 1, expirySeconds = 30).expiryTotalSeconds)
    }

    @Test
    fun `an expiry under three seconds is refused`() {
        assertNotNull("zero would risk vanishing before the glasses see it", expiryProblem("0", "0"))
        assertNotNull(expiryProblem("0", "1"))
        assertNotNull(expiryProblem("0", "2"))
    }

    @Test
    fun `three seconds is the floor and is accepted`() {
        assertNull(expiryProblem("0", "3"))
        assertEquals(3_000L, watcher(expiryMinutes = 0, expirySeconds = 3).expiryMillis)
    }

    @Test
    fun `an empty box counts as zero`() {
        assertEquals(0, partOrNull(""))
        assertEquals(0, partOrNull("   "))
        assertNull(intervalProblem("", "1", ""))
    }

    @Test
    fun `negative and unparseable parts are refused`() {
        assertNull(partOrNull("-1"))
        assertNull(partOrNull("soon"))
        assertNotNull(expiryProblem("-1", "30"))
        assertNotNull(intervalProblem("0", "0", "later"))
    }

    @Test
    fun `a total under the WorkManager floor is refused rather than clamped`() {
        val problem = intervalProblem("0", "0", "5")
        assertNotNull("five minutes total is below the floor", problem)
        assertTrue("should say the number: $problem", problem!!.contains("15"))
        assertNotNull("nothing at all is not an interval", intervalProblem("0", "0", "0"))
        assertNotNull("one under the floor is still under it", intervalProblem("0", "0", "14"))
    }

    @Test
    fun `a total at or above the floor is accepted however it is spelled`() {
        assertNull(intervalProblem("0", "0", "15"))
        assertNull(intervalProblem("0", "1", "0"))
        assertNull(intervalProblem("1", "0", "0"))
        assertNull("a day and a bit is fine", intervalProblem("1", "2", "30"))
    }

    @Test
    fun `max length must be at least one`() {
        assertNull(maxLengthProblem("100"))
        assertNotNull(maxLengthProblem("0"))
        assertNotNull(maxLengthProblem("lots"))
    }

    @Test
    fun `a watcher must be named, so the list means something`() {
        assertNull(nameProblem("Kotlin tips"))
        assertNotNull(nameProblem(""))
        assertNotNull(nameProblem("   "))
    }

    @Test
    fun `a new watcher starts on the values DESIGN states`() {
        val fresh = Watcher.blank(1)
        assertEquals("once a day", 1440L, fresh.intervalMinutes)
        assertEquals("three seconds", 3L, fresh.expiryTotalSeconds)
        assertEquals(80, fresh.maxLength)

        // The starting values must themselves satisfy the rules the screen enforces,
        // or a new watcher would be born refusing to save.
        assertNull(intervalProblem("1", "0", "0"))
        assertNull(expiryProblem("0", "3"))
        assertNull(maxLengthProblem("80"))
    }

    @Test
    fun `a watcher survives a round trip through json`() {
        val before = watcher(checkDays = 1, checkHours = 2, checkMinutes = 3)
            .copy(lastRunFailed = true)
        val after = Watcher.fromJson(JSONObject(before.toJson().toString()))
        assertEquals(before, after)
    }

    @Test
    fun `a malformed stored watcher throws rather than becoming a half-configured one`() {
        val missingUrl = JSONObject(watcher().toJson().toString())
        missingUrl.remove("url")
        try {
            Watcher.fromJson(missingUrl)
            throw AssertionError("a missing url must not be defaulted to empty")
        } catch (expected: org.json.JSONException) {
            // C1: a missing member is an error, not a stand-in.
        }
    }
}
