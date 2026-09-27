package dev.liamchu.glance

import android.content.Context

/** Both transports use the same notification lifetime and failure policy. */
object Delivery {
    suspend fun show(context: Context, watcher: Watcher, outcome: Outcome) {
        if (!Notifier.canPost(context)) {
            Diagnostics.event(DiagnosticEvent.NOTIFICATIONS_DISABLED, watcher.id)
            return
        }
        WatcherStore.updateCurrent(context, watcher) { current ->
            when (outcome) {
                is Outcome.Content -> {
                    Notifier.postContent(context, current, outcome.title, outcome.text)
                    Notifier.clearFailure(context, current.id)
                    Diagnostics.event(DiagnosticEvent.CONTENT_POSTED, current.id)
                    current.copy(lastRunFailed = false)
                }
                is Outcome.NothingToSay -> current
                is Outcome.Failed -> {
                    Diagnostics.event(DiagnosticEvent.DELIVERY_FAILED, current.id)
                    if (!current.lastRunFailed) Notifier.postFailure(context, current, outcome.reason)
                    current.copy(lastRunFailed = true)
                }
            }
        }
    }
}
