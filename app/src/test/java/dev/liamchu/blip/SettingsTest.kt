package dev.liamchu.blip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md "What the user can change" — the units, and what is refused. */
class SettingsTest {

    private fun settings(
        intervalValue: Int = 4,
        intervalUnit: IntervalUnit = IntervalUnit.HOURS,
        expiryValue: Int = 10,
        expiryUnit: ExpiryUnit = ExpiryUnit.MINUTES,
    ) = Settings(
        url = "http://example.com/blip",
        credential = "",
        intervalValue = intervalValue,
        intervalUnit = intervalUnit,
        expiryValue = expiryValue,
        expiryUnit = expiryUnit,
        maxLength = 120,
        lastRunFailed = false,
    )

    @Test
    fun `interval converts to minutes`() {
        assertEquals(30L, settings(30, IntervalUnit.MINUTES).intervalMinutes)
        assertEquals(240L, settings(4, IntervalUnit.HOURS).intervalMinutes)
        assertEquals(2880L, settings(2, IntervalUnit.DAYS).intervalMinutes)
    }

    @Test
    fun `expiry converts to milliseconds`() {
        assertEquals(30_000L, settings(expiryValue = 30, expiryUnit = ExpiryUnit.SECONDS).expiryMillis)
        assertEquals(600_000L, settings(expiryValue = 10, expiryUnit = ExpiryUnit.MINUTES).expiryMillis)
    }

    @Test
    fun `zero expiry is legal and means at once`() {
        assertNull(expiryProblem("0"))
        assertEquals(0L, settings(expiryValue = 0, expiryUnit = ExpiryUnit.SECONDS).expiryMillis)
        assertEquals(0L, settings(expiryValue = 0, expiryUnit = ExpiryUnit.MINUTES).expiryMillis)
    }

    @Test
    fun `negative and unparseable expiry is refused`() {
        assertNotNull(expiryProblem("-1"))
        assertNotNull(expiryProblem(""))
        assertNotNull(expiryProblem("soon"))
    }

    @Test
    fun `an interval under the WorkManager floor is refused rather than clamped`() {
        val problem = intervalProblem("5", IntervalUnit.MINUTES)
        assertNotNull("5 minutes is below the floor and must be refused", problem)
        assertTrue("should say the number: $problem", problem!!.contains("15"))
    }

    @Test
    fun `an interval at or above the floor is accepted in every unit`() {
        assertNull(intervalProblem("15", IntervalUnit.MINUTES))
        assertNull(intervalProblem("1", IntervalUnit.HOURS))
        assertNull(intervalProblem("1", IntervalUnit.DAYS))
    }

    @Test
    fun `an interval of zero or nonsense is refused`() {
        assertNotNull(intervalProblem("0", IntervalUnit.HOURS))
        assertNotNull(intervalProblem("-3", IntervalUnit.HOURS))
        assertNotNull(intervalProblem("later", IntervalUnit.HOURS))
    }

    @Test
    fun `max length must be at least one`() {
        assertNull(maxLengthProblem("100"))
        assertNotNull(maxLengthProblem("0"))
        assertNotNull(maxLengthProblem("lots"))
    }
}
