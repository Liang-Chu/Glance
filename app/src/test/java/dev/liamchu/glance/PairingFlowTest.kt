package dev.liamchu.glance

import androidx.compose.runtime.saveable.SaverScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URLEncoder

class PairingFlowTest {
    private val token = "sample-key-12345678901234567890+%"
    private var server: HttpServer? = null
    @After fun stop() { server?.stop(0) }

    private fun connection(origin: String = "http://192.0.2.10:4317", key: String = token) =
        PairingConnection.parse("$origin/#pilot-pair=1&pilot-token=" + URLEncoder.encode(key, "UTF-8"))

    private fun saved(id: Int = 1) = Watcher.blank(id).copy(name = "My alerts", url = connection().registrationUrl,
        credential = token, pushKey = "existing-subscription", pushRegistered = true, pushAddress = "existing-fid",
        expiryMinutes = 2, expirySeconds = 7, maxLength = 120, checkDays = 3)

    @Test fun `new scan has PUSH defaults and unique editable name`() {
        val draft = WatcherDraft(Watcher.blank(9)).scanned(connection(), listOf(
            saved().copy(name = "even-pilot"), saved(2).copy(name = "Even-PIlot (2)")))
        assertEquals("Even-PIlot (3)", draft.name)
        assertEquals(Watcher.PUSH, draft.delivery)
        assertEquals("0", draft.expiryMinutes)
        assertEquals("30", draft.expirySeconds)
        assertEquals("80", draft.maxLength)
        assertEquals(32, Backend.TITLE_MAX_CHARS)
        assertEquals(token, draft.credential)
        assertNull(draft.problem(emptyList()))
        assertTrue(draft.original.pushKey.isEmpty())
    }

    @Test fun `existing scan preserves user preferences and changes only connection and mode`() {
        val original = saved().copy(delivery = Watcher.POLL)
        val draft = WatcherDraft(original).copy(name = "Unsaved name", expirySeconds = "12", maxLength = "160")
            .scanned(connection("https://bridge.example:9431", "replacement-key-1234567890"), emptyList())
        assertEquals("Unsaved name", draft.name)
        assertEquals("2", draft.expiryMinutes)
        assertEquals("12", draft.expirySeconds)
        assertEquals("160", draft.maxLength)
        assertEquals("3", draft.checkDays)
        assertEquals(Watcher.PUSH, draft.delivery)
        assertEquals("https://bridge.example:9431/api/glance", draft.url)
        assertEquals(original, draft.original)
    }

    @Test fun `rescan of a new draft preserves changes after its first scan`() {
        val draft = WatcherDraft(Watcher.blank(1)).scanned(connection(), emptyList())
            .copy(name = "Custom", expiryMinutes = "4", expirySeconds = "9", maxLength = "100")
        val again = draft.scanned(connection("https://bridge.example"), emptyList())
        assertEquals("Custom", again.name)
        assertEquals("4", again.expiryMinutes)
        assertEquals("9", again.expirySeconds)
        assertEquals("100", again.maxLength)
    }

    @Test fun `matching connections offer existing identities including key rotation`() {
        val one = saved()
        val two = saved(2).copy(url = "HTTP://BRIDGE.EXAMPLE:80/api/glance", credential = "old-key")
        assertEquals(listOf(one), matchingConnections(connection(), listOf(one, two)))
        assertEquals(listOf(two), matchingConnections(connection("http://bridge.example"), listOf(one, two)))
        assertTrue(matchingConnections(connection("https://other.example"), listOf(one, two)).isEmpty())
        val chosen = WatcherDraft(one).scanned(connection(), listOf(one, two)).forSave()
        assertEquals(one.id, chosen.id)
        assertEquals(one.pushKey, chosen.pushKey)
        assertEquals(one.expiryTotalSeconds, chosen.expiryTotalSeconds)
        assertEquals(one.maxLength, chosen.maxLength)
    }

    @Test fun `repeat scan save and retry keep one subscription while changed keys rotate it`() {
        val original = saved()
        val retry = WatcherDraft(original).scanned(connection(), listOf(original)).forSave()
        val again = WatcherDraft(retry).scanned(connection(), listOf(retry)).forSave()
        assertEquals(original.pushKey, retry.pushKey)
        assertEquals(retry.pushKey, again.pushKey)
        assertFalse(retry.pushRegistered)
        val changed = WatcherDraft(original).scanned(connection(key = "different-key-1234567890123456"), emptyList()).forSave()
        assertNotEquals(original.pushKey, changed.pushKey)
    }

    @Test fun `cancelled or invalid scan leaves the original and unsaved preferences intact`() {
        val original = saved()
        val draft = WatcherDraft(original).copy(name = "My unsaved name")
        val session = ScanSession()
        session.cancel()
        var after = draft
        if (session.accept()) after = draft.scanned(connection("https://different.example"), emptyList())
        assertSame(draft, after)
        try { PairingConnection.parse("not a URL") } catch (_: InvalidPairing) { /* displayed without changing draft */ }
        assertEquals(original, draft.original)
        assertEquals("My unsaved name", after.name)
    }

    @Test fun `duplicate names fail validation and existing name is allowed`() {
        val original = saved()
        val newDraft = WatcherDraft(Watcher.blank(2)).scanned(connection(), emptyList()).copy(name = " my ALERTS ")
        assertTrue(newDraft.problem(listOf(original))!!.contains("NAME ALREADY EXISTS"))
        assertNull(WatcherDraft(original).problem(listOf(original)))
    }

    @Test fun `draft and original survive activity recreation without a stored blank subscription`() {
        listOf(Watcher.blank(3), saved()).forEach { original ->
            val draft = WatcherDraft(original).scanned(connection(), emptyList()).copy(expirySeconds = "41")
            val bundle = with(WatcherDraft.Saver) { SaverScope { true }.save(draft) }!!
            assertEquals(draft, WatcherDraft.Saver.restore(bundle))
            assertEquals(original, restoreEditor(editorRecord(original)))
        }
    }

    @Test fun `QR pixels decode then fill save and register using the unchanged HTTP contract`() {
        var body = JSONObject()
        var authorization: String? = null
        var path = ""
        val origin = serve(204) {
            body = JSONObject(it.requestBody.bufferedReader().readText())
            authorization = it.requestHeaders.getFirst("Authorization")
            path = it.requestURI.toString()
            assertEquals("POST", it.requestMethod)
        }
        // Same URLSearchParams-compatible form as desktop pairingUrl(), with synthetic key only.
        val raw = "$origin/#pilot-pair=1&pilot-token=" + URLEncoder.encode(token, "UTF-8")
        val bits = QRCodeWriter().encode(raw, BarcodeFormat.QR_CODE, 600, 600)
        val pixels = IntArray(600 * 600) { if (bits[it % 600, it / 600]) 0xff000000.toInt() else 0xffffffff.toInt() }
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(600, 600, pixels))))
        val draft = WatcherDraft(Watcher.blank(1)).scanned(PairingConnection.parse(decoded.text), emptyList())
        assertNull(draft.problem(emptyList()))
        // Exercise the same record format used by private DataStore before sending.
        val stored = Watcher.fromJson(JSONObject(draft.forSave().toJson().toString()))
        assertEquals(Outcome.NothingToSay, runBlocking { Backend.register(stored, "test-fid", "test-project") })
        assertEquals("/api/glance", path)
        assertEquals("Bearer $token", authorization)
        assertEquals(setOf("operation", "subscription_id", "installation_id", "firebase_project_id", "watcher",
            "max_length", "title_max_length", "expires_after_seconds", "client"), body.keys().asSequence().toSet())
        assertEquals("register_push", body.getString("operation"))
        assertEquals(stored.pushKey, body.getString("subscription_id"))
        assertEquals("test-fid", body.getString("installation_id"))
        assertEquals("test-project", body.getString("firebase_project_id"))
        assertEquals(30, body.getInt("expires_after_seconds"))
        assertEquals(80, body.getInt("max_length"))
        assertEquals(32, body.getInt("title_max_length"))
        assertFalse(body.toString().contains(token))
    }

    @Test fun `rejected credentials keep saved connection and explain how to retry`() {
        listOf(401, 403).forEach { status ->
            val origin = serve(status) { it.requestBody.close() }
            val stored = WatcherDraft(Watcher.blank(1)).scanned(connection(origin), emptyList()).forSave()
            val outcome = runBlocking { Backend.register(stored, "test-fid", "test-project") } as Outcome.Failed
            assertTrue(outcome.reason.contains("key rejected"))
            assertTrue(outcome.reason.contains("retry"))
            assertFalse(outcome.reason.contains(token))
            assertEquals(stored, Watcher.fromJson(JSONObject(stored.toJson().toString())))
            server!!.stop(0)
        }
    }

    @Test fun `unreachable endpoint retains the saved watcher for retry`() {
        val origin = serve(204) { it.requestBody.close() }
        server!!.stop(0)
        val stored = WatcherDraft(Watcher.blank(1)).scanned(connection(origin), emptyList()).forSave()
        val before = stored.toJson().toString()
        val outcome = runBlocking { Backend.register(stored, "test-fid", "test-project") } as Outcome.Failed
        assertEquals("backend unreachable", outcome.reason)
        assertEquals(before, stored.toJson().toString())
        assertEquals(stored.pushKey, WatcherDraft(stored).forSave().pushKey)
    }

    private fun serve(status: Int, inspect: (com.sun.net.httpserver.HttpExchange) -> Unit): String {
        val created = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server = created
        created.createContext("/api/glance") {
            inspect(it)
            it.sendResponseHeaders(status, -1)
            it.close()
        }
        created.start()
        return "http://127.0.0.1:" + created.address.port
    }
}
