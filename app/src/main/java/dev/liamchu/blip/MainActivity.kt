package dev.liamchu.blip

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The five fields of DESIGN.md "What the user can change" and nothing else
 * (criterion settings-1). Once they are filled in there is no reason to come
 * back here: the work runs without the app being opened.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { SettingsScreen() }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var maxLength by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var notificationsAllowed by remember { mutableStateOf(true) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = Notifier.canPost(context) }

    LaunchedEffect(Unit) {
        val stored = SettingsStore.read(context)
        url = stored.url
        credential = stored.credential
        frequency = stored.frequencyHours.toString()
        expiry = stored.expiryMinutes.toString()
        maxLength = stored.maxLength.toString()
        notificationsAllowed = Notifier.canPost(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Blip", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Calls your backend on a schedule and shows whatever it returns, " +
                "then takes it away again.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Field("Backend URL", url, KeyboardType.Uri) { url = it }
        Field("Credential (optional)", credential, KeyboardType.Password) { credential = it }
        Field("Check every (hours)", frequency, KeyboardType.Number) { frequency = it }
        Field("Expires after (minutes)", expiry, KeyboardType.Number) { expiry = it }
        Field("Maximum length (characters)", maxLength, KeyboardType.Number) { maxLength = it }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val problem = firstProblem(url, frequency, expiry, maxLength)
                if (problem != null) {
                    message = problem
                    return@Button
                }
                scope.launch {
                    val hours = frequency.trim().toInt()
                    SettingsStore.save(
                        context = context,
                        url = url.trim(),
                        credential = credential,
                        frequencyHours = hours,
                        expiryMinutes = expiry.trim().toInt(),
                        maxLength = maxLength.trim().toInt(),
                    )
                    Scheduler.schedule(context, hours)
                    message = "Saved. Next check within " + hours + "h."
                }
            },
        ) { Text("Save and schedule") }

        if (!notificationsAllowed) {
            Text(
                "Notifications are switched off, so nothing can be shown. " +
                    "Allow them, and add Blip in the Even Realities app under Notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Allow notifications")
                }
            }
        }

        if (message.isNotEmpty()) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

/**
 * Criterion settings-3: refused where it is typed. Nothing is coerced or
 * defaulted on the way through — a field that does not parse stops the save.
 */
private fun firstProblem(
    url: String,
    frequency: String,
    expiry: String,
    maxLength: String,
): String? {
    if (!isUsableUrl(url.trim())) return "The backend URL must be a full http:// or https:// address."
    if (!isPositive(frequency)) return "Check every: whole number of hours, at least 1."
    if (!isPositive(expiry)) return "Expires after: whole number of minutes, at least 1."
    if (!isPositive(maxLength)) return "Maximum length: whole number of characters, at least 1."
    return null
}

private fun isPositive(field: String): Boolean {
    val parsed = field.trim().toIntOrNull()
    return parsed != null && parsed >= 1
}
