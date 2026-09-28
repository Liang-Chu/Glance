package dev.liamchu.glance

import androidx.compose.runtime.saveable.listSaver
import org.json.JSONObject
import java.util.UUID

/** Unsaved editor values. Scanning only changes a draft; only Save writes private preferences. */
data class WatcherDraft(
    val original: Watcher,
    val name: String = original.name,
    val url: String = original.url,
    val credential: String = original.credential,
    val delivery: String = original.delivery,
    val checkDays: String = original.checkDays.toString(),
    val checkHours: String = original.checkHours.toString(),
    val checkMinutes: String = original.checkMinutes.toString(),
    val expiryMinutes: String = original.expiryMinutes.toString(),
    val expirySeconds: String = original.expirySeconds.toString(),
    val maxLength: String = original.maxLength.toString(),
    val paired: Boolean = false,
) {
    override fun toString() = "WatcherDraft(redacted)"

    fun scanned(connection: PairingConnection, watchers: List<Watcher>): WatcherDraft {
        val firstNewScan = original.url.isEmpty() && !paired
        return copy(
            name = if (firstNewScan && name.isBlank()) nextPairingName(watchers) else name,
            url = connection.registrationUrl, credential = connection.credential, delivery = Watcher.PUSH,
            expiryMinutes = if (firstNewScan) "0" else expiryMinutes,
            expirySeconds = if (firstNewScan) PAIRING_EXPIRY_SECONDS.toString() else expirySeconds,
            paired = true,
        )
    }

    fun problem(watchers: List<Watcher>): String? {
        nameProblem(name)?.let { return it }
        if (watchers.any { it.id != original.id && it.name.trim().equals(name.trim(), ignoreCase = true) }) {
            return "NAME ALREADY EXISTS. CHOOSE A DIFFERENT WATCHER NAME."
        }
        if (!isUsableUrl(url.trim())) return "BACKEND URL MUST BE A FULL HTTP:// OR HTTPS:// ADDRESS."
        credentialProblem(credential)?.let { return it }
        if (delivery == Watcher.POLL) intervalProblem(checkDays, checkHours, checkMinutes)?.let { return it }
        return expiryProblem(expiryMinutes, expirySeconds) ?: maxLengthProblem(maxLength)
    }

    /** Called only after problem() succeeds. Re-scanning the same connection preserves its routing ID. */
    fun forSave(): Watcher = original.copy(
        name = name.trim(), url = url.trim(), credential = credential, delivery = delivery,
        checkDays = if (delivery == Watcher.PUSH) original.checkDays else checkNotNull(partOrNull(checkDays)),
        checkHours = if (delivery == Watcher.PUSH) original.checkHours else checkNotNull(partOrNull(checkHours)),
        checkMinutes = if (delivery == Watcher.PUSH) original.checkMinutes else checkNotNull(partOrNull(checkMinutes)),
        expiryMinutes = checkNotNull(partOrNull(expiryMinutes)), expirySeconds = checkNotNull(partOrNull(expirySeconds)),
        maxLength = maxLength.trim().toInt(),
        pushKey = if (delivery != Watcher.PUSH) "" else if (!original.isPush || original.pushKey.isEmpty() ||
            url.trim() != original.url || credential != original.credential) UUID.randomUUID().toString() else original.pushKey,
        pushRegistered = false, lastRunFailed = false,
    ).preserveRegistrationFrom(original)

    companion object {
        const val PAIRING_EXPIRY_SECONDS = 30
        val Saver = listSaver<WatcherDraft, Any>(
            save = { listOf(editorRecord(it.original), it.name, it.url, it.credential, it.delivery,
                it.checkDays, it.checkHours, it.checkMinutes, it.expiryMinutes, it.expirySeconds, it.maxLength, it.paired) },
            restore = { WatcherDraft(restoreEditor(it[0] as String), it[1] as String, it[2] as String,
                it[3] as String, it[4] as String, it[5] as String, it[6] as String, it[7] as String,
                it[8] as String, it[9] as String, it[10] as String, it[11] as Boolean) },
        )
    }
}

// Blank editors have no subscription ID and must not be parsed as persisted PUSH records.
fun editorRecord(watcher: Watcher): String =
    if (watcher.url.isEmpty()) watcher.id.toString() else watcher.toJson().toString()

fun restoreEditor(record: String): Watcher =
    if (record.startsWith("{")) Watcher.fromJson(JSONObject(record)) else Watcher.blank(record.toInt())
