package dev.liamchu.glance

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var editing by remember { mutableStateOf<Watcher?>(null) }
    var notificationsAllowed by remember { mutableStateOf(true) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = Notifier.canPost(context) }

    suspend fun reload() {
        watchers = WatcherStore.all(context)
        notificationsAllowed = Notifier.canPost(context)
    }

    LaunchedEffect(Unit) { reload() }

    val open = editing
    if (open != null) {
        WatcherEditScreen(
            watcher = open,
            onDone = { scope.launch { reload(); editing = null } },
            onCancel = { editing = null },
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
            WatcherRow(watcher) { editing = watcher }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = GlanceWhite,
                contentColor = GlanceBlack,
            ),
            onClick = {
                scope.launch { editing = Watcher.blank(WatcherStore.nextId(context)) }
            },
        ) {
            Text("+ NEW WATCHER", style = MaterialTheme.typography.labelLarge)
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
            }
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
            watcher.hostLabel() + "  ·  EVERY " + watcher.intervalLabel() +
                "  ·  " + watcher.expiryLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = GlanceGrey,
        )
        if (watcher.lastRunFailed) {
            Text(
                "! LAST RUN FAILED",
                style = MaterialTheme.typography.labelSmall,
                color = GlanceWhite,
            )
        }
    }
}
