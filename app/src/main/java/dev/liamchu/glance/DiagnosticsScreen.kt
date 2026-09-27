package dev.liamchu.glance

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var hasCrash by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { hasCrash = withContext(Dispatchers.IO) { Diagnostics.hasCrash() } }
    BackHandler(busy) { /* Keep the export alive while writing the selected document. */ }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val report = Diagnostics.report(context)
                        val stream = context.contentResolver.openOutputStream(uri, "wt")
                            ?: throw IOException("Export destination unavailable")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(report) }
                    }
                    message = "SAVED. ATTACH THE TEXT FILE WHEN REPORTING THE PROBLEM."
                } catch (e: IOException) {
                    Diagnostics.failure(DiagnosticEvent.EXPORT_FAILED, e)
                    message = "COULD NOT SAVE. CHECK SPACE OR CHOOSE ANOTHER LOCATION."
                } catch (e: SecurityException) {
                    Diagnostics.failure(DiagnosticEvent.EXPORT_FAILED, e)
                    message = "ACCESS DENIED. CHOOSE ANOTHER LOCATION."
                } finally { busy = false }
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("DIAGNOSTICS", style = MaterialTheme.typography.headlineSmall)
        PixelRule()
        Text(if (hasCrash) "A CRASH REPORT IS SAVED" else "LOCAL LOGGING IS ACTIVE",
            style = MaterialTheme.typography.labelLarge)
        Text("After a problem, reopen Glance and tap EXPORT LOGS. Save the text file, then attach it to your bug report.",
            style = MaterialTheme.typography.bodyMedium)
        Text("Includes recent push, registration and polling events, HTTP status codes, and the last recorded crash stack. " +
            "Android 11+ can also report recent process exits such as a crash, ANR or system termination.",
            style = MaterialTheme.typography.bodyMedium)
        Text("Logs stay on this phone until you export them. Credentials, URLs, Firebase IDs, watcher names, " +
            "notification content and exception messages are excluded. No automatic upload.",
            style = MaterialTheme.typography.bodyMedium)
        Text("Storage is bounded: two 256 KiB event files and one 64 KiB crash report. Logs survive restart and " +
            "Clear cache. Clearing app data or uninstalling removes them. A sudden kill may leave no crash stack.",
            style = MaterialTheme.typography.bodySmall)
        if (Diagnostics.writeFailed) Text("LOG STORAGE FAILED. EXPORT AVAILABLE RECORDS AND CHECK FREE SPACE.",
            style = MaterialTheme.typography.bodySmall)
        Filled(if (busy) "WORKING..." else "EXPORT LOGS", enabled = !busy) {
            export.launch("glance-diagnostics-${LocalDate.now()}.txt")
        }
        Outlined("CLEAR LOCAL LOGS", enabled = !busy) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { Diagnostics.clear() }
                    hasCrash = false
                    message = "LOCAL LOGS CLEARED. ANDROID EXIT HISTORY AND EXPORTED COPIES ARE UNAFFECTED."
                } catch (_: IOException) {
                    message = "COULD NOT CLEAR LOCAL LOGS."
                } finally { busy = false }
            }
        }
        if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodySmall)
        Outlined("BACK", enabled = !busy, onClick = onBack)
    }
}
