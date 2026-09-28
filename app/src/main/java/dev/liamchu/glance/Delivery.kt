package dev.liamchu.glance

import android.content.Context

enum class FailureSource { DELIVERY, REGISTRATION }

internal fun failureExplanation(watcher: Watcher, reason: String, source: FailureSource): String =
    if (source == FailureSource.REGISTRATION && watcher.pushRegistered) {
        "$reason. Could not refresh push registration. Previously registered pushes may still arrive."
    } else reason

/** Both transports use the same notification lifetime and failure policy. */
object Delivery {
    suspend fun show(context: Context, watcher: Watcher, outcome: Outcome,
        failureSource: FailureSource = FailureSource.DELIVERY) {
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
                    Diagnostics.event(if (failureSource == FailureSource.REGISTRATION)
                        DiagnosticEvent.REGISTRATION_FAILED else DiagnosticEvent.DELIVERY_FAILED, current.id)
                    if (!current.lastRunFailed) Notifier.postFailure(context, current,
                        failureExplanation(current, outcome.reason, failureSource), failureSource)
                    current.copy(lastRunFailed = true)
                }
            }
        }
    }
}
