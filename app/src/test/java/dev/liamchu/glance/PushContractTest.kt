package dev.liamchu.glance

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress

class PushContractTest {
    private var server: HttpServer? = null

    private fun watcher() = Watcher.blank(7).copy(name = "Push tips", delivery = Watcher.PUSH,
        pushKey = "subscription-a", url = "http://127.0.0.1", credential = "test-credential")

    @After fun stop() { server?.stop(0) }

    @Test fun `old stored watchers remain polling`() {
        val json = Watcher.blank(1).toJson()
        json.remove("delivery")
        json.remove("push_key")
        json.remove("push_registered")
        assertEquals(Watcher.POLL, Watcher.fromJson(json).delivery)
    }

    @Test fun `push settings survive a round trip`() {
        val before = watcher().copy(pushRegistered = true)
        assertEquals(before, Watcher.fromJson(JSONObject(before.toJson().toString())))
    }

    @Test(expected = org.json.JSONException::class)
    fun `stored push watcher without routing key is refused`() {
        Watcher.fromJson(watcher().toJson().put("push_key", ""))
    }

    @Test fun `routing requires the current subscription and push mode`() {
        val current = watcher()
        val data = mapOf("subscription_id" to current.pushKey)
        assertEquals(current, PushMessage.target(data, listOf(current)))
        assertNull(PushMessage.target(data, emptyList()))
        assertNull(PushMessage.target(data, listOf(current.copy(delivery = Watcher.POLL))))
        assertNull(PushMessage.target(data, listOf(current.copy(pushKey = "replacement"))))
        assertNull(PushMessage.target(emptyMap(), listOf(current)))
        assertNull(PushMessage.target(mapOf("subscription_id" to ""), listOf(Watcher.blank(1))))
    }

    @Test fun `push rejects missing empty and oversized content without truncating`() {
        val current = watcher().copy(maxLength = 4)
        assertEquals(Outcome.Content("T", "1234"), PushMessage.content(mapOf("title" to "T", "text" to "1234"), current))
        listOf(mapOf("title" to "T"), mapOf("text" to "123"),
            mapOf("title" to "", "text" to "123"),
            mapOf("title" to "T", "text" to "12345"),
            mapOf("title" to "T".repeat(Backend.TITLE_MAX_CHARS + 1), "text" to "123")
        ).forEach { assertTrue(PushMessage.content(it, current) is Outcome.Failed) }
    }

    @Test fun `registration posts the address subscription and current limits to the exact url`() {
        var seen = JSONObject()
        var method = ""
        var auth: String? = null
        var contentType: String? = null
        val url = serve(204) { exchange ->
            seen = JSONObject(exchange.requestBody.bufferedReader().readText())
            method = exchange.requestMethod
            auth = exchange.requestHeaders.getFirst("Authorization")
            contentType = exchange.requestHeaders.getFirst("Content-Type")
        }
        val current = watcher().copy(url = url)
        assertEquals(Outcome.NothingToSay, runBlocking { Backend.register(current, "fcm-address", "sample-project") })
        assertEquals("POST", method)
        assertEquals("application/json", contentType)
        assertEquals("Bearer test-credential", auth)
        assertEquals("register_push", seen.getString("operation"))
        assertEquals(current.pushKey, seen.getString("subscription_id"))
        assertEquals("fcm-address", seen.getString("installation_id"))
        assertEquals("sample-project", seen.getString("firebase_project_id"))
        assertEquals(current.name, seen.getString("watcher"))
        assertEquals(current.maxLength, seen.getInt("max_length"))
        assertEquals(Backend.TITLE_MAX_CHARS, seen.getInt("title_max_length"))
        assertEquals(current.expiryTotalSeconds, seen.getLong("expires_after_seconds"))
        assertFalse("push has no polling schedule", seen.has("interval_minutes"))
    }

    @Test fun `polling response cannot falsely confirm push registration`() {
        val url = serve(200) { it.requestBody.close() }
        assertTrue(runBlocking { Backend.register(watcher().copy(url = url), "address", "sample-project") } is Outcome.Failed)
    }

    @Test fun `unregistration identifies only the retired subscription`() {
        var seen = JSONObject()
        var auth: String? = "not checked"
        val url = serve(204) {
            seen = JSONObject(it.requestBody.bufferedReader().readText())
            auth = it.requestHeaders.getFirst("Authorization")
        }
        assertEquals(Outcome.NothingToSay, runBlocking { Backend.unregister(watcher().copy(url = url, credential = "")) })
        assertEquals("unregister_push", seen.getString("operation"))
        assertEquals("subscription-a", seen.getString("subscription_id"))
        assertFalse(seen.has("installation_id"))
        assertNull(auth)
    }

    private fun serve(status: Int, inspect: (com.sun.net.httpserver.HttpExchange) -> Unit): String {
        val created = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server = created
        created.createContext("/registration") {
            inspect(it)
            it.sendResponseHeaders(status, -1)
            it.close()
        }
        created.start()
        return "http://127.0.0.1:" + created.address.port + "/registration"
    }
}
