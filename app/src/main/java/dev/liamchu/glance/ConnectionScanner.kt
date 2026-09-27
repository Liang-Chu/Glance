package dev.liamchu.glance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/** Live camera only. No photos, image files, raw-result logs or external scanner application. */
@Composable
fun ConnectionScanner(onRead: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun allowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var permissionGranted by remember { mutableStateOf(allowed()) }
    var cameraError by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    LaunchedEffect(Unit) { if (!permissionGranted) permission.launch(Manifest.permission.CAMERA) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = allowed()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // A camera frame contains a backend credential. Exclude it from screenshots and task previews.
    DisposableEffect(activity) {
        val window = activity?.window
        val wasSecure = window != null && window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    BackHandler { onCancel() }
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("SCAN CONNECTION QR", style = MaterialTheme.typography.headlineSmall, color = GlanceWhite)
        Text("On Even-PIlot desktop, open Connect phone · QR. Hold the whole code in view. Recognition stops the camera automatically.",
            style = MaterialTheme.typography.bodySmall, color = GlanceGrey)
        when {
            !permissionGranted -> {
                Text("CAMERA PERMISSION IS REQUIRED TO SCAN. Allow it below, or enable Camera in app settings if Android no longer asks.",
                    color = GlanceWhite)
                Outlined("ALLOW CAMERA / RETRY") { permission.launch(Manifest.permission.CAMERA) }
                Outlined("OPEN APP SETTINGS") {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        ("package:" + context.packageName).toUri()))
                }
            }
            cameraError -> {
                Text("CAMERA UNAVAILABLE. Close other camera apps, check camera access, then retry.", color = GlanceWhite)
                Outlined("RETRY SCAN") { cameraError = false; attempt++ }
            }
            else -> key(attempt) {
                LiveQrPreview(Modifier.weight(1f).fillMaxWidth().border(1.dp, GlanceGrey),
                    onRead = onRead, onError = { cameraError = true })
            }
        }
        Text("No recognition? Enlarge the desktop QR and reduce screen glare. You can cancel and enter the connection manually.",
            style = MaterialTheme.typography.bodySmall, color = GlanceGrey)
        Outlined("CANCEL SCAN") { onCancel() }
    }
}

@Composable
private fun LiveQrPreview(modifier: Modifier, onRead: (String) -> Unit, onError: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentRead by rememberUpdatedState(onRead)
    val currentError by rememberUpdatedState(onError)
    val session = remember { ScanSession() }
    val view = remember { BarcodeView(context).apply {
        keepScreenOn = true
        decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
    } }
    AndroidView(factory = { view }, modifier = modifier)
    DisposableEffect(view, lifecycle) {
        view.addStateListener(object : CameraPreview.StateListener {
            override fun previewSized() = Unit
            override fun previewStarted() = Unit
            override fun previewStopped() = Unit
            override fun cameraClosed() = Unit
            override fun cameraError(error: Exception) {
                if (session.accept()) { view.pause(); currentError() }
            }
        })
        view.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (session.accept()) {
                    view.pause()
                    currentRead(result.text)
                }
            }
        })
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.resume()
                Lifecycle.Event.ON_PAUSE -> view.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.resume()
        onDispose {
            session.cancel()
            lifecycle.removeObserver(observer)
            view.stopDecoding()
            view.pause()
        }
    }
}
