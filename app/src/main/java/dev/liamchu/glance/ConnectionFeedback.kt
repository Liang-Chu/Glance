package dev.liamchu.glance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import java.security.MessageDigest

internal const val REGISTRATION_CONFIRMED = "registration_confirmed"
internal const val REGISTRATION_REASON = "registration_reason"
internal const val REGISTRATION_ADDRESS_HASH = "registration_address_hash"

internal enum class ConnectionPhase { CONNECTING, WAITING, CONNECTED, RETRYING, FAILED, CANCELLED }

internal fun registrationAddressHash(address: String): String =
    MessageDigest.getInstance("SHA-256").digest(address.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

internal fun registrationConfirmed(expected: Watcher, current: Watcher?, reported: Boolean,
    reportedAddressHash: String?): Boolean =
    reported && current != null && current.sameConfiguration(expected) && current.pushRegistered &&
        current.pushAddress.isNotEmpty() && registrationAddressHash(current.pushAddress) == reportedAddressHash

internal fun connectionPhase(state: WorkInfo.State?, confirmed: Boolean, reason: String?, attempts: Int): ConnectionPhase =
    when (state) {
        WorkInfo.State.SUCCEEDED -> if (confirmed) ConnectionPhase.CONNECTED else ConnectionPhase.FAILED
        WorkInfo.State.FAILED -> ConnectionPhase.FAILED
        WorkInfo.State.CANCELLED -> ConnectionPhase.CANCELLED
        WorkInfo.State.RUNNING -> if (reason.isNullOrEmpty()) ConnectionPhase.CONNECTING else ConnectionPhase.RETRYING
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> if (attempts > 0) ConnectionPhase.RETRYING else ConnectionPhase.WAITING
        null -> ConnectionPhase.CONNECTING
    }

/** Follow this save's request, not a previous registration or a different watcher's work. */
@Composable
internal fun ConnectionFeedback(requestId: String, watcher: Watcher, current: Watcher?, onRetry: () -> Unit,
    onClose: () -> Unit, onReview: () -> Unit) {
    val context = LocalContext.current
    val flow = remember(requestId, context) {
        WorkManager.getInstance(context).getWorkInfoByIdFlow(UUID.fromString(requestId))
    }
    val work by flow.collectAsStateWithLifecycle(initialValue = null)
    val info = work
    val reason = info?.outputData?.getString(REGISTRATION_REASON)
        ?: info?.progress?.getString(REGISTRATION_REASON)
    val phase = connectionPhase(info?.state,
        registrationConfirmed(watcher, current, info?.outputData?.getBoolean(REGISTRATION_CONFIRMED, false) == true,
            info?.outputData?.getString(REGISTRATION_ADDRESS_HASH)),
        reason, info?.runAttemptCount ?: 0)
    val pending = phase == ConnectionPhase.CONNECTING || phase == ConnectionPhase.WAITING
    val retryable = phase == ConnectionPhase.RETRYING || phase == ConnectionPhase.FAILED || phase == ConnectionPhase.CANCELLED
    AlertDialog(
        onDismissRequest = if (retryable) onReview else onClose,
        shape = RectangleShape,
        containerColor = GlanceWhite,
        titleContentColor = GlanceBlack,
        textContentColor = GlanceBlack,
        title = { Text(when (phase) {
            ConnectionPhase.CONNECTING -> "CONNECTING..."
            ConnectionPhase.WAITING -> "WAITING TO CONNECT..."
            ConnectionPhase.CONNECTED -> "CONNECTED"
            ConnectionPhase.RETRYING -> "COULD NOT CONNECT YET"
            ConnectionPhase.FAILED -> "CONNECTION NOT COMPLETED"
            ConnectionPhase.CANCELLED -> "CONNECTION INTERRUPTED"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(watcher.name, style = MaterialTheme.typography.titleMedium)
                Text(when (phase) {
                    ConnectionPhase.CONNECTING -> "Registering this watcher with your backend."
                    ConnectionPhase.WAITING -> "Waiting for network access or an earlier registration task."
                    ConnectionPhase.CONNECTED -> "Push registration complete. Send a test from your backend to check delivery."
                    ConnectionPhase.RETRYING -> "${reason.orEmpty()}\nYour watcher is saved. Glance will retry automatically, or you can retry now."
                    ConnectionPhase.FAILED -> reason ?: "Registration was not completed. Review the connection details and retry."
                    ConnectionPhase.CANCELLED -> "Your watcher is saved. Retry or review the connection details."
                })
                if (pending) LinearProgressIndicator(color = GlanceBlack, trackColor = GlanceGrey)
            }
        },
        confirmButton = {
            TextButton(onClick = if (retryable) onRetry else onClose) {
                Text(if (retryable) "RETRY NOW" else if (phase == ConnectionPhase.CONNECTED) "DONE" else "CONTINUE IN BACKGROUND",
                    color = GlanceBlack)
            }
        },
        dismissButton = if (retryable) {
            { TextButton(onClick = onReview) { Text("REVIEW SETTINGS", color = GlanceBlack) } }
        } else null,
    )
}
