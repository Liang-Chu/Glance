package dev.liamchu.glance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import java.net.URI
import java.net.URISyntaxException

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val SECONDS_PER_MINUTE = 60L

/**
 * Every watcher, kept as one JSON array in one preference.
 *
 * There is still no database: DESIGN.md "The stack" says the settings are the
 * whole of what the app stores, and a handful of watchers is settings however
 * many of them there are.
 */
object WatcherStore {

    /** WorkManager will not repeat work more often than this, and rounds up silently. */
    const val MINIMUM_INTERVAL_MINUTES = 15L

    /**
     * The shortest a notification may live. Long enough that the listener feeding
     * the glasses has certainly been handed it before Android takes it back.
     */
    const val MINIMUM_EXPIRY_SECONDS = 3L

    private val KEY_WATCHERS = stringPreferencesKey("watchers")

    suspend fun all(context: Context): List<Watcher> {
        val raw = context.store.data.first()[KEY_WATCHERS] ?: return emptyList()
        return decode(raw)
    }

    suspend fun byId(context: Context, id: Int): Watcher? = all(context).firstOrNull { it.id == id }

    /** Adds when the id is unknown, replaces when it is not. */
    suspend fun put(context: Context, watcher: Watcher) {
        val kept = all(context).filterNot { it.id == watcher.id }
        write(context, kept + watcher)
    }

    suspend fun delete(context: Context, id: Int) {
        write(context, all(context).filterNot { it.id == id })
    }

    suspend fun setLastRunFailed(context: Context, id: Int, failed: Boolean) {
        val watcher = byId(context, id) ?: return
        put(context, watcher.copy(lastRunFailed = failed))
    }

    /** One higher than the highest ever used, so a deleted id is never reissued. */
    suspend fun nextId(context: Context): Int = (all(context).maxOfOrNull { it.id } ?: 0) + 1

    private suspend fun write(context: Context, watchers: List<Watcher>) {
        val array = JSONArray()
        watchers.sortedBy { it.id }.forEach { array.put(it.toJson()) }
        context.store.edit { stored -> stored[KEY_WATCHERS] = array.toString() }
    }

    private fun decode(raw: String): List<Watcher> {
        val array = try {
            JSONArray(raw)
        } catch (e: JSONException) {
            // Stored input parsed at its entry point. Unreadable storage is empty
            // storage; it is never half-read into partly-configured watchers.
            return emptyList()
        }
        val watchers = mutableListOf<Watcher>()
        for (index in 0 until array.length()) {
            try {
                watchers.add(Watcher.fromJson(array.getJSONObject(index)))
            } catch (e: JSONException) {
                continue
            }
        }
        return watchers
    }
}

/**
 * Criterion settings-3: a URL is refused where it is typed, not where it is used.
 * Rejecting malformed input at its entry point is what C1 asks for.
 */
fun isUsableUrl(candidate: String): Boolean {
    val parsed = try {
        URI(candidate)
    } catch (e: URISyntaxException) {
        // URI(String) throws this, not IllegalArgumentException. Catching the wrong
        // one crashed the settings screen on exactly the input this refuses.
        return false
    }
    val scheme = parsed.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return false
    return !parsed.host.isNullOrEmpty()
}

/** An empty box counts as zero; anything else must be a whole number, zero or more. */
fun partOrNull(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return 0
    val value = trimmed.toIntOrNull()
    return if (value == null || value < 0) null else value
}

fun nameProblem(raw: String): String? =
    if (raw.trim().isEmpty()) "NAME: GIVE IT ONE, SO THE LIST MEANS SOMETHING." else null

/**
 * Criterion settings-8. WorkManager silently rounds a shorter period up to its
 * own floor, so a refusal here is the only way the user learns that "every 5
 * minutes" was never going to happen. Clamping it quietly would be the
 * derivation CONSTRAINTS.md "C5 — Independent axes stay independent" forbids.
 */
fun intervalProblem(days: String, hours: String, minutes: String): String? {
    val d = partOrNull(days)
    val h = partOrNull(hours)
    val m = partOrNull(minutes)
    if (d == null || h == null || m == null) {
        return "CHECK EVERY: WHOLE NUMBERS, 0 OR MORE."
    }
    val total = d * MINUTES_PER_DAY + h * MINUTES_PER_HOUR + m
    if (total < WatcherStore.MINIMUM_INTERVAL_MINUTES) {
        return "CHECK EVERY: AT LEAST " + WatcherStore.MINIMUM_INTERVAL_MINUTES +
            " MINUTES IN TOTAL. ANDROID WILL NOT REPEAT WORK FASTER."
    }
    return null
}

/**
 * Criterion settings-9. Three seconds is a floor rather than a preference: below
 * it there is no guarantee the notification is still there when the listener that
 * feeds the glasses goes looking.
 */
fun expiryProblem(minutes: String, seconds: String): String? {
    val m = partOrNull(minutes)
    val s = partOrNull(seconds)
    if (m == null || s == null) return "EXPIRES AFTER: WHOLE NUMBERS, 0 OR MORE."
    if (m * SECONDS_PER_MINUTE + s < WatcherStore.MINIMUM_EXPIRY_SECONDS) {
        return "EXPIRES AFTER: AT LEAST " + WatcherStore.MINIMUM_EXPIRY_SECONDS +
            " SECONDS IN TOTAL, SO THE GLASSES RECEIVE IT."
    }
    return null
}

fun maxLengthProblem(raw: String): String? {
    val value = raw.trim().toIntOrNull()
    if (value == null || value < 1) return "MAX LENGTH: A WHOLE NUMBER, AT LEAST 1."
    return null
}
