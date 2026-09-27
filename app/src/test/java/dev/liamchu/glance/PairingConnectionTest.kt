package dev.liamchu.glance

import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class PairingConnectionTest {
    private val token = "sample-key-12345678901234567890"
    private fun qr(origin: String = "http://192.0.2.10:4317", value: String = token) =
        "$origin/#pilot-pair=1&pilot-token=" + URLEncoder.encode(value, "UTF-8")

    @Test fun `handoff vector uses fragment and decodes once`() {
        val connection = PairingConnection.parse("http://192.0.2.10:4317/#pilot-pair=1&pilot-token=$token%2B%25")
        assertEquals("http://192.0.2.10:4317/api/glance", connection.registrationUrl)
        assertEquals("$token+%", connection.credential)
    }

    @Test fun `IPv4 HTTPS IPv6 and arbitrary valid ports come from the code`() {
        listOf("http://192.0.2.11:9321", "https://bridge.example:7443", "http://[2001:db8::1]:4317",
            "https://[2001:db8::2]", "http://192.0.2.12:1", "https://bridge.example:65535").forEach {
            assertEquals("$it/api/glance", PairingConnection.parse(qr(it)).registrationUrl)
        }
    }

    @Test fun `normalization matches browser origin and accepts empty root path`() {
        assertEquals("https://bridge.example/api/glance", PairingConnection.parse(
            "HTTPS://BRIDGE.EXAMPLE:443#pilot-pair=1&pilot-token=$token").registrationUrl)
        assertEquals("http://bridge.example/api/glance", PairingConnection.parse(qr("http://bridge.example:80")).registrationUrl)
    }

    @Test fun `form encoding distinguishes raw plus encoded plus and literal percent escape`() {
        assertEquals("$token +%2B%", PairingConnection.parse(
            "http://192.0.2.10/#pilot-pair=1&pilot-token=$token+%2B%252B%25").credential)
        val exact = "  $token &x=y+%  "
        assertEquals(exact, PairingConnection.parse(qr(value = exact)).credential)
    }

    @Test fun `token is measured in UTF16 units without trimming or ASCII coercion`() {
        listOf("a".repeat(24), "a".repeat(512), "😀".repeat(12)).forEach {
            assertEquals(it, PairingConnection.parse(qr(value = it)).credential)
        }
        listOf("a".repeat(23), "a".repeat(513), "😀".repeat(257), "").forEach { reject(qr(value = it)) }
    }

    @Test fun `required parameters cannot repeat even with encoded names`() {
        listOf("pilot-pair=1&pilot-pair=1&pilot-token=$token",
            "pilot-pair=1&pilot-token=$token&pilot-token=$token",
            "pilot-pair=1&pilot-token=$token&pilot%2Dtoken=$token",
            "pilot-pair=1&pilot-token=$token&extra=a&extra=b").forEach {
            reject("https://bridge.example/#$it")
        }
    }

    @Test fun `missing fields wrong versions and JSON are rejected`() {
        listOf("", "{}", "https://bridge.example/", "https://bridge.example/#pilot-token=$token",
            "https://bridge.example/#pilot-pair=1", "https://bridge.example/#pilot-pair=2&pilot-token=$token",
            "https://bridge.example/#pilot-pair=01&pilot-token=$token").forEach(::reject)
    }

    @Test fun `non HTTP origins userinfo paths queries missing hosts and bad ports are rejected`() {
        listOf("ftp://bridge.example/", "javascript:alert(1)", "file:///", "//bridge.example/", "http:///",
            "http://user@bridge.example/", "http://user:pass@bridge.example/", "http://@bridge.example/",
            "http://bridge.example/path", "http://bridge.example//", "http://bridge.example/%2F",
            "http://bridge.example/?", "http://bridge.example/?desktop=1", "http://bridge.example:0/",
            "http://bridge.example:65536/", "http://bridge.example:-1/", "http://bridge.example:/").forEach {
            reject("$it#pilot-pair=1&pilot-token=$token")
        }
    }

    @Test fun `CR LF malformed escapes and oversized raw input never leak into errors`() {
        listOf(qr(value = "$token\r"), qr(value = "$token\n"), qr() + "%ZZ", qr() + "%",
            qr() + "x".repeat(8192)).forEach(::reject)
    }

    @Test fun `unknown single parameters are harmless and parameter order is irrelevant`() {
        assertEquals(token, PairingConnection.parse(
            "https://bridge.example/#extra=1&pilot-token=$token&pilot-pair=1").credential)
    }

    @Test fun `connection string representation redacts the key and address`() {
        val result = PairingConnection.parse(qr())
        assertFalse(result.toString().contains(token))
        assertFalse(result.toString().contains("192.0.2.10"))
    }

    @Test fun `concurrent camera frames are consumed only once`() {
        val session = ScanSession()
        val executor = Executors.newFixedThreadPool(4)
        try {
            assertEquals(1, executor.invokeAll(List(80) { Callable { session.accept() } }).count { it.get() })
        } finally { executor.shutdownNow() }
    }

    @Test fun `cancellation blocks late frames while retry creates a new session`() {
        val cancelled = ScanSession()
        cancelled.cancel()
        repeat(10) { assertFalse(cancelled.accept()) }
        assertTrue(ScanSession().accept())
    }

    private fun reject(raw: String) {
        try {
            PairingConnection.parse(raw)
            fail("Invalid code was accepted")
        } catch (e: InvalidPairing) {
            assertNull(e.cause)
            assertFalse(e.toString().contains(token))
            if (raw.isNotEmpty()) assertFalse(e.toString().contains(raw))
        }
    }
}
