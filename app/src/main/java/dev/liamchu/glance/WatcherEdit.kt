package dev.liamchu.glance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * One watcher's settings. Everything here belongs to this watcher alone: another
 * watcher shares none of it, which is the whole point of DESIGN.md "Many
 * watchers, each named".
 */
@Composable
fun WatcherEditScreen(
    watcher: Watcher,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(watcher.name) }
    var url by remember { mutableStateOf(watcher.url) }
    var credential by remember { mutableStateOf(watcher.credential) }
    var checkDays by remember { mutableStateOf(watcher.checkDays.toString()) }
    var checkHours by remember { mutableStateOf(watcher.checkHours.toString()) }
    var checkMinutes by remember { mutableStateOf(watcher.checkMinutes.toString()) }
    var expiryMinutes by remember { mutableStateOf(watcher.expiryMinutes.toString()) }
    var expirySeconds by remember { mutableStateOf(watcher.expirySeconds.toString()) }
    var maxLength by remember { mutableStateOf(watcher.maxLength.toString()) }
    var message by remember { mutableStateOf("") }

    fun saveThen(action: (Watcher) -> Unit, note: (Watcher) -> String) {
        val problem = firstProblem(
            name, url, checkDays, checkHours, checkMinutes,
            expiryMinutes, expirySeconds, maxLength,
        )
        if (problem != null) {
            message = problem
            return
        }
        val edited = watcher.copy(
            name = name.trim(),
            url = url.trim(),
            credential = credential,
            checkDays = partOrNull(checkDays)!!,
            checkHours = partOrNull(checkHours)!!,
            checkMinutes = partOrNull(checkMinutes)!!,
            expiryMinutes = partOrNull(expiryMinutes)!!,
            expirySeconds = partOrNull(expirySeconds)!!,
            maxLength = maxLength.trim().toInt(),
        )
        scope.launch {
            WatcherStore.put(context, edited)
            action(edited)
            message = note(edited)
            onDone()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            if (watcher.name.isEmpty()) "NEW WATCHER" else "EDIT WATCHER",
            style = MaterialTheme.typography.headlineSmall,
            color = GlanceWhite,
        )
        PixelRule()

        Field("Name", name, KeyboardType.Text) { name = it }
        Field("Backend URL", url, KeyboardType.Uri) { url = it }
        Field("Credential / optional", credential, KeyboardType.Password) { credential = it }

        Duration("Check every") {
            NumberCell("DAYS", checkDays, Modifier.weight(1f)) { checkDays = it }
            NumberCell("HOURS", checkHours, Modifier.weight(1f)) { checkHours = it }
            NumberCell("MINS", checkMinutes, Modifier.weight(1f)) { checkMinutes = it }
        }
        Text(
            "> AT LEAST 15 MINUTES IN TOTAL.",
            style = MaterialTheme.typography.bodySmall,
            color = GlanceGrey,
        )

        Duration("Expires after") {
            NumberCell("MINS", expiryMinutes, Modifier.weight(1f)) { expiryMinutes = it }
            NumberCell("SECS", expirySeconds, Modifier.weight(1f)) { expirySeconds = it }
            Box(Modifier.weight(1f))
        }
        Text(
            "> AT LEAST 3 SECONDS, SO THE GLASSES RECEIVE IT FIRST.",
            style = MaterialTheme.typography.bodySmall,
            color = GlanceGrey,
        )

        Field("Max length / characters", maxLength, KeyboardType.Number) { maxLength = it }

        Filled("SAVE AND SCHEDULE") {
            saveThen({ Scheduler.schedule(context, it) }) { "SAVED." }
        }

        Outlined("SAVE AND CHECK NOW") {
            saveThen({
                Scheduler.schedule(context, it)
                Scheduler.checkNow(context, it)
            }) { "CHECKING NOW." }
        }

        Outlined("BACK") { onCancel() }

        Outlined("DELETE THIS WATCHER") {
            scope.launch {
                Scheduler.cancel(context, watcher.id)
                Notifier.clear(context, watcher.id)
                WatcherStore.delete(context, watcher.id)
                onDone()
            }
        }

        if (message.isNotEmpty()) {
            Text(
                "> " + message,
                style = MaterialTheme.typography.bodySmall,
                color = GlanceGrey,
            )
        }
    }
}

@Composable
private fun Filled(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = GlanceWhite,
            contentColor = GlanceBlack,
        ),
        onClick = onClick,
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun Outlined(label: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RectangleShape,
        border = BorderStroke(1.dp, GlanceWhite),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlanceBlack,
            contentColor = GlanceWhite,
        ),
        onClick = onClick,
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

/**
 * Criterion settings-3: refused where it is typed. Nothing is coerced or
 * defaulted on the way through — a field that does not parse stops the save.
 */
private fun firstProblem(
    name: String,
    url: String,
    checkDays: String,
    checkHours: String,
    checkMinutes: String,
    expiryMinutes: String,
    expirySeconds: String,
    maxLength: String,
): String? {
    nameProblem(name)?.let { return it }
    if (!isUsableUrl(url.trim())) return "BACKEND URL MUST BE A FULL HTTP:// OR HTTPS:// ADDRESS."
    return intervalProblem(checkDays, checkHours, checkMinutes)
        ?: expiryProblem(expiryMinutes, expirySeconds)
        ?: maxLengthProblem(maxLength)
}
