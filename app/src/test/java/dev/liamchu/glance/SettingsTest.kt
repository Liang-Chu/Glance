package dev.liamchu.glance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md "What the user can change" — durations add up, and what is refused. */
class SettingsTest {

    private fun settings(
        checkDays: Int = 0,
        checkHours: Int = 4,
        checkMinutes: Int = 0,
        expiryMinutes: Int = 10,
        expirySeconds: Int = 0,
    ) = Settings(
        url = "http://example.com/glance",
        credential = "",
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
        assertEquals(240L, settings(checkHours = 4).intervalMinutes)
        assertEquals(30L, settings(checkHours = 0, checkMinutes = 30).intervalMinutes)
        assertEquals(1560L, settings(checkDays = 1, checkHours = 2, checkMinutes = 0).intervalMinutes)
        assertEquals(1591L, settings(checkDays = 1, checkHours = 2, checkMinutes = 31).intervalMinutes)
    }

    @Test
    fun `sixty minutes and one hour are the same interval`() {
        assertEquals(
            settings(checkHours = 1, checkMinutes = 0).intervalMinutes,
            settings(checkHours = 0, checkMinutes = 60).intervalMinutes,
        )
    }

    @Test
    fun `expiry total seconds add up`() {
        assertEquals(90L, settings(expiryMinutes = 1, expirySeconds = 30).expiryTotalSeconds)
    }

    @Test
    fun `the expiry parts add up`() {
        assertEquals(600_000L, settings(expiryMinutes = 10, expirySeconds = 0).expiryMillis)
        assertEquals(30_000L, settings(expiryMinutes = 0, expirySeconds = 30).expiryMillis)
        assertEquals(90_000L, settings(expiryMinutes = 1, expirySeconds = 30).expiryMillis)
    }

    @Test
    fun `an expiry under three seconds is refused`() {
        assertNotNull("zero would risk vanishing before the glasses see it",
            expiryProblem("0", "0"))
        assertNotNull(expiryProblem("0", "1"))
        assertNotNull(expiryProblem("0", "2"))
    }

    @Test
    fun `three seconds is the floor and is accepted`() {
        assertNull(expiryProblem("0", "3"))
        assertEquals(3_000L, settings(expiryMinutes = 0, expirySeconds = 3).expiryMillis)
        assertEquals(3L, settings(expiryMinutes = 0, expirySeconds = 3).expiryTotalSeconds)
    }

    @Test
    fun `the floor counts the total, not one box`() {
        assertNull("one minute is well over the floor", expiryProblem("1", "0"))
        assertNull(expiryProblem("0", "30"))
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
}
