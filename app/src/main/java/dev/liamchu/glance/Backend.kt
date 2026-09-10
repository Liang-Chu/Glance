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

    suspend fun fetch(watcher: Watcher): Outcome = withContext(Dispatchers.IO) {
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

            // Everything the backend needs in order to answer well, and nothing
            // it could not have worked out for itself. CONTRACT.md "The call".
            val request = JSONObject()
                .put("watcher", watcher.name)
                .put("max_length", watcher.maxLength)
                .put("title_max_length", TITLE_MAX_CHARS)
                .put("expires_after_seconds", watcher.expiryTotalSeconds)
                .put("interval_minutes", watcher.intervalMinutes)
                .put("client", CLIENT_NAME)
                .toString()
            connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }

            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NO_CONTENT -> Outcome.NothingToSay
                HttpURLConnection.HTTP_OK -> parse(
                    connection.inputStream.bufferedReader().use { it.readText() },
                    watcher.maxLength,
                )
                else -> Outcome.Failed("backend returned " + code)
            }
        } catch (e: IOException) {
            Outcome.Failed("backend unreachable")
        } catch (e: ClassCastException) {
            Outcome.Failed("the configured URL is not HTTP")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parse(body: String, maxLength: Int): Outcome {
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return Outcome.Failed("response was not JSON")
        }

        val title: String
        val text: String
        try {
            // getString throws on a missing member. optString would default to "",
            // which is the silent stand-in C1 forbids.
            title = json.getString("title")
            text = json.getString("text")
        } catch (e: JSONException) {
            return Outcome.Failed("response had no title or no text")
        }

        if (title.isEmpty() || text.isEmpty()) return Outcome.Failed("title or text was empty")
        if (title.length > TITLE_MAX_CHARS) {
            return Outcome.Failed("title over " + TITLE_MAX_CHARS + " characters")
        }
        if (text.length > maxLength) {
            return Outcome.Failed("text over " + maxLength + " characters")
        }
        return Outcome.Content(title, text)
    }
}
