package dev.liamchu.glance

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicBoolean

/** The desktop's v1 URL. Never include the source URL or credential in diagnostics. */
class PairingConnection(val registrationUrl: String, val credential: String) {
    override fun toString() = "PairingConnection(redacted)"

    companion object {
        fun parse(raw: String): PairingConnection {
            // Bound camera input before allocating a list of fragment parameters.
            if (raw.length > 8192) throw InvalidPairing("CONNECTION QR IS TOO LARGE.")
            val uri = try { URI(raw) } catch (_: URISyntaxException) {
                throw InvalidPairing("INVALID CONNECTION URL. SCAN THE DESKTOP CONNECT PHONE QR.")
            }
            val scheme = uri.scheme?.lowercase()
            if ((scheme != "http" && scheme != "https") || uri.host.isNullOrEmpty() ||
                uri.rawUserInfo != null || uri.rawQuery != null ||
                (uri.rawPath != "" && uri.rawPath != "/") ||
                (uri.port != -1 && uri.port !in 1..65535) || uri.rawAuthority.endsWith(":")) {
                throw InvalidPairing("USE A ROOT HTTP(S) CONNECTION URL WITHOUT A LOGIN, PATH OR QUERY.")
            }
            val fragment = uri.rawFragment
                ?: throw InvalidPairing("MISSING CONNECTION PARAMETERS. SCAN THE DESKTOP CONNECT PHONE QR.")
            val parameters = mutableMapOf<String, String>()
            for (part in fragment.split('&').filter { it.isNotEmpty() }) {
                val name = formDecode(part.substringBefore('='))
                val value = formDecode(part.substringAfter('=', ""))
                if (parameters.containsKey(name)) throw InvalidPairing("DUPLICATE CONNECTION QR PARAMETER.")
                parameters[name] = value
            }
            if (parameters["pilot-pair"] != "1") throw InvalidPairing("MISSING OR UNSUPPORTED CONNECTION QR VERSION.")
            val credential = parameters["pilot-token"]
                ?: throw InvalidPairing("CONNECTION QR HAS NO KEY.")
            // String.length counts UTF-16 units, as does JavaScript. Do not trim or decode again.
            if (credential.length !in 24..512 || credential.any { it == '\r' || it == '\n' }) {
                throw InvalidPairing("INVALID CONNECTION KEY. EXPECT 24–512 CHARACTERS WITHOUT LINE BREAKS.")
            }
            val port = if ((scheme == "http" && uri.port == 80) || (scheme == "https" && uri.port == 443)) -1 else uri.port
            val endpoint = URI(scheme, null, uri.host.lowercase(), port, "/api/glance", null, null).toASCIIString()
            return PairingConnection(endpoint, credential)
        }

        private fun formDecode(raw: String): String = try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (_: IllegalArgumentException) {
            // Do not retain a decoder exception: its message may contain the key.
            throw InvalidPairing("INVALID CONNECTION QR ENCODING.")
        }
    }
}

class InvalidPairing(message: String) : IllegalArgumentException(message)

/** One camera session may deliver at most one result; late frames after cancellation are ignored. */
class ScanSession {
    private val stopped = AtomicBoolean(false)
    fun accept(): Boolean = stopped.compareAndSet(false, true)
    fun cancel() { stopped.set(true) }
}

/** Match the endpoint even when its key has rotated; never silently add a second subscription. */
fun matchingConnections(connection: PairingConnection, watchers: List<Watcher>): List<Watcher> =
    watchers.filter { sameRegistrationUrl(it.url, connection.registrationUrl) }

private fun sameRegistrationUrl(first: String, second: String): Boolean {
    if (!isUsableUrl(first) || !isUsableUrl(second)) return false
    fun normalized(value: String): URI {
        val uri = URI(value)
        val scheme = uri.scheme.lowercase()
        val port = if ((scheme == "http" && uri.port == 80) || (scheme == "https" && uri.port == 443)) -1 else uri.port
        return URI(scheme, null, uri.host.lowercase(), port, uri.path, uri.query, null)
    }
    return normalized(first) == normalized(second)
}

fun nextPairingName(watchers: List<Watcher>): String {
    val names = watchers.map { it.name.trim().lowercase() }.toSet()
    var candidate = "Even-PIlot"
    var suffix = 2
    while (candidate.lowercase() in names) candidate = "Even-PIlot (${suffix++})"
    return candidate
}
