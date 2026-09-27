package dev.liamchu.glance

import org.json.JSONObject
import org.json.JSONException

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1000L

/**
 * One named thing being watched: where to ask, how often, and how it is shown.
 *
 * DESIGN.md "Many watchers, each named". Everything that used to belong to the
 * app as a whole belongs to one of these now. Schedules, notifications and
 * failures are separate; push watchers share one Firebase installation.
 */
data class Watcher(
    val id: Int,
    val name: String,
    val url: String,
    val credential: String,
    val checkDays: Int,
    val checkHours: Int,
    val checkMinutes: Int,
    val expiryMinutes: Int,
    val expirySeconds: Int,
    val maxLength: Int,
    val lastRunFailed: Boolean,
    val delivery: String = POLL,
    val pushKey: String = "",
    val pushRegistered: Boolean = false,
    val pushAddress: String = "",
) {
    val isPush: Boolean get() = delivery == PUSH

    fun sameConfiguration(other: Watcher): Boolean =
        copy(lastRunFailed = other.lastRunFailed, pushRegistered = other.pushRegistered,
            pushAddress = other.pushAddress) == other
    val intervalMinutes: Long
        get() = checkDays * MINUTES_PER_DAY + checkHours * MINUTES_PER_HOUR + checkMinutes

    val expiryTotalSeconds: Long
        get() = expiryMinutes * SECONDS_PER_MINUTE + expirySeconds

    val expiryMillis: Long
        get() = expiryTotalSeconds * MILLIS_PER_SECOND

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("url", url)
        .put("credential", credential)
        .put("check_days", checkDays)
        .put("check_hours", checkHours)
        .put("check_minutes", checkMinutes)
        .put("expiry_minutes", expiryMinutes)
        .put("expiry_seconds", expirySeconds)
        .put("max_length", maxLength)
        .put("last_run_failed", lastRunFailed)
        .put("delivery", delivery)
        .put("push_key", pushKey)
        .put("push_registered", pushRegistered)
        .put("push_address", pushAddress)

    companion object {
        const val POLL = "poll"
        const val PUSH = "push"
        /**
         * The values a new watcher starts with, written here and nowhere else
         * (criterion settings-2).
         */
        const val STARTING_CHECK_DAYS = 1
        const val STARTING_CHECK_HOURS = 0
        const val STARTING_CHECK_MINUTES = 0
        const val STARTING_EXPIRY_MINUTES = 0
        const val STARTING_EXPIRY_SECONDS = 3
        const val STARTING_MAX_LENGTH = 80

        fun blank(id: Int) = Watcher(
            id = id,
            name = "",
            url = "",
            credential = "",
            checkDays = STARTING_CHECK_DAYS,
            checkHours = STARTING_CHECK_HOURS,
            checkMinutes = STARTING_CHECK_MINUTES,
            expiryMinutes = STARTING_EXPIRY_MINUTES,
            expirySeconds = STARTING_EXPIRY_SECONDS,
            maxLength = STARTING_MAX_LENGTH,
            lastRunFailed = false,
            delivery = PUSH,
        )

        /**
         * Throws on a malformed member rather than defaulting one — this is stored
         * input being parsed at its entry point, which is where C1 wants the failure.
         */
        fun fromJson(json: JSONObject): Watcher {
            // Existing installations have polling watchers without push fields.
            val delivery = if (json.has("delivery")) json.getString("delivery") else POLL
            if (delivery != POLL && delivery != PUSH) throw JSONException("unknown delivery")
            val key = if (json.has("push_key")) json.getString("push_key") else ""
            if (delivery == PUSH && key.isEmpty()) throw JSONException("missing push key")
            return Watcher(
                id = json.getInt("id"),
                name = json.getString("name"),
                url = json.getString("url"),
                credential = json.getString("credential"),
                checkDays = json.getInt("check_days"),
                checkHours = json.getInt("check_hours"),
                checkMinutes = json.getInt("check_minutes"),
                expiryMinutes = json.getInt("expiry_minutes"),
                expirySeconds = json.getInt("expiry_seconds"),
                maxLength = json.getInt("max_length"),
                lastRunFailed = json.getBoolean("last_run_failed"),
                delivery = delivery,
                pushKey = key,
                pushRegistered = if (json.has("push_registered")) json.getBoolean("push_registered") else false,
                pushAddress = if (json.has("push_address")) json.getString("push_address") else "",
            )
        }
    }
}
