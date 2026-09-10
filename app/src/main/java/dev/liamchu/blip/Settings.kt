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

/**
 * The five settings of DESIGN.md "What the user can change", plus the one flag
 * DESIGN.md "What a failed run shows" needs in order to speak only once.
 */
data class Settings(
    val url: String,
    val credential: String,
    val frequencyHours: Int,
    val expiryMinutes: Int,
    val maxLength: Int,
    val lastRunFailed: Boolean,
)

object SettingsStore {

    /**
     * The starting values, written here and nowhere else (criterion settings-2).
     * Supplying them when a stored preference is absent is the one place C1 allows
     * a stand-in: nothing downstream of this file defaults anything.
     */
    const val STARTING_URL = ""
    const val STARTING_CREDENTIAL = ""
    const val STARTING_FREQUENCY_HOURS = 4
    const val STARTING_EXPIRY_MINUTES = 10
    const val STARTING_MAX_LENGTH = 120

    private val KEY_URL = stringPreferencesKey("url")
    private val KEY_CREDENTIAL = stringPreferencesKey("credential")
    private val KEY_FREQUENCY = intPreferencesKey("frequency_hours")
    private val KEY_EXPIRY = intPreferencesKey("expiry_minutes")
    private val KEY_MAX_LENGTH = intPreferencesKey("max_length")
    private val KEY_LAST_RUN_FAILED = booleanPreferencesKey("last_run_failed")

    suspend fun read(context: Context): Settings {
        val stored = context.store.data.first()
        return Settings(
            url = stored[KEY_URL] ?: STARTING_URL,
            credential = stored[KEY_CREDENTIAL] ?: STARTING_CREDENTIAL,
            frequencyHours = stored[KEY_FREQUENCY] ?: STARTING_FREQUENCY_HOURS,
            expiryMinutes = stored[KEY_EXPIRY] ?: STARTING_EXPIRY_MINUTES,
            maxLength = stored[KEY_MAX_LENGTH] ?: STARTING_MAX_LENGTH,
            lastRunFailed = stored[KEY_LAST_RUN_FAILED] ?: false,
        )
    }

    suspend fun save(
        context: Context,
        url: String,
        credential: String,
        frequencyHours: Int,
        expiryMinutes: Int,
        maxLength: Int,
    ) {
        context.store.edit { stored ->
            stored[KEY_URL] = url
            stored[KEY_CREDENTIAL] = credential
            stored[KEY_FREQUENCY] = frequencyHours
            stored[KEY_EXPIRY] = expiryMinutes
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
