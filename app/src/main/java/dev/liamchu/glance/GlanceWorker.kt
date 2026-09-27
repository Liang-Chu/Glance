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

    override suspend fun doWork(): Result = Diagnostics.worker(DiagnosticEvent.POLL_WORK,
        inputData.getInt(Scheduler.KEY_WATCHER_ID, -1), runAttemptCount) { run() }

    private suspend fun run(): Result {
        val context = applicationContext

        // Which watcher this run belongs to. A run with no id is a bug, not a
        // default: -1 matches nothing and the run ends without contacting anyone.
        val id = inputData.getInt(Scheduler.KEY_WATCHER_ID, -1)
        val watcher = WatcherStore.byId(context, id) ?: return Result.success()
        if (watcher.isPush) return Result.success()

        // Criterion content-1: nothing configured, nothing contacted.
        if (watcher.url.isEmpty()) return Result.success()

        // With notifications switched off there is no channel to report through —
        // including no channel to report *this* through. The list screen says so.
        if (!Notifier.canPost(context)) {
            Diagnostics.event(DiagnosticEvent.NOTIFICATIONS_DISABLED, watcher.id)
            return Result.success()
        }

        // Re-read after the network call: an edit or deletion revokes this run.
        val outcome = Backend.fetch(watcher)
        val current = WatcherStore.byId(context, id) ?: return Result.success()
        if (!current.sameConfiguration(watcher)) return Result.success()
        Delivery.show(context, watcher, outcome)
        return Result.success()
    }
}
