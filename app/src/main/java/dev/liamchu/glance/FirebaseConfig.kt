package dev.liamchu.glance

import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun readFirebaseConfig(stream: InputStream, packageName: String): FirebaseConfig {
    val bytes = ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (true) {
        val count = stream.read(buffer, 0, minOf(buffer.size, FirebaseConfig.MAX_FILE_BYTES + 1 - bytes.size()))
        if (count < 0) break
        bytes.write(buffer, 0, count)
        require(bytes.size() <= FirebaseConfig.MAX_FILE_BYTES) { "Configuration file exceeds 256 KiB." }
    }
    return FirebaseConfig.fromGoogleServices(bytes.toString(Charsets.UTF_8.name()), packageName)
}

/** Only public Android client configuration is accepted, never server credentials. */
data class FirebaseConfig(
    val projectId: String,
    val applicationId: String,
    val senderId: String,
    val apiKey: String,
) {
    fun toJson(): String = JSONObject()
        .put("project_id", projectId).put("app_id", applicationId)
        .put("sender_id", senderId).put("api_key", apiKey).toString()

    companion object {
        const val MAX_FILE_BYTES = 262144

        fun fromGoogleServices(raw: String, packageName: String): FirebaseConfig {
            try {
                val root = JSONObject(raw)
                require(!root.has("private_key") && !root.has("type")) {
                    "Choose google-services.json, not a service-account private key."
                }
                val project = root.getJSONObject("project_info")
                val clients = root.getJSONArray("client")
                val matching = (0 until clients.length()).map { clients.getJSONObject(it) }.filter {
                    it.optJSONObject("client_info")?.optJSONObject("android_client_info")
                        ?.optString("package_name") == packageName
                }
                require(matching.size == 1) { "The file must contain one Android app for $packageName." }
                val client = matching.single()
                val keys = client.getJSONArray("api_key")
                require(keys.length() == 1) { "Expected one Android API key in google-services.json." }
                return validate(FirebaseConfig(
                    project.requiredString("project_id"),
                    client.getJSONObject("client_info").requiredString("mobilesdk_app_id"),
                    project.requiredString("project_number"),
                    keys.getJSONObject(0).requiredString("current_key"),
                ))
            } catch (_: JSONException) {
                throw IllegalArgumentException("Invalid google-services.json: required Android configuration is missing.")
            }
        }

        fun fromStored(raw: String): FirebaseConfig {
            val json = JSONObject(raw)
            return validate(FirebaseConfig(json.requiredString("project_id"), json.requiredString("app_id"),
                json.requiredString("sender_id"), json.requiredString("api_key")))
        }

        private fun validate(config: FirebaseConfig): FirebaseConfig {
            require(config.projectId.matches(Regex("[a-z][a-z0-9-]{4,28}[a-z0-9]"))) { "Invalid Firebase project ID." }
            require(config.senderId.matches(Regex("[0-9]+"))) { "Invalid Firebase project number." }
            require(config.applicationId.matches(Regex("1:${config.senderId}:android:[a-fA-F0-9]+"))) {
                "Android app ID does not match the Firebase project number."
            }
            require(config.apiKey.matches(Regex("AIza[A-Za-z0-9_-]{35}"))) { "Invalid Android API key." }
            return config
        }

        private fun JSONObject.requiredString(name: String): String =
            (get(name) as? String)?.takeIf { it.isNotBlank() }
                ?: throw JSONException("Expected string")
    }
}
