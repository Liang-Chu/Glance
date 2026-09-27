package dev.liamchu.glance

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

/**
 * The watchers, and one of them being edited. DESIGN.md "Many watchers, each
 * named", in the monochrome of DESIGN.md "How it looks".
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlanceTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = GlanceBlack) {
                    Screens()
                }
            }
        }
    }
}

@Composable
private fun Screens() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var watchers by remember { mutableStateOf(emptyList<Watcher>()) }
    var editingRecord by rememberSaveable { mutableStateOf<String?>(null) }
    var configuring by rememberSaveable { mutableStateOf(false) }
    var firebaseProject by remember { mutableStateOf<String?>(null) }
    var firebaseLoaded by remember { mutableStateOf(false) }
    var diagnosing by rememberSaveable { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(true) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = Notifier.canPost(context) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationsAllowed = Notifier.canPost(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(editingRecord != null || configuring || diagnosing) {
        editingRecord = null; configuring = false; diagnosing = false
    }

    suspend fun reload() {
        watchers = WatcherStore.all(context)
        notificationsAllowed = Notifier.canPost(context)
    }

    // Read the saved selection on entry and after returning from setup, including a pending restart.
    LaunchedEffect(configuring) {
        if (!configuring) {
            firebaseProject = WatcherStore.firebaseConfig(context)?.projectId
            firebaseLoaded = true
        }
    }

    LaunchedEffect(Unit) {
        reload()
        PushRegistration.enqueueRemovals(context)
        if (FirebaseRuntime.ready) watchers.filter { it.isPush }.forEach {
            PushRegistration.enqueue(context, it.id, androidx.work.ExistingWorkPolicy.KEEP)
        }
        WatcherStore.observe(context).collect { watchers = it }
    }

    val open = remember(editingRecord) { editingRecord?.let(::restoreEditor) }
    if (diagnosing) {
        DiagnosticsScreen(onBack = { diagnosing = false })
        return
    }
    if (configuring) {
        FirebaseSetupScreen(onBack = { configuring = false })
        return
    }
    if (open != null) {
        WatcherEditScreen(
            watcher = open,
            watchers = watchers,
            onDone = { scope.launch { reload(); editingRecord = null } },
            onCancel = { editingRecord = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("GLANCE", style = MaterialTheme.typography.headlineSmall, color = GlanceWhite)
        PixelRule()
        FirebaseSetupEntry(firebaseProject, firebaseLoaded) { configuring = true }
        if (FirebaseRuntime.restartRequired) {
            Text("PUSH PAUSED. FINISH RESTART IN FIREBASE SETUP.",
                style = MaterialTheme.typography.bodySmall, color = GlanceGrey)
        }
        Outlined("DIAGNOSTICS") { diagnosing = true }
        Text(
            "BACKEND -> NOTIFICATION -> GONE",
            style = MaterialTheme.typography.labelSmall,
            color = GlanceGrey,
        )

        if (watchers.isEmpty()) {
            Text(
                "NO WATCHERS YET. ADD ONE AND POINT IT AT A BACKEND YOU RUN.",
                style = MaterialTheme.typography.bodySmall,
                color = GlanceGrey,
            )
        }

        watchers.forEach { watcher ->
            WatcherRow(watcher) { editingRecord = editorRecord(watcher) }
        }

        Filled("+ NEW WATCHER") {
            scope.launch { editingRecord = editorRecord(Watcher.blank(WatcherStore.nextId(context))) }
        }

        if (!notificationsAllowed) {
            PixelRule(GlanceWhite)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlanceWhite)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "NOTIFICATIONS OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlanceBlack,
                )
                Text(
                    "Nothing can be shown. Allow them, then add Glance in the " +
                        "Even Realities app under Notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlanceBlack,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlanceBlack,
                            contentColor = GlanceWhite,
                        ),
                        onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    ) {
                        Text("ALLOW", style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("OPEN NOTIFICATION SETTINGS", color = GlanceBlack) }
            }
        }
    }
}

@Composable
private fun FirebaseSetupEntry(projectId: String?, loaded: Boolean, onClick: () -> Unit) {
    val needsSetup = loaded && projectId == null
    Button(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        enabled = loaded,
        shape = RectangleShape,
        border = BorderStroke(1.dp, GlanceWhite),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (needsSetup) GlanceWhite else GlanceBlack,
            contentColor = if (needsSetup) GlanceBlack else GlanceWhite,
            disabledContainerColor = GlanceBlack,
            disabledContentColor = GlanceGrey,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("FIREBASE SETUP", style = MaterialTheme.typography.labelLarge)
            Text(when {
                !loaded -> "LOADING PROJECT..."
                projectId != null -> projectId
                else -> "IMPORT YOUR PROJECT"
            }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WatcherRow(watcher: Watcher, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, if (watcher.lastRunFailed) GlanceWhite else GlanceDimGrey))
            .background(GlanceNearBlack)
            .clickable { onOpen() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            watcher.name.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = GlanceWhite,
        )
        Text(
            watcher.hostLabel() + "  ·  " +
                (if (watcher.isPush) "PUSH" else "EVERY " + watcher.intervalLabel()) +
                "  ·  " + watcher.expiryLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = GlanceGrey,
        )
        if (watcher.isPush) {
            Text(
                when {
                    FirebaseRuntime.restartRequired -> "PUSH PAUSED: RESTART REQUIRED"
                    !FirebaseRuntime.ready -> "FIREBASE SETUP REQUIRED"
                    watcher.pushRegistered -> "REGISTERED FOR PUSH"
                    else -> "PUSH NOT REGISTERED YET"
                },
                style = MaterialTheme.typography.labelSmall,
                color = GlanceGrey,
            )
        }
        if (watcher.lastRunFailed) {
            Text(
                "! LAST RUN FAILED. OPEN AND SAVE AGAIN TO RETRY; SEE NOTIFICATION / DIAGNOSTICS.",
                style = MaterialTheme.typography.labelSmall,
                color = GlanceWhite,
            )
        }
    }
}
