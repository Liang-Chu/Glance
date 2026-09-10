package dev.liamchu.glance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.net.URI
import java.net.URISyntaxException

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1000L

/**
 * The settings of DESIGN.md "What the user can change", plus the one flag
 * DESIGN.md "What a failed run shows" needs in order to speak only once.
 *
 * A duration is a field per unit, added together — days plus hours plus minutes,
 * or minutes plus seconds — rather than one number and a chosen unit.
 */
data class Settings(
    val url: String,
    val credential: String,
    val checkDays: Int,
    val checkHours: Int,
    val checkMinutes: Int,
    val expiryMinutes: Int,
    val expirySeconds: Int,
    val maxLength: Int,
    val lastRunFailed: Boolean,
) {
    val intervalMinutes: Long
        get() = checkDays * MINUTES_PER_DAY + checkHours * MINUTES_PER_HOUR + checkMinutes

    val expiryTotalSeconds: Long
        get() = expiryMinutes * SECONDS_PER_MINUTE + expirySeconds

    val expiryMillis: Long
        get() = expiryTotalSeconds * MILLIS_PER_SECOND
}

object SettingsStore {

    /**
     * The starting values, written here and nowhere else (criterion settings-2).
     * Supplying them when a stored preference is absent is the one place C1 allows
     * a stand-in: nothing downstream of this file defaults anything.
     */
    const val STARTING_URL = ""
    const val STARTING_CREDENTIAL = ""
    const val STARTING_CHECK_DAYS = 0
    const val STARTING_CHECK_HOURS = 4
    const val STARTING_CHECK_MINUTES = 0
    const val STARTING_EXPIRY_MINUTES = 10
    const val STARTING_EXPIRY_SECONDS = 0
    const val STARTING_MAX_LENGTH = 120

    /** WorkManager will not repeat work more often than this, and rounds up silently. */
    const val MINIMUM_INTERVAL_MINUTES = 15L

    /**
     * The shortest a notification may live. Long enough that the listener feeding
     * the glasses has certainly been handed it before Android takes it back, and
     * short enough to be gone from the phone before anyone looks.
     */
    const val MINIMUM_EXPIRY_SECONDS = 3L

    private val KEY_URL = stringPreferencesKey("url")
    private val KEY_CREDENTIAL = stringPreferencesKey("credential")
    private val KEY_CHECK_DAYS = intPreferencesKey("check_days")
    private val KEY_CHECK_HOURS = intPreferencesKey("check_hours")
    private val KEY_CHECK_MINUTES = intPreferencesKey("check_minutes")
    private val KEY_EXPIRY_MINUTES = intPreferencesKey("expiry_minutes")
    private val KEY_EXPIRY_SECONDS = intPreferencesKey("expiry_seconds")
    private val KEY_MAX_LENGTH = intPreferencesKey("max_length")
    private val KEY_LAST_RUN_FAILED = booleanPreferencesKey("last_run_failed")

    suspend fun read(context: Context): Settings {
        val stored = context.store.data.first()
        return Settings(
            url = stored[KEY_URL] ?: STARTING_URL,
            credential = stored[KEY_CREDENTIAL] ?: STARTING_CREDENTIAL,
            checkDays = stored[KEY_CHECK_DAYS] ?: STARTING_CHECK_DAYS,
            checkHours = stored[KEY_CHECK_HOURS] ?: STARTING_CHECK_HOURS,
            checkMinutes = stored[KEY_CHECK_MINUTES] ?: STARTING_CHECK_MINUTES,
            expiryMinutes = stored[KEY_EXPIRY_MINUTES] ?: STARTING_EXPIRY_MINUTES,
            expirySeconds = stored[KEY_EXPIRY_SECONDS] ?: STARTING_EXPIRY_SECONDS,
            maxLength = stored[KEY_MAX_LENGTH] ?: STARTING_MAX_LENGTH,
            lastRunFailed = stored[KEY_LAST_RUN_FAILED] ?: false,
        )
    }

    suspend fun save(
        context: Context,
        url: String,
        credential: String,
        checkDays: Int,
        checkHours: Int,
        checkMinutes: Int,
        expiryMinutes: Int,
        expirySeconds: Int,
        maxLength: Int,
    ) {
        context.store.edit { stored ->
            stored[KEY_URL] = url
            stored[KEY_CREDENTIAL] = credential
            stored[KEY_CHECK_DAYS] = checkDays
            stored[KEY_CHECK_HOURS] = checkHours
            stored[KEY_CHECK_MINUTES] = checkMinutes
            stored[KEY_EXPIRY_MINUTES] = expiryMinutes
            stored[KEY_EXPIRY_SECONDS] = expirySeconds
            stored[KEY_MAX_LENGTH] = maxLength
        }
    }

    suspend fun setLastRunFailed(context: Context, failed: Boolean) {
        context.store.edit { stored -> stored[KEY_LAST_RUN_FAILED] = failed }
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
    if (total < SettingsStore.MINIMUM_INTERVAL_MINUTES) {
        return "CHECK EVERY: AT LEAST " + SettingsStore.MINIMUM_INTERVAL_MINUTES +
            " MINUTES IN TOTAL. ANDROID WILL NOT REPEAT WORK FASTER."
    }
    return null
}

/**
 * Criterion settings-9. Three seconds is a floor rather than a preference: below
 * it there is no guarantee the notification is still there when the listener that
 * feeds the glasses goes looking, so a shorter one risks vanishing from both
 * places rather than just the phone.
 */
fun expiryProblem(minutes: String, seconds: String): String? {
    val m = partOrNull(minutes)
    val s = partOrNull(seconds)
    if (m == null || s == null) return "EXPIRES AFTER: WHOLE NUMBERS, 0 OR MORE."
    if (m * SECONDS_PER_MINUTE + s < SettingsStore.MINIMUM_EXPIRY_SECONDS) {
        return "EXPIRES AFTER: AT LEAST " + SettingsStore.MINIMUM_EXPIRY_SECONDS +
            " SECONDS IN TOTAL, SO THE GLASSES RECEIVE IT."
    }
    return null
}

fun maxLengthProblem(raw: String): String? {
    val value = raw.trim().toIntOrNull()
    if (value == null || value < 1) return "MAX LENGTH: A WHOLE NUMBER, AT LEAST 1."
    return null
}
