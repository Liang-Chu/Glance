package dev.liamchu.glance

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

/**
 * CONTRACT.md, exercised. Every clause in that file that can be checked without a
 * phone is checked here against a real HTTP server on loopback, because a contract
 * nothing has ever answered is a guess.
 */
class BackendContractTest {

    private var server: HttpServer? = null
    private var seenMethod: String = ""
    private var seenBody: String = ""
    private var seenAuth: String? = null

    @After
    fun stop() {
        server?.stop(0)
        server = null
    }

    // --- what Glance sends -----------------------------------------------------

    @Test
    fun `posts to the configured url`() {
        serve(200, """{"title":"T","text":"ok"}""")
        runBlocking { Backend.fetch(watcher(maxLength = 120)) }

        assertEquals("POST", seenMethod)
    }

    @Test
    fun `tells the backend every limit it has to work within`() {
        serve(200, """{"title":"T","text":"ok"}""")
        runBlocking { Backend.fetch(watcher(maxLength = 100)) }

        val sent = org.json.JSONObject(seenBody)
        assertEquals(100, sent.getInt("max_length"))
        assertEquals(Backend.TITLE_MAX_CHARS, sent.getInt("title_max_length"))
        assertEquals(30, sent.getInt("expires_after_seconds"))
        assertEquals(240, sent.getInt("interval_minutes"))
        assertEquals("Test watcher", sent.getString("watcher"))
        assertTrue("client should name the app: " + sent.getString("client"),
            sent.getString("client").startsWith("Glance/"))
    }

    @Test
    fun `the limits it sends are the ones it then enforces`() {
        // A backend that obeys max_length to the character must not be refused.
        serve(200, """{"title":"T","text":"""" + "x".repeat(100) + """"}""")
        val outcome = runBlocking { Backend.fetch(watcher(maxLength = 100)) }

        val sent = org.json.JSONObject(seenBody)
        assertEquals(100, sent.getInt("max_length"))
        assertTrue("obeying the stated limit must be accepted, got $outcome",
            outcome is Outcome.Content)
    }

    @Test
    fun `sends the credential as a bearer token`() {
        serve(200, """{"title":"T","text":"ok"}""")
        runBlocking { Backend.fetch(watcher(credential = "s3cret")) }

        assertEquals("Bearer s3cret", seenAuth)
    }

    @Test
    fun `omits the authorization header entirely when no credential is set`() {
        serve(200, """{"title":"T","text":"ok"}""")
        runBlocking { Backend.fetch(watcher(credential = "")) }

        assertNull("an empty credential must not become an empty header", seenAuth)
    }

    // --- what Glance accepts ---------------------------------------------------

    @Test
    fun `two hundred with title and text is content`() {
        serve(200, """{"title":"Kotlin","text":"buildList beats mutableListOf here."}""")
        val outcome = runBlocking { Backend.fetch(watcher()) }

        assertEquals(
            Outcome.Content("Kotlin", "buildList beats mutableListOf here."),
            outcome,
        )
    }

    @Test
    fun `unknown members are ignored rather than rejected`() {
        serve(200, """{"title":"T","text":"ok","source":"https://example.com","score":3}""")
        assertEquals(Outcome.Content("T", "ok"), runBlocking { Backend.fetch(watcher()) })
    }

    @Test
    fun `two hundred and four is not a failure`() {
        serve(204, null)
        assertEquals(Outcome.NothingToSay, runBlocking { Backend.fetch(watcher()) })
    }

    // --- what Glance refuses ---------------------------------------------------

    @Test
    fun `text longer than max length is refused rather than trimmed`() {
        val tooLong = "x".repeat(121)
        serve(200, """{"title":"T","text":"$tooLong"}""")
        val outcome = runBlocking { Backend.fetch(watcher(maxLength = 120)) }

        assertFailedBecause(outcome, "over 120 characters")
    }

    @Test
    fun `text exactly at max length is accepted`() {
        val exact = "x".repeat(120)
        serve(200, """{"title":"T","text":"$exact"}""")
        assertEquals(Outcome.Content("T", exact), runBlocking { Backend.fetch(watcher(maxLength = 120)) })
    }

    @Test
    fun `title longer than its limit is refused`() {
        val tooLong = "t".repeat(Backend.TITLE_MAX_CHARS + 1)
        serve(200, """{"title":"$tooLong","text":"ok"}""")
        assertFailedBecause(runBlocking { Backend.fetch(watcher()) }, "title over")
    }

    @Test
    fun `a missing text is a failure and not an empty notification`() {
        serve(200, """{"title":"T"}""")
        assertFailedBecause(runBlocking { Backend.fetch(watcher()) }, "no title or no text")
    }

    @Test
    fun `an empty text is a failure`() {
        serve(200, """{"title":"T","text":""}""")
        assertFailedBecause(runBlocking { Backend.fetch(watcher()) }, "empty")
    }

    @Test
    fun `a body that is not json is a failure`() {
        serve(200, "not json at all")
        assertFailedBecause(runBlocking { Backend.fetch(watcher()) }, "not JSON")
    }

    @Test
    fun `a server error is a failure carrying the status`() {
        serve(500, "boom")
        assertFailedBecause(runBlocking { Backend.fetch(watcher()) }, "500")
    }

    @Test
    fun `a redirect is not followed`() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server!!.createContext("/glance") { exchange ->
            exchange.responseHeaders.add("Location", "http://127.0.0.1:1/elsewhere")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server!!.start()

        assertFailedBecause(runBlocking { Backend.fetch(watcher()) }, "302")
    }

    @Test
    fun `an unreachable backend is a failure`() {
        // Port 1 on loopback: nothing listens, and nothing is expected to.
        val built = Watcher(
            id = 1,
            name = "Unreachable",
            url = "http://127.0.0.1:1/glance",
            credential = "",
            checkDays = 0,
            checkHours = 4,
            checkMinutes = 0,
            expiryMinutes = 10,
            expirySeconds = 0,
            maxLength = 120,
            lastRunFailed = false,
        )
        assertFailedBecause(runBlocking { Backend.fetch(built) }, "unreachable")
    }

    // --- harness -------------------------------------------------------------

    private fun assertFailedBecause(outcome: Outcome, fragment: String) {
        assertTrue("expected a failure, got $outcome", outcome is Outcome.Failed)
        val reason = (outcome as Outcome.Failed).reason
        assertTrue("reason was: $reason", reason.contains(fragment))
    }

    private fun serve(status: Int, body: String?) {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server!!.createContext("/glance") { exchange ->
            seenMethod = exchange.requestMethod
            seenAuth = exchange.requestHeaders.getFirst("Authorization")
            seenBody = exchange.requestBody.bufferedReader().readText()

            if (body == null) {
                exchange.sendResponseHeaders(status, -1)
            } else {
                val bytes = body.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        server!!.start()
    }

    private fun watcher(
        maxLength: Int = 120,
        credential: String = "",
        name: String = "Test watcher",
    ) = Watcher(
        id = 1,
        name = name,
        url = "http://127.0.0.1:" + server!!.address.port + "/glance",
        credential = credential,
        checkDays = 0,
        checkHours = 4,
        checkMinutes = 0,
        expiryMinutes = 0,
        expirySeconds = 30,
        maxLength = maxLength,
        lastRunFailed = false,
    )
}
