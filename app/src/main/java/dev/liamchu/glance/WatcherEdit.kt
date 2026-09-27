package dev.liamchu.glance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Scan and manual entry edit the same unsaved draft, then use the same save/registration path. */
@Composable
fun WatcherEditScreen(watcher: Watcher, watchers: List<Watcher>, onDone: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable(watcher.id, stateSaver = WatcherDraft.Saver) { mutableStateOf(WatcherDraft(watcher)) }
    var message by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var duplicate by remember { mutableStateOf<PairingConnection?>(null) }
    BackHandler(saving) { /* Finish the settings transaction before leaving. */ }

    fun fill(connection: PairingConnection) {
        if (draft.original.url.isEmpty() && matchingConnections(connection, watchers).isNotEmpty()) {
            duplicate = connection
        } else {
            draft = draft.scanned(connection, watchers)
            message = "CONNECTION FILLED. REVIEW IT, THEN SAVE AND REGISTER."
        }
    }

    fun saveThen(action: (Watcher) -> Unit) {
        if (saving) return
        draft.problem(watchers)?.let { message = it; return }
        if (draft.paired && draft.original.url.isEmpty()) {
            val connection = PairingConnection(draft.url.trim(), draft.credential)
            if (matchingConnections(connection, watchers).isNotEmpty()) { duplicate = connection; return }
        }
        if (draft.delivery == Watcher.PUSH) {
            PushRegistration.unavailableReason(context)?.let { message = it; return }
        }
        val edited = draft.forSave()
        saving = true
        scope.launch {
            try {
                Diagnostics.event(DiagnosticEvent.SETTINGS_SAVE_STARTED, edited.id)
                WatcherStore.put(context, edited)
                draft = draft.copy(original = edited)
                Diagnostics.event(DiagnosticEvent.SETTINGS_SAVED, edited.id)
                action(edited)
                PushRegistration.enqueueRemovals(context)
                PushRegistration.updateAutoInit(context)
                Diagnostics.event(DiagnosticEvent.SETTINGS_SAVE_FINISHED, edited.id)
                onDone()
            } catch (e: java.io.IOException) {
                Diagnostics.failure(DiagnosticEvent.SETTINGS_WRITE_FAILED, e, edited.id)
                message = "COULD NOT SAVE SETTINGS. CHECK AVAILABLE STORAGE AND TRY AGAIN."
            } finally { saving = false }
        }
    }

    if (scanning) {
        ConnectionScanner(onRead = { raw ->
            if (scanning) {
                scanning = false
                try {
                    fill(PairingConnection.parse(raw))
                } catch (e: InvalidPairing) {
                    // Only this exception's fixed messages are safe; never display the scanned input.
                    message = e.message.orEmpty() + " TAP SCAN CONNECTION QR TO RETRY."
                }
            }
        }, onCancel = { scanning = false })
        return
    }

    duplicate?.let { connection ->
        AlertDialog(
            onDismissRequest = { duplicate = null },
            title = { Text("CONNECTION ALREADY EXISTS") },
            text = { Text("Choose the watcher to update. Its name and notification preferences will be kept. Nothing changes until you save.") },
            confirmButton = {
                Column {
                    matchingConnections(connection, watchers).forEach { existing ->
                        TextButton(onClick = {
                            draft = WatcherDraft(existing).scanned(connection, watchers)
                            duplicate = null
                            message = "EXISTING WATCHER SELECTED. SAVE AND REGISTER TO UPDATE."
                        }) { Text("UPDATE " + existing.name) }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { duplicate = null }) { Text("CANCEL") } },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (draft.original.url.isEmpty()) "NEW WATCHER" else "EDIT WATCHER",
            style = MaterialTheme.typography.headlineSmall, color = GlanceWhite)
        PixelRule()
        Outlined("SCAN CONNECTION QR", enabled = !saving) { message = ""; scanning = true }
        if (message.isNotEmpty()) Text("> " + message, style = MaterialTheme.typography.bodySmall, color = GlanceWhite)
        LocalNetworkAccess(enabled = !saving)
        if (draft.original.lastRunFailed) Text(
            "REGISTRATION / LAST RUN FAILED. Check the failure notification or DIAGNOSTICS. Verify the PC is online and reachable over LAN/Tailscale; re-scan if its key changed. Save again to retry.",
            style = MaterialTheme.typography.bodySmall, color = GlanceGrey)

        Field("Name", draft.name, KeyboardType.Text) { draft = draft.copy(name = it) }
        Outlined(if (draft.delivery == Watcher.PUSH) "DELIVERY: PUSH" else "DELIVERY: POLL", enabled = !saving) {
            draft = draft.copy(delivery = if (draft.delivery == Watcher.PUSH) Watcher.POLL else Watcher.PUSH)
        }
        Field(if (draft.delivery == Watcher.PUSH) "Registration URL" else "Backend URL", draft.url, KeyboardType.Uri) {
            draft = draft.copy(url = it)
        }
        Field("Credential / optional", draft.credential, KeyboardType.Password) { draft = draft.copy(credential = it) }
        if (draft.delivery == Watcher.POLL) {
            Duration("Check every") {
                NumberCell("DAYS", draft.checkDays, Modifier.weight(1f)) { draft = draft.copy(checkDays = it) }
                NumberCell("HOURS", draft.checkHours, Modifier.weight(1f)) { draft = draft.copy(checkHours = it) }
                NumberCell("MINS", draft.checkMinutes, Modifier.weight(1f)) { draft = draft.copy(checkMinutes = it) }
            }
            Text("> AT LEAST 15 MINUTES IN TOTAL.", style = MaterialTheme.typography.bodySmall, color = GlanceGrey)
        } else Text("> YOUR BACKEND SENDS UPDATES WHEN THEY ARE READY. NO POLLING INTERVAL.",
            style = MaterialTheme.typography.bodySmall, color = GlanceGrey)

        Duration("Expires after") {
            NumberCell("MINS", draft.expiryMinutes, Modifier.weight(1f)) { draft = draft.copy(expiryMinutes = it) }
            NumberCell("SECS", draft.expirySeconds, Modifier.weight(1f)) { draft = draft.copy(expirySeconds = it) }
            Box(Modifier.weight(1f))
        }
        Text("> AT LEAST 3 SECONDS, SO THE GLASSES RECEIVE IT FIRST.",
            style = MaterialTheme.typography.bodySmall, color = GlanceGrey)
        Field("Max length / characters", draft.maxLength, KeyboardType.Number) { draft = draft.copy(maxLength = it) }

        Filled(if (draft.delivery == Watcher.PUSH) "SAVE AND REGISTER" else "SAVE AND SCHEDULE", enabled = !saving) {
            saveThen { Scheduler.schedule(context, it) }
        }
        if (draft.delivery == Watcher.POLL) Outlined("SAVE AND CHECK NOW", enabled = !saving) {
            saveThen { Scheduler.schedule(context, it); Scheduler.checkNow(context, it) }
        }
        Outlined("BACK", enabled = !saving) { onCancel() }
        if (draft.original.url.isNotEmpty()) Outlined("DELETE THIS WATCHER", enabled = !saving) {
            if (saving) return@Outlined
            saving = true
            scope.launch {
                val id = draft.original.id
                try {
                    WatcherStore.delete(context, id)
                    Scheduler.cancel(context, id)
                    Notifier.clear(context, id)
                    PushRegistration.enqueueRemovals(context)
                    PushRegistration.updateAutoInit(context)
                    onDone()
                } catch (e: java.io.IOException) {
                    Diagnostics.failure(DiagnosticEvent.SETTINGS_WRITE_FAILED, e, id)
                    message = "COULD NOT DELETE. CHECK AVAILABLE STORAGE AND TRY AGAIN."
                } finally { saving = false }
            }
        }
    }
}
