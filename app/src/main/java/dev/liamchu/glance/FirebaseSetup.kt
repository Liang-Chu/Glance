package dev.liamchu.glance

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun FirebaseSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<FirebaseConfig?>(null) }
    var candidate by remember { mutableStateOf<FirebaseConfig?>(null) }
    var busy by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var importedProject by rememberSaveable { mutableStateOf<String?>(null) }
    var restart by remember { mutableStateOf(FirebaseRuntime.restartRequired) }
    var backendHelp by rememberSaveable { mutableStateOf(false) }
    BackHandler(busy) { /* Finish the configuration transaction before leaving. */ }

    LaunchedEffect(Unit) {
        current = WatcherStore.firebaseConfig(context)
        loaded = true
        busy = false
    }

    suspend fun applyConfig(config: FirebaseConfig) {
        WatcherStore.configureFirebase(context, config)
        FirebaseRuntime.select(context, config)
        Diagnostics.event(DiagnosticEvent.CONFIG_IMPORTED)
        current = config
        restart = FirebaseRuntime.restartRequired
        PushRegistration.enqueueRemovals(context)
        val watchers = WatcherStore.all(context).filter { it.isPush }
        if (restart) watchers.forEach { PushRegistration.cancel(context, it.id) }
        else {
            PushRegistration.updateAutoInit(context)
            watchers.forEach { PushRegistration.enqueue(context, it.id) }
        }
        message = ""
        importedProject = config.projectId
    }

    fun confirmImport(config: FirebaseConfig) {
        busy = true
        message = ""
        scope.launch {
            try { applyConfig(config) }
            catch (e: IOException) {
                Diagnostics.failure(DiagnosticEvent.CONFIG_IMPORT_FAILED, e)
                message = "COULD NOT SAVE. CHECK AVAILABLE STORAGE AND TRY AGAIN."
            }
            finally { busy = false }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            message = ""
            scope.launch {
                try {
                    val parsed = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("File unavailable")
                        stream.use { readFirebaseConfig(it, context.packageName) }
                    }
                    if (current != null && current != parsed) candidate = parsed else applyConfig(parsed)
                } catch (e: IllegalArgumentException) {
                    Diagnostics.failure(DiagnosticEvent.CONFIG_IMPORT_FAILED, e)
                    message = e.message ?: "INVALID FIREBASE CONFIGURATION."
                } catch (e: IOException) {
                    Diagnostics.failure(DiagnosticEvent.CONFIG_IMPORT_FAILED, e)
                    message = "COULD NOT READ OR SAVE. DOWNLOAD GOOGLE-SERVICES.JSON AND TRY AGAIN."
                } catch (e: SecurityException) {
                    Diagnostics.failure(DiagnosticEvent.CONFIG_IMPORT_FAILED, e)
                    message = "FILE ACCESS WAS DENIED. CHOOSE THE FILE AGAIN."
                } finally { busy = false }
            }
        }
    }

    importedProject?.let { projectId ->
        AlertDialog(
            onDismissRequest = { importedProject = null },
            shape = RectangleShape,
            containerColor = GlanceWhite,
            titleContentColor = GlanceBlack,
            textContentColor = GlanceBlack,
            title = { Text("IMPORT SUCCESSFUL", style = MaterialTheme.typography.labelLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Firebase project", style = MaterialTheme.typography.bodySmall)
                    SelectionContainer {
                        Text(projectId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(if (restart) "Saved. Restart Glance to activate this project. Follow the steps below."
                        else "Saved on this phone. Authorize your backend, then add a PUSH watcher.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { importedProject = null }) { Text("GOT IT", color = GlanceBlack) }
            },
        )
    }

    candidate?.let { replacement ->
        AlertDialog(onDismissRequest = { candidate = null }, shape = RectangleShape,
            containerColor = GlanceNearBlack, titleContentColor = GlanceWhite, textContentColor = GlanceGrey,
            title = { Text("CHANGE FIREBASE PROJECT?", style = MaterialTheme.typography.labelLarge) },
            text = { Text("All PUSH watchers will register again. Their backends need permission to send to " +
                replacement.projectId + ". You must force-stop and reopen Glance to activate the change.",
                style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { candidate = null; confirmImport(replacement) }) { Text("IMPORT") } },
            dismissButton = { TextButton(onClick = { candidate = null }) { Text("CANCEL") } })
    }

    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp, 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("PUSH SETUP", style = MaterialTheme.typography.headlineSmall)
            PixelRule()
            current?.let { config ->
                Column(Modifier.fillMaxWidth().border(1.dp, GlanceWhite).background(GlanceNearBlack).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (restart) "SAVED · RESTART REQUIRED" else "PROJECT IMPORTED",
                        style = MaterialTheme.typography.labelLarge, color = GlanceWhite)
                    SelectionContainer {
                        Text(config.projectId, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold, color = GlanceWhite)
                    }
                }
            } ?: Text(if (loaded) "NO PROJECT IMPORTED" else "LOADING PROJECT...",
                style = MaterialTheme.typography.labelLarge)
            Text("1. Open Firebase Console and create a project. Skip Analytics.")
            Outlined("OPEN FIREBASE CONSOLE") {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://console.firebase.google.com/".toUri()))
            }
            Text("2. Open Project settings > General > Your apps > Add app > Android. Enter this package name:")
            SelectionContainer { Text(context.packageName, style = MaterialTheme.typography.labelLarge) }
            Text("3. Register the app. Download google-services.json to this phone, then import it below. Skip the SDK steps.")
            Filled(when {
                !loaded -> "LOADING..."
                busy -> "IMPORTING..."
                current != null -> "CHANGE CONFIG FILE"
                else -> "IMPORT CONFIG FILE"
            }, enabled = !busy) {
                picker.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
            }
            if (message.isNotEmpty()) Text(message, color = GlanceWhite)
            Text("4. In Project settings > Cloud Messaging, check that Firebase Cloud Messaging API (V1) is enabled. " +
                "Give your backend permission to send to this project.")
            Outlined(if (backendHelp) "HIDE BACKEND SETUP" else "HOW TO AUTHORIZE MY BACKEND") { backendHelp = !backendHelp }
            if (backendHelp) {
                Text("Same project: Project settings > Service accounts > Generate new private key. " +
                    "Configure that key on your backend only.")
                Text("Another project: in this project's Google Cloud IAM, grant your backend's service account " +
                    "the Firebase Cloud Messaging API Admin role. Enable the FCM API in its project too.")
                Text("Send to the project shown above. Only authorize trusted backends; access covers the whole project. " +
                    "Never import a private key into Glance.")
                Outlined("OPEN AUTHORIZATION GUIDE") {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        "https://firebase.google.com/docs/cloud-messaging/send/v1-api#authorize_a_service_account_from_a_different_project".toUri()))
                }
            }
            Text("5. Go back > NEW WATCHER. Scan your connection QR or enter the backend URL and credential. " +
                "Tap SAVE AND REGISTER, then send a test from your backend.")
            Text("All PUSH watchers share this project. Push requires Google Play services and connectivity to FCM.",
                style = MaterialTheme.typography.bodySmall)
            if (restart) {
                PixelRule()
                Text("RESTART REQUIRED: Open Android app settings, tap Force stop, then reopen Glance. " +
                    "Your watchers are kept. Push is paused until the restart.", color = GlanceWhite)
                Outlined("OPEN ANDROID APP SETTINGS") {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ("package:" + context.packageName).toUri()))
                }
            }
            Outlined("BACK", enabled = !busy, onClick = onBack)
        }
    }
}
