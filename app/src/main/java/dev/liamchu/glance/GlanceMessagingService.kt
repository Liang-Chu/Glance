package dev.liamchu.glance

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking

// FID registration uses onRegistered(), the replacement for onNewToken().
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class GlanceMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (!FirebaseRuntime.ready || message.from != FirebaseRuntime.senderId) {
            Diagnostics.event(DiagnosticEvent.PUSH_IGNORED_PROJECT)
            return
        }
        Diagnostics.event(DiagnosticEvent.PUSH_RECEIVED)
        // FCM invokes this off the main thread. Complete the small local read and
        // post before returning; no network or detached coroutine in this callback.
        runBlocking {
            val watcher = PushMessage.target(message.data, WatcherStore.all(this@GlanceMessagingService))
            if (watcher == null) {
                Diagnostics.event(DiagnosticEvent.PUSH_IGNORED_SUBSCRIPTION)
                return@runBlocking
            }
            Delivery.show(this@GlanceMessagingService, watcher, PushMessage.content(message.data, watcher))
        }
    }

    override fun onRegistered(installationId: String) {
        if (!FirebaseRuntime.ready) return
        // register() also invokes this callback. An already registered address
        // must not enqueue another register() forever.
        runBlocking {
            WatcherStore.all(this@GlanceMessagingService)
                .filter { it.isPush && it.pushAddress != installationId }.forEach {
                // Record the observed address even if the backend is down, so
                // retries of register() cannot keep appending registration work.
                WatcherStore.observePushAddress(this@GlanceMessagingService, it, installationId)
                Diagnostics.event(DiagnosticEvent.PUSH_ADDRESS_CHANGED, it.id)
                PushRegistration.enqueue(this@GlanceMessagingService, it.id, androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
        }
    }

    override fun onDeletedMessages() {
        if (!FirebaseRuntime.ready) return
        Diagnostics.event(DiagnosticEvent.PUSH_MESSAGES_LOST)
        runBlocking {
            WatcherStore.all(this@GlanceMessagingService).filter { it.isPush }.forEach {
                Delivery.show(this@GlanceMessagingService, it,
                    Outcome.Failed("Push messages were lost. Check the source for missed updates."))
            }
        }
    }
}
