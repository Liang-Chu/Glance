package dev.liamchu.glance

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * DESIGN.md "The stack": WorkManager owns the interval, because a repeating job
 * survives Doze only when the operating system is the thing scheduling it. It
 * also re-registers itself after a reboot, which is criterion purpose-5.
 *
 * One schedule per watcher, named after its id, so watchers cannot disturb one
 * another and deleting one takes only its own work with it.
 */
object Scheduler {

    const val KEY_WATCHER_ID = "watcher_id"

    private val onlyWhenOnline = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun repeatingName(id: Int) = "glance-watcher-" + id

    private fun immediateName(id: Int) = "glance-now-" + id

    fun schedule(context: Context, watcher: Watcher) {
        val request = PeriodicWorkRequestBuilder<GlanceWorker>(
            watcher.intervalMinutes,
            TimeUnit.MINUTES,
        ).setConstraints(onlyWhenOnline)
            .setInputData(workDataOf(KEY_WATCHER_ID to watcher.id))
            .build()

        // UPDATE so criterion settings-4 holds: a new interval reaches the next run
        // without a reinstall, and without restarting the period from zero.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            repeatingName(watcher.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * One run, immediately. The floor on repeating work is fifteen minutes, which
     * makes "does this work at all" a fifteen-minute question; this makes it a
     * ten-second one. One-shot work has no floor.
     *
     * It does not touch the repeating schedule — that is still the thing that has
     * to be proven, and a check that ran because a button was pressed proves the
     * call and the notification, not the waking up.
     */
    fun checkNow(context: Context, watcher: Watcher) {
        val request = OneTimeWorkRequestBuilder<GlanceWorker>()
            .setConstraints(onlyWhenOnline)
            .setInputData(workDataOf(KEY_WATCHER_ID to watcher.id))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(immediateName(watcher.id), ExistingWorkPolicy.REPLACE, request)
    }

    /** Deleting a watcher must stop it waking up, not merely hide it. */
    fun cancel(context: Context, watcherId: Int) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(repeatingName(watcherId))
        manager.cancelUniqueWork(immediateName(watcherId))
    }
}
