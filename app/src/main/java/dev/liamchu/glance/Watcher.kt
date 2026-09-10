package dev.liamchu.glance

import org.json.JSONObject

private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1000L

/**
 * One named thing being watched: where to ask, how often, and how it is shown.
 *
 * DESIGN.md "Many watchers, each named". Everything that used to belong to the
 * app as a whole belongs to one of these now, so two watchers share nothing but
 * the code — not a schedule, not a notification, not a failure.
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
) {
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

    companion object {
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
        )

        /**
         * Throws on a malformed member rather than defaulting one — this is stored
         * input being parsed at its entry point, which is where C1 wants the failure.
         */
        fun fromJson(json: JSONObject) = Watcher(
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
        )
    }
}
