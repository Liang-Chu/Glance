package dev.liamchu.glance

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * One run of one watcher: read it, make one call, post or do not post.
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

        // Which watcher this run belongs to. A run with no id is a bug, not a
        // default: -1 matches nothing and the run ends without contacting anyone.
        val id = inputData.getInt(Scheduler.KEY_WATCHER_ID, -1)
        val watcher = WatcherStore.byId(context, id) ?: return Result.success()

        // Criterion content-1: nothing configured, nothing contacted.
        if (watcher.url.isEmpty()) return Result.success()

        // With notifications switched off there is no channel to report through —
        // including no channel to report *this* through. The list screen says so.
        if (!Notifier.canPost(context)) return Result.success()

        when (val outcome = Backend.fetch(watcher)) {
            is Outcome.Content -> {
                Notifier.postContent(context, watcher, outcome.title, outcome.text)
                if (watcher.lastRunFailed) {
                    WatcherStore.setLastRunFailed(context, watcher.id, false)
                }
            }

            // Criterion failure-3: nothing to say is not a failure, and does not
            // touch the failure state in either direction.
            is Outcome.NothingToSay -> Unit

            // Criterion failure-1: speak once, then stay quiet until a run succeeds.
            // Per watcher, so one dead backend cannot silence the others.
            is Outcome.Failed -> if (!watcher.lastRunFailed) {
                Notifier.postFailure(context, watcher, outcome.reason)
                WatcherStore.setLastRunFailed(context, watcher.id, true)
            }
        }

        return Result.success()
    }
}
