package dev.liamchu.blip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md "What the user can change", criterion settings-3. */
class UrlValidationTest {

    @Test
    fun `accepts absolute http and https urls`() {
        assertTrue(isUsableUrl("http://192.168.1.10:8080/blip"))
        assertTrue(isUsableUrl("https://example.com/blip"))
    }

    @Test
    fun `refuses anything that is not an absolute http url`() {
        assertFalse("empty", isUsableUrl(""))
        assertFalse("no scheme", isUsableUrl("example.com/blip"))
        assertFalse("no host", isUsableUrl("http:///blip"))
        assertFalse("wrong scheme", isUsableUrl("ftp://example.com/blip"))
        assertFalse("not a url", isUsableUrl("this is not a url"))
    }
}
