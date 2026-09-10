package dev.liamchu.blip

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

/** How often to check. Stored as a number and one of these. */
enum class IntervalUnit(val label: String, val minutesEach: Long) {
    MINUTES("MIN", 1L),
    HOURS("HOUR", 60L),
    DAYS("DAY", 1440L),
}

/** How long a notification stays on the phone. */
enum class ExpiryUnit(val label: String, val millisEach: Long) {
    SECONDS("SEC", 1_000L),
    MINUTES("MIN", 60_000L),
}

/**
 * The settings of DESIGN.md "What the user can change", plus the one flag
 * DESIGN.md "What a failed run shows" needs in order to speak only once.
 */
data class Settings(
    val url: String,
    val credential: String,
    val intervalValue: Int,
    val intervalUnit: IntervalUnit,
    val expiryValue: Int,
    val expiryUnit: ExpiryUnit,
    val maxLength: Int,
    val lastRunFailed: Boolean,
) {
    val intervalMinutes: Long get() = intervalValue * intervalUnit.minutesEach

    /** Zero is legal and means "gone from the phone at once". */
    val expiryMillis: Long get() = expiryValue * expiryUnit.millisEach
}

object SettingsStore {

    /**
     * The starting values, written here and nowhere else (criterion settings-2).
     * Supplying them when a stored preference is absent is the one place C1 allows
     * a stand-in: nothing downstream of this file defaults anything.
     */
    const val STARTING_URL = ""
    const val STARTING_CREDENTIAL = ""
    const val STARTING_INTERVAL_VALUE = 4
    val STARTING_INTERVAL_UNIT = IntervalUnit.HOURS
    const val STARTING_EXPIRY_VALUE = 10
    val STARTING_EXPIRY_UNIT = ExpiryUnit.MINUTES
    const val STARTING_MAX_LENGTH = 120

    /** WorkManager will not schedule repeating work more often than this. */
    const val MINIMUM_INTERVAL_MINUTES = 15L

    private val KEY_URL = stringPreferencesKey("url")
    private val KEY_CREDENTIAL = stringPreferencesKey("credential")
    private val KEY_INTERVAL_VALUE = intPreferencesKey("interval_value")
    private val KEY_INTERVAL_UNIT = stringPreferencesKey("interval_unit")
    private val KEY_EXPIRY_VALUE = intPreferencesKey("expiry_value")
    private val KEY_EXPIRY_UNIT = stringPreferencesKey("expiry_unit")
    private val KEY_MAX_LENGTH = intPreferencesKey("max_length")
    private val KEY_LAST_RUN_FAILED = booleanPreferencesKey("last_run_failed")

    suspend fun read(context: Context): Settings {
        val stored = context.store.data.first()
        return Settings(
            url = stored[KEY_URL] ?: STARTING_URL,
            credential = stored[KEY_CREDENTIAL] ?: STARTING_CREDENTIAL,
            intervalValue = stored[KEY_INTERVAL_VALUE] ?: STARTING_INTERVAL_VALUE,
            intervalUnit = intervalUnitNamed(stored[KEY_INTERVAL_UNIT]),
            expiryValue = stored[KEY_EXPIRY_VALUE] ?: STARTING_EXPIRY_VALUE,
            expiryUnit = expiryUnitNamed(stored[KEY_EXPIRY_UNIT]),
            maxLength = stored[KEY_MAX_LENGTH] ?: STARTING_MAX_LENGTH,
            lastRunFailed = stored[KEY_LAST_RUN_FAILED] ?: false,
        )
    }

    suspend fun save(
        context: Context,
        url: String,
        credential: String,
        intervalValue: Int,
        intervalUnit: IntervalUnit,
        expiryValue: Int,
        expiryUnit: ExpiryUnit,
        maxLength: Int,
    ) {
        context.store.edit { stored ->
            stored[KEY_URL] = url
            stored[KEY_CREDENTIAL] = credential
            stored[KEY_INTERVAL_VALUE] = intervalValue
            stored[KEY_INTERVAL_UNIT] = intervalUnit.name
            stored[KEY_EXPIRY_VALUE] = expiryValue
            stored[KEY_EXPIRY_UNIT] = expiryUnit.name
            stored[KEY_MAX_LENGTH] = maxLength
        }
    }

    suspend fun setLastRunFailed(context: Context, failed: Boolean) {
        context.store.edit { stored -> stored[KEY_LAST_RUN_FAILED] = failed }
    }

    private fun intervalUnitNamed(stored: String?): IntervalUnit =
        IntervalUnit.entries.firstOrNull { it.name == stored } ?: STARTING_INTERVAL_UNIT

    private fun expiryUnitNamed(stored: String?): ExpiryUnit =
        ExpiryUnit.entries.firstOrNull { it.name == stored } ?: STARTING_EXPIRY_UNIT
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

/**
 * Criterion settings-8. WorkManager silently rounds a shorter period up to its
 * own floor, so a refusal here is the only way the user learns that "every 5
 * minutes" was never going to happen. Clamping it quietly would be the
 * derivation CONSTRAINTS.md "C5 — Independent axes stay independent" forbids.
 */
fun intervalProblem(rawValue: String, unit: IntervalUnit): String? {
    val value = rawValue.trim().toIntOrNull()
    if (value == null || value < 1) return "CHECK EVERY: A WHOLE NUMBER, AT LEAST 1."
    if (value * unit.minutesEach < SettingsStore.MINIMUM_INTERVAL_MINUTES) {
        return "ANDROID WILL NOT CHECK MORE OFTEN THAN EVERY " +
            SettingsStore.MINIMUM_INTERVAL_MINUTES + " MINUTES."
    }
    return null
}

/** Criterion settings-9: zero is legal here, and means "gone at once". */
fun expiryProblem(rawValue: String): String? {
    val value = rawValue.trim().toIntOrNull()
    if (value == null || value < 0) return "EXPIRES AFTER: A WHOLE NUMBER, 0 OR MORE."
    return null
}

fun maxLengthProblem(rawValue: String): String? {
    val value = rawValue.trim().toIntOrNull()
    if (value == null || value < 1) return "MAX LENGTH: A WHOLE NUMBER, AT LEAST 1."
    return null
}
