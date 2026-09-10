package dev.liamchu.glance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * One run: read the settings, make one call, post or do not post.
 *
 * Always returns success, never retry. CONTRACT.md gives the backend one attempt
 * per run and makes the next run the retry, so a WorkManager backoff here would
 * quietly turn one scheduled call into several.
 */
class GlanceWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsStore.read(context)

        // Criterion content-1: nothing configured, nothing contacted.
        if (settings.url.isEmpty()) return Result.success()

        // With notifications switched off there is no channel to report through —
        // including no channel to report *this* through. The settings screen says so.
        if (!Notifier.canPost(context)) return Result.success()

        when (val outcome = Backend.fetch(settings)) {
            is Outcome.Content -> {
                Notifier.postContent(
                    context,
                    outcome.title,
                    outcome.text,
                    settings.expiryMillis,
                )
                if (settings.lastRunFailed) SettingsStore.setLastRunFailed(context, false)
            }

            // Criterion failure-3: nothing to say is not a failure, and does not
            // touch the failure state in either direction.
            is Outcome.NothingToSay -> Unit

            // Criterion failure-1: speak once, then stay quiet until a run succeeds.
            is Outcome.Failed -> if (!settings.lastRunFailed) {
                Notifier.postFailure(context, outcome.reason)
                SettingsStore.setLastRunFailed(context, true)
            }
        }

        return Result.success()
    }
}
