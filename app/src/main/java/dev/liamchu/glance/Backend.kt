package dev.liamchu.glance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** What one call to the backend produced. There is no fourth outcome. */
sealed interface Outcome {
    data class Content(val title: String, val text: String) : Outcome
    data object NothingToSay : Outcome
    data class Failed(val reason: String) : Outcome
}

/**
 * The whole of CONTRACT.md, and the only door external input comes through.
 *
 * Every failure below is named and returned rather than swallowed, and nothing
 * here supplies a value the backend did not send — no truncating an over-length
 * response, no standing in for a missing field. That is CONSTRAINTS.md
 * "C1 — Fail fast; no fallbacks", and the `catch` blocks in this file are the
 * validation of external input at its entry point that C1 explicitly allows.
 */
object Backend {

    const val TITLE_MAX_CHARS = 32
    const val MAX_RESPONSE_BYTES = 65536

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000
    private val USER_AGENT = "Glance/" + BuildConfig.VERSION_NAME

    /**
     * What the backend is told it is talking to. Deliberately unversioned: a
     * backend that branched on the version would break on an upgrade it never
     * asked for. The User-Agent above carries the version for debugging, where a
     * changing value costs nothing.
     */
    private const val CLIENT_NAME = "Glance"

    private fun limits(watcher: Watcher) = JSONObject()
        .put("watcher", watcher.name)
        .put("max_length", watcher.maxLength)
        .put("title_max_length", TITLE_MAX_CHARS)
        .put("expires_after_seconds", watcher.expiryTotalSeconds)
        .put("client", CLIENT_NAME)

    suspend fun fetch(watcher: Watcher): Outcome = post(
        watcher, limits(watcher).put("interval_minutes", watcher.intervalMinutes),
        DiagnosticEvent.HTTP_POLL,
    ) { connection ->
        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_NO_CONTENT -> Outcome.NothingToSay
            HttpURLConnection.HTTP_OK -> connection.inputStream.use { stream ->
                val bytes = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = stream.read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_BYTES + 1 - bytes.size()))
                    if (count < 0) break
                    bytes.write(buffer, 0, count)
                    if (bytes.size() > MAX_RESPONSE_BYTES) return@use rejected(
                        DiagnosticEvent.RESPONSE_TOO_LARGE, "response exceeds 64 KiB")
                }
                parse(bytes.toString(Charsets.UTF_8.name()), watcher.maxLength)
            }
            else -> Outcome.Failed("backend returned " + code)
        }
    }

    suspend fun register(watcher: Watcher, installationId: String, projectId: String): Outcome = post(
        watcher, limits(watcher)
            .put("operation", "register_push")
            .put("subscription_id", watcher.pushKey)
            .put("firebase_project_id", projectId)
            .put("installation_id", installationId),
        DiagnosticEvent.HTTP_REGISTER,
    ) { connection ->
        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_NO_CONTENT -> Outcome.NothingToSay
            HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                Outcome.Failed("Connection key rejected (HTTP " + code + "). Re-scan the desktop QR or check Credential, then save to retry.")
            else -> Outcome.Failed("push registration returned " + code + "; expected 204")
        }
    }

    suspend fun unregister(watcher: Watcher): Outcome = post(watcher, JSONObject()
        .put("operation", "unregister_push")
        .put("subscription_id", watcher.pushKey)
        .put("client", CLIENT_NAME),
        DiagnosticEvent.HTTP_REMOVE,
    ) { connection ->
        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_NO_CONTENT -> Outcome.NothingToSay
            else -> Outcome.Failed("push removal returned " + code + "; expected 204")
        }
    }

    private suspend fun post(
        watcher: Watcher,
        request: JSONObject,
        operation: DiagnosticEvent,
        response: (HttpURLConnection) -> Outcome,
    ): Outcome = withContext(Dispatchers.IO) {
        Diagnostics.event(operation, watcher.id)
        if (!isUsableUrl(watcher.url)) return@withContext rejected(DiagnosticEvent.INVALID_URL,
            "the configured URL is not valid HTTP(S)")
        if (credentialProblem(watcher.credential) != null) return@withContext rejected(DiagnosticEvent.INVALID_CREDENTIAL,
            "invalid backend credential format")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(watcher.url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                // A redirect is a failed run: the app calls the URL it was given, verbatim.
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                if (watcher.credential.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer " + watcher.credential)
                }
            }

            connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
            Diagnostics.event(DiagnosticEvent.HTTP_STATUS, watcher.id, connection.responseCode)
            response(connection)
        } catch (e: IOException) {
            Diagnostics.failure(DiagnosticEvent.HTTP_EXCEPTION, e, watcher.id)
            Outcome.Failed("backend unreachable")
        } catch (e: ClassCastException) {
            Diagnostics.failure(DiagnosticEvent.HTTP_EXCEPTION, e, watcher.id)
            Outcome.Failed("the configured URL is not HTTP")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parse(body: String, maxLength: Int): Outcome {
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return rejected(DiagnosticEvent.INVALID_JSON, "response was not JSON")
        }

        val title: String
        val text: String
        try {
            // Android's getString coerces numbers/objects; the wire contract requires strings.
            title = json.get("title") as? String ?: return rejected(DiagnosticEvent.INVALID_CONTENT_TYPE, "title must be a string")
            text = json.get("text") as? String ?: return rejected(DiagnosticEvent.INVALID_CONTENT_TYPE, "text must be a string")
        } catch (e: JSONException) {
            return rejected(DiagnosticEvent.MISSING_CONTENT, "response had no title or no text")
        }

        return validateContent(title, text, maxLength)
    }

    fun validateContent(title: String, text: String, maxLength: Int): Outcome {
        if (title.isEmpty() || text.isEmpty()) return rejected(DiagnosticEvent.EMPTY_CONTENT, "title or text was empty")
        if (title.length > TITLE_MAX_CHARS) {
            return rejected(DiagnosticEvent.TITLE_TOO_LONG, "title over " + TITLE_MAX_CHARS + " characters")
        }
        if (text.length > maxLength) {
            return rejected(DiagnosticEvent.TEXT_TOO_LONG, "text over " + maxLength + " characters")
        }
        return Outcome.Content(title, text)
    }

    private fun rejected(event: DiagnosticEvent, reason: String): Outcome.Failed {
        Diagnostics.event(event)
        return Outcome.Failed(reason)
    }
}
