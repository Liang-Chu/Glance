package dev.liamchu.glance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** SDK 37 gates direct LAN access. Public internet endpoints still work without this permission. */
@Composable
fun LocalNetworkAccess(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < 37) return
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun allowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(allowed()) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = allowed()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (!granted) {
        Text("BACKEND ON YOUR LOCAL NETWORK? Android requires local network access. Allow it before registering a LAN connection. Public internet URLs can work without it.",
            style = MaterialTheme.typography.bodySmall, color = GlanceGrey)
        Outlined("ALLOW LOCAL NETWORK / RETRY", enabled = enabled) { request.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) }
        Outlined("OPEN NETWORK PERMISSION SETTINGS", enabled = enabled) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                ("package:" + context.packageName).toUri()))
        }
    }
}
