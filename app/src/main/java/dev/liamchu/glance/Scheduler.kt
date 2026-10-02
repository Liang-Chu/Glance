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
 * WorkManager owns polling intervals and defers them during Doze. Push watchers
 * register once and receive their content through Firebase Cloud Messaging.
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

    /** Pre-watcher releases left id-less jobs behind; they can only wake up and do nothing. */
    fun cancelObsoleteWork(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork("glance-poll")
        manager.cancelUniqueWork("glance-now")
    }

    fun schedule(context: Context, watcher: Watcher): java.util.UUID? {
        if (watcher.isPush) {
            cancel(context, watcher.id)
            // SAVE AND REGISTER is an explicit retry even when the saved values are unchanged.
            return PushRegistration.enqueue(context, watcher.id, force = true)
        }
        PushRegistration.cancel(context, watcher.id)
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
        return null
    }

    /** Run one manual check without changing the repeating schedule. */
    fun checkNow(context: Context, watcher: Watcher) {
        if (watcher.isPush) return
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
        PushRegistration.cancel(context, watcherId)
    }
}
