package dev.liamchu.glance

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FirebaseConfigTest {
    private val packageName = "dev.liamchu.glance"
    // Construct a synthetic key at runtime so it cannot be mistaken for a leaked credential.
    private val fakeApiKey = "AIza" + "0".repeat(35)
    private fun json() = JSONObject("""{
        "project_info":{"project_number":"123456789","project_id":"sample-project"},
        "client":[{"client_info":{"mobilesdk_app_id":"1:123456789:android:abcdef",
            "android_client_info":{"package_name":"dev.liamchu.glance"}},
            "api_key":[{"current_key":"${fakeApiKey}"}]}]
    }""")

    @Test fun `imports matching Android client and stores only required public fields`() {
        val root = json().put("unrelated", "not stored")
        val other = JSONObject(root.getJSONArray("client").getJSONObject(0).toString())
        other.getJSONObject("client_info").getJSONObject("android_client_info").put("package_name", "other.app")
        root.getJSONArray("client").put(other)
        val config = readFirebaseConfig(root.toString().byteInputStream(), packageName)
        assertEquals("sample-project", config.projectId)
        assertEquals(config, FirebaseConfig.fromStored(config.toJson()))
        assertFalse(config.toJson().contains("unrelated"))
    }

    @Test fun `rejects service account keys even with otherwise valid configuration`() {
        rejected(json().put("private_key", "PRIVATE KEY"))
        rejected(json().put("type", "service_account"))
    }

    @Test fun `rejects mismatched package and ambiguous clients`() {
        val wrong = json()
        wrong.getJSONArray("client").getJSONObject(0).getJSONObject("client_info")
            .getJSONObject("android_client_info").put("package_name", "other.app")
        rejected(wrong)
        val duplicate = json()
        duplicate.getJSONArray("client").put(duplicate.getJSONArray("client").getJSONObject(0))
        rejected(duplicate)
    }

    @Test fun `rejects missing malformed and inconsistent project fields`() {
        listOf("project_number", "project_id").forEach {
            val missing = json()
            missing.getJSONObject("project_info").remove(it)
            rejected(missing)
        }
        rejected(json().apply { getJSONObject("project_info").put("project_number", "999") })
        rejected(json().apply { getJSONObject("project_info").put("project_number", 123456789) })
        rejected(json().apply { getJSONArray("client").getJSONObject(0).getJSONArray("api_key")
            .getJSONObject(0).put("current_key", "invalid") })
        assertThrows(IllegalArgumentException::class.java) {
            FirebaseConfig.fromGoogleServices("not json", packageName)
        }
    }

    @Test fun `limits input size before parsing`() {
        assertThrows(IllegalArgumentException::class.java) {
            readFirebaseConfig(ByteArray(FirebaseConfig.MAX_FILE_BYTES + 1).inputStream(), packageName)
        }
    }

    private fun rejected(json: JSONObject) {
        assertThrows(IllegalArgumentException::class.java) { FirebaseConfig.fromGoogleServices(json.toString(), packageName) }
    }
}
