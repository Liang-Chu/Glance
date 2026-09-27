package dev.liamchu.glance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import java.util.UUID
import java.net.URI
import java.net.URISyntaxException

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val SECONDS_PER_MINUTE = 60L

/**
 * Every watcher, kept as one JSON array in one preference.
 *
 * Watcher settings and registration state. Changes are atomic because the UI,
 * polling workers and FCM callbacks may all update them at once.
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
    private val KEY_NEXT_ID = intPreferencesKey("next_watcher_id")
    private val KEY_FIREBASE = stringPreferencesKey("firebase_config")
    private val KEY_RETIRED = stringPreferencesKey("retired_push_subscriptions")

    suspend fun firebaseConfig(context: Context): FirebaseConfig? =
        context.store.data.first()[KEY_FIREBASE]?.let { FirebaseConfig.fromStored(it) }

    suspend fun configureFirebase(context: Context, config: FirebaseConfig) {
        context.store.edit { stored ->
            if (stored[KEY_FIREBASE] == config.toJson()) return@edit
            val watchers = stored[KEY_WATCHERS]?.let { decode(it) } ?: emptyList()
            retainRetired(stored, watchers.filter { it.isPush })
            stored[KEY_WATCHERS] = encode(watchers.map {
                if (it.isPush) it.copy(pushKey = UUID.randomUUID().toString(), pushRegistered = false,
                    pushAddress = "", lastRunFailed = false) else it
            })
            stored[KEY_FIREBASE] = config.toJson()
        }
    }

    suspend fun retired(context: Context): List<Watcher> =
        context.store.data.first()[KEY_RETIRED]?.let { decode(it) } ?: emptyList()

    suspend fun forgetRetired(context: Context, key: String) {
        context.store.edit { stored ->
            stored[KEY_RETIRED] = encode((stored[KEY_RETIRED]?.let { decode(it) } ?: emptyList())
                .filterNot { it.pushKey == key })
        }
    }

    suspend fun all(context: Context): List<Watcher> {
        val raw = context.store.data.first()[KEY_WATCHERS] ?: return emptyList()
        return decode(raw)
    }

    fun observe(context: Context) = context.store.data.map { stored ->
        stored[KEY_WATCHERS]?.let { decode(it) } ?: emptyList()
    }

    suspend fun byId(context: Context, id: Int): Watcher? = all(context).firstOrNull { it.id == id }

    /** Adds when the id is unknown, replaces when it is not. */
    suspend fun put(context: Context, watcher: Watcher) {
        context.store.edit { stored ->
            val current = stored[KEY_WATCHERS]?.let { decode(it) } ?: emptyList()
            retainRetired(stored, current.filter { it.id == watcher.id && it.isPush && it.pushKey != watcher.pushKey })
            stored[KEY_WATCHERS] = encode(current.filterNot { it.id == watcher.id } + watcher)
        }
    }

    suspend fun delete(context: Context, id: Int) {
        context.store.edit { stored ->
            val current = stored[KEY_WATCHERS]?.let { decode(it) } ?: emptyList()
            retainRetired(stored, current.filter { it.id == id && it.isPush })
            stored[KEY_WATCHERS] = encode(current.filterNot { it.id == id })
        }
    }

    /** Serialize delivery with edits/deletion and other deliveries, including failure suppression. */
    suspend fun updateCurrent(context: Context, expected: Watcher, update: (Watcher) -> Watcher) {
        change(context) { current -> current.map { if (it.sameConfiguration(expected)) update(it) else it } }
    }

    suspend fun setPushRegistered(context: Context, expected: Watcher, registered: Boolean) {
        change(context) { current ->
            current.map {
                if (it.sameConfiguration(expected)) {
                    it.copy(pushRegistered = registered)
                } else it
            }
        }
    }

    suspend fun observePushAddress(context: Context, expected: Watcher, address: String) {
        change(context) { current ->
            current.map {
                if (it.sameConfiguration(expected)) {
                    it.copy(pushRegistered = false, pushAddress = address)
                } else it
            }
        }
    }

    suspend fun recordPushRegistration(context: Context, expected: Watcher, address: String) {
        change(context) { current ->
            current.map {
                if (it.sameConfiguration(expected) && (it.pushAddress.isEmpty() || it.pushAddress == address)) {
                    Notifier.clearFailure(context, it.id)
                    it.copy(pushRegistered = true, pushAddress = address, lastRunFailed = false)
                } else it
            }
        }
    }

    /** One higher than the highest ever used, so a deleted id is never reissued. */
    suspend fun nextId(context: Context): Int {
        val stored = context.store.edit { preferences ->
            val current = preferences[KEY_WATCHERS]?.let { decode(it) } ?: emptyList()
            val next = maxOf(preferences[KEY_NEXT_ID] ?: 1, (current.maxOfOrNull { it.id } ?: 0) + 1)
            check(next < Int.MAX_VALUE) { "Watcher IDs exhausted" }
            preferences[KEY_NEXT_ID] = next + 1
        }
        return checkNotNull(stored[KEY_NEXT_ID]) - 1
    }

    private suspend fun change(context: Context, transform: (List<Watcher>) -> List<Watcher>) {
        // Push delivery, registration and the editor may write concurrently.
        context.store.edit { stored ->
            val current = stored[KEY_WATCHERS]?.let { decode(it) } ?: emptyList()
            stored[KEY_WATCHERS] = encode(transform(current))
        }
    }

    private fun retainRetired(stored: androidx.datastore.preferences.core.MutablePreferences, retiring: List<Watcher>) {
        val previous = stored[KEY_RETIRED]?.let { decode(it) } ?: emptyList()
        stored[KEY_RETIRED] = encode((previous + retiring).filter { it.pushKey.isNotEmpty() }.distinctBy { it.pushKey })
    }

    private fun encode(watchers: List<Watcher>): String = JSONArray().apply {
        watchers.sortedBy { it.id }.forEach { put(it.toJson()) }
    }.toString()

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
    return !parsed.host.isNullOrEmpty() && parsed.userInfo == null && parsed.fragment == null &&
        (parsed.port == -1 || parsed.port in 1..65535)
}

fun credentialProblem(raw: String): String? =
    if (raw.any { it.code !in 32..126 }) "CREDENTIAL: USE PRINTABLE ASCII, WITHOUT LINE BREAKS." else null

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
            " MINUTES IN TOTAL FOR SCHEDULED POLLING."
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
